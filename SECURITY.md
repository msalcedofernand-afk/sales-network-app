# Seguridad

## Secretos

Las claves `service_role`, contraseñas de base, tokens de Supabase, keystores y contraseñas de firma solo viven en el gestor de secretos local, GitHub Actions o Supabase Functions. Las variables `NEXT_PUBLIC_*` contienen únicamente URL y clave publicable. Si un secreto se expone, revócalo y genera uno nuevo de inmediato.

## Datos y autorización

Supabase es la fuente de autorización. Todas las tablas expuestas usan RLS y cada Edge Function valida el JWT y la pertenencia al equipo. Los precios, comisiones, estados de pedido e idempotencia se calculan o validan en servidor. El bucket `order-proofs` es privado y sus rutas comienzan con el UUID del usuario.

## Android

Los tokens se guardan en almacenamiento protegido por Android Keystore. El selector de comprobantes comprime la imagen y elimina EXIF antes de subirla. No se registran tokens, contraseñas, correos completos ni respuestas de Auth. El actualizador comprueba versión, canal, hash SHA-256, package ID y firma.

## Reporte responsable

No publiques vulnerabilidades ni datos personales en Issues. Envía un reporte privado al mantenedor con pasos para reproducir, impacto, versión, entorno y evidencia mínima. No incluyes tokens ni bases exportadas.
