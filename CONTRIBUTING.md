# Contribuir a Sales Network

## Flujo de ramas

- `feature/*`: cambios pequeños y aislados.
- `develop`: integración diaria.
- `beta`: validación cerrada y APK beta.
- `main`: producción estable; solo recibe Pull Requests aprobados.

Cada Pull Request debe explicar el problema, el cambio, las pruebas ejecutadas y cualquier migración requerida. No se deben subir secretos, APK personales, bases locales, capturas con datos reales ni archivos `.env`.

## Desarrollo local

1. Clona el repositorio privado y abre la carpeta del proyecto.
2. Ejecuta `npm ci` dentro de `web/`.
3. Configura las variables públicas en `web/.env.local` usando `.env.example`.
4. Ejecuta `npm run build` y las pruebas Playwright cuando correspondan.
5. Para Android usa `gradlew.bat` y un emulador o dispositivo de prueba.

## Commits y revisión

Usa mensajes imperativos y claros, por ejemplo `fix: validar invitación vencida`. Antes de solicitar revisión ejecuta el build del sabor afectado, lint y las pruebas relevantes. Las migraciones SQL deben ser reintentables y revisarse contra el historial remoto antes de desplegarlas.

## Checklist del Pull Request

- [ ] No hay secretos ni datos reales.
- [ ] Se actualizaron documentos y changelog si aplica.
- [ ] Se revisaron permisos RLS y validación server-side.
- [ ] Pasan build, lint y pruebas del área modificada.
- [ ] Se verificó responsive o adaptación Android si cambió UI.
- [ ] Se indicó cómo revertir o migrar el cambio.
