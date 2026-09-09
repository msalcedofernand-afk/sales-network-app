"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import type { ReactNode } from "react";
import ReleaseBadge from "./ReleaseBadge";

type NavItem = {
  href: string;
  label: string;
  shortLabel: string;
  icon: ReactNode;
};

const APK_DOWNLOAD_URL = "https://raw.githubusercontent.com/msalcedofernand-afk/sales-network-app-releases/beta/releases/sales-network-beta.apk";

const iconProps = {
  width: 20,
  height: 20,
  viewBox: "0 0 24 24",
  fill: "none",
  stroke: "currentColor",
  strokeWidth: 1.8,
  strokeLinecap: "round" as const,
  strokeLinejoin: "round" as const,
  "aria-hidden": true,
};

const items: NavItem[] = [
  {
    href: "/catalogo",
    label: "Catálogo",
    shortLabel: "Catálogo",
    icon: <svg {...iconProps}><path d="M4 5.5A2.5 2.5 0 0 1 6.5 3H20v16H6.5A2.5 2.5 0 0 0 4 21.5z"/><path d="M4 5.5v16M8 7h8M8 11h6"/></svg>,
  },
  {
    href: "/carrito",
    label: "Carrito",
    shortLabel: "Carrito",
    icon: <svg {...iconProps}><path d="M3 4h2l2.1 10.2a2 2 0 0 0 2 1.6h7.8a2 2 0 0 0 2-1.6L20 7H6"/><circle cx="9" cy="20" r="1"/><circle cx="17" cy="20" r="1"/></svg>,
  },
  {
    href: "/pedidos",
    label: "Pedidos",
    shortLabel: "Pedidos",
    icon: <svg {...iconProps}><path d="M6 3h12v18l-3-2-3 2-3-2-3 2z"/><path d="M9 8h6M9 12h6"/></svg>,
  },
  {
    href: "/clientes",
    label: "Clientes",
    shortLabel: "Clientes",
    icon: <svg {...iconProps}><circle cx="12" cy="8" r="4"/><path d="M4 21a8 8 0 0 1 16 0"/></svg>,
  },
  {
    href: "/equipo",
    label: "Mi equipo",
    shortLabel: "Equipo",
    icon: <svg {...iconProps}><circle cx="9" cy="8" r="3"/><circle cx="17" cy="10" r="2"/><path d="M3 20a6 6 0 0 1 12 0M14 16a5 5 0 0 1 7 4"/></svg>,
  },
];

function NavLinks({ mobile = false }: { mobile?: boolean }) {
  const pathname = usePathname();
  return items.map((item) => {
    const active = pathname === item.href || pathname.startsWith(`${item.href}/`);
    return (
      <Link
        key={item.href}
        href={item.href}
        className={`nav-item${active ? " active" : ""}`}
        aria-current={active ? "page" : undefined}
      >
        {item.icon}
        <span>{mobile ? item.shortLabel : item.label}</span>
      </Link>
    );
  });
}

export default function AppNavigation() {
  const pathname = usePathname();
  if (pathname === "/login" || pathname === "/registro" || pathname === "/restablecer-contrasena" || pathname === "/onboarding") {
    return (
      <header className="mobile-header auth-header">
        <Link className="brand" href="/login" aria-label="Sales Network, ir al inicio de sesión">
          <span className="brand-mark" aria-hidden="true">VV</span>
          <span className="brand-copy"><strong>Sales Network</strong><small>Tu ruta de ventas</small></span>
        </Link>
        <ReleaseBadge />
        <a className="download-app-link" href={APK_DOWNLOAD_URL} download aria-label="Descargar aplicación Android">Descargar app</a>
      </header>
    );
  }
  return (
    <>
      <aside className="sidebar" aria-label="Navegación principal">
        <Link className="brand" href="/catalogo" aria-label="Sales Network, ir al catálogo">
          <span className="brand-mark" aria-hidden="true">VV</span>
          <span className="brand-copy"><strong>Sales Network</strong><small>Tu ruta de ventas</small></span>
        </Link>
        <p className="nav-section-label">Espacio de trabajo</p>
        <nav className="sidebar-nav"><NavLinks /></nav>
        <div className="sidebar-foot">
          <span className="sync-dot" aria-hidden="true" />
          <span>Conectado a tu equipo</span>
          <ReleaseBadge />
          <a className="download-app-link" href={APK_DOWNLOAD_URL} download>Descargar app</a>
        </div>
      </aside>

      <header className="mobile-header">
        <Link className="brand" href="/catalogo" aria-label="Sales Network, ir al catálogo">
          <span className="brand-mark" aria-hidden="true">VV</span>
          <span className="brand-copy"><strong>Sales Network</strong><small>Tu ruta de ventas</small></span>
        </Link>
        <ReleaseBadge />
        <a className="download-app-link" href={APK_DOWNLOAD_URL} download aria-label="Descargar aplicación Android">Descargar app</a>
      </header>

      <nav className="bottom-nav" aria-label="Navegación móvil"><NavLinks mobile /></nav>
    </>
  );
}
