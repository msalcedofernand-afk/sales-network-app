import { adminClient, json } from "../_shared/http.ts";

Deno.serve(async req => {
  if (req.method !== "POST") return json({ error: "method_not_allowed" }, 405);

  const authHeader = req.headers.get("Authorization");
  const cronSecret = Deno.env.get("CRON_SECRET");
  if (!cronSecret || authHeader !== `Bearer ${cronSecret}`) {
    return json({ error: "unauthorized" }, 401);
  }

  const sb = adminClient();

  const { data: expired, error: fetchError } = await sb
    .from("invitations")
    .update({ status: "EXPIRED" })
    .lt("expires_at", new Date().toISOString())
    .in("status", ["ACTIVE"])
    .select("id");

  if (fetchError) {
    return json({ error: fetchError.message }, 500);
  }

  return json({
    success: true,
    purged: expired?.length ?? 0,
    timestamp: new Date().toISOString()
  });
});
