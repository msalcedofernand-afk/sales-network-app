import { adminClient, client, json, user } from "../_shared/http.ts";

Deno.serve(async req => {
  if (req.method !== "POST") return json({ error: "method_not_allowed" }, 405);
  const authenticatedUser = await user(req);
  if (!authenticatedUser) return json({ error: "unauthorized" }, 401);
  let body: { team_id?: string; expires_at?: string; max_uses?: number };
  try { body = await req.json(); } catch { return json({ error: "invalid_json" }, 400); }
  if (!body.team_id || !body.expires_at) return json({ error: "team_id_and_expires_at_required" }, 400);
  if (!Number.isInteger(body.max_uses ?? 1) || (body.max_uses ?? 1) < 1 || (body.max_uses ?? 1) > 50) {
    return json({ error: "invalid_max_uses" }, 400);
  }
  const { data: allowed, error: rateError } = await adminClient().rpc("consume_edge_rate_limit", {
    input_user_id: authenticatedUser.id, input_action: "create-invitation", input_limit: 20, input_window_seconds: 3600,
  });
  if (rateError) return json({ error: "rate_limit_unavailable" }, 503);
  if (!allowed) return json({ error: "rate_limited" }, 429);
  const { data, error } = await client(req).rpc("create_invitation_secure", {
    input_team_id: body.team_id,
    input_expires_at: body.expires_at,
    input_max_uses: body.max_uses ?? 1,
  });
  return error ? json({ error: "invitation_not_created", message: error.message }, 400) : json({ invitation: data }, 201);
});
