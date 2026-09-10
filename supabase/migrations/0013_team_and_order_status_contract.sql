-- Keep one unambiguous team context per account for the 1.2 release.
-- This intentionally fails if historical duplicate memberships exist so an
-- operator can review them instead of deleting business data automatically.
create unique index if not exists team_members_one_team_per_user_idx
  on public.team_members (user_id);

alter type public.order_status add value if not exists 'DEVUELTO';

create or replace function public.is_team_member(target_team uuid)
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
  select exists (
    select 1
    from public.team_members tm
    where tm.team_id = target_team
      and tm.user_id = (select auth.uid())
  );
$$;

create or replace function public.is_team_leader(target_team uuid)
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
  select exists (
    select 1
    from public.team_members tm
    where tm.team_id = target_team
      and tm.user_id = (select auth.uid())
      and tm.role::text in ('LIDER', 'ROOT_ADMIN')
  );
$$;

revoke all on function public.is_team_member(uuid) from public, anon;
revoke all on function public.is_team_leader(uuid) from public, anon;
grant execute on function public.is_team_member(uuid) to authenticated;
grant execute on function public.is_team_leader(uuid) to authenticated;

create or replace function public.create_team_with_leader(team_name text)
returns public.teams
language plpgsql
security definer
set search_path = ''
as $$
declare
  created_team public.teams;
begin
  if auth.uid() is null then raise exception 'unauthorized'; end if;
  if length(trim(coalesce(team_name, ''))) not between 2 and 120 then
    raise exception 'invalid_team_name';
  end if;
  if exists (select 1 from public.team_members tm where tm.user_id = (select auth.uid())) then
    raise exception 'already_belongs_to_team';
  end if;

  insert into public.teams (name, created_by)
  values (trim(team_name), (select auth.uid()))
  returning * into created_team;

  insert into public.team_members (team_id, user_id, role)
  values (created_team.id, (select auth.uid()), 'LIDER');

  return created_team;
end;
$$;

revoke all on function public.create_team_with_leader(text) from public, anon;
grant execute on function public.create_team_with_leader(text) to authenticated;
