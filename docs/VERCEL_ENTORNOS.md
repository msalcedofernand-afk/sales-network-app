# Entornos visibles en la web

La navegación muestra `Producción · vX.Y.Z` o `Beta · vX.Y.Z`.

- `main` se publica en `https://sales-network-app.vercel.app` y se identifica como **Producción**.
- Los previews de Pull Request y la rama `beta` se identifican como **Beta**.
- La versión se controla con `NEXT_PUBLIC_APP_VERSION` en Vercel. Si no está definida, se muestra `1.0.0`.

Cada push a un Pull Request vuelve a construir la preview y refleja el commit que se está revisando. Actualiza `NEXT_PUBLIC_APP_VERSION` en Preview y Production cuando cambie la versión comercial.
