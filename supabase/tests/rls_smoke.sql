-- RLS integration contract. CI sets these settings with JWTs for two real test users.
-- The file fails when a session can read a row owned by the other team.
do $$
declare
  foreign_team uuid := current_setting('test.foreign_team_id', true)::uuid;
  leaked_count integer;
begin
  if foreign_team is null then
    raise exception 'test.foreign_team_id must be set by the integration harness';
  end if;
  select count(*) into leaked_count from public.customers where team_id = foreign_team;
  if leaked_count <> 0 then raise exception 'RLS leak: customers from another team are visible'; end if;
  select count(*) into leaked_count from public.orders where team_id = foreign_team;
  if leaked_count <> 0 then raise exception 'RLS leak: orders from another team are visible'; end if;
  select count(*) into leaked_count from public.invitations where team_id = foreign_team;
  if leaked_count <> 0 then raise exception 'RLS leak: invitations from another team are visible'; end if;
  select count(*) into leaked_count from public.crash_reports where team_id = foreign_team;
  if leaked_count <> 0 then raise exception 'RLS leak: crash reports from another team are visible'; end if;
end $$;
