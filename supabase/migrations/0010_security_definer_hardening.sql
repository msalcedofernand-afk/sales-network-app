-- Keep SECURITY DEFINER routines deterministic and immune to caller-controlled
-- search paths. The routines remain in public only because PostgREST invokes
-- them as RPCs; application roles receive execute only on the listed routines.

alter function public.validate_cart_item_product_team() set search_path = pg_catalog, public;
alter function public.create_team_with_leader(text) set search_path = pg_catalog, public;
alter function public.accept_invitation_code(text) set search_path = pg_catalog, public;
alter function public.checkout_active_cart(uuid, uuid, text) set search_path = pg_catalog, public;
alter function public.transition_order_status(uuid, public.order_status) set search_path = pg_catalog, public;
alter function public.transition_order_status_with_details(uuid, public.order_status, text, text) set search_path = pg_catalog, public;

revoke all on function public.validate_cart_item_product_team() from public, anon, authenticated;
revoke all on function public.create_team_with_leader(text) from public, anon;
revoke all on function public.accept_invitation_code(text) from public, anon;
revoke all on function public.checkout_active_cart(uuid, uuid, text) from public, anon;
revoke all on function public.transition_order_status(uuid, public.order_status) from public, anon;
revoke all on function public.transition_order_status_with_details(uuid, public.order_status, text, text) from public, anon;

grant execute on function public.create_team_with_leader(text) to authenticated;
grant execute on function public.accept_invitation_code(text) to authenticated;
grant execute on function public.checkout_active_cart(uuid, uuid, text) to authenticated;
grant execute on function public.transition_order_status(uuid, public.order_status) to authenticated;
grant execute on function public.transition_order_status_with_details(uuid, public.order_status, text, text) to authenticated;
