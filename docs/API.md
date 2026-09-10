# API

Las Edge Functions aceptan JSON y devuelven `{ error: string }` en fallos. Las funciones protegidas exigen `Authorization: Bearer <supabase_access_token>`.

Las funciones llamadas desde navegador responden preflight CORS y aceptan únicamente los orígenes configurados en `WEB_ALLOWED_ORIGINS`, localhost de desarrollo y previews reconocidos del proyecto Vercel. Los errores públicos usan códigos estables y no incluyen mensajes internos de PostgreSQL.

Implementadas como base:

- `create-invitation`: líder → `{ team_id, expires_at, max_uses }`.
- `create-team`: usuario autenticado → `{ name }`.
- `accept-invitation`: usuario → `{ code }`.
- `calculate-cart`: usuario → `{ cart_id }`.
- `checkout-cart`: usuario → `{ cart_id, customer_id, idempotency_key }`.
- `transition_order_status_v2`: usuario autorizado → estado, motivo, pago y evidencia. Registra el historial y repone stock al cancelar o devolver.
- `get_or_create_active_cart`: devuelve un único carrito activo sin carreras entre dispositivos.
- `increment_cart_item`: incrementa o reduce una línea comprobando equipo, disponibilidad y stock.
- `replace_cart_items`: reemplaza todas las líneas dentro de una sola transacción; Android lo usa antes del checkout.
- `publish_campaign_atomic`: publica una campaña y despublica las demás en una sola operación.

Funciones adicionales:

- `publish-campaign`: líder → publica una campaña validada.
- `report-crash`: usuario autenticado → reporte sanitizado de versión y excepción.
- `calculate-route`: usuario → calcula una ruta solo con proveedor configurado.
- `send-order-summary`: usuario → genera o envía un resumen del pedido.
- `sync-catalog`: líder o tarea autorizada → importa un feed validado e idempotente.

Las imágenes de comprobantes se suben al bucket privado `order-proofs` con una ruta `<user_id>/<order_id>-<uuid>.jpg`. El servidor recibe únicamente la ruta resultante al cambiar el pedido de estado; nunca acepta precios ni totales del cliente.

Estados admitidos: `PENDIENTE → CONFIRMADO → COBRADO → ENTREGADO`. Un pedido previo a entrega puede pasar a `CANCELADO`; uno entregado puede pasar a `DEVUELTO`. Cancelaciones y devoluciones requieren motivo.

Los conectores de catálogo, rutas y mensajería devuelven `501` hasta configurar un proveedor autorizado. No se deben activar con datos falsos.
