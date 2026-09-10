import { adminClient, json, user } from "../_shared/http.ts";

const MAX_STACKTRACE_BYTES = 32 * 1024;
const allowedDeviceFields = new Set(["model", "os_version", "sdk"]);

function cleanText(value: unknown, max: number): string {
  if (typeof value !== "string") return "";
  return value
    .replace(/Bearer\s+[A-Za-z0-9._-]+/gi, "Bearer [redacted]")
    .replace(/[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}/gi, "[email redacted]")
    .slice(0, max);
}

Deno.serve(async req => {
  if (req.method !== "POST") return json({ error: "method_not_allowed" }, 405);
  const authenticatedUser = await user(req);
  if (!authenticatedUser) return json({ error: "unauthorized" }, 401);

  let body: Record<string, unknown>;
  try { body = await req.json(); } catch { return json({ error: "invalid_json" }, 400); }
  const stacktrace = cleanText(body.stacktrace, MAX_STACKTRACE_BYTES);
  const stackBytes = new TextEncoder().encode(stacktrace).byteLength;
  if (!stacktrace || stackBytes > MAX_STACKTRACE_BYTES) return json({ error: "invalid_stacktrace" }, 413);

  const admin = adminClient();
  const { data: allowed, error: limitError } = await admin.rpc("consume_edge_rate_limit", {
    input_user_id: authenticatedUser.id,
    input_action: "report-crash",
    input_limit: 10,
    input_window_seconds: 3600,
  });
  if (limitError) return json({ error: "rate_limit_unavailable" }, 503);
  if (!allowed) return json({ error: "rate_limited" }, 429);

  const { data: membership } = await admin.from("team_members")
    .select("team_id").eq("user_id", authenticatedUser.id).limit(1).maybeSingle();
  if (!membership) return json({ error: "team_membership_required" }, 403);

  const rawDevice = body.device_info && typeof body.device_info === "object" ? body.device_info as Record<string, unknown> : {};
  const deviceInfo = Object.fromEntries(Object.entries(rawDevice)
    .filter(([key, value]) => allowedDeviceFields.has(key) && ["string", "number"].includes(typeof value)));

  const record = {
    user_id: authenticatedUser.id,
    team_id: membership.team_id,
    app_version: cleanText(body.app_version, 40),
    version_code: Number(body.version_code),
    build_type: cleanText(body.build_type, 20) || "release",
    channel: cleanText(body.channel, 20) || "stable",
    platform: cleanText(body.platform, 20) || "android",
    exception_type: cleanText(body.exception_type, 160) || "unknown",
    stacktrace,
    device_info: deviceInfo,
  };
  if (!record.app_version || !Number.isInteger(record.version_code) || record.version_code < 1) {
    return json({ error: "invalid_version" }, 400);
  }
  const { error } = await admin.from("crash_reports").insert(record);
  return error ? json({ error: "crash_not_stored" }, 500) : json({ success: true }, 201);
});
