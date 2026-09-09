import type { Metadata } from "next";
import AppNavigation from "./components/AppNavigation";
import "./globals.css";
export const metadata: Metadata = { title: "Sales Network", description: "Catálogo privado para equipos de venta" };
export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return <html lang="es"><body><a className="skip-link" href="#contenido">Saltar al contenido</a><div className="app-shell"><AppNavigation/><div className="app-content" id="contenido">{children}</div></div></body></html>;
}
