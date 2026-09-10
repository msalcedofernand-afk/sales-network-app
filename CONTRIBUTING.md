# Contribuir

## Flujo de Ramas

```
feature/* ──→ develop ──→ beta ──→ main
```

### Ramas
- `main`: Producción estable. Solo via PR desde `beta`.
- `beta`: Pruebas y desarrollo activo. Commits directos permitidos.
- `develop`: Integración local. Features se mergean aquí primero.
- `feature/*`, `fix/*`, `chore/*`: Ramas de trabajo desde `develop`.

## Después de Cada Cambio

1. **Commit** con formato: `tipo: descripción corta`
   - Tipos: `feat`, `fix`, `chore`, `docs`, `refactor`
2. **Push** a `develop`
3. **Merge** a `beta`:
   ```bash
   git checkout beta
   git merge develop
   git push origin beta
   git checkout develop
   ```

## Para Release a Producción

1. Crear **PR** de `beta` → `main`
2. Después del merge:
   ```bash
   git checkout main
   git tag v{versionName}
   git push origin v{versionName}
   git checkout develop
   ```

## Reglas

- No hacer commits directos a `main`
- Todo cambio debe actualizar `REGISTRO_CAMBIOS_AVON.md`
- Ejecutar build Android antes de push
- No incluir `.env`, keystores, claves administrativas ni datos reales
- Migraciones de base de datos deben ser aditivas y versionadas
