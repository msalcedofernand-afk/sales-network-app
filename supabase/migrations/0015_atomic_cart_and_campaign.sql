create or replace function public.get_or_create_active_cart(input_team_id uuid)
returns public.carts
language plpgsql
security definer
set search_path = ''
as $$
declare
  active_cart public.carts;
begin
  if auth.uid() is null then raise exception 'unauthorized'; end if;
  if not public.is_team_member(input_team_id) then raise exception 'team_membership_required'; end if;

  perform pg_catalog.pg_advisory_xact_lock(
    pg_catalog.hashtext((select auth.uid())::text || ':' || input_team_id::text || ':cart')
  );
  select * into active_cart
  from public.carts c
  where c.user_id = (select auth.uid())
    and c.team_id = input_team_id
    and c.status = 'ACTIVE'
  for update;
  if found then return active_cart; end if;

  insert into public.carts (team_id, user_id)
  values (input_team_id, (select auth.uid()))
  returning * into active_cart;
  return active_cart;
end;
$$;

create or replace function public.increment_cart_item(
  input_cart_id uuid,
  input_product_id uuid,
  input_delta integer default 1
) returns integer
language plpgsql
security definer
set search_path = ''
as $$
declare
  active_cart public.carts;
  selected_product public.products;
  current_quantity integer := 0;
  next_quantity integer;
begin
  if auth.uid() is null then raise exception 'unauthorized'; end if;
  if input_delta = 0 or input_delta not between -999 and 999 then raise exception 'invalid_quantity'; end if;

  select * into active_cart
  from public.carts c
  where c.id = input_cart_id
    and c.user_id = (select auth.uid())
    and c.status = 'ACTIVE'
  for update;
  if not found then raise exception 'active_cart_not_found'; end if;

  select * into selected_product
  from public.products p
  where p.id = input_product_id and p.team_id = active_cart.team_id
  for update;
  if not found or not selected_product.available then raise exception 'product_unavailable'; end if;

  select ci.quantity into current_quantity
  from public.cart_items ci
  where ci.cart_id = active_cart.id and ci.product_id = selected_product.id
  for update;
  current_quantity := coalesce(current_quantity, 0);
  next_quantity := current_quantity + input_delta;

  if next_quantity <= 0 then
    delete from public.cart_items ci
    where ci.cart_id = active_cart.id and ci.product_id = selected_product.id;
    return 0;
  end if;
  if next_quantity > 999 then raise exception 'invalid_quantity'; end if;
  if selected_product.stock_quantity is not null and next_quantity > selected_product.stock_quantity then
    raise exception 'product_out_of_stock';
  end if;

  insert into public.cart_items (cart_id, product_id, quantity)
  values (active_cart.id, selected_product.id, next_quantity)
  on conflict (cart_id, product_id)
  do update set quantity = excluded.quantity;
  return next_quantity;
end;
$$;

create or replace function public.replace_cart_items(
  input_cart_id uuid,
  input_items jsonb
) returns integer
language plpgsql
security definer
set search_path = ''
as $$
declare
  active_cart public.carts;
  item_count integer;
begin
  if auth.uid() is null then raise exception 'unauthorized'; end if;
  if jsonb_typeof(input_items) <> 'array' or jsonb_array_length(input_items) > 200 then
    raise exception 'invalid_cart_items';
  end if;

  select * into active_cart
  from public.carts c
  where c.id = input_cart_id
    and c.user_id = (select auth.uid())
    and c.status = 'ACTIVE'
  for update;
  if not found then raise exception 'active_cart_not_found'; end if;

  select count(*) into item_count
  from jsonb_to_recordset(input_items) as item(product_id uuid, quantity integer)
  where item.product_id is not null and item.quantity between 1 and 999;
  if item_count <> jsonb_array_length(input_items) then raise exception 'invalid_cart_items'; end if;

  if exists (
    select item.product_id
    from jsonb_to_recordset(input_items) as item(product_id uuid, quantity integer)
    group by item.product_id having count(*) > 1
  ) then raise exception 'duplicate_cart_product'; end if;

  perform 1
  from public.products p
  join jsonb_to_recordset(input_items) as item(product_id uuid, quantity integer)
    on item.product_id = p.id
  order by p.id
  for update of p;

  if exists (
    select 1
    from jsonb_to_recordset(input_items) as item(product_id uuid, quantity integer)
    left join public.products p on p.id = item.product_id and p.team_id = active_cart.team_id
    where p.id is null or not p.available
      or (p.stock_quantity is not null and item.quantity > p.stock_quantity)
  ) then raise exception 'product_unavailable_or_out_of_stock'; end if;

  delete from public.cart_items ci where ci.cart_id = active_cart.id;
  insert into public.cart_items (cart_id, product_id, quantity)
  select active_cart.id, item.product_id, item.quantity
  from jsonb_to_recordset(input_items) as item(product_id uuid, quantity integer);
  return item_count;
end;
$$;

create or replace function public.publish_campaign_atomic(
  input_team_id uuid,
  input_campaign_id uuid
) returns public.campaigns
language plpgsql
security definer
set search_path = ''
as $$
declare
  target public.campaigns;
begin
  if auth.uid() is null then raise exception 'unauthorized'; end if;
  if not public.is_team_leader(input_team_id) then raise exception 'leader_required'; end if;

  perform 1 from public.campaigns c
  where c.team_id = input_team_id order by c.id for update;
  select * into target from public.campaigns c
  where c.id = input_campaign_id and c.team_id = input_team_id;
  if not found then raise exception 'campaign_not_found'; end if;

  update public.campaigns c set published = (c.id = input_campaign_id)
  where c.team_id = input_team_id;
  select * into target from public.campaigns c where c.id = input_campaign_id;
  return target;
end;
$$;

revoke all on function public.get_or_create_active_cart(uuid) from public, anon;
revoke all on function public.increment_cart_item(uuid, uuid, integer) from public, anon;
revoke all on function public.replace_cart_items(uuid, jsonb) from public, anon;
revoke all on function public.publish_campaign_atomic(uuid, uuid) from public, anon;
grant execute on function public.get_or_create_active_cart(uuid) to authenticated;
grant execute on function public.increment_cart_item(uuid, uuid, integer) to authenticated;
grant execute on function public.replace_cart_items(uuid, jsonb) to authenticated;
grant execute on function public.publish_campaign_atomic(uuid, uuid) to authenticated;
