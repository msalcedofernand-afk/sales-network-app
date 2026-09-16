# REGISTRO_CAMBIOS_AVON.md - Historial de Cambios del Proyecto VV Lideres Chiclayo

## 2026-09-16 — Beta personal con APK debug

- **Qué:** la publicación de la rama `beta` genera `assembleBetaDebug` y no requiere keystore de producción; `main` conserva el flujo release firmado.
- **Por qué:** la beta se usa para desarrollo personal y no necesita distribución de producción.
- **Archivos:** `.github/workflows/publish-release-repo.yml`.
- **Resultado:** pendiente de confirmar la nueva ejecución y descarga del APK debug.

## 2026-09-16 — Corrección del flujo remoto de generación APK

- **Qué:** los workflows usan el comando `gradle` disponible en GitHub Actions y se sincronizó el manifiesto beta publicado en web.
- **Por qué:** la ejecución anterior no pudo compilar porque el repositorio no tenía `gradlew`, y la validación de manifiestos encontró una copia beta desactualizada.
- **Archivos:** `.github/workflows/ci.yml`, `.github/workflows/publish-release-repo.yml`, `web/public/updates/beta.json`.
- **Resultado:** pendiente de confirmar la nueva ejecución remota.

## 2026-09-16 — APK beta identificable por versión y código

- **Qué:** el workflow de publicación conserva cada APK con nombre `sales-network-beta-v<versión>-code<código>.apk`, mantiene el alias de descarga actual y apunta el manifiesto al archivo histórico con su código.
- **Por qué:** identificar exactamente qué build se instaló y evitar que las publicaciones nuevas oculten el APK anterior.
- **Archivos:** `.github/workflows/publish-release-repo.yml`, `scripts/update-version-json.ps1`, `README.md`.
- **Resultado:** pendiente de generar la primera publicación con el nuevo nombre.

## 2026-09-16 — Dev Inspector disponible en login Android

- **Qué:** se añadieron objetivos inspeccionables al formulario de login (tarjeta, logo, título, campos, selector de rol y botones), y el detector táctil del inspector ahora solo se activa cuando el modo está encendido.
- **Por qué:** el modo de inspección no permitía seleccionar componentes en el inicio de sesión y podía interceptar gestos normales cuando estaba apagado.
- **Archivos:** `app/src/main/java/com/salesnetwork/avon/app/inspector/DevInspector.kt`, `app/src/main/java/com/salesnetwork/avon/app/ui/auth/LoginRegisterScreen.kt`.
- **Resultado:** pendiente de validar con build Android.

## 2026-09-15 — APK beta descargable desde este repositorio

- **Qué:** se publicará `releases/sales-network-beta.apk` en la rama `beta` y se actualizarán el README y el manifiesto beta para descargarlo desde este repositorio.
- **Por qué:** el repositorio externo de releases permite lectura, pero rechazó el push por falta de permisos de escritura.
- **Versión:** `1.2.0-beta.1`, código `45`, paquete `com.salesnetwork.avon.app.beta`.
- **SHA-256:** `9922edef6af038d1db4d22d4b6bb8dd29b6274e1ecdaa0d224d1bdbba9289680`.
- **Resultado:** pendiente de completar el push de la APK.

## 2026-09-15 — Descarga directa de APK beta en README

- **Qué:** se añadió al README un enlace público para descargar directamente la última APK beta desde el repositorio de releases.
- **Por qué:** facilitar la instalación de la beta y el acceso al Dev Inspector sin buscar manualmente el artefacto.
- **Archivos:** `README.md` y `REGISTRO_CAMBIOS_AVON.md`.
- **Resultado:** documentación actualizada; el enlace depende de que el workflow de publicación haya subido la APK al repositorio de releases.

## 2026-09-15 — Dev Inspector web para revisión beta

