"use client";

import Link from "next/link";
import { FormEvent, useState } from "react";
import { createClient } from "../../lib/supabase";

export default function ResetPasswordPage() {
  const [password, setPassword] = useState("");
  const [confirmation, setConfirmation] = useState("");
  const [message, setMessage] = useState("");
  const [loading, setLoading] = useState(false);

  async function submit(event: FormEvent) {
    event.preventDefault();
    if (password.length < 8) return setMessage("La contraseña debe tener al menos 8 caracteres.");
    if (password !== confirmation) return setMessage("Las contraseñas no coinciden.");
    setLoading(true);
    const { error } = await createClient().auth.updateUser({ password });
    setMessage(error ? "El enlace venció o no es válido. Solicita uno nuevo." : "Contraseña actualizada. Ya puedes ingresar.");
    setLoading(false);
  }

  return <main className="auth-shell"><section className="auth-story"><div><span className="auth-route">Acceso seguro</span><h1>Crea una nueva contraseña.</h1><p>El enlace de recuperación valida tu identidad antes de guardar el cambio.</p></div></section><section className="auth-panel"><h2>Restablecer contraseña</h2><form className="auth-form" onSubmit={submit}><label>Nueva contraseña<input type="password" autoComplete="new-password" minLength={8} required value={password} onChange={event => setPassword(event.target.value)} /></label><label>Repite la contraseña<input type="password" autoComplete="new-password" minLength={8} required value={confirmation} onChange={event => setConfirmation(event.target.value)} /></label><button disabled={loading}>{loading ? "Guardando…" : "Guardar contraseña"}</button></form>{message && <p className="auth-feedback" role="status">{message}</p>}<p><Link className="auth-link" href="/login">Volver a ingresar</Link></p></section></main>;
}

