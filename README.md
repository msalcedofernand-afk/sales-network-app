# Sales Network

Aplicación Android Compose para equipos de venta, con catálogo web Next.js, Supabase y Vercel.

## Estado

La app Android mantiene datos demo locales. `web/` contiene un catálogo Next.js funcional como base visual. `supabase/` contiene la primera migración PostgreSQL/RLS y funciones Edge para invitaciones y carrito. Los conectores reales se activan después de configurar un proyecto Supabase y sus secretos.

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

Configura las variables de `.env.example` en Vercel. La clave administrativa nunca debe estar en `NEXT_PUBLIC_*`.

## Android

```powershell
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:lintDebug
```

Consulta `UI_UX_GUIA.md` para los criterios de experiencia y `docs/DEPLOYMENT.md` para publicar.

