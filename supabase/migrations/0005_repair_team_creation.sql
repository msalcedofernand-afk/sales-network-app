-- Repara instalaciones donde 0004 fue marcado como aplicado después de un fallo parcial.
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
  values (created_team.id, auth.uid(), 'LIDER')
  on conflict (team_id, user_id) do nothing;

  return created_team;
end;
$$;

grant execute on function public.create_team_with_leader(text) to authenticated;
