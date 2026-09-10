import { client, json, preflight, user } from "../_shared/http.ts";

Deno.serve(async req => {
  const preflightResponse = preflight(req);
  if (preflightResponse) return preflightResponse;
  if (req.method !== "POST") return json(req, { error: "method_not_allowed" }, 405);
  const authUser = await user(req);
  if (!authUser) return json(req, { error: "unauthorized" }, 401);
  let body: { team_id?: string; campaign_id?: string };
  try { body = await req.json(); } catch { return json(req, { error: "invalid_json" }, 400); }
  if (!body.team_id || !body.campaign_id) return json(req, { error: "team_id_and_campaign_id_required" }, 400);
  const { data: campaign, error } = await client(req).rpc("publish_campaign_atomic", {
    input_team_id: body.team_id,
    input_campaign_id: body.campaign_id,
  });
  return error ? json(req, { error: "campaign_not_found" }, 404) : json(req, { campaign });
});
