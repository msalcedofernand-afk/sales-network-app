# Sales Network

Aplicación Android Compose para equipos de venta, con catálogo web Next.js, Supabase y Vercel.

## Estado

La beta conecta Android y `web/` con Supabase Auth, PostgreSQL/RLS y Edge Functions para sesión, catálogo, clientes, carrito y pedidos. Room/offline completo, la APK release firmada y la promoción a producción siguen pendientes de validación beta.

## Estructura

- `app/`: aplicación Android existente.
- `web/`: catálogo privado y páginas de carrito/pedidos para Vercel.
- `supabase/migrations/`: esquema y políticas RLS.
- `supabase/functions/`: endpoints protegidos.
- `docs/`: arquitectura, API y despliegue.

## Desarrollo web

```powershell
cd web
npm install
npm run build
```

Configura las variables públicas de `.env.example` en Vercel. La clave administrativa solo se usa en Edge Functions y nunca debe estar en `NEXT_PUBLIC_*`.

## Android

```powershell
.\gradlew.bat :app:testBetaDebugUnitTest :app:lintBetaDebug
.\gradlew.bat :app:testStableDebugUnitTest :app:lintStableDebug
```

La versión se prepara explícitamente y nunca durante la compilación:

```powershell
.\scripts\prepare-release.ps1 -VersionName "1.2.0" -VersionCode 45 -BetaNumber 1 -ReleaseNotes "Cambios de seguridad"
```

Las compilaciones `release` requieren una única clave de firma almacenada fuera del repositorio. En GitHub se configuran `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS` y `ANDROID_KEY_PASSWORD`.

Los canales no interfieren: estable conserva `com.salesnetwork.avon.app` y solo cambia su versión al aprobar una entrega. Beta usa `com.salesnetwork.avon.app.beta`; cada ejecución de GitHub Actions recibe automáticamente `versionCode = 450000 + número de ejecución` y `versionName = <versión>-beta.<número de ejecución>`.

Consulta `UI_UX_GUIA.md` para los criterios de experiencia y `docs/DEPLOYMENT.md` para publicar.

## Documentación

- [Guía completa del proyecto](docs/PROJECT_GUIDE.md): flujos, componentes, operación y limitaciones.
- [Arquitectura](docs/ARCHITECTURE.md) · [Base de datos](docs/DATABASE.md) · [API](docs/API.md).
- [Despliegue](docs/DEPLOYMENT.md) · [GitHub](docs/GITHUB.md) · [Entornos Vercel](docs/VERCEL_ENTORNOS.md).
- [Guía UI/UX](docs/UI_UX_GUIA.md) · [Actualizaciones](docs/UPDATES.md).
- [Contribución](CONTRIBUTING.md) · [Seguridad](SECURITY.md).
