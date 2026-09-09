import { adminClient, json, user } from "../_shared/http.ts";

Deno.serve(async req => {
  if (req.method !== "POST") return json({ error: "method_not_allowed" }, 405);
  const authUser = await user(req);
  if (!authUser) return json({ error: "unauthorized" }, 401);
  let body: { team_id?: string; campaign_id?: string };
  try { body = await req.json(); } catch { return json({ error: "invalid_json" }, 400); }
  if (!body.team_id || !body.campaign_id) return json({ error: "team_id_and_campaign_id_required" }, 400);
  const admin = adminClient();
  const { data: leader } = await admin.from("team_members").select("role").eq("team_id", body.team_id).eq("user_id", authUser.id).maybeSingle();
  if (leader?.role !== "LIDER") return json({ error: "leader_required" }, 403);
  const { error: unpublishError } = await admin.from("campaigns").update({ published: false }).eq("team_id", body.team_id);
  if (unpublishError) return json({ error: "campaign_update_failed", message: unpublishError.message }, 400);
  const { data: campaign, error } = await admin.from("campaigns").update({ published: true }).eq("id", body.campaign_id).eq("team_id", body.team_id).select().single();
  return error ? json({ error: "campaign_not_found", message: error.message }, 404) : json({ campaign });
});
