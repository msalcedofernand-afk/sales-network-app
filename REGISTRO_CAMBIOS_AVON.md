# REGISTRO_CAMBIOS_AVON.md - Historial de Cambios del Proyecto VV Lideres Chiclayo

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

## 2026-09-15 - Optimización de costo en GitHub Actions (CI)

### Que se hizo
1. **Ejecución selectiva por impacto**:
   - Se agregaron filtros por ramas y `paths` en CI para no correr el workflow cuando los cambios no afectan Android/Web.
2. **Cancelación de ejecuciones redundantes**:
   - Se agregó `concurrency` con `cancel-in-progress: true` en `ci.yml` y `update-manifest.yml`.
3. **CI ligera vs completa**:
   - Android y Web ahora se dividen en jobs para PR (rápido) y push a ramas principales (completo).
4. **Optimización de dependencias web**:
   - `npm install` se reemplazó por `npm ci` en CI para instalaciones más rápidas y reproducibles.
5. **Control de consumo por job**:
   - Se definieron `timeout-minutes` para cortar ejecuciones colgadas.
6. **Revisión de jobs lentos**:
   - Se revisaron runs recientes de Actions para identificar etapas costosas y priorizar optimización sobre pipelines de PR.
7. **Runner strategy**:
   - Se mantuvo Android en Windows por dependencia actual de `gradlew.bat` (uso estricto y acotado).
   - Web y validación de manifiestos continúan en Linux para reducir costo.
8. **Escalabilidad futura**:
   - Se documenta evaluar `self-hosted runners` si el volumen de builds crece.

### Archivos modificados
- `.github/workflows/ci.yml`
- `.github/workflows/update-manifest.yml`
- `REGISTRO_CAMBIOS_AVON.md`

### Resultado del Build y Prueba
- Validación YAML workflows (`ci.yml` y `update-manifest.yml`) -> OK.
- `cd web && npm ci && npm run build` -> BUILD SUCCESSFUL.
- Revisión de Actions recientes: identificadas oportunidades de ahorro con cancelación temprana y jobs ligeros en PR.
