import { json, user } from "../_shared/http.ts";
Deno.serve(async req => (await user(req)) ? json({ error: "implementation_pending" }, 501) : json({ error: "unauthorized" }, 401));

