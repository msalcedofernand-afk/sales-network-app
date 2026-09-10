import { client, json, user } from "../_shared/http.ts";

Deno.serve(async req => {
  if (req.method !== "POST") return json({ error: "method_not_allowed" }, 405);
  if (!(await user(req))) return json({ error: "unauthorized" }, 401);

  let body: { cart_id?: string; customer_id?: string; idempotency_key?: string };
  try { body = await req.json(); } catch { return json({ error: "invalid_json" }, 400); }
  if (!body.cart_id || !body.customer_id || !body.idempotency_key) {
    return json({ error: "cart_customer_and_idempotency_key_required" }, 400);
  }

  const { data, error } = await client(req).rpc("checkout_active_cart", {
    input_cart_id: body.cart_id,
    input_customer_id: body.customer_id,
    input_idempotency_key: body.idempotency_key,
  });
  if (error) return json({ error: "checkout_failed", message: error.message }, 400);
  return json({ order: data }, 201);
});

