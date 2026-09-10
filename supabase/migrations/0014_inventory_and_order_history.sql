alter table public.products
  add column if not exists stock_updated_at timestamptz;

alter table public.orders
  add column if not exists payment_method text,
  add column if not exists amount_paid_cents integer,
  add column if not exists delivery_proof_path text,
  add column if not exists status_updated_at timestamptz not null default now(),
  add column if not exists paid_at timestamptz,
  add column if not exists delivered_at timestamptz,
  add column if not exists cancelled_at timestamptz,
  add column if not exists returned_at timestamptz;

alter table public.orders drop constraint if exists orders_amount_paid_cents_check;
alter table public.orders add constraint orders_amount_paid_cents_check
  check (amount_paid_cents is null or amount_paid_cents >= 0);
alter table public.orders drop constraint if exists orders_payment_method_check;
alter table public.orders add constraint orders_payment_method_check
  check (payment_method is null or payment_method in ('EFECTIVO', 'YAPE', 'PLIN', 'TRANSFERENCIA', 'OTRO'));

create table if not exists public.inventory_movements (
  id uuid primary key default gen_random_uuid(),
  team_id uuid not null references public.teams(id) on delete cascade,
  product_id uuid not null references public.products(id) on delete cascade,
  order_id uuid references public.orders(id) on delete set null,
  movement_type text not null check (movement_type in ('IMPORT', 'SALE', 'CANCEL_RESTORE', 'RETURN_RESTORE', 'ADMIN_ADJUSTMENT')),
  quantity_delta integer not null check (quantity_delta <> 0),
  quantity_before integer check (quantity_before is null or quantity_before >= 0),
  quantity_after integer check (quantity_after is null or quantity_after >= 0),
  reason text,
  created_by uuid references auth.users(id) on delete set null,
  idempotency_key text not null,
  created_at timestamptz not null default now(),
  unique (team_id, idempotency_key)
);

create table if not exists public.order_status_events (
  id uuid primary key default gen_random_uuid(),
  order_id uuid not null references public.orders(id) on delete cascade,
  team_id uuid not null references public.teams(id) on delete cascade,
  actor_user_id uuid references auth.users(id) on delete set null,
  previous_status public.order_status not null,
  next_status public.order_status not null,
  reason text,
  payment_method text,
  amount_paid_cents integer,
  proof_path text,
  source text not null default 'APP' check (source in ('ANDROID', 'WEB', 'APP', 'SYSTEM')),
  created_at timestamptz not null default now()
);

create index if not exists inventory_movements_product_created_idx
  on public.inventory_movements (product_id, created_at desc);
create index if not exists inventory_movements_order_id_idx
  on public.inventory_movements (order_id);
create index if not exists order_status_events_order_created_idx
  on public.order_status_events (order_id, created_at desc);
create index if not exists order_status_events_team_created_idx
  on public.order_status_events (team_id, created_at desc);

alter table public.inventory_movements enable row level security;
alter table public.order_status_events enable row level security;

drop policy if exists inventory_movements_leader_read on public.inventory_movements;
create policy inventory_movements_leader_read on public.inventory_movements
for select to authenticated
using ((select public.is_team_leader(team_id)));

drop policy if exists order_status_events_order_access on public.order_status_events;
create policy order_status_events_order_access on public.order_status_events
for select to authenticated
using (
  exists (
    select 1
    from public.orders o
    where o.id = order_id
      and (o.user_id = (select auth.uid()) or (select public.is_team_leader(o.team_id)))
  )
);

revoke all on public.inventory_movements, public.order_status_events from anon;
revoke insert, update, delete on public.inventory_movements, public.order_status_events from authenticated;
grant select on public.inventory_movements, public.order_status_events to authenticated;

create or replace function public.checkout_active_cart(
  input_cart_id uuid,
  input_customer_id uuid,
  input_idempotency_key text
) returns public.orders
language plpgsql
security definer
set search_path = ''
as $$
declare
  active_cart public.carts;
  selected_customer public.customers;
  created_order public.orders;
  line record;
  calculated_total integer := 0;
  line_count integer := 0;
