# Entornos visibles en la web

La navegación muestra `Producción · vX.Y.Z` o `Beta · vX.Y.Z`. La página `/cambios` explica qué se agregó, mejoró, corrigió o retiró.

- `main` se publica en `https://sales-network-app.vercel.app` y se identifica como **Producción**.
- Los previews de Pull Request y la rama `beta` se identifican como **Beta**.
- Cada build incorpora automáticamente los primeros 7 caracteres de `VERCEL_GIT_COMMIT_SHA`. Por ejemplo: `Beta · v1.0.1-beta.03b5343`.
- `NEXT_PUBLIC_APP_VERSION` es opcional y permite definir la base semántica; el identificador del commit siempre se añade al final.

Cada push a un Pull Request vuelve a construir la preview y refleja el commit que se está revisando. Actualiza `NEXT_PUBLIC_APP_VERSION` en Preview y Production cuando cambie la versión comercial.
