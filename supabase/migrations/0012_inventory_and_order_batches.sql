-- Inventory is optional for legacy products: NULL means stock is managed externally.
alter table public.products add column if not exists stock_quantity integer;
alter table public.products drop constraint if exists products_stock_quantity_check;
alter table public.products add constraint products_stock_quantity_check check (stock_quantity is null or stock_quantity >= 0);
create index if not exists products_team_stock_idx on public.products (team_id, available, stock_quantity);

create table if not exists public.order_batches (
  id uuid primary key default gen_random_uuid(),
  team_id uuid not null references public.teams(id) on delete cascade,
  user_id uuid not null references auth.users(id) on delete cascade,
  idempotency_key text not null,
  created_at timestamptz not null default now(),
  unique (user_id, idempotency_key)
);
alter table public.order_batches enable row level security;
drop policy if exists order_batches_owner_read on public.order_batches;
create policy order_batches_owner_read on public.order_batches for select using (user_id = auth.uid() or public.is_team_leader(team_id));
alter table public.orders add column if not exists batch_id uuid references public.order_batches(id) on delete set null;
create index if not exists orders_batch_id_idx on public.orders(batch_id);

-- Lock each product row before checking and decrementing stock. This prevents two
-- concurrent checkouts from selling the same last unit.
create or replace function public.checkout_active_cart(
  input_cart_id uuid,
  input_customer_id uuid,
  input_idempotency_key text
) returns public.orders
language plpgsql security definer set search_path = public
as $$
declare
  active_cart public.carts;
  selected_customer public.customers;
  created_order public.orders;
  line record;
  current_product public.products;
  calculated_total integer := 0;
begin
  if auth.uid() is null then raise exception 'unauthorized'; end if;
  if length(trim(coalesce(input_idempotency_key, ''))) not between 1 and 120 then raise exception 'invalid_idempotency_key'; end if;
  perform pg_advisory_xact_lock(hashtext(auth.uid()::text || ':' || input_idempotency_key));
  select * into created_order from public.orders where user_id = auth.uid() and idempotency_key = input_idempotency_key;
  if found then return created_order; end if;
  select * into active_cart from public.carts where id = input_cart_id and user_id = auth.uid() and status = 'ACTIVE' for update;
  if not found then raise exception 'active_cart_not_found'; end if;
  select * into selected_customer from public.customers where id = input_customer_id and team_id = active_cart.team_id and archived = false;
  if not found or (selected_customer.owner_user_id <> auth.uid() and not public.is_team_leader(active_cart.team_id)) then raise exception 'customer_not_allowed'; end if;

  for line in select product_id, quantity from public.cart_items where cart_id = active_cart.id loop
    select * into current_product from public.products where id = line.product_id and team_id = active_cart.team_id for update;
    if not found or not current_product.available then raise exception 'cart_empty_or_product_unavailable'; end if;
    if current_product.stock_quantity is not null and current_product.stock_quantity < line.quantity then raise exception 'product_out_of_stock'; end if;
    calculated_total := calculated_total + line.quantity * current_product.price_cents;
  end loop;
  if calculated_total = 0 then raise exception 'cart_empty_or_product_unavailable'; end if;

  insert into public.orders (team_id, user_id, customer_id, total_cents, commission_cents, idempotency_key)
  values (active_cart.team_id, auth.uid(), selected_customer.id, calculated_total, 0, input_idempotency_key) returning * into created_order;
  insert into public.order_items (order_id, product_id, sku, product_name, quantity, unit_price_cents)
  select created_order.id, ci.product_id, p.sku, p.name, ci.quantity, p.price_cents from public.cart_items ci join public.products p on p.id = ci.product_id where ci.cart_id = active_cart.id;
  for line in select product_id, quantity from public.cart_items where cart_id = active_cart.id loop
    update public.products set stock_quantity = case when stock_quantity is null then null else stock_quantity - line.quantity end,
      available = case when stock_quantity is null then available else stock_quantity - line.quantity > 0 end
      where id = line.product_id;
  end loop;
  update public.carts set status = 'CONVERTED' where id = active_cart.id;
  return created_order;
end;
$$;
revoke all on function public.checkout_active_cart(uuid, uuid, text) from public, anon;
grant execute on function public.checkout_active_cart(uuid, uuid, text) to authenticated;