- **Qué:** se añadió un inspector local para builds de desarrollo/beta que permite activar selección de componentes, consultar ruta, ID, tipo, dimensiones, contenido y nombre accesible, capturar evidencia, registrar incidencias JSONL y exportar un ZIP.
- **Por qué:** facilitar la revisión pantalla por pantalla y dejar evidencia reproducible antes y después de corregir problemas visuales, funcionales y de accesibilidad.
- **Seguridad:** el inspector se habilita solo con `NEXT_PUBLIC_ENABLE_DEV_INSPECTOR=true` o durante desarrollo; las capturas permanecen en el navegador y no se envían automáticamente a Supabase. Se limita la información a datos sanitizados del DOM.
- **Archivos:** `web/app/components/DevInspector.tsx`, `web/app/layout.tsx`, `web/app/catalogo/page.tsx`, `web/app/globals.css`, `web/package.json`, `web/package-lock.json`, `.env.example`.
- **Resultado:** `npm run build` correcto en `web/`.

## 2026-09-15 — Base Android del Dev Inspector

- **Qué:** se añadió un inspector local para builds debug Android con selección por pulsación larga de la pantalla, captura de evidencia, JSONL local y exportación ZIP mediante `FileProvider`.
- **Seguridad:** está protegido por `BuildConfig.DEBUG`; no se activa en release ni envía información automáticamente.
- **Archivos:** `app/src/main/java/com/salesnetwork/avon/app/inspector/DevInspector.kt`, `MainActivity.kt`, `SalesNetworkMainApp.kt`, `app/src/main/res/xml/update_file_paths.xml`.
- **Resultado:** pendiente de validar con Gradle Android en este entorno.
- **Corrección:** las rutas de capturas declaradas en JSONL ahora coinciden con los archivos incluidos en la exportación ZIP.
- **Ampliación Android:** el panel debug permite activar/desactivar el modo inspector, seleccionar tarjetas, nombres, precios y navegación, resaltar el área seleccionada y completar tipo, prioridad, título y descripción antes de guardar.
- **Build:** APK `beta debug` generada correctamente con Android SDK API 35 y JDK 17.

## 2026-09-10 — Pedidos auditables con comprobantes y devoluciones

- **Qué:** web y Android usan `transition_order_status_v2`; se añadieron cobro, método, comprobante comprimido, evidencia opcional de entrega, cancelación y devolución con motivo.
- **Seguridad:** Storage comprueba propiedad, permite limpiar archivos temporales y la base rechaza rutas que no existan. Un fallo posterior a la subida elimina el objeto huérfano.
- **Consistencia:** se retiraron de Android las acciones de cobro y eliminación que solo modificaban memoria; las transiciones permitidas viven en un único modelo probado.
- **Resultado:** build web correcto y pruebas/lint Android beta correctos.

## 2026-09-10 — Catálogo y carrito iguales en Android y web

- **Qué:** web y Android consumen stock y galería desde Supabase; el carrito usa RPC transaccionales para crearlo, incrementar y reemplazar líneas; la campaña se publica atómicamente.
- **Por qué:** evitar cantidades reiniciadas, escrituras parciales, carreras entre dispositivos y diferencias de disponibilidad entre plataformas.
- **Archivos:** migración `0015`, catálogo/carrito web, catálogo/checkout Android, caché Room y `publish-campaign`.
- **Resultado:** build web correcto, pruebas y lint Android beta correctos, y once Edge Functions empaquetadas correctamente.

## 2026-09-10 — Acceso web seguro a Edge Functions e inventario importable

- **Qué:** todas las Edge Functions comparten preflight CORS con allowlist, devuelven errores públicos sin detalles SQL y el importador admite `stock_quantity`, origen controlado y fallos de galería.
- **Por qué:** corregir “Failed to send a request to the Edge Function”, igualar permisos de administradores y activar el control real de existencias.
- **Archivos:** `supabase/functions/**`, `.env.example`, `docs/API.md`.
- **Resultado:** código preparado localmente; el despliegue remoto requiere `SUPABASE_ACCESS_TOKEN` y configurar `WEB_ALLOWED_ORIGINS`.

## 2026-09-10 — Compresión de comprobantes

- Se añadió un compresor Android para fotos de comprobantes antes de subirlas.
- Limita el lado mayor a 1600 px y usa JPEG calidad 84 para equilibrar lectura y almacenamiento.
- La recodificación elimina metadatos EXIF de ubicación y dispositivo; la foto original no se modifica.

## 2026-09-10 — Estados de pedido consistentes en Android

- Android valida las transiciones antes de llamar al servidor: pendiente → confirmado → cobrado → entregado.
- Pedidos entregados o cancelados ya no pueden modificarse desde la interfaz.
- El ticket compartido muestra “PAGADO” cuando corresponde, evitando un estado de cancelación incorrecto.

