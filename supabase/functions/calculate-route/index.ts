import { json, user } from "../_shared/http.ts";
Deno.serve(async req => { if (!(await user(req))) return json({ error: "unauthorized" }, 401); const { origin, destination } = await req.json(); if (!origin || !destination) return json({ error: "origin_and_destination_required" }, 400); return json({ error: "routing_provider_not_configured" }, 501); });

