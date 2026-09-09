import ReleaseBadge from "../components/ReleaseBadge";

const sections = [
  { title: "Nuevo", items: ["Etiqueta de entorno y versión visible en toda la navegación.", "Página de cambios accesible desde móvil, tablet y escritorio.", "Previews de Vercel actualizadas automáticamente desde cada Pull Request."] },
  { title: "Mejorado", items: ["La beta se identifica claramente para evitar confundirla con producción.", "La versión se puede controlar con NEXT_PUBLIC_APP_VERSION.", "La navegación conserva foco visible y controles adaptativos."] },
  { title: "Corregido", items: ["La etiqueta permanece visible con pantallas pequeñas.", "La documentación de entornos queda centralizada para el equipo."] },
  { title: "Retirado", items: ["No se retiró ninguna función comercial en esta entrega."] },
];

export default function CambiosPage() {
  return <main className="page-wrap changelog-page">
    <header className="section-intro"><div><p className="eyebrow">Historial de producto</p><h1>Qué cambió en esta versión</h1><p>Consulta las novedades antes de probar la beta y reporta cualquier detalle desde el Pull Request.</p></div><ReleaseBadge /></header>
    <div className="changelog-grid">{sections.map((section) => <section className="card changelog-card" key={section.title}><h2>{section.title}</h2><ul>{section.items.map((item) => <li key={item}>{item}</li>)}</ul></section>)}</div>
  </main>;
}