begin
  if auth.uid() is null then raise exception 'unauthorized'; end if;
  if length(trim(coalesce(input_idempotency_key, ''))) not between 1 and 120 then
    raise exception 'invalid_idempotency_key';
  end if;

  perform pg_catalog.pg_advisory_xact_lock(
    pg_catalog.hashtext((select auth.uid())::text || ':' || input_idempotency_key)
  );

  select * into created_order
  from public.orders o
  where o.user_id = (select auth.uid())
    and o.idempotency_key = input_idempotency_key;
  if found then return created_order; end if;

  select * into active_cart
  from public.carts c
  where c.id = input_cart_id
    and c.user_id = (select auth.uid())
    and c.status = 'ACTIVE'
  for update;
  if not found then raise exception 'active_cart_not_found'; end if;

  select * into selected_customer
  from public.customers c
  where c.id = input_customer_id
    and c.team_id = active_cart.team_id
    and c.archived = false;
  if not found or (
    selected_customer.owner_user_id <> (select auth.uid())
    and not public.is_team_leader(active_cart.team_id)
  ) then raise exception 'customer_not_allowed'; end if;

  -- Every checkout acquires product locks in UUID order to prevent deadlocks.
  perform 1
  from public.products p
  join public.cart_items ci on ci.product_id = p.id
  where ci.cart_id = active_cart.id
    and p.team_id = active_cart.team_id
  order by p.id
  for update of p;

  for line in
    select p.id as product_id, p.sku, p.name, p.price_cents, p.available,
           p.stock_quantity, ci.quantity
    from public.cart_items ci
    join public.products p on p.id = ci.product_id
    where ci.cart_id = active_cart.id
      and p.team_id = active_cart.team_id
    order by p.id
  loop
    line_count := line_count + 1;
    if not line.available or line.quantity < 1 then
      raise exception 'product_unavailable';
    end if;
    if line.stock_quantity is not null and line.stock_quantity < line.quantity then
      raise exception 'product_out_of_stock';
    end if;
    calculated_total := calculated_total + line.quantity * line.price_cents;
  end loop;

  if line_count = 0 then raise exception 'cart_empty'; end if;

  insert into public.orders (
    team_id, user_id, customer_id, total_cents, commission_cents, idempotency_key
  ) values (
    active_cart.team_id, (select auth.uid()), selected_customer.id,
    calculated_total, 0, input_idempotency_key
  ) returning * into created_order;

  insert into public.order_items (
    order_id, product_id, sku, product_name, quantity, unit_price_cents
  )
  select created_order.id, ci.product_id, p.sku, p.name, ci.quantity, p.price_cents
  from public.cart_items ci
  join public.products p on p.id = ci.product_id
  where ci.cart_id = active_cart.id
    and p.team_id = active_cart.team_id;

  insert into public.order_status_events (
    order_id, team_id, actor_user_id, previous_status, next_status, source
  ) values (
    created_order.id, active_cart.team_id, (select auth.uid()),
    'PENDIENTE', 'PENDIENTE', 'APP'
  );

  for line in
    select p.id as product_id, p.stock_quantity, ci.quantity
    from public.cart_items ci
    join public.products p on p.id = ci.product_id
    where ci.cart_id = active_cart.id
      and p.team_id = active_cart.team_id
      and p.stock_quantity is not null
    order by p.id
  loop
    update public.products
    set stock_quantity = line.stock_quantity - line.quantity,
        stock_updated_at = pg_catalog.now(),
        available = line.stock_quantity - line.quantity > 0
    where id = line.product_id;

    insert into public.inventory_movements (
      team_id, product_id, order_id, movement_type, quantity_delta,
      quantity_before, quantity_after, created_by, idempotency_key
    ) values (
      active_cart.team_id, line.product_id, created_order.id, 'SALE', -line.quantity,
      line.stock_quantity, line.stock_quantity - line.quantity, (select auth.uid()),
      'checkout:' || created_order.id::text || ':' || line.product_id::text
    );
  end loop;

  update public.carts set status = 'CONVERTED' where id = active_cart.id;
  return created_order;
end;
$$;

create or replace function public.transition_order_status_v2(
  input_order_id uuid,
  next_status public.order_status,
  reason text default null,
  payment_method text default null,
  input_amount_paid_cents integer default null,
  proof_path text default null,
  input_delivery_proof_path text default null,
  event_source text default 'APP'
) returns public.orders
language plpgsql
security definer
set search_path = ''
as $$
declare
  target public.orders;
  line record;
  allowed boolean := false;
  normalized_reason text := nullif(trim(coalesce(reason, '')), '');
  normalized_method text := nullif(upper(trim(coalesce(payment_method, ''))), '');
  normalized_source text := upper(trim(coalesce(event_source, 'APP')));
  previous_status public.order_status;
