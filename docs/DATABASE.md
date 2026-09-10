# Base de datos

El esquema se versiona en `supabase/migrations/` y se aplica en orden. Las tablas de negocio principales son `profiles`, `teams`, `team_members`, `invitations`, `campaigns`, `products`, `product_images`, `carts`, `cart_items`, `customers`, `addresses`, `orders`, `order_items`, `sync_events` y `crash_reports`.

Las relaciones incluyen siempre `team_id` cuando el dato pertenece a un equipo. `team_members` es la autoridad para roles. `order_items` conserva nombre, SKU y precio al confirmar para que el historial no cambie aunque el catálogo se actualice.

## Reglas operativas

- Aplicar RLS a cualquier tabla accesible desde cliente.
- Probar aislamiento con dos usuarios de equipos distintos.
- Usar funciones SQL con `search_path` seguro y permisos mínimos.
- No borrar tablas para reparar el historial de migraciones.
- El bucket `order-proofs` es privado; las políticas limitan inserción y lectura al prefijo del usuario autenticado.
- Las migraciones nuevas deben tolerar reintentos (`if exists`, `if not exists`, `on conflict`) cuando sea seguro.
