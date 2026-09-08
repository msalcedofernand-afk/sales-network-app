import type { Metadata } from "next";
import Link from "next/link";
import "./globals.css";
export const metadata: Metadata = { title: "Sales Network", description: "Catálogo privado para equipos de venta" };
export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) { return <html lang="es"><body><header><Link href="/catalogo"><strong>Sales Network</strong></Link><nav><Link href="/catalogo">Catálogo</Link><Link href="/carrito">Carrito</Link><Link href="/pedidos">Pedidos</Link></nav></header><main>{children}</main></body></html>; }

