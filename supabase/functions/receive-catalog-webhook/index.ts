import { json } from "../_shared/http.ts";
Deno.serve(async req => req.method === "POST" ? json({ error: "webhook_secret_not_configured" }, 501) : json({ error: "method_not_allowed" }, 405));

