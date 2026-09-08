import { json, user } from "../_shared/http.ts";
Deno.serve(async req => (await user(req)) ? json({ error: "messaging_provider_not_configured" }, 501) : json({ error: "unauthorized" }, 401));

