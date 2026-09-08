# Seguridad

- Las tablas de Supabase deben tener RLS habilitado.
- `service_role` y `sb_secret` solo pueden vivir en servidor.
- Validar JWT y pertenencia al equipo en cada función.
- Calcular precios y comisiones en servidor.
- Usar idempotency keys para checkout y escrituras repetibles.
- No guardar contraseñas propias en Android cuando se conecte Supabase Auth.
- No registrar tokens, contraseñas, teléfonos completos ni notas privadas.
- Reportar vulnerabilidades sin publicar datos de usuarios.

