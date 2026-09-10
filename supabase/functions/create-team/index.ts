import { preflight, client, json, user } from "../_shared/http.ts";

Deno.serve(async req => {
  const preflightResponse = preflight(req);
  if (preflightResponse) return preflightResponse;
  if (req.method !== "POST") return json(req, { error: "method_not_allowed" }, 405);
  if (!(await user(req))) return json(req, { error: "unauthorized" }, 401);
  let body: { name?: string };
  try { body = await req.json(); } catch { return json(req, { error: "invalid_json" }, 400); }
  if (!body.name?.trim()) return json(req, { error: "name_required" }, 400);

  const { data, error } = await client(req).rpc("create_team_with_leader", { team_name: body.name.trim() });
  return error ? json(req, { error: "team_create_failed" }, 400) : json(req, { team: data }, 201);
});