## 2026-09-10 — Configuración Supabase Android centralizada

- Se centralizó la URL y clave pública de Supabase para catálogo, clientes, pedidos, checkout y sesión.
- Se eliminaron copias de configuración repetidas para reducir errores al cambiar de proyecto.
- No se incorporan claves `service_role` ni secretos al APK.
- Pruebas beta y estable completadas correctamente.

## 2026-09-09 — Correcciones de seguridad y experiencia para beta

### Qué cambió
- Android actualiza el catálogo aprobado en Supabase; ya no inicia scraping desde el teléfono.
- Se eliminó la persistencia local heredada de hashes de contraseña y cada líder ve únicamente sus propias invitaciones activas.
- La web usa Edge Functions para crear equipos, invitaciones y confirmar pedidos; los mensajes invitan a reintentar ante errores de red.
- La navegación inferior móvil conserva cinco destinos y el distintivo de versión abre el historial de cambios.
- Checkout tiene rate limiting del lado servidor y las funciones `SECURITY DEFINER` usan un `search_path` explícito.
- Las sesiones y la caché cifrada Android quedan excluidas de backup y transferencia. El panel de invitación del líder es plegable.

### Validación
- `npm run build`: correcto.
- `:app:testBetaDebugUnitTest`: correcto.
- `:app:lintBetaDebug`: 0 errores.

### Pendiente remoto
- Aplicar migración `0010_security_definer_hardening.sql` y desplegar `checkout-cart` después de reconciliar el historial de Supabase.

## 2026-09-09 — Beta 1.2.0-beta.1 (code=45) preparada

### Qué cambió
- Cada build ahora recibe un identificador visible: `commit.ejecución` en CI o `local.timestamp` en compilaciones locales. La versión comercial y el `versionCode` permanecen estables hasta preparar una nueva entrega.
- Beta y estable tienen contadores independientes: cada CI de beta usa `450000 + github.run_number`; estable conserva su código hasta ser aprobada.
- `version.properties` pasó a ser la única fuente de versión (`1.2.0`, code `45`, beta `1`). Compilar ya no modifica archivos ni aumenta el código.
- El actualizador usa manifiestos versionados con SHA-256, canal, versión mínima y notas. Solo bloquea cuando la app instalada queda por debajo de `minSupportedVersionCode`.
- La descarga se hace dentro de Android, comprueba SHA-256, package ID y firma; se comparte mediante un `FileProvider` privado.
- Refresh token y token de acceso se guardan juntos cifrados con Android Keystore. La sesión se restaura y se refresca mediante un mutex.
- Invitaciones y reportes de fallos se validan en Supabase con JWT, límites por usuario, transacciones e identidad derivada de la sesión.
- Room usa SQLCipher y una clave aleatoria protegida por Android Keystore. La caché conserva UUIDs de productos y se borra al cerrar sesión.
- CI ejecuta pruebas y lint de ambos sabores; el flujo de publicación crea un APK release firmado y genera el manifiesto desde el APK real.

### Archivos principales
- `version.properties`, `scripts/prepare-release.ps1`, `scripts/update-version-json.ps1`
- `app/build.gradle.kts`, `AppUpdateChecker.kt`, `AppUpdateInstaller.kt`, `SecureTokenStore.kt`, `CrashLogger.kt`
- `supabase/migrations/0009_release_1_2_security.sql` y funciones de invitaciones/crashes
- `.github/workflows/ci.yml`, `.github/workflows/publish-release-repo.yml`

### Validación
- `:app:testBetaDebugUnitTest`, `:app:testStableDebugUnitTest`, `:app:lintBetaDebug` y `:app:lintStableDebug`: correctos.
- `web/npm run build`: correcto.
- La publicación de la APK release queda pendiente de configurar los cuatro secretos de firma en GitHub y desplegar la migración `0009` en Supabase.

---

## 2026-09-09 - Skill de Versionado Agregada (v1.1.0)

### Que se hizo
1. **Skill de versionado** creada en `.agents/skills/versioning/SKILL.md` con:
   - Reglas completas de MAJOR.MINOR.PATCH
   - Algoritmo de decisión paso a paso
   - Ejemplos de cada caso
   - Reglas que el agente NO debe romper
   - Flujo de Git tags y CHANGELOG

