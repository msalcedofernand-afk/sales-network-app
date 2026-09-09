# REGISTRO_CAMBIOS_AVON.md - Historial de Cambios del Proyecto VV Lideres Chiclayo

## 2026-09-09 - UX Profesional + API Key + Versionado Semántico (v1.1.0)

### Que se hizo
1. **UX Redesign Completo**:
   - Tokens centralizados en `Design.kt` (S, C, B, SH)
   - Componenets reutilizables: KpiCard, StatusBadge, EmptyState, SectionHeader, SectionIntro
   - Nav bar animada con `animateColorAsState`/`animateDpAsState`
   - Inputs redondeados, header con gradiente en login
   - KPIs interactivos en pedidos (click para detalle)
   - Empty states con icono + acción en todas las pantallas

2. **API Key Supabase Actualizada**:
   - Reemplazada key inválida por nueva key pública
   - Login funciona correctamente

3. **Versionado Semántico Implementado**:
   - `versionName`: 1.0.0 → 1.1.0 (nuevas features)
   - `versionCode`: 35 (stable), 36 (beta)
   - Reglas documentadas en `AGENTS.md`

4. **Compilación e Instalación**:
   - `assembleStableDebug` + `assembleBetaDebug` exitosos
   - Ambas APKs instaladas vía `adb install -r`
   - Releases actualizados en repo de releases

### Archivos modificados
- `app/src/main/java/com/salesnetwork/avon/app/ui/Design.kt`
- `app/src/main/java/com/salesnetwork/avon/app/ui/SalesNetworkMainApp.kt`
- `app/src/main/java/com/salesnetwork/avon/app/ui/auth/LoginRegisterScreen.kt`
- `app/src/main/java/com/salesnetwork/avon/app/ui/catalog/CatalogScreen.kt`
- `app/src/main/java/com/salesnetwork/avon/app/ui/customer/CustomerListScreen.kt`
- `app/src/main/java/com/salesnetwork/avon/app/ui/network/TeamNetworkScreen.kt`
- `app/src/main/java/com/salesnetwork/avon/app/ui/order/OrderListScreen.kt`
- `app/src/main/java/com/salesnetwork/avon/app/data/LeaderNetworkRepository.kt`
- `web/public/updates/stable.json`
- `web/public/updates/beta.json`
- `version.properties`
- `AGENTS.md`
- `REGISTRO_CAMBIOS_AVON.md`

### Resultado del build
- BUILD SUCCESSFUL (stable v1.1.0 code=35, beta v1.1.0 code=36)
- Instalación exitosa en dispositivo 7c13e912

---

## 2026-09-08 - Reemplazo de Marca a "VV" & Rol Root Admin Total

### Que se hizo
1. **Rebranding Completo a "VV"**:
   - Reemplazada la marca "Avon" por "VV" en toda la interfaz de usuario, strings, catalogos y codigos de referido (ej. `VV-2026`, `VV-${(1000..9999).random()}`, "VV Lideres Chiclayo", "Catalogo Oficial VV").
2. **Rol y Cuenta Root Admin Total**:
   - Agregado `UserRole.ROOT_ADMIN` al modelo de dominio `User.kt`.
   - Creado usuario maestro Root Admin (`root@vv.com`, clave: `RootAdmin2026!`) con control y visibilidad global de todas las redes, lideres, miembros y pedidos.
3. **Barra Flotante Inferior (Solo Iconos)**:
   - Capsula flotante con elevacion 12dp en la parte inferior central de la pantalla, solo iconos.
4. **Validacion Obligatoria de Codigo de Red**:
   - Obligatorio para registrarse como vendedor en la red.

### Archivos modificados
- `res/values/strings.xml`
- `domain/model/User.kt`
- `data/LeaderNetworkRepository.kt`
- `data/ProductCatalogRepository.kt`
- `data/OrderRepository.kt`
- `data/CustomerRepository.kt`
- `scraper/CatalogScraperEngine.kt`
- `ui/auth/LoginRegisterScreen.kt`
- `ui/catalog/CatalogScreen.kt`
- `ui/network/TeamNetworkScreen.kt`
- `ui/order/OrderListScreen.kt`
- `ui/viewmodel/OrderViewModel.kt`
- `SalesNetworkModuleTest.kt`
- `REGISTRO_CAMBIOS_AVON.md`

### Resultado del Build y Prueba
- `.\gradlew.bat testDebugUnitTest` -> BUILD SUCCESSFUL (100% OK)
- `.\gradlew.bat assembleDebug` -> BUILD SUCCESSFUL
- `adb install -r app/build/outputs/apk/debug/app-debug.apk` -> Success
- Aplicacion iniciada exitosamente en `emulator-5554`.
- **Supervision Super Admin verificada visualmente en emulador**:
  - Panel Maestro con indicadores globales (Líderes Activas: 3, Total Vendedoras: 4, Facturación: S/ 5,239.60).
  - Desglose interactivo por cada líder expandible mostrando el listado de vendedoras de su red y estado (Activa/Pendiente).
  - Chips de acceso directo en pantalla de autenticación para conmutación rápida entre Root Admin y Líder.
  - Barra de navegación flotante tipo cápsula de solo iconos funcionando fluidamente.


