import ReleaseBadge from "../components/ReleaseBadge";
const releases=[
 {version:"1.0.1-beta.319fde2",date:"Actual",title:"Versionado automático",items:["Cada deploy muestra el commit que está activo.","La beta y producción se distinguen visualmente."]},
 {version:"1.0.1-beta.03b5343",date:"Anterior",title:"Onboarding",items:["Mensajes detallados al crear equipos.","Nueva prueba de versión beta."]},
 {version:"1.0.1-beta.1ea6aba",date:"Anterior",title:"Alta de equipos",items:["Validación de nombre y sesión.","Errores de Supabase más claros."]},
 {version:"1.0.1-beta.96aa413",date:"Anterior",title:"Resiliencia",items:["Fallback para creación de equipos.","Funciones protegidas con JWT."]},
 {version:"1.0.1-beta.bc6ff97",date:"Anterior",title:"Base de datos",items:["Reparación del RPC de equipos.","Migración 0005 aplicada en Supabase."]},
];
export default function CambiosPage(){return <main className="page-wrap changelog-page"><header className="section-intro"><div><p className="eyebrow">Historial de producto</p><h1>Qué cambió en esta versión</h1><p>Revisa las últimas cinco entregas y abre GitHub para consultar el historial completo.</p></div><ReleaseBadge/></header><div className="changelog-grid">{releases.map(r=><section className="card changelog-card" key={r.version}><div className="section-head"><div><span className="badge">{r.date}</span><h2>{r.title}</h2></div><code>{r.version}</code></div><ul>{r.items.map(i=><li key={i}>{i}</li>)}</ul></section>)}</div><p className="changelog-more"><a className="button secondary" href="https://github.com/msalcedofernand-afk/sales-network-app/commits/develop" target="_blank" rel="noreferrer">Ver todo el historial en GitHub ↗</a></p></main>}
