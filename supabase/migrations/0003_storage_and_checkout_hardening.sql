-- Public release bucket: APKs are public artifacts, while uploads remain server-only.
insert into storage.buckets (id, name, public)
values ('app-releases', 'app-releases', true)
on conflict (id) do update set public = excluded.public;

create policy app_releases_public_read
on storage.objects for select
using (bucket_id = 'app-releases');

-- No authenticated client receives write access. CI or an Edge Function must use
-- the service role to publish a reviewed APK.
