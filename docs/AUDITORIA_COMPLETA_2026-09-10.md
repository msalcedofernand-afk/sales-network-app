# Auditoría completa de Sales Network

Fecha: 2026-09-10  
Rama revisada: `develop`  
Alcance: Android, web, Supabase, CI/CD, seguridad, datos, UX/UI, accesibilidad, pruebas y documentación.

## Dictamen

El proyecto compila y tiene una arquitectura razonable para una beta cerrada, pero todavía no debe promoverse a estable. La principal brecha no es visual: varios flujos muestran una capacidad que aún no queda persistida o protegida de extremo a extremo. Los bloqueos principales son el estado remoto de las migraciones, el contrato de actualización, la equivalencia de roles, el flujo de cobro Android y la falta de pruebas integradas con dos equipos.

Estado recomendado: **beta técnica con datos de prueba**. No usar todavía para inventario o cobros reales.

## Verificaciones ejecutadas

| Verificación | Resultado |
|---|---|
| `npm run build` | Correcto; 15 rutas generadas |
| Android beta unit tests | Correcto |
| Android estable unit tests | Correcto |
| Android lint beta/estable | Termina correctamente, con 52 advertencias en beta |
| Playwright local | No ejecutado: falta instalar el binario Chromium local |
| Estado remoto de migraciones | No verificable sin `SUPABASE_ACCESS_TOKEN` |
| RLS con dos equipos reales | No ejecutado contra el proyecto remoto |

Las 52 advertencias Android se concentran en `DefaultLocale` (26), dependencias disponibles (11), escritura de preferencias (3), referencias estáticas a contexto (3), parámetros de `Modifier` (2), recursos sin usar (2) y cinco advertencias menores.

## Hallazgos bloqueantes

### P0-01 — El esquema remoto no está confirmado

Las migraciones `0011_order_proofs_storage.sql` y `0012_inventory_and_order_batches.sql` existen en Git, pero no se pudo comprobar que estén aplicadas en Supabase. Sin `0011`, subir comprobantes falla. Sin `0012`, no existe el control transaccional de inventario ni `order_batches`.

Acción: reconciliar el historial remoto, ejecutar `supabase db push` y verificar mediante consultas que existen el bucket, `stock_quantity`, `order_batches` y la versión vigente de `checkout_active_cart`.

### P0-02 — Los manifiestos versionados son inválidos para el actualizador

`updates/beta.json`, `updates/stable.json` y sus copias web contienen SHA-256 compuesto por ceros y `releasedAt: null`. `AppUpdateChecker.parseManifest` rechaza ambos valores, pero el workflow de validación solo comprueba el formato del hash y no rechaza los 64 ceros ni una fecha nula.

Impacto: CI puede quedar verde mientras la app ignora el manifiesto. La actualización funciona únicamente si el repositorio público ya contiene un manifiesto válido generado por el workflow.

Acción: validar hash no nulo, fecha ISO-8601, coherencia con el APK y versión extraída del artefacto.

## Seguridad y autorización

### P1-01 — `ROOT_ADMIN` no equivale a líder en todas las capas

- `is_team_leader` comprueba solo `role = 'LIDER'` en `0002_app_flow_policies.sql:16`.
- El proxy web permite `/admin` solo a `LIDER` en `web/proxy.ts:39`.
- `sync-catalog` permite importar solo a `LIDER` en `supabase/functions/sync-catalog/index.ts:22`.
- Otras funciones nuevas sí aceptan `ROOT_ADMIN`.

Impacto: un administrador raíz puede entrar en Android pero quedar bloqueado por RLS o por la web.

Acción: centralizar `is_team_leader` con `role::text in ('LIDER','ROOT_ADMIN')` y reutilizar el mismo criterio en web y funciones.

### P1-02 — La migración 0012 vuelve a debilitar `search_path`

`0010_security_definer_hardening.sql` fija `pg_catalog, public`, pero `0012_inventory_and_order_batches.sql:28` vuelve a crear `checkout_active_cart` con `set search_path = public`.

Impacto: regresión en una función `SECURITY DEFINER` sensible.

