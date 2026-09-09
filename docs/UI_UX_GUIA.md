# Guía UI/UX compartida

## Identidad

Sales Network usa una identidad azul petróleo y verde menta en Android y web.

- Primario: #165C59
- Oscuro de marca: #123D49
- Fondo: #F3F6F5
- Superficie: #FFFFFF
- Acento positivo: #D5EEE3
- Texto principal: #192D2C
- Texto secundario: #4F625E
- Error: #B42318

Los componentes usan radios de 20–24 dp/px, elevación baja y espaciado de 8, 12, 16, 24 y 32.

## Estructura de pantalla

Cada vista debe tener un encabezado breve, una acción principal visible y estados claros de carga, vacío, error, éxito y desconexión. Los textos explican la acción siguiente; no se muestran valores inventados como metas o ETA.

El catálogo usa búsqueda, categoría, imagen, descripción, disponibilidad, precio y acción de carrito. Clientes y pedidos usan tarjetas en móvil y tablas o paneles en escritorio.

## Adaptación

- Android usa barra inferior en teléfonos y debe migrar a rail lateral en tabletas/plegables.
- Web usa una columna bajo 640 px, dos columnas entre 640 y 1023 px y paneles divididos desde 1024 px.
- Las zonas táctiles mínimas son 48 dp/px.
- Los formularios deben desplazarse con el teclado abierto.
- El contenido nunca depende solo del color; estados llevan texto y, cuando corresponda, icono.
- Se respeta prefers-reduced-motion en web.

## Accesibilidad

Todos los controles tienen etiqueta o descripción, foco visible y contraste suficiente. Las listas vacías ofrecen una acción para continuar. Las operaciones destructivas requieren confirmación.

## Datos

Supabase es la fuente de verdad. Room y cachés web sirven para continuidad offline y se sincronizan al recuperar conexión. Los precios se muestran en moneda y se calculan en servidor al confirmar un pedido.