## 2026-09-08 - Rediseño Android, build 25

- Identidad visual petróleo y verde, superficies neutras y encabezados compartidos.
- Navegación flotante con nombres para identificar cada destino.
- Catálogo con imágenes disponibles, columnas adaptables y categorías derivadas de los productos.
- Clientes y pedidos con nueva jerarquía visual. Formularios desplazables; selector compacto de zona y teclado telefónico. Guardar cliente requiere nombre y al menos siete dígitos.
- Controles de cantidad de 48 dp. Eliminados márgenes de sistema duplicados en pantallas anidadas.
- Eliminado aviso fijo de cierre en tres días y meta fija que no provenían de datos de campaña.
- Archivos: MainActivity.kt, ui/Design.kt, SalesNetworkMainApp.kt y pantallas auth/catalog/customer/network/order.
- Validación: assembleDebug y testDebugUnitTest correctos; instalación adb install -r correcta en emulator-5554. APK releases/sales-network-redesign-debug.apk, versionCode 25.
- Alcance: rediseño Android. No certifica autenticación remota ni sincronización del catálogo. Persisten datos demo antiguos con algunos textos mal codificados. Pendiente validación completa en tablet, letra grande y todos los flujos de producción.

## 2026-09-08 - Unificación web y flujos Supabase

- Web oficial web/ alineada visualmente con Android: azul petróleo, verde menta, tarjetas, estados y responsive móvil.
- Navegación web ampliada a catálogo, carrito, pedidos, clientes y equipo.
- Catálogo y ficha conectados a productos de Supabase; se añadió acción para añadir productos al carrito.
- Carrito persistente por usuario/equipo con cantidades y total.
- Clientes con alta, búsqueda, WhatsApp y archivado mediante RLS.
- Pedidos con lectura de líneas, estados y acciones de confirmación/cobro/cancelación.
- Equipo con miembros e invitaciones mediante Edge Function.
- Panel de importación JSON para sync-catalog.
- Migración supabase/migrations/0002_app_flow_policies.sql con políticas de inserción/actualización para checkout.
- publish-campaign dejó de ser placeholder y valida el rol de líder.
- Documentación visual actualizada en docs/UI_UX_GUIA.md.
- Validación: web npm run build correcto; Android testDebugUnitTest y assembleDebug correctos, APK build 26 generada y copiada a releases/sales-network-redesign-debug.apk. La instalación final quedó pendiente porque el emulador ADB apareció en estado recovery. La migración de repositorios Android locales a Supabase requiere una siguiente iteración.

## 2026-09-08 - Sistema premium compartido y ruta de venta

- Aplicadas las guías `ui-ux-design-pro` y `ui-ux-pro-max` al sistema compartido.
- Documentada la fuente de verdad en `.interface-design/system.md` con intención, paleta, profundidad, tipografía, espaciado, firma y reglas de aceptación.
- Nueva navegación web con destino activo, iconografía vectorial consistente, barra lateral en escritorio y barra inferior segura en móvil.
- Login y registro web rediseñados con narrativa comercial, formularios claros, mensajes comprensibles, recuperación de contraseña y atributos de accesibilidad.
- Tokens web renombrados según el mundo de Sales Network: petróleo, menta, papel, tinta y estados semánticos.
- Añadido soporte para foco visible, contraste reforzado, Windows High Contrast y movimiento reducido.
- Android centraliza colores, radios y espaciado en `SalesDesignTokens`; la cabecera incorpora la firma visual de ruta de venta.
- La navegación Android anuncia el destino seleccionado a tecnologías de asistencia y usa profundidad basada en superficies/bordes.
- Validación: `npm run build`, `testDebugUnitTest` y `assembleDebug` correctos. APK de prueba actualizado a versionCode 28.
- Actualizador: el APK generado contiene el detector de versiones; el manifiesto estable apunta a versionCode 28 y beta a 29.
- Beta: generado APK versionCode 29 para validar el aviso de actualización desde la rama `beta`; `main` conserva el APK estable 28.
## 2026-09-09 — Sesión Android protegida

- **Qué:** Se cifró el token de acceso de Supabase con Android Keystore y AES/GCM.
- **Por qué:** Evitar guardar credenciales de sesión en texto plano en SharedPreferences.
- **Archivos:** `SecureTokenStore.kt`, `LeaderNetworkRepository.kt` y clientes Supabase de catálogo, clientes, carrito y pedidos.
- **Resultado:** La sesión se guarda, lee y revoca desde almacenamiento cifrado; validado con el build Android.
## 2026-09-09 — Checkout idempotente en Android

