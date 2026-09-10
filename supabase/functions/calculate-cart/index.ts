import { client, json, preflight, user } from "../_shared/http.ts";

Deno.serve(async req => {
  const preflightResponse = preflight(req);
  if (preflightResponse) return preflightResponse;
  if (req.method !== "POST") return json(req, { error: "method_not_allowed" }, 405);

  const authenticatedUser = await user(req);
  if (!authenticatedUser) return json(req, { error: "unauthorized" }, 401);

  let body: { cart_id?: string };
  try {
    body = await req.json();
  } catch {
    return json(req, { error: "invalid_json" }, 400);
  }
  if (!body.cart_id) return json(req, { error: "cart_id_required" }, 400);

  const supabase = client(req);
  const { data: cart } = await supabase
    .from("carts")
    .select("id,team_id,status")
    .eq("id", body.cart_id)
    .eq("user_id", authenticatedUser.id)
    .maybeSingle();
  if (!cart) return json(req, { error: "cart_not_found" }, 404);

  const { data: items, error } = await supabase
    .from("cart_items")
    .select("quantity,products(id,sku,name,price_cents,currency,available,stock_quantity)")
    .eq("cart_id", body.cart_id);
  if (error) return json(req, { error: "cart_items_unavailable" }, 400);

  const lines = (items ?? []).map((row: any) => ({
    ...row,
    line_total_cents: row.quantity * row.products.price_cents,
    can_checkout: row.products.available &&
      (row.products.stock_quantity === null || row.products.stock_quantity >= row.quantity),
  }));
  return json(req, {
    cart_id: body.cart_id,
    currency: lines[0]?.products?.currency ?? "PEN",
    items: lines,
    can_checkout: lines.length > 0 && lines.every((row: any) => row.can_checkout),
    total_cents: lines.reduce((sum: number, row: any) => sum + row.line_total_cents, 0),
  });
});