2. **AGENTS.md actualizado**:
   - Sección de versiones expandida con resumen de la skill
   - Referencia al archivo completo de la skill
   - Regla de CHANGELOG

### Archivos modificados
- `.agents/skills/versioning/SKILL.md` (nuevo)
- `AGENTS.md`

---

## 2026-09-09 - Release v1.2.0 (code=42)

### Que se hizo
- **Bump de versión** de 1.1.0 a 1.2.0
- **versionCode** incrementado a 42
- **stable.json** actualizado a v1.2.0
- **beta.json** actualizado a v1.2.0 con APK URL correcta
- **Build exitoso** para ambos flavors (stable + beta)

### Contenido de v1.2.0
| Feature | Descripción |
|---------|-------------|
| FASE 1 | Código de referido único por equipo + expiración 90 días |
| FASE 2 | Ventana de actualización obligatoria + rate limiting |
| FASE 3 | Git workflow documentado |
| FASE 4 | Rate limiting login + JWT validation + auto-refresh |
| FASE 5 | Crash reporting a Supabase |
| FASE 6 | Offline mode con Room cache |
| Skill | Versionado semántico implementado |

### Archivos modificados
- `version.properties` (code=42, name=1.2.0)
- `web/public/updates/stable.json`
- `web/public/updates/beta.json`

### Resultado del build
- BUILD SUCCESSFUL (stable + beta, v1.2.0)

---

## 2026-09-09 - FASE 6: Offline Mode - Catálogo (v1.1.0)

### Que se hizo
1. **Room Database**:
   - Tabla `cached_products` con campos: sku, name, brand, category, price, description, imageUrl, cachedAt
   - Dao con métodos: getAll, getByCategory, search, insertAll, deleteAll, count

2. **ProductCatalogRepository actualizado**:
   - Carga datos de Room al iniciar (offline-first)
   - Guarda en Room después de cada fetch exitoso de Supabase
   - Mapeo entre entidades Room y dominio

3. **Flujo offline**:
   - Sin conexión: muestra productos cacheados
   - Con conexión: actualiza cache automáticamente

### Archivos modificados
- `app/build.gradle.kts` (Room + KSP dependency)
- `build.gradle.kts` (KSP plugin)
- `app/src/main/java/com/salesnetwork/avon/app/data/ProductCatalogRepository.kt`
- `app/src/main/java/com/salesnetwork/avon/app/data/local/AppDatabase.kt` (nuevo)
- `app/src/main/java/com/salesnetwork/avon/app/data/local/CachedProduct.kt` (nuevo)
- `app/src/main/java/com/salesnetwork/avon/app/data/local/ProductDao.kt` (nuevo)

### Resultado del build
- BUILD SUCCESSFUL (stable v1.1.0 code=41)

---

## 2026-09-09 - FASE 5: Crash Reporting (v1.1.0)

### Que se hizo
1. **Tabla crash_reports**:
   - Campos: id, user_id, app_version, version_code, build_type, stacktrace, device_info, created_at
   - RLS: service_role insert, user read own, leader read team

2. **Edge Function report-crash**:
   - Recibe crash data y guarda en Supabase
   - Validación de campos requeridos

3. **Android CrashLogger actualizado**:
   - Envía crashes automáticamente al servidor
   - Incluye: stacktrace, device_info (model, SDK, release), version, user_id

### Archivos modificados
- `app/src/main/java/com/salesnetwork/avon/app/update/CrashLogger.kt`
- `supabase/functions/report-crash/index.ts` (nuevo)
- `supabase/migrations/0008_crash_reports.sql` (nuevo)

### Resultado del build
- BUILD SUCCESSFUL (stable v1.1.0 code=40)

---

## 2026-09-09 - FASE 4: Seguridad y Auth (v1.1.0)

### Que se hizo
1. **Rate Limiting en Login**:
   - Máximo 5 intentos fallidos
   - Lockout de 5 minutos después de 5 fallos
   - Mensaje claro con tiempo restante

2. **Validación JWT**:
   - Decodificación del token para extraer `exp`
   - Verificación de expiración en cada request

3. **Refresh Token**:
   - Almacenamiento del `refresh_token` de Supabase
   - Auto-refresh antes de que expire el access token

