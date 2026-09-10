create index if not exists orders_payment_proof_path_idx
  on public.orders (payment_proof_path) where payment_proof_path is not null;
create index if not exists orders_delivery_proof_path_idx
  on public.orders (delivery_proof_path) where delivery_proof_path is not null;

drop policy if exists order_proofs_owner_delete on storage.objects;
create policy order_proofs_owner_delete on storage.objects
for delete to authenticated
using (
  bucket_id = 'order-proofs'
  and split_part(name, '/', 1) = (select auth.uid())::text
);

drop policy if exists order_proofs_order_read on storage.objects;
create policy order_proofs_order_read on storage.objects
for select to authenticated
using (
  bucket_id = 'order-proofs'
  and exists (
    select 1 from public.orders o
    where (o.payment_proof_path = name or o.delivery_proof_path = name)
      and (o.user_id = (select auth.uid()) or (select public.is_team_leader(o.team_id)))
  )
);

create or replace function public.validate_order_proof_paths()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
declare
  candidate text;
begin
  if new.payment_proof_path is distinct from old.payment_proof_path then
    candidate := new.payment_proof_path;
  elsif new.delivery_proof_path is distinct from old.delivery_proof_path then
    candidate := new.delivery_proof_path;
  else
    return new;
  end if;

  if candidate is not null and not exists (
    select 1 from storage.objects obj
    where obj.bucket_id = 'order-proofs'
      and obj.name = candidate
      and split_part(obj.name, '/', 1) = (select auth.uid())::text
  ) then raise exception 'order_proof_not_found'; end if;
  return new;
end;
$$;

drop trigger if exists orders_validate_proof_paths on public.orders;
create trigger orders_validate_proof_paths
before update of payment_proof_path, delivery_proof_path on public.orders
for each row execute function public.validate_order_proof_paths();

revoke all on function public.validate_order_proof_paths() from public, anon, authenticated;

