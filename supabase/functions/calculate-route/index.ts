import { json, preflight, user } from "../_shared/http.ts";

Deno.serve(async req => {
  const preflightResponse = preflight(req);
  if (preflightResponse) return preflightResponse;
  if (req.method !== "POST") return json(req, { error: "method_not_allowed" }, 405);
  if (!(await user(req))) return json(req, { error: "unauthorized" }, 401);

  let body: { origin?: unknown; destination?: unknown };
  try {
    body = await req.json();
  } catch {
    return json(req, { error: "invalid_json" }, 400);
  }
  if (!body.origin || !body.destination) {
    return json(req, { error: "origin_and_destination_required" }, 400);
  }
  return json(req, { error: "routing_provider_not_configured" }, 501);
});
