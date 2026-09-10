import { client, json, user } from "../_shared/http.ts";

Deno.serve(async req => {
  if (req.method !== "POST") return json({ error: "method_not_allowed" }, 405);
  if (!(await user(req))) return json({ error: "unauthorized" }, 401);
  let body: { code?: string };
  try { body = await req.json(); } catch { return json({ error: "invalid_json" }, 400); }
  if (!body.code?.trim()) return json({ error: "code_required" }, 400);

  const { data, error } = await client(req).rpc("accept_invitation_code", {
    invitation_code: body.code.trim(),
  });
  return error
    ? json({ error: "invitation_invalid_or_expired", message: error.message }, 400)
    : json({ team_id: data });
});

