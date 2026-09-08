import { json, user } from "../_shared/http.ts";
Deno.serve(async req => (await user(req)) ? json({ error: "catalog_source_not_configured", message: "Configure an authorized feed before importing." }, 501) : json({ error: "unauthorized" }, 401));

