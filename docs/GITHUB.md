# Publicar en GitHub

El repositorio privado `sales-network-app` contiene el código fuente y las ramas `develop`, `beta` y `main`. Las APK instalables se publican en el repositorio público separado `sales-network-app-releases`, que no contiene código fuente ni secretos.

El workflow `publish-release-repo.yml` compila la APK y la copia al repositorio público usando el secreto `RELEASES_REPO_TOKEN`. Ese token debe tener alcance mínimo y escritura únicamente sobre el repositorio de releases.

El repositorio local ya está inicializado en la raíz del proyecto y usa la rama `main`. No se creó un remoto porque este equipo no tiene sesión ni nombre de repositorio del propietario.

Después de crear un repositorio privado vacío en GitHub:

```powershell
git config user.name "TU NOMBRE"
git config user.email "TU CORREO"
git add .
git commit -m "feat: prepare Supabase Vercel platform"
git remote add origin https://github.com/TU_USUARIO/TU_REPOSITORIO.git
git push -u origin main
git switch -c develop
git push -u origin develop
```

Conectar después el repositorio a Vercel con `web` como Root Directory. Configurar Preview para Pull Requests y Production para `main`.
