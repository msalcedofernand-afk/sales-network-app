-- Security contract for 1.2.0. Safe to run after 0001-0008.

alter type public.team_role add value if not exists 'ROOT_ADMIN';

create unique index if not exists invitations_code_upper_unique
  on public.invitations (upper(code));
create index if not exists invitations_team_status_expires_idx
  on public.invitations (team_id, status, expires_at);
create index if not exists addresses_customer_id_idx on public.addresses (customer_id);
create index if not exists orders_customer_id_idx on public.orders (customer_id);
create index if not exists order_items_product_id_idx on public.order_items (product_id);

alter table public.invitations drop constraint if exists invitations_max_uses_check;
alter table public.invitations add constraint invitations_max_uses_check check (max_uses between 1 and 50);
alter table public.invitations drop constraint if exists invitations_expiration_window_check;
alter table public.invitations add constraint invitations_expiration_window_check
  check (expires_at between created_at + interval '5 minutes' and created_at + interval '30 days');

create table if not exists public.edge_rate_limits (
  user_id uuid not null references auth.users(id) on delete cascade,
  action text not null,
  window_started_at timestamptz not null,
  request_count integer not null default 1 check (request_count > 0),
  primary key (user_id, action, window_started_at)
);
alter table public.edge_rate_limits enable row level security;
revoke all on public.edge_rate_limits from anon, authenticated;
grant select, insert, update, delete on public.edge_rate_limits to service_role;

create or replace function public.consume_edge_rate_limit(
  input_user_id uuid,
  input_action text,
  input_limit integer,
  input_window_seconds integer
) returns boolean
language plpgsql
security definer
set search_path = ''
as $$
declare
  bucket timestamptz;
  new_count integer;
begin
  if auth.role() <> 'service_role' then raise exception 'forbidden'; end if;
  if input_limit not between 1 and 1000 or input_window_seconds not between 1 and 86400 then
    raise exception 'invalid_rate_limit';
  end if;
  bucket := to_timestamp(floor(extract(epoch from now()) / input_window_seconds) * input_window_seconds);
  insert into public.edge_rate_limits(user_id, action, window_started_at, request_count)
  values (input_user_id, left(input_action, 80), bucket, 1)
  on conflict (user_id, action, window_started_at)
  do update set request_count = public.edge_rate_limits.request_count + 1
  returning request_count into new_count;
  return new_count <= input_limit;
end;
$$;
revoke all on function public.consume_edge_rate_limit(uuid, text, integer, integer) from public, anon, authenticated;
grant execute on function public.consume_edge_rate_limit(uuid, text, integer, integer) to service_role;

create or replace function public.create_invitation_secure(
  input_team_id uuid,
  input_expires_at timestamptz,
  input_max_uses integer default 1
) returns public.invitations
language plpgsql
security definer
set search_path = ''
as $$
declare
  created public.invitations;
  generated_code text;
  attempt integer;
begin
  if auth.uid() is null then raise exception 'unauthorized'; end if;
  if input_expires_at not between now() + interval '5 minutes' and now() + interval '30 days' then
    raise exception 'invalid_expiration';
  end if;
  if input_max_uses not between 1 and 50 then raise exception 'invalid_max_uses'; end if;
  if not exists (
    select 1 from public.team_members tm
    where tm.team_id = input_team_id and tm.user_id = auth.uid() and tm.role::text in ('LIDER', 'ROOT_ADMIN')
  ) then raise exception 'leader_required'; end if;

  for attempt in 1..5 loop
    generated_code := upper(encode(extensions.gen_random_bytes(8), 'hex'));
    begin
      insert into public.invitations(team_id, code, expires_at, max_uses, created_by)
      values (input_team_id, generated_code, input_expires_at, input_max_uses, auth.uid())
      returning * into created;
      return created;
    exception when unique_violation then
      if attempt = 5 then raise; end if;
    end;
  end loop;
  raise exception 'invitation_code_generation_failed';
end;
$$;
revoke all on function public.create_invitation_secure(uuid, timestamptz, integer) from public, anon;
grant execute on function public.create_invitation_secure(uuid, timestamptz, integer) to authenticated;

create or replace function public.accept_invitation_code(invitation_code text)
returns uuid
language plpgsql
security definer
set search_path = ''
as $$
declare
  invite public.invitations;
begin
  if auth.uid() is null then raise exception 'unauthorized'; end if;
  select * into invite from public.invitations
  where upper(code) = upper(trim(invitation_code)) for update;
  if not found then raise exception 'invitation_not_found'; end if;

  if exists (select 1 from public.team_members where team_id = invite.team_id and user_id = auth.uid()) then
    return invite.team_id;
  end if;
  if invite.status = 'ACTIVE' and invite.expires_at <= now() then
    update public.invitations set status = 'EXPIRED' where id = invite.id;
    raise exception 'invitation_expired';
  end if;
  if invite.status <> 'ACTIVE' or invite.uses >= invite.max_uses then
    raise exception 'invitation_unavailable';
  end if;
  if exists (select 1 from public.team_members where user_id = auth.uid() and team_id <> invite.team_id) then
    raise exception 'already_belongs_to_another_team';
  end if;

  insert into public.team_members(team_id, user_id, role)
  values (invite.team_id, auth.uid(), 'MIEMBRO');
  update public.invitations
  set uses = uses + 1,
      status = case when uses + 1 >= max_uses then 'USED' else 'ACTIVE' end
  where id = invite.id;
  return invite.team_id;
end;
$$;

alter table public.crash_reports add column if not exists team_id uuid references public.teams(id) on delete set null;
alter table public.crash_reports add column if not exists channel text not null default 'stable';
alter table public.crash_reports add column if not exists platform text not null default 'android';
alter table public.crash_reports add column if not exists exception_type text not null default 'unknown';
alter table public.crash_reports add constraint crash_reports_stacktrace_size_check
  check (octet_length(stacktrace) <= 32768) not valid;
create index if not exists crash_reports_team_created_idx on public.crash_reports(team_id, created_at desc);

drop policy if exists crash_reports_read_leader on public.crash_reports;
create policy crash_reports_read_leader on public.crash_reports for select using (
  exists (
    select 1 from public.team_members mine
    where mine.team_id = crash_reports.team_id
      and mine.user_id = (select auth.uid())
      and mine.role::text in ('LIDER', 'ROOT_ADMIN')
  )
);

select cron.unschedule(jobid) from cron.job where jobname = 'cleanup-old-crash-reports';
select cron.schedule(
  'cleanup-old-crash-reports', '0 4 * * *',
  $$delete from public.crash_reports where created_at < now() - interval '30 days';$$
);
select cron.unschedule(jobid) from cron.job where jobname = 'cleanup-edge-rate-limits';
select cron.schedule(
  'cleanup-edge-rate-limits', '15 4 * * *',
  $$delete from public.edge_rate_limits where window_started_at < now() - interval '2 days';$$
);
