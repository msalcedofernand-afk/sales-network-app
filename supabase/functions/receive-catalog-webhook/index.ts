import { json, preflight } from "../_shared/http.ts";

Deno.serve(async req => {
  const preflightResponse = preflight(req);
  if (preflightResponse) return preflightResponse;
  return req.method === "POST"
    ? json(req, { error: "webhook_secret_not_configured" }, 501)
    : json(req, { error: "method_not_allowed" }, 405);
});
