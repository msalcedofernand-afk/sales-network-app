# Sales Network — sistema de interfaz

## Dirección

**Persona:** consultora o líder de ventas que revisa clientes, catálogo y pedidos entre visitas, a menudo con una sola mano y poco tiempo.

**Objetivo principal:** encontrar un producto, asociarlo a un cliente y convertirlo en pedido sin perder el contexto de campaña o equipo.

**Sensación:** una libreta comercial bien organizada: cercana, confiable y ágil. La interfaz debe ayudar a vender, no parecer un panel administrativo genérico.

**Personalidad:** confianza cercana.
**Fundación:** neutros claros con tinte verde.
**Profundidad:** cambios suaves de superficie y bordes discretos; sin sombras en contenido.
**Tema inicial:** claro.

## Territorio visual

- Catálogo y colección.
- Ruta y próxima visita.
- Campaña activa.
- Carrito y cierre.
- Equipo y comisión.

El mundo de color usa papel claro, tinta verde oscura, azul petróleo y menta. El rojo y el ámbar se reservan para estados que requieren atención.

## Firma: ruta de venta

La cabecera de cada flujo usa un punto y una línea para representar el avance de una venta. La misma lógica aparece en navegación activa, sincronización, estados de pedido y pasos de checkout. Debe poder identificarse en:

1. Cabeceras de sección.
2. Navegación activa.
3. Indicador de conexión.
4. Estado de pedido.
5. Progreso de checkout.

## Tokens

### Colores

| Token | Valor | Uso |
|---|---:|---|
| `petroleo-900` | `#123D49` | títulos, marca y énfasis |
| `petroleo-700` | `#165C59` | acción principal y navegación activa |
| `petroleo-600` | `#21716C` | foco y estados interactivos |
| `menta-200` | `#D5EEE3` | selección y apoyo de marca |
| `menta-100` | `#EAF6F0` | superficies destacadas |
| `papel-50` | `#F3F6F5` | fondo de aplicación |
| `papel-0` | `#FFFFFF` | paneles y tarjetas |
| `tinta-900` | `#192D2C` | texto principal |
| `tinta-700` | `#405552` | texto secundario |
| `tinta-500` | `#60736F` | metadatos |
| `peligro` | `#B42318` | errores y acciones destructivas |

### Espaciado

Base de 4 px/dp. Escala: `4, 8, 12, 16, 20, 24, 32, 48, 64`.

- Separación icono/texto: 8.
- Separación entre campos: 16.
- Interior de tarjeta: 16–24.
- Separación de secciones: 24–32.
- Separación principal de página: 48–64.

### Radios

- Campo o control: 10–14.
- Tarjeta: 20.
- Cabecera principal: 24.
- Chip: radio completo.

### Tipografía

- Familia: Inter o fuente del sistema con números tabulares.
- Título principal: `clamp(32px, 4vw, 54px)`, peso 700, tracking cerrado.
- Título de sección: 20–24 px, peso 600–700.
- Cuerpo: 16 px, línea 1.5.
- Etiqueta: 12–14 px, peso 600.
- Precio, comisión y total: números tabulares.

## Patrones

### Navegación

- Escritorio: lateral de 248 px, cinco destinos y estado activo visible.
- Móvil: cinco destinos en barra inferior dentro de la zona segura.
- Iconos vectoriales de un solo estilo.
- Cada destino anuncia `aria-current` en web y estado seleccionado en Compose.

### Formularios

- Etiqueta siempre visible sobre el campo.
- Campo ligeramente hundido respecto a la tarjeta.
- Validación cercana al campo y resumen cuando existan varios errores.
- Acción principal de ancho completo en móvil.
- Mantener los datos tras un error.

### Catálogo

- Imagen, categoría, nombre, descripción breve, precio y acciones en ese orden.
- Una columna hasta 430 px; dos o más según espacio disponible.
- La imagen reserva altura para evitar saltos de contenido.

### Estados

- Cargando: skeleton con la forma del contenido cuando sea posible.
- Vacío: explicar qué aparecerá y ofrecer un siguiente paso.
- Error: decir qué falló y ofrecer reintento.
- Sin conexión: mostrar caché y fecha de sincronización.
- Éxito: confirmación visible, sin depender solo del color.

### Movimiento

- Pulsación: 100 ms.
- Hover y cambio de estado: 150 ms.
- Panel o diálogo: 200–250 ms.
- Animar solo opacidad y transformación.
- Respetar movimiento reducido.

## Reglas de aceptación

- Contraste AA, foco visible y navegación completa por teclado.
- Objetivos táctiles de 48 dp en Android y al menos 44 px en web.
- Sin contenido oculto por barra inferior o áreas seguras.
- Una acción primaria por región visual.
- Color acompañado de texto o icono en estados.
- Prueba a 375, 768, 1024 y 1440 px; teléfono vertical/horizontal y tablet Android.

## Decisiones

- 2026-09-08: se conserva la identidad petróleo/menta ya aprobada para mantener continuidad entre APK y web.
- 2026-09-08: se adopta la “ruta de venta” como firma exclusiva del producto.
- 2026-09-08: se usa profundidad por superficies y bordes para mantener fluidez en dispositivos modestos.
