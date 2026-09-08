import Link from "next/link";
export default async function ProductPage({params}:{params:Promise<{slug:string}>}){const {slug}=await params;return <article className="card"><p className="muted">Producto: {slug}</p><h1>Detalle del producto</h1><p>La ficha se conectará a products de Supabase respetando el equipo de la sesión.</p><button>Agregar al carrito</button><p><Link href="/catalogo">← Volver al catálogo</Link></p></article>;}

