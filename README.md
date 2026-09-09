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
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:lintDebug
```

Consulta `UI_UX_GUIA.md` para los criterios de experiencia y `docs/DEPLOYMENT.md` para publicar.
