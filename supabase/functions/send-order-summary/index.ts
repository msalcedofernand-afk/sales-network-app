import { json, preflight, user } from "../_shared/http.ts";

Deno.serve(async req => {
  const preflightResponse = preflight(req);
  if (preflightResponse) return preflightResponse;
  return (await user(req))
    ? json(req, { error: "messaging_provider_not_configured" }, 501)
    : json(req, { error: "unauthorized" }, 401);
});
