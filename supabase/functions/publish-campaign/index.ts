import { preflight, adminClient, json, user } from "../_shared/http.ts";

Deno.serve(async req => {
  const preflightResponse = preflight(req);
  if (preflightResponse) return preflightResponse;
  if (req.method !== "POST") return json(req, { error: "method_not_allowed" }, 405);
  const authUser = await user(req);
  if (!authUser) return json(req, { error: "unauthorized" }, 401);
  let body: { team_id?: string; campaign_id?: string };
  try { body = await req.json(); } catch { return json(req, { error: "invalid_json" }, 400); }
  if (!body.team_id || !body.campaign_id) return json(req, { error: "team_id_and_campaign_id_required" }, 400);
  const admin = adminClient();
  const { data: leader } = await admin.from("team_members").select("role").eq("team_id", body.team_id).eq("user_id", authUser.id).maybeSingle();
  if (!leader || !["LIDER", "ROOT_ADMIN"].includes(leader.role)) return json(req, { error: "leader_required" }, 403);
  const { error: unpublishError } = await admin.from("campaigns").update({ published: false }).eq("team_id", body.team_id);
  if (unpublishError) return json(req, { error: "campaign_update_failed" }, 400);
  const { data: campaign, error } = await admin.from("campaigns").update({ published: true }).eq("id", body.campaign_id).eq("team_id", body.team_id).select().single();
  return error ? json(req, { error: "campaign_not_found" }, 404) : json(req, { campaign });
});
