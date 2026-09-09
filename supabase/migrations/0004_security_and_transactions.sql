-- Production hardening: keep order creation and membership changes atomic.
-- All SECURITY DEFINER functions set an explicit search_path and authorize with auth.uid().

alter table public.products
  add constraint products_team_slug_unique unique (team_id, slug);

create index if not exists team_members_user_team_idx on public.team_members (user_id, team_id);
create index if not exists products_team_campaign_available_idx on public.products (team_id, campaign_id, available);
create index if not exists customers_team_owner_archived_idx on public.customers (team_id, owner_user_id, archived);
create index if not exists orders_team_user_created_idx on public.orders (team_id, user_id, created_at desc);
create index if not exists cart_items_cart_idx on public.cart_items (cart_id);
create index if not exists product_images_product_sort_idx on public.product_images (product_id, sort_order);

create policy profiles_team_read on public.profiles
for select using (
  id = auth.uid()
  or exists (
    select 1
    from public.team_members mine
    join public.team_members peer on peer.team_id = mine.team_id
    where mine.user_id = auth.uid() and peer.user_id = profiles.id
  )
);

create or replace function public.validate_cart_item_product_team()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
  if not exists (
    select 1
    from public.carts c
    join public.products p on p.id = new.product_id
    where c.id = new.cart_id and c.team_id = p.team_id
  ) then
    raise exception 'cart and product must belong to the same team';
  end if;
  return new;
end;
$$;

create trigger cart_items_same_team
before insert or update of cart_id, product_id on public.cart_items
for each row execute function public.validate_cart_item_product_team();

revoke insert, update, delete on public.orders from authenticated;
revoke insert, update, delete on public.order_items from authenticated;
grant select on public.orders, public.order_items to authenticated;

create or replace function public.create_team_with_leader(team_name text)
returns public.teams
language plpgsql
security definer
set search_path = public
as $$
declare
  created_team public.teams;
begin
  if auth.uid() is null then raise exception 'unauthorized'; end if;
  if length(trim(coalesce(team_name, ''))) < 2 then raise exception 'name_required'; end if;

  insert into public.teams (name, created_by)
  values (trim(team_name), auth.uid())
  returning * into created_team;

  insert into public.team_members (team_id, user_id, role)
  values (created_team.id, auth.uid(), 'LIDER');

  return created_team;
end;
$$;

create or replace function public.accept_invitation_code(invitation_code text)
returns uuid
language plpgsql
security definer
set search_path = public
as $$
declare
  invite public.invitations;
begin
  if auth.uid() is null then raise exception 'unauthorized'; end if;

  select * into invite
  from public.invitations
  where code = upper(trim(invitation_code))
  for update;

  if not found
     or invite.status <> 'ACTIVE'
     or invite.expires_at <= now()
     or invite.uses >= invite.max_uses then
    raise exception 'invitation_invalid_or_expired';
  end if;

  insert into public.team_members (team_id, user_id, role)
  values (invite.team_id, auth.uid(), 'MIEMBRO')
  on conflict (team_id, user_id) do nothing;

  update public.invitations
  set uses = uses + 1,
      status = case when uses + 1 >= max_uses then 'USED' else 'ACTIVE' end
  where id = invite.id;

  return invite.team_id;
end;
$$;

create or replace function public.checkout_active_cart(
  input_cart_id uuid,
  input_customer_id uuid,
  input_idempotency_key text
)
returns public.orders
language plpgsql
security definer
set search_path = public
as $$
declare
  active_cart public.carts;
  selected_customer public.customers;
  created_order public.orders;
  line record;
  calculated_total integer := 0;
begin
  if auth.uid() is null then raise exception 'unauthorized'; end if;
  if length(trim(coalesce(input_idempotency_key, ''))) not between 1 and 120 then
    raise exception 'invalid_idempotency_key';
  end if;

  perform pg_advisory_xact_lock(hashtext(auth.uid()::text || ':' || input_idempotency_key));

  select * into created_order
  from public.orders
  where user_id = auth.uid() and idempotency_key = input_idempotency_key;
  if found then return created_order; end if;

  select * into active_cart
  from public.carts
  where id = input_cart_id and user_id = auth.uid() and status = 'ACTIVE'
  for update;
  if not found then raise exception 'active_cart_not_found'; end if;

  select * into selected_customer
  from public.customers
  where id = input_customer_id and team_id = active_cart.team_id and archived = false;
  if not found then raise exception 'customer_not_allowed'; end if;
  if selected_customer.owner_user_id <> auth.uid()
     and not public.is_team_leader(active_cart.team_id) then
    raise exception 'customer_not_allowed';
  end if;

  for line in
    select ci.product_id, ci.quantity, p.sku, p.name, p.price_cents, p.available
    from public.cart_items ci
    join public.products p on p.id = ci.product_id
    where ci.cart_id = active_cart.id and p.team_id = active_cart.team_id
  loop
    if not line.available or line.quantity < 1 then
      raise exception 'cart_empty_or_product_unavailable';
    end if;
    calculated_total := calculated_total + line.quantity * line.price_cents;
  end loop;

  if calculated_total = 0 then raise exception 'cart_empty_or_product_unavailable'; end if;

  insert into public.orders (team_id, user_id, customer_id, total_cents, commission_cents, idempotency_key)
  values (active_cart.team_id, auth.uid(), selected_customer.id, calculated_total, 0, input_idempotency_key)
  returning * into created_order;

  insert into public.order_items (order_id, product_id, sku, product_name, quantity, unit_price_cents)
  select created_order.id, ci.product_id, p.sku, p.name, ci.quantity, p.price_cents
  from public.cart_items ci
  join public.products p on p.id = ci.product_id
  where ci.cart_id = active_cart.id and p.team_id = active_cart.team_id;

  update public.carts set status = 'CONVERTED' where id = active_cart.id;
  return created_order;
end;
$$;

create or replace function public.transition_order_status(
  input_order_id uuid,
  next_status public.order_status
)
returns public.orders
language plpgsql
security definer
set search_path = public
as $$
declare
  target public.orders;
  allowed boolean := false;
begin
  if auth.uid() is null then raise exception 'unauthorized'; end if;

  select * into target from public.orders where id = input_order_id for update;
  if not found then raise exception 'order_not_found'; end if;
  if target.user_id <> auth.uid() and not public.is_team_leader(target.team_id) then
    raise exception 'forbidden';
  end if;

  allowed :=
    (target.status = 'PENDIENTE' and next_status in ('CONFIRMADO', 'CANCELADO'))
    or (target.status = 'CONFIRMADO' and next_status in ('COBRADO', 'CANCELADO'))
    or (target.status = 'COBRADO' and next_status in ('ENTREGADO', 'CANCELADO'));

  if not allowed then raise exception 'invalid_status_transition'; end if;

  update public.orders set status = next_status where id = target.id returning * into target;
  return target;
end;
$$;

grant execute on function public.create_team_with_leader(text) to authenticated;
grant execute on function public.accept_invitation_code(text) to authenticated;
grant execute on function public.checkout_active_cart(uuid, uuid, text) to authenticated;
grant execute on function public.transition_order_status(uuid, public.order_status) to authenticated;