Acción: recrearla con `set search_path = ''` y referencias totalmente calificadas, o volver a aplicar el endurecimiento en una migración posterior.

### P1-03 — Las Edge Functions web no tienen manejo CORS común

`supabase/functions/_shared/http.ts` no añade cabeceras CORS y las funciones no atienden `OPTIONS`. Esto es compatible con el error observado de “Failed to send a request to the Edge Function” desde navegador.

Acción: crear un manejador común con allowlist para los dominios estable, beta y localhost de desarrollo; responder `OPTIONS` y reutilizarlo en todas las funciones invocadas por la web.

### P1-04 — Se devuelven mensajes internos de PostgreSQL

Varias funciones devuelven `error.message` directamente: `checkout-cart/index.ts:27`, `create-team/index.ts:11`, `create-invitation/index.ts:23` y `sync-catalog/index.ts:55`.

Impacto: exposición de nombres de funciones, restricciones o estructura interna; además produce mensajes poco claros.

Acción: mapear excepciones conocidas a códigos públicos y registrar el detalle solo del lado servidor.

### P2-01 — La clave publicable está embebida en Android

`SupabaseConfig.kt` contiene URL y clave publicable. Esto no es un secreto y es válido con RLS correcta, pero aumenta el impacto de cualquier política RLS defectuosa.

Acción: mantenerla como configuración de build y tratar RLS como frontera obligatoria. Nunca reemplazarla por `service_role`.

### P2-02 — Comprobantes huérfanos

Android sube la imagen primero y después cambia el estado. Si la transición falla, el objeto queda en Storage sin referencia.

Acción: añadir limpieza compensatoria o una Edge Function que firme/suba y confirme el pedido con una operación coordinada. Añadir retención para objetos sin referencia.

### Alineación Android Intent

El `FileProvider` está correctamente declarado con `exported=false`, autoridad por `applicationId`, permiso temporal de lectura y una ruta limitada. No se encontraron receivers, services o providers exportados adicionales. `MainActivity` es exportada únicamente como launcher y no procesa extras ni redirige intents recibidos.

## Datos, inventario y pedidos

### P1-05 — El inventario existe pero no se carga

`0012` añade `stock_quantity`, pero `sync-catalog` no acepta ni guarda ese campo; Android y web tampoco lo consultan. Para los productos existentes queda `NULL`, que significa inventario externo y evita el rechazo por falta de unidades.

Impacto: el bloqueo concurrente está bien diseñado, pero permanece inactivo en la operación normal.

Acción: incorporar `stock_quantity` al CSV/JSON, validarlo como entero no negativo, mostrar existencias y actualizarlo desde el panel administrativo.

### P1-06 — Pedidos multicliente incompletos

Existe `order_batches`, pero no hay función de checkout agrupado, inserción de `batch_id` ni UI para asignar líneas a varios clientes. El contrato continúa aceptando un solo `customer_id`.

Acción: crear `checkout_order_batch(jsonb, idempotency_key)`, bloquear productos en orden estable para evitar deadlocks, crear un pedido por cliente y descontar el total de unidades dentro de una transacción.

### P1-07 — “Cobrar” en Android modifica solo memoria

`SalesNetworkMainApp.kt:271` llama `registerPayment`; `OrderRepository.kt:101` cambia importe y estado únicamente en `_orders`. No persiste en Supabase, no exige comprobante y puede saltarse la transición protegida.

Acción: eliminar ese camino local. Todo cobro debe pasar por la función remota con método, importe y comprobante.

### P1-08 — “Eliminar pedido” en Android no elimina ni cancela en servidor

`OrderRepository.kt:46` solo retira el pedido de la lista en memoria. Reaparece al sincronizar. Además, un pedido comercial debería cancelarse y conservar auditoría.

Acción: sustituir “Eliminar” por “Cancelar”, exigir motivo y usar la transición remota.

### P1-09 — Ganancia estimada vuelve a calcularse en Android

`OrderListScreen.kt:206` calcula `totalCart * 0.35` y lo presenta como “Tu Ganancia”, aunque el modelo ya fue cambiado para aceptar comisiones del servidor.

