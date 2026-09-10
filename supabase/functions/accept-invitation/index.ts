import { adminClient, client, json, user } from "../_shared/http.ts";

Deno.serve(async req => {
  if (req.method !== "POST") return json({ error: "method_not_allowed" }, 405);
  const authenticatedUser = await user(req);
  if (!authenticatedUser) return json({ error: "unauthorized" }, 401);
  let body: { code?: string };
  try { body = await req.json(); } catch { return json({ error: "invalid_json" }, 400); }
  const code = body.code?.trim().toUpperCase();
  if (!code || !/^[A-Z0-9]{8,32}$/.test(code)) return json({ error: "invalid_code" }, 400);
  const { data: allowed, error: rateError } = await adminClient().rpc("consume_edge_rate_limit", {
    input_user_id: authenticatedUser.id, input_action: "accept-invitation", input_limit: 20, input_window_seconds: 3600,
  });
  if (rateError) return json({ error: "rate_limit_unavailable" }, 503);
  if (!allowed) return json({ error: "rate_limited" }, 429);

  const { data, error } = await client(req).rpc("accept_invitation_code", {
    invitation_code: code,
  });
  return error
    ? json({ error: "invitation_invalid_or_expired", message: error.message }, 400)
    : json({ team_id: data });
});

