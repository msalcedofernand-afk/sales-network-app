export default function PrivacyPage() {
  return (
    <main className="page-shell narrow-page">
      <p className="eyebrow">Privacidad</p>
      <h1>Datos de diagnóstico</h1>
      <p>Cuando una persona ya inició sesión, Sales Network puede enviar automáticamente un reporte de fallo para corregir problemas de la app.</p>
      <p>El reporte incluye versión, canal, plataforma, modelo general del dispositivo, versión del sistema y una traza saneada del error. No incluye contraseña, token, correo, teléfono, dirección ni contenido de pedidos.</p>
      <p>Los reportes se asocian al usuario y equipo de la sesión, se conservan por 30 días y solo se muestran a la persona afectada y a líderes autorizados de su equipo.</p>
    </main>
  );
}