- **Qué:** La clave de checkout queda guardada hasta que Supabase confirma el pedido.
- **Por qué:** Un reintento después de perder la respuesta de red no debe duplicar la venta.
- **Archivos:** `SupabaseCheckoutApi.kt`.
- **Resultado:** Reintentos del mismo carrito reutilizan la clave y el servidor mantiene la operación idempotente.
## 2026-09-09 — ETA sin estimaciones inventadas

- **Qué:** La app ya no muestra una distancia o tiempo calculado si el proveedor de rutas no responde.
- **Por qué:** Evitar presentar una distancia en línea recta como si fuera una ruta real.
- **Archivos:** `RouteEtaService.kt`, `SalesNetworkModuleTest.kt`.
- **Resultado:** El cliente aparece como “Sin calcular” hasta tener una ruta confirmada.
## 2026-09-09 — Clientes web

- **Qué:** Se añadió edición de clientes, confirmación antes de archivar y acciones para llamar o abrir WhatsApp.
- **Por qué:** Completar el flujo de gestión de clientes y evitar archivados accidentales.
- **Archivos:** `web/app/clientes/page.tsx`.
- **Resultado:** Build de Next.js correcto y cambios publicados en `develop`.
## 2026-09-09 — Equipo sin cifras demo

- **Qué:** Se eliminaron ventas y comisiones de ejemplo que aparecían cuando no había pedidos sincronizados.
- **Por qué:** Los indicadores deben representar únicamente datos reales de Supabase.
- **Archivos:** `TeamViewModel.kt`.
- **Resultado:** Sin pedidos, los indicadores muestran cero hasta completar la sincronización.
## 2026-09-09 — Automatización de despliegue Supabase

- **Qué:** Se añadió `scripts/deploy-supabase.ps1` y la guía de puesta en marcha remota.
- **Por qué:** Aplicar migraciones y Edge Functions de forma repetible sin exponer secretos.
- **Archivos:** `scripts/deploy-supabase.ps1`, `docs/DEPLOYMENT.md`.
- **Resultado:** El despliegue queda listo para ejecutarse cuando estén disponibles el token y la contraseña del proyecto.
## 2026-09-09 — Pruebas web y RLS

- **Qué:** Se añadió Playwright con pruebas de login y catálogo, integración en CI y un smoke test SQL para aislamiento RLS.
- **Por qué:** Validar accesibilidad básica y detectar fugas entre equipos antes de publicar beta.
- **Archivos:** `web/playwright.config.ts`, `web/tests/public-pages.spec.ts`, `.github/workflows/ci.yml`, `supabase/tests/rls_smoke.sql`.
- **Resultado:** Suite preparada; la descarga del navegador Playwright queda para CI o una máquina con acceso al binario.
## 2026-09-09 — Migración Supabase reintentable

- **Qué:** Las definiciones de enums de la migración inicial toleran tipos que ya existan en el proyecto remoto.
- **Por qué:** El primer `supabase db push` se detuvo porque `team_role` ya estaba creado.
- **Archivos:** `supabase/migrations/0001_sales_network.sql`.
- **Resultado:** El despliegue puede reintentarse sin fallar por tipos duplicados.
## 2026-09-09 — Despliegue remoto con historial seguro

- **Qué:** El script ahora se detiene cuando `db push` falla y la guía documenta cómo reparar el historial de migraciones existentes.
- **Por qué:** El proyecto remoto ya contiene tablas; no se deben recrear ni borrar datos.
- **Archivos:** `scripts/deploy-supabase.ps1`, `docs/DEPLOYMENT.md`.
- **Resultado:** Las Edge Functions no se publican si la base de datos no está sincronizada y el siguiente paso queda explícito.
## 2026-09-09 — Corrección de smoke test web

- **Qué:** El smoke test de login ahora busca el texto real del botón `Ingresar`.
- **Por qué:** CI fallaba aunque la pantalla funcionaba correctamente.
- **Archivos:** `web/tests/public-pages.spec.ts`.
- **Resultado:** La prueba coincide con la interfaz actual y el Pull Request se actualizará automáticamente.
## 2026-09-09 — Estado real del README

- **Qué:** Se actualizó el estado del proyecto para reflejar la integración remota real de Android, web y Supabase.
- **Por qué:** Evitar que la documentación diga que la app sigue usando datos demo locales.
- **Archivos:** `README.md`.
- **Resultado:** Se documentan también Room/offline, firma de APK y validación beta como pendientes.
## 2026-09-09 — Descarga de APK desde la web

- **Qué:** Se añadió un botón visible para descargar la APK beta desde la navegación web.
- **Por qué:** Facilitar que las vendedoras instalen la aplicación Android desde el catálogo.
- **Archivos:** `web/app/components/AppNavigation.tsx`, `web/app/globals.css`.
- **Resultado:** El botón apunta al artefacto público beta de GitHub Releases y funciona en móvil, escritorio y login.