Acción: mostrar “Comisión por calcular” antes del checkout y el valor de `commission_cents` después de confirmarlo.

### P2-03 — Reintentos de carrito Android pueden dejar líneas inconsistentes

`SupabaseCheckoutApi` escribe cada línea antes del checkout. Una falla intermedia puede dejar un carrito parcialmente actualizado. El header `resolution=merge-duplicates` tampoco garantiza incrementar o reemplazar de forma explícita todas las cantidades.

Acción: enviar el contenido deseado a una función `replace-cart-items` transaccional o usar un RPC que reemplace todas las líneas de una vez.

### P2-04 — Añadir desde la ficha web restablece cantidad

`web/app/catalogo/[slug]/page.tsx:9` hace upsert con `quantity: 1`. Si el artículo ya tenía 3 unidades, vuelve a 1.

Acción: usar el mismo incremento seguro que la página de catálogo o un RPC `increment_cart_item`.

### P2-05 — Líder web solo consulta sus pedidos

`web/app/pedidos/page.tsx:15-21` filtra por `user_id`. RLS ya permite al líder leer el equipo, pero la consulta impide mostrarlo.

Acción: consultar por `team_id` para líderes y por `user_id` para miembros, con selector de vendedora.

### P2-06 — Pedidos Android pierden datos al hidratarse

`SupabaseOrderApi.kt:62` usa “Cliente” como nombre fijo, no carga campaña, método de pago, importe abonado, motivos, comprobante ni sobrecomisión. Las campañas del ViewModel también están codificadas.

Acción: ampliar el select con relaciones y campos reales, y obtener campañas desde `campaigns`.

## Sesión y equipos

### P1-10 — Selección de equipo ambigua

Android y web usan `limit(1)` al consultar `team_members`. Un usuario con varias membresías obtiene un equipo no determinado.

Acción: decidir si una cuenta puede pertenecer a un solo equipo. Si sí, añadir restricción única por `user_id`; si no, guardar `active_team_id` y ofrecer selector.

### P2-07 — El proveedor de sesión web no es realmente único

Cada llamada a `getSessionContext` crea un nuevo browser client y vuelve a consultar usuario y membresía. Varias páginas repiten consultas y manejo de errores.

Acción: crear un `SessionProvider` con cliente estable, contexto de equipo y renovación coordinada. Usar hooks o consultas cacheadas para evitar cascadas.

### P2-08 — Logout y recuperación requieren pruebas reales

La sesión Android está cifrada con AES-GCM y Android Keystore, y la base Room se elimina al cerrar sesión. Falta demostrar restauración, refresh concurrente, correo verificado y recuperación mediante pruebas de integración.

## Catálogo e importación

### P1-11 — Galería todavía se reduce a una portada

La importación llena `product_images`, pero Android y web consultan solo `products.image_url`. La caché Room guarda una sola URL.

Acción: consultar `product_images` ordenado por `sort_order`, guardar la galería en caché y crear carrusel accesible en ambas plataformas.

### P1-12 — La importación no es totalmente atómica

El upsert de productos puede completarse y la actualización de imágenes fallar sin marcar el evento como error, porque no se comprueba el resultado de delete/insert de `product_images`.

Acción: mover importación a una función SQL transaccional o comprobar cada error y conservar el catálogo anterior en tablas staging antes de publicar.

### P2-09 — Fuente de sincronización fija

`sync_events.source` se guarda siempre como `viveoficial.com`, incluso para CSV o JSON administrativo.

Acción: usar valores controlados (`ADMIN_CSV`, `ADMIN_JSON`, `AUTHORIZED_FEED`) derivados del tipo real de importación.

### P2-10 — Disponibilidad inconsistente

Android elimina productos no disponibles en `SupabaseCatalogApi.kt:36`; web los muestra con estado. Por eso ambas plataformas no enseñan el mismo catálogo.

Acción: conservarlos en Android y deshabilitar la acción de añadir.

## Android UX, adaptación y rendimiento

### P1-13 — No hay interfaz adaptativa real para tablet

La app usa Compose, pero no Navigation 3, `NavigationSuiteScaffold`, `NavigationRail` ni escenas list-detail. La navegación y las pantallas son esencialmente de teléfono.

