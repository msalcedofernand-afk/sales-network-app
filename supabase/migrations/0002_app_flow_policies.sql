-- Policies required by the interactive web/Android flows.
create policy orders_owner_insert on public.orders for insert
  with check (user_id = auth.uid() and public.is_team_member(team_id));

create policy orders_owner_update on public.orders for update
  using (user_id = auth.uid() or public.is_team_leader(team_id))
  with check (user_id = auth.uid() or public.is_team_leader(team_id));

create policy order_items_owner_insert on public.order_items for insert
  with check (exists (select 1 from public.orders o where o.id = order_id and o.user_id = auth.uid()));

create or replace function public.is_team_leader(target_team uuid)
returns boolean language sql stable security definer set search_path = public
as $$ select exists (
  select 1 from public.team_members
  where team_id = target_team and user_id = auth.uid() and role = 'LIDER'
); $$;
