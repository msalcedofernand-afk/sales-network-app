create extension if not exists pgcrypto;

create type public.team_role as enum ('LIDER', 'MIEMBRO');
create type public.invitation_status as enum ('ACTIVE', 'USED', 'REVOKED', 'EXPIRED');
create type public.cart_status as enum ('ACTIVE', 'CONVERTED', 'ABANDONED');
create type public.order_status as enum ('PENDIENTE', 'CONFIRMADO', 'COBRADO', 'ENTREGADO', 'CANCELADO');

create table public.profiles (
  id uuid primary key references auth.users(id) on delete cascade,
  name text not null check (length(trim(name)) between 2 and 120),
  phone text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table public.teams (
  id uuid primary key default gen_random_uuid(),
  name text not null check (length(trim(name)) between 2 and 120),
  created_by uuid not null references auth.users(id),
  created_at timestamptz not null default now()
);

create table public.team_members (
  team_id uuid not null references public.teams(id) on delete cascade,
  user_id uuid not null references auth.users(id) on delete cascade,
  role public.team_role not null default 'MIEMBRO',
  created_at timestamptz not null default now(),
  primary key (team_id, user_id)
);

create table public.invitations (
  id uuid primary key default gen_random_uuid(),
  team_id uuid not null references public.teams(id) on delete cascade,
  code text not null unique check (code ~ '^[A-Z0-9-]{8,32}$'),
  status public.invitation_status not null default 'ACTIVE',
  created_by uuid not null references auth.users(id),
  expires_at timestamptz not null,
  max_uses integer not null default 1 check (max_uses > 0),
  uses integer not null default 0 check (uses >= 0),
  created_at timestamptz not null default now()
);

create table public.campaigns (
  id uuid primary key default gen_random_uuid(),
  team_id uuid not null references public.teams(id) on delete cascade,
  name text not null,
  starts_at timestamptz,
  ends_at timestamptz,
  published boolean not null default false,
  created_at timestamptz not null default now()
);

create table public.products (
  id uuid primary key default gen_random_uuid(),
  team_id uuid not null references public.teams(id) on delete cascade,
  campaign_id uuid references public.campaigns(id) on delete set null,
  sku text not null,
  slug text not null,
  name text not null,
  description text not null default '',
  category text not null default 'Otros',
  price_cents integer not null check (price_cents >= 0),
  currency char(3) not null default 'PEN',
  available boolean not null default true,
  image_url text,
  source_url text,
  updated_at timestamptz not null default now(),
  unique (team_id, sku)
);

create table public.product_images (
  id uuid primary key default gen_random_uuid(),
  product_id uuid not null references public.products(id) on delete cascade,
  storage_path text not null,
  sort_order integer not null default 0
);

create table public.customers (
  id uuid primary key default gen_random_uuid(),
  team_id uuid not null references public.teams(id) on delete cascade,
  owner_user_id uuid not null references auth.users(id),
  name text not null,
  phone text not null,
  notes text not null default '',
  archived boolean not null default false,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table public.addresses (
  id uuid primary key default gen_random_uuid(),
  customer_id uuid not null references public.customers(id) on delete cascade,
  label text not null default 'Principal',
  address text not null,
  city text not null default 'Chiclayo',
  latitude double precision check (latitude between -90 and 90),
  longitude double precision check (longitude between -180 and 180),
  location_confirmed boolean not null default false
);

create table public.carts (
  id uuid primary key default gen_random_uuid(),
  team_id uuid not null references public.teams(id) on delete cascade,
  user_id uuid not null references auth.users(id) on delete cascade,
  status public.cart_status not null default 'ACTIVE',
  updated_at timestamptz not null default now(),
  created_at timestamptz not null default now()
);
create unique index carts_one_active_per_user_team on public.carts(user_id, team_id) where status = 'ACTIVE';

create table public.cart_items (
  id uuid primary key default gen_random_uuid(),
  cart_id uuid not null references public.carts(id) on delete cascade,
  product_id uuid not null references public.products(id),
  quantity integer not null check (quantity > 0),
  unique (cart_id, product_id)
);

create table public.orders (
  id uuid primary key default gen_random_uuid(),
  team_id uuid not null references public.teams(id) on delete cascade,
  user_id uuid not null references auth.users(id),
  customer_id uuid references public.customers(id) on delete set null,
  status public.order_status not null default 'PENDIENTE',
  total_cents integer not null default 0 check (total_cents >= 0),
  commission_cents integer not null default 0 check (commission_cents >= 0),
  idempotency_key text not null,
  created_at timestamptz not null default now(),
  unique (user_id, idempotency_key)
);

create table public.order_items (
  id uuid primary key default gen_random_uuid(),
  order_id uuid not null references public.orders(id) on delete cascade,
  product_id uuid references public.products(id) on delete set null,
  sku text not null,
  product_name text not null,
  quantity integer not null check (quantity > 0),
  unit_price_cents integer not null check (unit_price_cents >= 0)
);

create table public.sync_events (
  id uuid primary key default gen_random_uuid(),
  team_id uuid references public.teams(id) on delete cascade,
  source text not null,
  status text not null check (status in ('RUNNING','SUCCESS','FAILED')),
  imported_count integer not null default 0,
  error_message text,
  created_at timestamptz not null default now()
);

create or replace function public.is_team_member(target_team uuid)
returns boolean language sql stable security definer set search_path = public
as $$ select exists (select 1 from public.team_members where team_id = target_team and user_id = auth.uid()); $$;

create or replace function public.is_team_leader(target_team uuid)
returns boolean language sql stable security definer set search_path = public
as $$ select exists (select 1 from public.team_members where team_id = target_team and user_id = auth.uid() and role = 'LIDER'); $$;

create or replace function public.touch_updated_at() returns trigger language plpgsql as $$ begin new.updated_at = now(); return new; end; $$;
create or replace function public.handle_new_user() returns trigger language plpgsql security definer set search_path = public as $$ begin insert into public.profiles (id, name) values (new.id, coalesce(new.raw_user_meta_data ->> 'name', split_part(new.email, '@', 1))); return new; end; $$;
create trigger on_auth_user_created after insert on auth.users for each row execute function public.handle_new_user();
create trigger profiles_touch before update on public.profiles for each row execute function public.touch_updated_at();
create trigger products_touch before update on public.products for each row execute function public.touch_updated_at();
create trigger customers_touch before update on public.customers for each row execute function public.touch_updated_at();
create trigger carts_touch before update on public.carts for each row execute function public.touch_updated_at();

alter table public.profiles enable row level security;
alter table public.teams enable row level security;
alter table public.team_members enable row level security;
alter table public.invitations enable row level security;
alter table public.campaigns enable row level security;
alter table public.products enable row level security;
alter table public.product_images enable row level security;
alter table public.customers enable row level security;
alter table public.addresses enable row level security;
alter table public.carts enable row level security;
alter table public.cart_items enable row level security;
alter table public.orders enable row level security;
alter table public.order_items enable row level security;
alter table public.sync_events enable row level security;

create policy profiles_self on public.profiles for all using (id = auth.uid()) with check (id = auth.uid());
create policy teams_member_read on public.teams for select using (public.is_team_member(id));
create policy teams_leader_manage on public.teams for update using (public.is_team_leader(id)) with check (public.is_team_leader(id));
create policy team_members_read on public.team_members for select using (public.is_team_member(team_id));
create policy team_members_leader_manage on public.team_members for all using (public.is_team_leader(team_id)) with check (public.is_team_leader(team_id));
create policy invitations_member_read on public.invitations for select using (public.is_team_member(team_id));
create policy invitations_leader_manage on public.invitations for all using (public.is_team_leader(team_id)) with check (public.is_team_leader(team_id));
create policy campaigns_member_read on public.campaigns for select using (public.is_team_member(team_id));
create policy campaigns_leader_manage on public.campaigns for all using (public.is_team_leader(team_id)) with check (public.is_team_leader(team_id));
create policy products_member_read on public.products for select using (public.is_team_member(team_id));
create policy products_leader_manage on public.products for all using (public.is_team_leader(team_id)) with check (public.is_team_leader(team_id));
create policy images_member_read on public.product_images for select using (exists (select 1 from public.products p where p.id = product_id and public.is_team_member(p.team_id)));
create policy customers_owner_or_leader on public.customers for all using (owner_user_id = auth.uid() or public.is_team_leader(team_id)) with check (public.is_team_member(team_id) and (owner_user_id = auth.uid() or public.is_team_leader(team_id)));
create policy addresses_customer_access on public.addresses for all using (exists (select 1 from public.customers c where c.id = customer_id and (c.owner_user_id = auth.uid() or public.is_team_leader(c.team_id))));
create policy carts_owner on public.carts for all using (user_id = auth.uid() and public.is_team_member(team_id)) with check (user_id = auth.uid() and public.is_team_member(team_id));
create policy cart_items_owner on public.cart_items for all using (exists (select 1 from public.carts c where c.id = cart_id and c.user_id = auth.uid())) with check (exists (select 1 from public.carts c where c.id = cart_id and c.user_id = auth.uid()));
create policy orders_owner_or_leader on public.orders for select using (user_id = auth.uid() or public.is_team_leader(team_id));
create policy order_items_owner_or_leader on public.order_items for select using (exists (select 1 from public.orders o where o.id = order_id and (o.user_id = auth.uid() or public.is_team_leader(o.team_id))));
create policy sync_leader_read on public.sync_events for select using (public.is_team_leader(team_id));

revoke all on all tables in schema public from anon;
grant select on public.products, public.campaigns to authenticated;
grant select, insert, update, delete on all tables in schema public to authenticated;