Acción: migrar navegación, añadir rail a partir del ancho expandido y usar dos paneles para catálogo/detalle, clientes/detalle y pedidos/detalle.

### P1-14 — Controles táctiles de 36 dp

Hay botones de cantidad, borrado y edición con `Modifier.size(36.dp)` en `OrderListScreen.kt:248`, `OrderListScreen.kt:252`, `OrderListScreen.kt:399` y `CustomerListScreen.kt:192`.

Impacto: objetivo táctil inferior a los 48 dp recomendados.

Acción: mantener icono visual pequeño dentro de un área interactiva mínima de 48 dp.

### P1-15 — Formularios de pedido recortan información

`OrderListScreen` usa `availableCustomers.take(4)` y `availableProducts.take(4)`. El quinto cliente o producto no puede seleccionarse.

Acción: búsqueda, lista paginada y selector completo; en tablet, panel lateral.

### P2-11 — Observación de estado no lifecycle-aware

`SalesNetworkMainApp.kt:65-69` usa `collectAsState()` para cinco flujos.

Acción: usar `collectAsStateWithLifecycle()` para detener observación cuando la actividad no está activa.

### P2-12 — Doble sincronización periódica

`CatalogViewModel` ejecuta un bucle cada 30 minutos y WorkManager programa el mismo intervalo. Puede duplicar solicitudes cuando la app está abierta.

Acción: dejar WorkManager para segundo plano y una actualización al entrar o mediante pull-to-refresh en primer plano.

### P2-13 — Caché offline incompleta

Room solo contiene `CachedProduct`. Clientes, pedidos y líneas de pedido no tienen entidades locales aunque la documentación afirma caché de consulta para todos.

Acción: añadir entidades aisladas por usuario/equipo, transacciones de reemplazo y fecha de última sincronización.

### P3-01 — Localización y formato

Lint reporta 26 usos de formato dependiente del locale. También hay textos sin tildes y fechas/campañas codificadas.

Acción: usar recursos de texto, `Locale("es", "PE")`, formateadores monetarios y fechas reales.

## Web UX, accesibilidad y rendimiento

### P1-16 — Gestión de comprobantes mediante `window.prompt`

`web/app/pedidos/page.tsx:31-39` solicita texto como supuesto comprobante. No sube una imagen, no comprime y no presenta vista previa.

Acción: selector de archivo accesible, compresión, progreso, validación y subida privada igual que Android.

### P2-14 — Imágenes sin dimensiones en detalle

`web/app/catalogo/[slug]/page.tsx:9` renderiza `<img>` sin `width`/`height`; puede causar salto de layout. También se usan `<img>` normales en lugar de `next/image`.

Acción: reservar relación de aspecto y usar `Image` con dominio exacto o loader controlado.

### P2-15 — Filtros fuera de la URL

Búsqueda y categoría del catálogo viven solo en `useState`; recargar o compartir la URL pierde el estado.

Acción: sincronizar `q` y `category` con search params.

### P2-16 — Formularios incompletos para navegador y accesibilidad

Varios campos no tienen `name`, `spellCheck={false}` donde corresponde ni errores inline asociados. Los placeholders no muestran ejemplo y no terminan en elipsis. La búsqueda de clientes usa un `aria-label`, lo cual es válido, pero no conserva el filtro en URL.

Acción: normalizar controles mediante componentes de formulario y enfocar el primer error.

### P2-17 — Código de páginas difícil de mantener

Varias páginas están comprimidas en una sola línea y mezclan fetch, reglas de negocio y JSX. Esto dificulta revisión, tests y memoización.

Acción: dividir en hooks de datos, componentes y estados comunes. Introducir deduplicación/caché de solicitudes.

### P2-18 — Navegación muestra conexión sin comprobarla

`AppNavigation` presenta “Conectado a tu equipo” de forma estática. No representa sesión, red ni última sincronización.

Acción: mostrar estado real o cambiar el texto por uno neutral.

### P3-02 — CSS sólido con detalles pendientes