4. **Auto-Logout**:
   - Verificación cada 5 minutos del estado del token
   - Si el refresh falla, logout automático

### Archivos modificados
- `app/src/main/java/com/salesnetwork/avon/app/data/LeaderNetworkRepository.kt`
- `app/src/main/java/com/salesnetwork/avon/app/ui/SalesNetworkMainApp.kt`
- `app/src/main/java/com/salesnetwork/avon/app/ui/viewmodel/AuthViewModel.kt`

### Resultado del build
- BUILD SUCCESSFUL (stable v1.1.0 code=39)

---

## 2026-09-09 - FASE 2: Ventana de Actualización (v1.1.0)

### Que se hizo
1. **Update Mandatory Banner**:
   - Banner rojo persistente cuando `mandatory=true`
   - No se puede cerrar mientras sea obligatoria
   - Incluye icono de advertencia

2. **Update No-Mandatory Banner**:
   - Banner con botón de cerrar
   - Cierra y reaparece después de 5 minutos

3. **Rate Limiting**:
   - Máximo 1 check por hora
   - Se guarda timestamp del último check

4. **minVersionCode**:
   - Campo opcional en JSON para forzar update desde versión específica
   - Si `BuildConfig.VERSION_CODE < minVersionCode`, muestra update

5. **apkUrl Corregido**:
   - Ahora apunta al nombre correcto del APK: `SalesNetworkAvon-v{version}-{channel}.apk`

### Archivos modificados
- `app/src/main/java/com/salesnetwork/avon/app/update/AppUpdateChecker.kt`
- `app/src/main/java/com/salesnetwork/avon/app/ui/SalesNetworkMainApp.kt`
- `web/public/updates/stable.json`
- `web/public/updates/beta.json`

### Resultado del build
- BUILD SUCCESSFUL (stable v1.1.0 code=38)

---

## 2026-09-09 - FASE 1: Código de Referido Único por Equipo (v1.1.0)

### Que se hizo
1. **Código de Referido Único por Usuario**:
   - Generado con `VV-{SHA256(user_id).take(3).uppercase}` (ej: VV-A1B2C3)
   - Cada usuario tiene un código único e inmutable
   - Expiración a 90 días desde el login

2. **Modelo User Actualizado**:
   - Nuevo campo `referralCodeExpiresAt: Long?` para tracking de expiración

3. **UI - TeamNetworkScreen**:
   - Mostrar código con color de advertencia si expiró
   - Mostrar días restantes hasta expiración
   - Botones deshabilitados si código expirado

4. **Edge Function `purge-expired-invitations`**:
   - Marca como `EXPIRED` invitaciones pasadas de fecha
   - Autenticación via `CRON_SECRET`

5. **pg_cron para Purge Diario**:
   - Limpieza automática a las 3am UTC
   - También limpia crash reports mayores a 30 días

### Archivos modificados
- `app/src/main/java/com/salesnetwork/avon/app/domain/model/User.kt`
- `app/src/main/java/com/salesnetwork/avon/app/data/LeaderNetworkRepository.kt`
- `app/src/main/java/com/salesnetwork/avon/app/ui/network/TeamNetworkScreen.kt`
- `supabase/functions/purge-expired-invitations/index.ts` (nuevo)
- `supabase/migrations/0007_cron_purge.sql` (nuevo)
- `web/public/updates/stable.json`

### Resultado del build
- BUILD SUCCESSFUL (stable v1.1.0 code=37)
- Pendiente: Instalar en dispositivo y probar login

---

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

## 2026-09-09 — Comprobante de pago con foto comprimida

- **Qué:** El cambio de un pedido a `COBRADO` permite elegir una imagen, comprimirla antes de subirla y guardar la referencia en Supabase Storage.
- **Por qué:** Evitar comprobantes pesados, reducir almacenamiento y conservar evidencia del pago sin bloquear el flujo con archivos innecesariamente grandes.
- **Archivos:** `app/src/main/java/com/salesnetwork/avon/app/utils/PhotoCompressor.kt`, `app/src/main/java/com/salesnetwork/avon/app/data/SupabaseOrderApi.kt`, `app/src/main/java/com/salesnetwork/avon/app/ui/order/OrderListScreen.kt`, `app/src/main/java/com/salesnetwork/avon/app/ui/viewmodel/OrderViewModel.kt`, `supabase/migrations/0011_order_proofs_storage.sql`.
- **Resultado:** La imagen se reduce a un máximo de 1600 px por lado, calidad JPEG 84 y 2,5 MB; se eliminan metadatos EXIF y el pedido solo cambia a `COBRADO` después de una subida autenticada correcta.

