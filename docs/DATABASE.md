# Base de datos

El esquema se versiona en `supabase/migrations/` y se aplica en orden. Las tablas de negocio principales son `profiles`, `teams`, `team_members`, `invitations`, `campaigns`, `products`, `product_images`, `carts`, `cart_items`, `customers`, `addresses`, `orders`, `order_items`, `order_status_events`, `inventory_movements`, `sync_events` y `crash_reports`.

Las relaciones incluyen siempre `team_id` cuando el dato pertenece a un equipo. `team_members` es la autoridad para roles. `order_items` conserva nombre, SKU y precio al confirmar para que el historial no cambie aunque el catálogo se actualice.

## Reglas operativas

- Aplicar RLS a cualquier tabla accesible desde cliente.
- Probar aislamiento con dos usuarios de equipos distintos.
- Usar funciones SQL con `search_path` seguro y permisos mínimos.
- No borrar tablas para reparar el historial de migraciones.
- El bucket `order-proofs` es privado; las políticas limitan inserción y lectura al prefijo del usuario autenticado.
- Las migraciones nuevas deben tolerar reintentos (`if exists`, `if not exists`, `on conflict`) cuando sea seguro.
- Durante la versión 1.2 una cuenta pertenece a un solo equipo. `team_members(user_id)` lo garantiza y evita seleccionar un equipo al azar.
- `inventory_movements` registra cada descuento y reposición de stock con una clave idempotente.
- `order_status_events` conserva el historial de transiciones; los pedidos comerciales no se eliminan.
- El flujo permitido es `PENDIENTE → CONFIRMADO → COBRADO → ENTREGADO`; antes de entregar se puede cancelar y después de entregar se puede registrar una devolución.
- Los comprobantes viven en el bucket privado `order-proofs`; una transición solo puede vincular un objeto existente subido por el usuario autenticado.
- `NULL` en `products.stock_quantity` significa inventario administrado externamente. Un entero activa el control transaccional.
