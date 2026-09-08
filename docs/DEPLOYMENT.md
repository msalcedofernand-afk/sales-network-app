# Despliegue

1. Crear un proyecto Supabase y ejecutar la migración con Supabase CLI.
2. Configurar Auth con correo, verificación y recuperación.
3. Probar RLS con usuarios de dos equipos.
4. Conectar el directorio `web/` a Vercel como Root Directory.
5. Definir `NEXT_PUBLIC_SUPABASE_URL` y `NEXT_PUBLIC_SUPABASE_PUBLISHABLE_KEY` en Preview y Production.
6. Mantener `SUPABASE_SERVICE_ROLE_KEY` solo como secreto server-side si alguna función lo necesita.
7. Conectar GitHub a Vercel: Preview para Pull Requests y Production únicamente desde `main`.
8. Configurar Android con URL y clave publicable mediante mecanismo de build; nunca subir secretos.

Este entorno no tiene sesión de GitHub, Vercel ni Supabase configurada, por lo que la creación del proyecto remoto y el primer despliegue requieren las cuentas del propietario.

El proyecto usa `web/proxy.ts` para renovar cookies y proteger las rutas web. Si se cambia la versión mayor de Next.js, conservar esa responsabilidad y actualizar la convención de proxy/middleware según la documentación vigente.
