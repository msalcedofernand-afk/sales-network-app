-- Ejecutar con dos sesiones autenticadas después de aplicar las migraciones.
-- Nunca ejecutar estas comprobaciones desde el navegador con service_role.
select count(*) as visible_customers_from_current_team from public.customers;
-- Con la sesión del equipo B, este conteo debe ser 0 para el UUID del equipo A:
select count(*) as cross_team_customers from public.customers
where team_id = '00000000-0000-0000-0000-000000000000'::uuid;
-- Repetir para products, carts y orders; cualquier fila cruzada bloquea beta.
