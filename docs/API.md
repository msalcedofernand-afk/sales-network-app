# API

Las Edge Functions aceptan JSON y devuelven `{ error: string }` en fallos. Las funciones protegidas exigen `Authorization: Bearer <supabase_access_token>`.

Implementadas como base:

- `create-invitation`: líder → `{ team_id, expires_at, max_uses }`.
- `create-team`: usuario autenticado → `{ name }`.
- `accept-invitation`: usuario → `{ code }`.
- `calculate-cart`: usuario → `{ cart_id }`.
- `checkout-cart`: usuario → `{ cart_id, customer_id, idempotency_key }`.

Los conectores de catálogo, rutas y mensajería devuelven `501` hasta configurar un proveedor autorizado. No se deben activar con datos falsos.
