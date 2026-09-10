# Guía completa del proyecto

Sales Network es una plataforma para equipos de venta. Android y la web oficial (`web/`) comparten Supabase Auth, PostgreSQL/RLS, Storage y Edge Functions. Vercel aloja Next.js; GitHub conserva el código y automatiza validaciones y publicaciones.

## Flujo principal

1. La persona se registra o inicia sesión con Supabase Auth.
2. Crea un equipo o acepta una invitación.
3. Consulta el catálogo de productos y campañas autorizadas.
4. Selecciona un cliente y prepara el carrito.
5. `checkout-cart` valida disponibilidad, precios e idempotencia y crea el pedido.
6. El pedido avanza por `PENDIENTE`, `CONFIRMADO`, `COBRADO`, `ENTREGADO` o `CANCELADO`.
7. Al cobrar, Android puede adjuntar un comprobante; la imagen se comprime y se guarda en Storage privado.

## Componentes

### Android

`Compose → ViewModel → Repository → Room/cache → Supabase`. La interfaz adapta navegación inferior en móvil y rail o paneles en pantallas anchas. El caché permite consulta offline; las escrituras sensibles requieren conexión.

### Web

Next.js App Router contiene login, onboarding, catálogo, carrito, pedidos, clientes, equipo, privacidad y cambios. `web/proxy.ts` renueva cookies y protege rutas. Los componentes visuales siguen los tokens de `docs/UI_UX_GUIA.md`.

### Supabase

Las migraciones en `supabase/migrations/` definen tablas, RLS, funciones SQL, Storage y cron. Las funciones en `supabase/functions/` son endpoints server-side; las operaciones administrativas no se ejecutan desde el cliente.

## Versiones y canales

`version.properties` es la fuente manual de versión. `beta` usa el application ID beta y `main` el estable. Un build no debe modificar la versión. Los manifests y APK se publican mediante GitHub Actions en el repositorio de releases.

## Operación diaria

- Revisar el último despliegue de Vercel y el workflow de GitHub.
- Revisar `sync_events` y errores de funciones después de importar catálogo.
- Probar login, RLS, carrito y checkout con dos equipos de prueba.
- Revisar reportes de crash sin descargar datos personales innecesarios.
- Promover beta a `main` solo después de aceptación.

## Limitaciones conocidas

La aplicación todavía requiere configurar credenciales de despliegue para aplicar migraciones remotas y firmar APK release. Los proveedores externos de scraping, rutas, mensajería y pagos no se activan hasta contar con una integración autorizada. La distribución pública de APK debe usar HTTPS y la misma clave de firma en cada canal.
