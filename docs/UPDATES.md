# Actualizaciones de la app

La app no descarga ni ejecuta código desde GitHub. Solo consulta un manifiesto JSON firmado por el repositorio y abre el APK publicado cuando hay un `versionCode` superior.

## Canales

- `main` → `updates/stable.json` → builds de producción.
- `beta` → `updates/beta.json` → builds de validación.
- `release` consulta `main`; `debug` consulta `beta` mediante `BuildConfig.UPDATE_CHANNEL`.

## Publicar una versión

1. Generar y probar el APK.
2. Subir el APK a `releases/sales-network-redesign-debug.apk` o a un Release de GitHub.
3. Actualizar el `versionCode`, `versionName`, `apkUrl` y `releaseNotes` del manifiesto correspondiente.
4. Abrir Pull Request hacia `beta` para validar.
5. Cuando esté revisado, abrir Pull Request de `beta` hacia `main`.

GitHub Actions valida ambos manifiestos en cada cambio. La app comprueba el canal al iniciar, con timeouts cortos y sin bloquear el acceso si no hay conexión.
