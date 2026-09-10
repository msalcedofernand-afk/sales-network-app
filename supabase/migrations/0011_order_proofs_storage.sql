-- Payment proofs are private customer data.
insert into storage.buckets (id, name, public)
values ('order-proofs', 'order-proofs', false)
on conflict (id) do update set public = false;

drop policy if exists order_proofs_owner_insert on storage.objects;
create policy order_proofs_owner_insert on storage.objects
for insert to authenticated
with check (bucket_id = 'order-proofs' and split_part(name, '/', 1) = (select auth.uid())::text);

drop policy if exists order_proofs_owner_read on storage.objects;
create policy order_proofs_owner_read on storage.objects
for select to authenticated
using (bucket_id = 'order-proofs' and split_part(name, '/', 1) = (select auth.uid())::text);
