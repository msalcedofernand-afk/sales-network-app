# Reglas del Proyecto - Avon Líderes Chiclayo

## Versiones
- Cada build incrementa `versionCode` automáticamente (vía `version.properties` + `build.gradle.kts`).
- No tocar `version.properties` manualmente — el build lo gestiona solo.
- `versionName` se actualiza manualmente solo en cambios mayores.

## Instalación
- Siempre usar `adb install -r` (reemplaza sin desinstalar).
- Nunca desinstalar la app — el versionCode ascendente garantiza actualización limpia.

## Cambios
- Registrar todo cambio en `REGISTRO_CAMBIOS_AVON.md`.
- Incluir: qué, por qué, archivos modificados, resultado del build.
