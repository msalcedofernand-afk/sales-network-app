import type { SupabaseClient } from "@supabase/supabase-js";

export async function getOrCreateActiveCart(supabase: SupabaseClient, teamId: string) {
  const { data, error } = await supabase.rpc("get_or_create_active_cart", {
    input_team_id: teamId,
  });
  if (error) throw error;
  if (!data?.id) throw new Error("No pudimos preparar el carrito.");
  return data as { id: string; team_id: string; user_id: string };
}

export async function incrementCartItem(
  supabase: SupabaseClient,
  cartId: string,
  productId: string,
  delta: number,
) {
  const { data, error } = await supabase.rpc("increment_cart_item", {
    input_cart_id: cartId,
    input_product_id: productId,
    input_delta: delta,
  });
  if (error) throw error;
  return Number(data ?? 0);
}
