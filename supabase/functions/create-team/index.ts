import { client, json, user } from "../_shared/http.ts";

Deno.serve(async req => {
  if (req.method !== "POST") return json({ error: "method_not_allowed" }, 405);
  if (!(await user(req))) return json({ error: "unauthorized" }, 401);
  let body: { name?: string };
  try { body = await req.json(); } catch { return json({ error: "invalid_json" }, 400); }
  if (!body.name?.trim()) return json({ error: "name_required" }, 400);

  const { data, error } = await client(req).rpc("create_team_with_leader", { team_name: body.name.trim() });
  return error ? json({ error: "team_create_failed", message: error.message }, 400) : json({ team: data }, 201);
});

