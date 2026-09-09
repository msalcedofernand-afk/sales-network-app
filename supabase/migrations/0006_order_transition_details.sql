alter table public.orders add column if not exists cancellation_reason text;
alter table public.orders add column if not exists payment_proof_path text;
alter table public.orders add column if not exists return_reason text;

create or replace function public.transition_order_status_with_details(input_order_id uuid, next_status public.order_status, reason text default null, proof_path text default null)
returns public.orders language plpgsql security definer set search_path = public as $$
declare target public.orders; allowed boolean := false;
begin
  if auth.uid() is null then raise exception 'unauthorized'; end if;
  select * into target from public.orders where id=input_order_id for update;
  if not found then raise exception 'order_not_found'; end if;
  if target.user_id <> auth.uid() and not public.is_team_leader(target.team_id) then raise exception 'forbidden'; end if;
  allowed := (target.status='PENDIENTE' and next_status in ('CONFIRMADO','CANCELADO')) or (target.status='CONFIRMADO' and next_status in ('COBRADO','CANCELADO')) or (target.status='COBRADO' and next_status in ('ENTREGADO','CANCELADO'));
  if not allowed then raise exception 'invalid_status_transition'; end if;
  if next_status='CANCELADO' and length(trim(coalesce(reason,''))) < 3 then raise exception 'cancellation_reason_required'; end if;
  if next_status='COBRADO' and length(trim(coalesce(proof_path,''))) < 1 then raise exception 'payment_proof_required'; end if;
  update public.orders set status=next_status, cancellation_reason=case when next_status='CANCELADO' then trim(reason) else cancellation_reason end, return_reason=case when next_status='CANCELADO' and target.status='ENTREGADO' then trim(reason) else return_reason end, payment_proof_path=case when next_status='COBRADO' then proof_path else payment_proof_path end where id=target.id returning * into target;
  return target;
end; $$;
grant execute on function public.transition_order_status_with_details(uuid, public.order_status, text, text) to authenticated;