## 2026-09-10 — Modelos de catálogo y comisiones alineados

- **Qué:** Android deja de mostrar stock ficticio, usa disponibilidad real y representa las imágenes como galería; las comisiones locales dejan de calcularse.
- **Por qué:** Mantener Android, web y Supabase con el mismo contrato y evitar que el cliente invente inventario o importes de comisión.
- **Archivos:** `app/src/main/java/com/salesnetwork/avon/app/domain/model/Product.kt`, `app/src/main/java/com/salesnetwork/avon/app/domain/model/Order.kt`, `app/src/main/java/com/salesnetwork/avon/app/data/SupabaseCatalogApi.kt`, `app/src/main/java/com/salesnetwork/avon/app/ui/catalog/CatalogScreen.kt`.
- **Resultado:** El catálogo muestra `Disponible/No disponible`, conserva múltiples URLs de imagen y las comisiones solo se consideran válidas cuando llegan calculadas por Supabase.

## 2026-09-10 — Inventario transaccional y sincronización periódica

- **Qué:** Se añadió `stock_quantity` opcional, descuento protegido por bloqueo de fila durante checkout y una tabla `order_batches` para agrupar futuros pedidos por operación. Android refresca el catálogo cada 30 minutos mientras está activo.
- **Por qué:** Evitar vender la última unidad dos veces y mantener precios y disponibilidad actualizados.
- **Archivos:** `supabase/migrations/0012_inventory_and_order_batches.sql`, `app/src/main/java/com/salesnetwork/avon/app/ui/viewmodel/CatalogViewModel.kt`.
- **Resultado:** Dos checkouts simultáneos se serializan por producto; si no quedan unidades, el segundo recibe `product_out_of_stock`. Los productos antiguos sin inventario conservan gestión externa hasta que se importe una cantidad.

## 2026-09-10 — Sincronización en segundo plano

- **Qué:** Se añadió `CatalogSyncWorker` con WorkManager y red requerida; Android programa una sincronización única cada 30 minutos, incluso si la aplicación está cerrada.
- **Por qué:** Mantener disponibilidad y precios recientes sin depender de que la persona abra manualmente el catálogo.
- **Archivos:** `app/src/main/java/com/salesnetwork/avon/app/data/CatalogSyncWorker.kt`, `app/src/main/java/com/salesnetwork/avon/app/SalesNetworkApplication.kt`, `app/build.gradle.kts`.
- **Resultado:** Los fallos se reintentan hasta tres veces y la caché anterior se conserva.

## 2026-09-10 — Auditoría integral del proyecto

- **Qué:** Se documentó la revisión completa de Android, web, Supabase, seguridad, UX/UI, CI/CD y pruebas.
- **Por qué:** Definir con evidencia qué está listo para beta, qué bloquea producción y en qué orden debe corregirse.
- **Archivos:** `docs/AUDITORIA_COMPLETA_2026-09-10.md`.
- **Resultado:** Informe priorizado P0–P3, verificaciones ejecutadas, fortalezas, riesgos y plan de cuatro fases para llegar a estable.
# 2026-09-10 — Contrato transaccional de equipos, inventario y pedidos

- **Qué:** se añadieron las migraciones `0013` y `0014` para fijar un equipo por cuenta, unificar permisos de `LIDER`/`ROOT_ADMIN`, registrar movimientos de inventario, conservar el historial de estados y soportar pagos, entrega, cancelación y devolución.
- **Por qué:** Android y web necesitan ejecutar las mismas reglas en Supabase y el stock debe mantenerse correcto ante compras simultáneas o reintentos.
- **Archivos:** `supabase/migrations/0013_team_and_order_status_contract.sql`, `supabase/migrations/0014_inventory_and_order_history.sql`, `docs/DATABASE.md`, `docs/API.md`.
- **Resultado:** pendiente de aplicar y validar contra el proyecto Supabase remoto antes de habilitar datos comerciales reales.
