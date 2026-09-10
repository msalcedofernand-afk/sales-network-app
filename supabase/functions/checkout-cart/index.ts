import { preflight, adminClient, client, json, user } from "../_shared/http.ts";

Deno.serve(async req => {
  const preflightResponse = preflight(req);
  if (preflightResponse) return preflightResponse;
  if (req.method !== "POST") return json(req, { error: "method_not_allowed" }, 405);
  const authenticatedUser = await user(req);
  if (!authenticatedUser) return json(req, { error: "unauthorized" }, 401);

  let body: { cart_id?: string; customer_id?: string; idempotency_key?: string };
  try { body = await req.json(); } catch { return json(req, { error: "invalid_json" }, 400); }
  if (!body.cart_id || !body.customer_id || !body.idempotency_key) {
    return json(req, { error: "cart_customer_and_idempotency_key_required" }, 400);
  }
  if (typeof body.idempotency_key !== "string" || body.idempotency_key.length > 120) {
    return json(req, { error: "invalid_idempotency_key" }, 400);
  }
  const { data: allowed, error: rateError } = await adminClient().rpc("consume_edge_rate_limit", {
    input_user_id: authenticatedUser.id, input_action: "checkout-cart", input_limit: 20, input_window_seconds: 3600,
  });
  if (rateError) return json(req, { error: "rate_limit_unavailable" }, 503);
  if (!allowed) return json(req, { error: "rate_limited" }, 429);

  const { data, error } = await client(req).rpc("checkout_active_cart", {
    input_cart_id: body.cart_id,
    input_customer_id: body.customer_id,
    input_idempotency_key: body.idempotency_key,
  });
  if (error) return json(req, { error: "checkout_failed" }, 400);
  return json(req, { order: data }, 201);
});