Son correctos el foco visible, reduced motion, safe area inferior, objetivos de 48 px, tablas desplazables, estados vacíos y navegación responsive. Faltan `touch-action: manipulation`, `overscroll-behavior` para paneles, `text-wrap` en títulos y safe area superior en cabecera.

## Pruebas y CI/CD

### P1-17 — La cobertura funcional es muy baja

Playwright solo tiene dos smoke tests públicos. No prueba autenticación real, onboarding, invitaciones, carrito, checkout, permisos de líder, comprobantes ni dos equipos. Android tiene pruebas unitarias pequeñas y no tiene instrumentación, screenshots ni pruebas de Room cifrado.

Acción: crear fixtures de Supabase para dos equipos, Page Objects y pruebas de los flujos críticos. Añadir pruebas Android del repositorio, sesión, WorkManager y pantallas principales.

### P1-18 — El test RLS no se ejecuta en CI

Existe `supabase/tests/rls_smoke.sql`, pero `ci.yml` solo compila Android/web y ejecuta Playwright.

Acción: levantar Supabase local en CI o ejecutar una base temporal; fallar ante cualquier fila cruzada.

### P2-19 — Playwright local no está listo

La suite falló porque falta `chromium_headless_shell`. CI sí instala Chromium antes de ejecutarla.

Acción local: `npx playwright install chromium`. No es un defecto del código, pero impide validación visual inmediata.

### P2-20 — Release sin prueba funcional previa

El workflow de publicación se activa al hacer push a `beta` o `main` y compila un APK firmado, pero no depende explícitamente del job de CI del mismo commit.

Acción: usar workflow reusable o `workflow_run` y publicar solo después de build, lint, tests y validación de manifests.

## Fortalezas confirmadas

- Separación clara entre Android, web y Supabase.
- RLS activada en las tablas principales.
- Checkout idempotente y con bloqueo de carrito.
- La migración de inventario usa bloqueo de filas y evita sobreventa cuando existe `stock_quantity`.
- Tokens Android cifrados con Android Keystore.
- Room usa SQLCipher y elimina la base al cerrar sesión.
- El actualizador valida canal, host, ruta, SHA-256, package ID y firma instalada.
- APK beta y estable usan identificadores separados.
- Web compila con Next.js y protege rutas mediante proxy.
- Diseño web consistente, responsive, con foco visible y reduced motion.
- Las imágenes de comprobante Android se comprimen y eliminan EXIF antes de subir.
- GitHub Actions compila y valida ambos sabores Android.

## Plan de corrección recomendado

### Fase 1 — Bloqueos de seguridad y operación

1. Reconciliar y aplicar migraciones remotas.
2. Crear migración correctiva para `ROOT_ADMIN` y `search_path`.
3. Añadir CORS con allowlist y errores públicos tipados.
4. Reparar contrato y validación de manifests.
5. Eliminar cobro y borrado local de pedidos Android.

### Fase 2 — Flujo comercial íntegro

1. Importar y mostrar `stock_quantity`.
2. Implementar checkout agrupado multicliente.
3. Completar hidratación de pedidos, campañas, pagos y clientes.
4. Implementar comprobantes web y limpieza de objetos huérfanos.
5. Hacer atómica la importación del catálogo.

### Fase 3 — UX adaptativa y consistencia

1. Sustituir listas recortadas por búsqueda y selección completa.
2. Añadir navegación Android para tablet y paneles list-detail.
3. Elevar todos los objetivos táctiles a 48 dp.
4. Unificar galería, disponibilidad, importes y estados entre web y Android.
5. Refactorizar páginas web y proveedor de sesión.

### Fase 4 — Pruebas y salida estable

1. Automatizar RLS con dos equipos.
2. Completar Playwright autenticado.
3. Añadir pruebas Android de integración, WorkManager y screenshots.
4. Hacer que publicación dependa de CI verde.
5. Validar beta con usuarios reales antes de promover a `main`.

## Criterio para aprobar estable

La versión estable puede aprobarse cuando no existan P0, todos los P1 estén corregidos o explícitamente aceptados, las migraciones estén verificadas, RLS pase con dos equipos, el flujo completo funcione en web y Android, y el APK firmado sea descargable y actualizable desde una instalación previa.
