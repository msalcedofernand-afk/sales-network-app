# REGISTRO_CAMBIOS_AVON.md - Historial de Cambios del Proyecto Avon Líderes Chiclayo

## 2026-09-08 - Plataforma base Supabase, Vercel y GitHub

### Qué se hizo
- Inicializado repositorio Git local en la rama `main`; no se añadió remoto porque no hay sesión ni URL de GitHub configurada.
- Añadido monorepo lógico con `web/`, `supabase/`, `docs/` y `.github/`, conservando el proyecto Android en la raíz para no romper Gradle.
- Añadido catálogo Next.js con login Supabase, protección de rutas, catálogo demo, detalle, carrito, pedidos, clientes, equipo y administración.
- Añadida migración PostgreSQL con perfiles, equipos, invitaciones, campañas, productos, imágenes, clientes, direcciones, carritos, pedidos y eventos de sincronización.
- Habilitado RLS para aislar usuarios y equipos; los totales del checkout se calculan en servidor.
- Añadidas Edge Functions base para crear/aceptar invitaciones, calcular carrito y confirmar pedido con idempotency key.
- Añadidos documentos de arquitectura, API, base de datos, despliegue, seguridad, contribución y publicación en GitHub.
- Añadido GitHub Actions para tests/Lint Android y build web.

### Verificación
- `:app:testDebugUnitTest` y `:app:lintDebug`: BUILD SUCCESSFUL.
- `web/npm install` y `web/npm run build`: BUILD SUCCESSFUL.

### Requiere configuración del propietario
- Crear proyecto Supabase, ejecutar migración y configurar Auth/secrets.
- Crear repositorio privado GitHub y añadir `origin` siguiendo `docs/GITHUB.md`.
- Conectar Vercel al directorio `web` y cargar variables públicas de Supabase.
- Configurar proveedor autorizado de catálogo, rutas y mensajería antes de activar sus funciones.

## 2026-09-07 - Correcciones de formulario y confianza de datos

### Qué se hizo
- El registro conserva sus campos con `rememberSaveable`, permite desplazamiento con el teclado visible y valida correo y contraseña.
- La contraseña se conecta al flujo local del prototipo mediante hash SHA-256; esto no sustituye autenticación de producción.
- Se corrigieron los callbacks de AuthViewModel y la persistencia del índice de usuarios para que el flujo local sea coherente tras reiniciar.
- Se cambió `adjustNothing` por `adjustResize` y se eliminó el permiso innecesario `CALL_PHONE` porque la app usa `ACTION_DIAL`.
- Las coordenadas de clientes ahora son opcionales; clientes nuevos muestran “Ruta sin calcular” y Maps usa la dirección como alternativa.
- Se añadió `sales_network_app/UI_UX_GUIA.md` con criterios de registro, clientes, catálogo, accesibilidad y aceptación.

### Pendiente antes de producción
- Sustituir la autenticación local por Firebase Auth o Supabase Auth y aplicar permisos por equipo en servidor.
- Persistir clientes/productos en Room y sincronizar con una API autorizada.
- Geocodificar direcciones con confirmación y usar un proveedor de rutas con condiciones de producción.

## 2026-09-07 - Implementación Completa de la App Red de Ventas / Avon Líderes Chiclayo

### Qué se hizo
- Creado proyecto Android Kotlin Jetpack Compose 100% independiente en `sales_network_app` (`com.salesnetwork.avon.app`).
- Implementado Módulo 1 (Auth & Red de Líderes): Registro/Login con código de referido obligatorio, persistencia con `SharedPreferences`, generación automática de link/código de invitación y dashboard de red.
- Implementado Módulo 2 (Catálogo de Productos & Scraper Web): Visualización de catálogo en soles (`S/`), buscador dinámico por nombre/categoría y parser HTML `CatalogScraperEngine` con soporte para decimales con coma (`89,90`).
- Implementado Módulo 3 (CRM Clientes, API de Rutas & Navegación GPS Chiclayo): Ficha de cliente con insignias de estimación de tiempo (ETA por calles vía OSRM y fallback Haversine urbano), botones de 1-tap para llamadas (`ACTION_DIAL`), WhatsApp (`+51`) y navegación GPS paso a paso turno a turno (`google.navigation:q=lat,lng&mode=d`).
- Corregida nulo-seguridad de coordenadas en `RouteEtaService.kt` para contactos sin geolocalización.
- Ejecutadas pruebas unitarias `SalesNetworkModuleTest` (100% PASS).
- Compilado APK Debug e instalado en emulador vía `adb install -r`.

### Archivos creados/modificados
- `domain/model/User.kt`
- `domain/model/Product.kt`
- `domain/model/CustomerContact.kt`
- `data/LeaderNetworkRepository.kt`
- `data/ProductCatalogRepository.kt`
- `data/CustomerRepository.kt`
- `data/RouteEtaService.kt`
- `scraper/CatalogScraperEngine.kt`
- `utils/ContactActionHelper.kt`
- `ui/auth/LoginRegisterScreen.kt`
- `ui/network/TeamNetworkScreen.kt`
- `ui/catalog/CatalogScreen.kt`
- `ui/customer/CustomerListScreen.kt`
- `ui/SalesNetworkMainApp.kt`
- `MainActivity.kt`
- `SalesNetworkModuleTest.kt`

### Resultado del Build y Prueba
- `.\gradlew.bat testDebugUnitTest` -> BUILD SUCCESSFUL (100% OK)
- `.\gradlew.bat assembleDebug` -> BUILD SUCCESSFUL
- `adb install -r app/build/outputs/apk/debug/app-debug.apk` -> Success
- Aplicación iniciada exitosamente en `emulator-5554`.