begin
  if auth.uid() is null then raise exception 'unauthorized'; end if;

  select * into target from public.orders o where o.id = input_order_id for update;
  if not found then raise exception 'order_not_found'; end if;
  if target.user_id <> (select auth.uid()) and not public.is_team_leader(target.team_id) then
    raise exception 'forbidden';
  end if;
  previous_status := target.status;

  allowed :=
    (target.status = 'PENDIENTE' and next_status in ('CONFIRMADO', 'CANCELADO'))
    or (target.status = 'CONFIRMADO' and next_status in ('COBRADO', 'CANCELADO'))
    or (target.status = 'COBRADO' and next_status in ('ENTREGADO', 'CANCELADO'))
    or (target.status = 'ENTREGADO' and next_status = 'DEVUELTO');
  if not allowed then raise exception 'invalid_status_transition'; end if;

  if next_status in ('CANCELADO', 'DEVUELTO') and length(coalesce(normalized_reason, '')) < 3 then
    raise exception 'reason_required';
  end if;
  if next_status = 'COBRADO' then
    if normalized_method not in ('EFECTIVO', 'YAPE', 'PLIN', 'TRANSFERENCIA', 'OTRO') then
      raise exception 'payment_method_required';
    end if;
    if input_amount_paid_cents is null or input_amount_paid_cents < target.total_cents then
      raise exception 'payment_amount_invalid';
    end if;
    if normalized_method <> 'EFECTIVO' and length(trim(coalesce(proof_path, ''))) < 1 then
      raise exception 'payment_proof_required';
    end if;
  end if;
  if normalized_source not in ('ANDROID', 'WEB', 'APP', 'SYSTEM') then
    raise exception 'invalid_event_source';
  end if;

  -- A cancelled or returned order restores managed inventory exactly once.
  if next_status in ('CANCELADO', 'DEVUELTO') then
    perform 1
    from public.products p
    join public.order_items oi on oi.product_id = p.id
    where oi.order_id = target.id and p.stock_quantity is not null
    order by p.id
    for update of p;

    for line in
      select p.id as product_id, p.stock_quantity, oi.quantity
      from public.order_items oi
      join public.products p on p.id = oi.product_id
      where oi.order_id = target.id and p.stock_quantity is not null
      order by p.id
    loop
      update public.products
      set stock_quantity = line.stock_quantity + line.quantity,
          stock_updated_at = pg_catalog.now(),
          available = true
      where id = line.product_id;

      insert into public.inventory_movements (
        team_id, product_id, order_id, movement_type, quantity_delta,
        quantity_before, quantity_after, reason, created_by, idempotency_key
      ) values (
        target.team_id, line.product_id, target.id,
        case when next_status = 'DEVUELTO' then 'RETURN_RESTORE' else 'CANCEL_RESTORE' end,
        line.quantity, line.stock_quantity, line.stock_quantity + line.quantity,
        normalized_reason, (select auth.uid()),
        lower(next_status::text) || ':' || target.id::text || ':' || line.product_id::text
      );
    end loop;
  end if;

  update public.orders
  set status = next_status,
      status_updated_at = pg_catalog.now(),
      cancellation_reason = case when next_status = 'CANCELADO' then normalized_reason else cancellation_reason end,
      return_reason = case when next_status = 'DEVUELTO' then normalized_reason else return_reason end,
      payment_method = case when next_status = 'COBRADO' then normalized_method else orders.payment_method end,
      amount_paid_cents = case when next_status = 'COBRADO' then input_amount_paid_cents else orders.amount_paid_cents end,
      payment_proof_path = case when next_status = 'COBRADO' then proof_path else payment_proof_path end,
      delivery_proof_path = case when next_status = 'ENTREGADO' then input_delivery_proof_path else orders.delivery_proof_path end,
      paid_at = case when next_status = 'COBRADO' then pg_catalog.now() else paid_at end,
      delivered_at = case when next_status = 'ENTREGADO' then pg_catalog.now() else delivered_at end,
      cancelled_at = case when next_status = 'CANCELADO' then pg_catalog.now() else cancelled_at end,
      returned_at = case when next_status = 'DEVUELTO' then pg_catalog.now() else returned_at end
  where id = target.id
  returning * into target;

  insert into public.order_status_events (
    order_id, team_id, actor_user_id, previous_status, next_status, reason,
    payment_method, amount_paid_cents, proof_path, source
  ) values (
    target.id, target.team_id, (select auth.uid()),
    previous_status, next_status, normalized_reason, normalized_method, input_amount_paid_cents,
    coalesce(proof_path, input_delivery_proof_path), normalized_source
  );

  return target;
end;
$$;

revoke all on function public.checkout_active_cart(uuid, uuid, text) from public, anon;
revoke all on function public.transition_order_status_v2(uuid, public.order_status, text, text, integer, text, text, text) from public, anon;
grant execute on function public.checkout_active_cart(uuid, uuid, text) to authenticated;
grant execute on function public.transition_order_status_v2(uuid, public.order_status, text, text, integer, text, text, text) to authenticated;
