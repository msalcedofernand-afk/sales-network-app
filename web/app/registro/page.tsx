"use client";

import Link from "next/link";
import { FormEvent, useState } from "react";
import { createClient } from "../../lib/supabase";

export default function RegisterPage() {
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [message, setMessage] = useState("");
  const [hasError, setHasError] = useState(false);
  const [loading, setLoading] = useState(false);

  async function submit(event: FormEvent) {
    event.preventDefault();
    setLoading(true);
    setMessage("");
    const { error } = await createClient().auth.signUp({
      email: email.trim(),
      password,
      options: { data: { name: name.trim() }, emailRedirectTo: `${window.location.origin}/login` },
    });
    setHasError(Boolean(error));
    setMessage(error
      ? "No pudimos crear la cuenta. Revisa los datos o usa otro correo."
      : "Cuenta creada. Revisa tu correo y confirma el registro antes de ingresar.");
    setLoading(false);
  }

  return (
    <main className="auth-shell">
      <section className="auth-story" aria-labelledby="register-story-title">
        <div>
          <span className="auth-route">Comienza tu ruta</span>
          <h1 id="register-story-title">Tu red crece con cada contacto.</h1>
          <p>Crea tu acceso personal. Después podrás crear un equipo o aceptar la invitación de tu líder.</p>
        </div>
        <div className="auth-points" aria-label="Pasos del registro">
          <span className="auth-point">Crea tu acceso seguro</span>
          <span className="auth-point">Confirma el correo</span>
          <span className="auth-point">Únete a tu equipo</span>
        </div>
      </section>

      <section className="auth-panel" aria-labelledby="register-title">
        <h2 id="register-title">Crear tu cuenta</h2>
        <p className="muted">Usa un correo al que tengas acceso para confirmar el registro.</p>
        <form className="auth-form" onSubmit={submit} aria-describedby={message ? "register-feedback" : undefined}>
          <label htmlFor="name">Nombre completo
            <input id="name" autoComplete="name" required value={name} onChange={(event) => setName(event.target.value)} />
          </label>
          <label htmlFor="register-email">Correo
            <input id="register-email" type="email" inputMode="email" autoComplete="email" required value={email} onChange={(event) => setEmail(event.target.value)} />
          </label>
          <label htmlFor="register-password">Contraseña
            <input id="register-password" type="password" autoComplete="new-password" required minLength={8} aria-describedby="password-help" value={password} onChange={(event) => setPassword(event.target.value)} />
            <small id="password-help" className="muted">Usa al menos 8 caracteres.</small>
          </label>
          <button type="submit" disabled={loading}>{loading ? "Creando cuenta…" : "Crear cuenta"}</button>
        </form>
        <p>¿Ya tienes cuenta? <Link className="auth-link" href="/login">Ingresar</Link></p>
        {message && <p id="register-feedback" className={`auth-feedback ${hasError ? "error" : "success"}`} role={hasError ? "alert" : "status"}>{message}</p>}
      </section>
    </main>
  );
}
