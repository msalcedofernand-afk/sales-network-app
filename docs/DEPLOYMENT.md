# Despliegue

1. Crear un proyecto Supabase y ejecutar `scripts/deploy-supabase.ps1` desde PowerShell. El script aplica las migraciones y despliega todas las Edge Functions con JWT.
2. Configurar Auth con correo, verificación y recuperación.
3. Probar RLS con usuarios de dos equipos.
4. Conectar el directorio `web/` a Vercel como Root Directory.
5. Definir `NEXT_PUBLIC_SUPABASE_URL` y `NEXT_PUBLIC_SUPABASE_PUBLISHABLE_KEY` en Preview y Production.
6. Mantener `SUPABASE_SERVICE_ROLE_KEY` solo como secreto server-side si alguna función lo necesita.
7. Conectar GitHub a Vercel: Preview para Pull Requests y Production únicamente desde `main`.
8. Configurar Android con URL y clave publicable mediante mecanismo de build; nunca subir secretos.
9. Aplicar las migraciones y publicar las APK en `app-releases/stable/sales-network.apk` y `app-releases/beta/sales-network.apk` usando una Edge Function o CI con `service_role` únicamente en el servidor.

## Puesta en marcha del proyecto remoto

Desde una terminal con el CLI de Supabase instalado, define los secretos solo en la sesión local:

```powershell
$env:SUPABASE_ACCESS_TOKEN = "<token-personal-de-supabase>"
$env:SUPABASE_DB_PASSWORD = "<password-de-la-base>"
.\scripts\deploy-supabase.ps1
```

El script vincula el proyecto `xceqwexdufdgnmctsxcg`, ejecuta `supabase db push` y publica `create-team`, `accept-invitation`, `checkout-cart`, `sync-catalog` y las demás funciones del directorio `supabase/functions`. No guardes esos valores en `.env`, GitHub ni el repositorio.

Si `db push` indica que `profiles`, `teams` u otra tabla ya existe, no borres tablas ni vuelvas a ejecutar SQL destructivo. Comprueba primero el historial:

```powershell
npx supabase migration list
```

Cuando el esquema remoto ya contiene las migraciones `0001`, `0002` y `0003` pero faltan en el historial, márcalas como aplicadas y vuelve a ejecutar el script para aplicar solo `0004`:

```powershell
npx supabase migration repair --status applied 0001 0002 0003
.\scripts\deploy-supabase.ps1
```

Si alguna migración no está realmente reflejada en el esquema, no la marques como aplicada: revisa esa diferencia en el SQL Editor de Supabase antes de continuar.

En Vercel configura para Preview y Production:

- `NEXT_PUBLIC_SUPABASE_URL=https://xceqwexdufdgnmctsxcg.supabase.co`
- `NEXT_PUBLIC_SUPABASE_PUBLISHABLE_KEY=<publishable-key>`

En Supabase Auth añade como redirect URLs `https://sales-network-app.vercel.app/**` y la URL de cada Preview de Vercel. La producción debe salir únicamente de `main`; `develop` se valida mediante Pull Request hacia `beta`.

Después del despliegue, crea dos usuarios en equipos distintos y verifica que cada uno solo pueda leer sus propios clientes, productos, carritos y pedidos. Esa prueba debe ejecutarse con usuarios reales autenticados, nunca con `service_role` desde el navegador.

Proyectos remotos creados:

- GitHub: https://github.com/msalcedofernand-afk/sales-network-app
- Vercel: https://sales-network-app.vercel.app (Root Directory `web`)
- Supabase: proyecto `sales-network-app`, ref. `xceqwexdufdgnmctsxcg`, URL https://xceqwexdufdgnmctsxcg.supabase.co

El proyecto usa `web/proxy.ts` para renovar cookies y proteger las rutas web. Si se cambia la versión mayor de Next.js, conservar esa responsabilidad y actualizar la convención de proxy/middleware según la documentación vigente.
