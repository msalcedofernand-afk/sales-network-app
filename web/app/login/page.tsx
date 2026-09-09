"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { FormEvent, useState } from "react";
import { createClient } from "../../lib/supabase";

type Feedback = { kind: "error" | "success"; text: string } | null;

function friendlyAuthError(message: string) {
  const value = message.toLowerCase();
  if (value.includes("invalid login")) return "El correo o la contraseña no son correctos.";
  if (value.includes("email not confirmed")) return "Confirma tu correo antes de iniciar sesión.";
  if (value.includes("rate limit")) return "Espera un momento antes de volver a intentarlo.";
  return "No pudimos iniciar sesión. Revisa tus datos e inténtalo nuevamente.";
}

export default function LoginPage() {
  const router = useRouter();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [feedback, setFeedback] = useState<Feedback>(null);
  const [loading, setLoading] = useState(false);

  async function submit(event: FormEvent) {
    event.preventDefault();
    if (loading) return;
    setFeedback(null);
    setLoading(true);
    try {
      const { error } = await createClient().auth.signInWithPassword({ email: email.trim(), password });
      if (error) setFeedback({ kind: "error", text: friendlyAuthError(error.message) });
      else router.replace("/catalogo");
    } catch {
      setFeedback({ kind: "error", text: "No pudimos conectar. Comprueba tu conexión e inténtalo nuevamente." });
    } finally { setLoading(false); }
  }

  async function recover() {
    if (!email.trim()) return setFeedback({ kind: "error", text: "Escribe tu correo para enviarte el enlace de recuperación." });
    setLoading(true);
    const { error } = await createClient().auth.resetPasswordForEmail(email.trim(), { redirectTo: window.location.origin + "/restablecer-contrasena" });
    setFeedback(error ? { kind: "error", text: "No pudimos enviar el enlace. Revisa el correo." } : { kind: "success", text: "Te enviamos un enlace para crear una contraseña nueva." });
    setLoading(false);
  }

  return <main className="auth-shell"><section className="auth-story" aria-labelledby="auth-story-title"><div><span className="auth-route">Tu siguiente venta</span><h1 id="auth-story-title">Todo tu negocio, en una sola ruta.</h1><p>Catálogo, clientes, pedidos y equipo sincronizados para que puedas vender desde cualquier lugar.</p></div><div className="auth-points" aria-label="Beneficios"><span className="auth-point">Continúa tu carrito en otro dispositivo</span><span className="auth-point">Consulta la campaña activa</span><span className="auth-point">Protege los datos de tu equipo</span></div></section><section className="auth-panel" aria-labelledby="login-title"><h2 id="login-title">Bienvenida de vuelta</h2><p className="muted">Ingresa con la cuenta confirmada de tu equipo.</p><form className="auth-form" onSubmit={submit} aria-describedby={feedback ? "auth-feedback" : undefined}><label htmlFor="email">Correo<input id="email" type="email" autoComplete="email" inputMode="email" required value={email} onChange={event => setEmail(event.target.value)} /></label><label htmlFor="password">Contraseña<input id="password" type="password" autoComplete="current-password" required minLength={8} value={password} onChange={event => setPassword(event.target.value)} /></label><div className="auth-actions"><button type="submit" disabled={loading}>{loading ? "Comprobando…" : "Ingresar"}</button><button className="button ghost" type="button" onClick={recover} disabled={loading}>Recuperar contraseña</button></div></form><p>¿Aún no tienes cuenta? <Link className="auth-link" href="/registro">Crear cuenta</Link></p>{feedback && <p id="auth-feedback" className={"auth-feedback " + feedback.kind} role={feedback.kind === "error" ? "alert" : "status"}>{feedback.text}</p>}</section></main>;
}
