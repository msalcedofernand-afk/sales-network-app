# Guía UI/UX — Sales Network

## Objetivo

La app debe ayudar a una vendedora a pasar de contacto a pedido con la menor fricción posible. La información de demostración debe estar etiquetada y nunca parecer un dato calculado o sincronizado.

## Registro e inicio de sesión

- Usar correo válido y contraseña de mínimo 8 caracteres; conservar el formulario durante rotación y apertura del teclado.
- Mostrar errores cerca del campo que se puede corregir y desactivar el botón durante una operación.
- Separar **Crear equipo** y **Unirme con invitación**. Mostrar el nombre del equipo antes de aceptar un código.
- Permitir mostrar/ocultar contraseña, recuperar acceso y verificar correo cuando exista un proveedor real.
- La implementación local actual sirve para pruebas del prototipo. Antes de publicar, migrar la autenticación a Firebase Auth o Supabase Auth; no guardar contraseñas en preferencias.

## Clientes

- Solicitar nombre y teléfono; normalizar teléfonos para detectar duplicados.
- La dirección no equivale a una coordenada. Mostrar “Ubicación sin confirmar” hasta seleccionar y confirmar un punto.
- Mostrar “Ruta sin calcular” cuando no exista ETA y guardar origen, fecha, modo y fuente al calcular.
- Añadir editar, archivar, notas de seguimiento, próxima acción y estado del contacto.

## Catálogo y pedidos

- Mostrar campaña, moneda, vigencia, disponibilidad y última actualización.
- Rechazar filas sin SKU o precio válido y conservar la última versión correcta.
- El flujo prioritario es cliente → productos → cantidades → total/comisión → estado del pedido.

## Adaptación y accesibilidad

- El formulario usa desplazamiento vertical y `adjustResize` para que el teclado no tape el botón.
- En teléfonos usar barra inferior; en tablets y plegables usar navegación lateral adaptativa.
- Usar columnas adaptativas y targets táctiles de al menos 48 dp.
- Probar modo oscuro, letra grande, orientación horizontal y ausencia de Maps/WhatsApp.
- No depender solo del color y no registrar contraseñas, tokens ni datos personales en logs.

## Criterios de aceptación

1. Una contraseña incorrecta no abre la app.
2. El registro funciona con teclado visible y letra grande.
3. Un cliente nuevo no navega a una coordenada inventada.
4. Un fallo de red conserva el último catálogo válido y no duplica clientes al reintentar.
5. Una cuenta no puede leer clientes de otro equipo.
