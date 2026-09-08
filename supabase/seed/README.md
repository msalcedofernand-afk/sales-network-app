# Catálogo de Vive

`vive_catalog.json` es una captura del catálogo público de https://viveoficial.com/productos.
Incluye 22 fichas, 102 URLs de imágenes, descripciones, categorías, slugs y URLs de origen.
La web no publica precios, por eso `price_cents` queda vacío y `available` es `false` hasta validarlo con el proveedor.

Regenerar la captura:

```bash
python supabase/seed/scrape_vive.py
```

El script hace una petición por ficha, no usa credenciales y conserva la URL de origen para auditoría.
Antes de mostrar o copiar imágenes en producción, confirmar permisos de uso con Vive Latam y descargar las imágenes a Storage solo cuando exista autorización.

Ejecutar las migraciones antes de importar. La carga debe hacerse mediante una función administrativa protegida que valide SKU, precio y equipo; nunca insertar esta captura directamente en producción sin revisar los precios.
