# Contribuir

- `main` contiene la versión estable revisada y es la rama de producción.
- `beta` contiene la próxima versión para pruebas; su APK usa el canal beta.
- `develop` queda como integración local; las funcionalidades terminadas pasan a `beta` y después a `main` mediante Pull Request.
- Usar ramas `feature/*`, `fix/*` y `chore/*` desde `develop`.
- Todo cambio entra por Pull Request; no hacer commits directos en `main` ni `beta`.
- Un cambio estable debe actualizar `updates/stable.json`; una prueba debe actualizar `updates/beta.json`.
- Ejecutar tests Android y build web antes de solicitar revisión.
- Toda migración de base de datos debe ser aditiva y estar versionada.
- No incluir `.env`, keystores, claves administrativas ni datos reales.
