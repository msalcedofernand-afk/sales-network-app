# Arquitectura

Android usa Compose → ViewModel → Repository → Room/Supabase. Next.js en Vercel sirve el catálogo y el panel. Supabase concentra Auth, PostgreSQL, Storage, Realtime y Edge Functions.

La autorización se aplica en PostgreSQL con RLS. La app puede usar la clave pública; las claves administrativas solo viven en funciones server-side.

