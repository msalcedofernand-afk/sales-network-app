# Base de datos

La migración `supabase/migrations/0001_sales_network.sql` crea perfiles, equipos, membresías, invitaciones, campañas, productos, clientes, carritos, pedidos y sincronizaciones.

Las relaciones de negocio usan `team_id` y `user_id`; el código de invitación no es una relación permanente. Antes de producción, revisar las políticas con `supabase db lint` y pruebas de aislamiento.

