import { adminClient, json } from "../_shared/http.ts";

Deno.serve(async req => {
  if (req.method !== "POST") return json({ error: "method_not_allowed" }, 405);

  let body: {
    user_id?: string;
    app_version?: string;
    version_code?: number;
    build_type?: string;
    stacktrace?: string;
    device_info?: Record<string, unknown>;
  };

  try {
    body = await req.json();
  } catch {
    return json({ error: "invalid_json" }, 400);
  }

  if (!body.stacktrace || !body.app_version || !body.version_code) {
    return json({ error: "missing_required_fields" }, 400);
  }

  const sb = adminClient();

  const { error } = await sb.from("crash_reports").insert({
    user_id: body.user_id || null,
    app_version: body.app_version,
    version_code: body.version_code,
    build_type: body.build_type || "debug",
    stacktrace: body.stacktrace,
    device_info: body.device_info || {}
  });

  if (error) {
    return json({ error: error.message }, 500);
  }

  return json({ success: true }, 201);
});
