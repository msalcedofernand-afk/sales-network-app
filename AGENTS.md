# Reglas del Proyecto - VV Lideres Chiclayo

## Git (OBLIGATORIO)
- **Después de CADA cambio** (sin importar lo básico que sea): commit + push
- Preguntar después de cada cambio: "¿A qué rama? `develop`, `beta` o `main`?"
- Nunca commit directo a `main` o `beta` — solo via PR desde `develop`
- Tag automático `v{versionName}` después de merge a `main`
- Formato de commit: `tipo: descripción corta`
  - Tipos: `feat`, `fix`, `chore`, `docs`, `refactor`

## Versiones (Semantic Versioning)
- Formato: `MAJOR.MINOR.PATCH` (ej: 1.2.0)
- `MAJOR`: cambios que rompen compatibilidad
- `MINOR`: nuevas features sin breaking changes
- `PATCH`: bug fixes
- `versionCode` se incrementa automáticamente en cada build
- `versionName` se actualiza manualmente según semver

## Instalación
- Siempre usar `adb install -r` (reemplaza sin desinstalar)
- Nunca desinstalar la app

## Cambios
- Registrar todo cambio en `REGISTRO_CAMBIOS_AVON.md`
- Incluir: qué, por qué, archivos modificados, resultado del build

## Seguridad
- `service_role` y `sb_secret` solo en servidor (nunca en código de app)
- RLS habilitado en todas las tablas Supabase
- No guardar contraseñas en Android cuando se use Supabase Auth
- No registrar tokens, contraseñas, ni datos sensibles en logs
