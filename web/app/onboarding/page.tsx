"use client";

import { FormEvent, useState } from "react";
import { useRouter } from "next/navigation";
import { createClient } from "../../lib/supabase";

export default function OnboardingPage() {
  const router = useRouter();
  const [teamName, setTeamName] = useState("");
  const [code, setCode] = useState("");
  const [message, setMessage] = useState("");
  const [busy, setBusy] = useState<"team" | "invite" | null>(null);

  async function createTeam(event: FormEvent) {
    event.preventDefault();
    if (!teamName.trim()) return;
    setBusy("team"); setMessage("");
    const { error } = await createClient().functions.invoke("create-team", { body: { name: teamName.trim() } });
    if (error) setMessage("No pudimos crear el equipo. Inténtalo nuevamente.");
    else router.replace("/catalogo");
    setBusy(null);
  }

  async function acceptInvitation(event: FormEvent) {
    event.preventDefault();
    if (!code.trim()) return;
    setBusy("invite"); setMessage("");
    const { error } = await createClient().functions.invoke("accept-invitation", { body: { code: code.trim() } });
    if (error) setMessage("La invitación no es válida, venció o fue revocada.");
    else router.replace("/catalogo");
    setBusy(null);
  }

  return <main className="auth-shell"><section className="auth-story"><div><span className="auth-route">Último paso</span><h1>Conecta tu cuenta a un equipo.</h1><p>Podrás administrar tu propio equipo o unirte mediante una invitación de tu líder.</p></div></section><section className="auth-panel"><h2>Configura tu espacio</h2><form className="auth-form" onSubmit={createTeam}><label htmlFor="team-name">Nombre del nuevo equipo<input id="team-name" autoComplete="organization" minLength={2} required value={teamName} onChange={event => setTeamName(event.target.value)} /></label><button disabled={busy !== null}>{busy === "team" ? "Creando…" : "Crear mi equipo"}</button></form><p className="muted">o únete a uno existente</p><form className="auth-form" onSubmit={acceptInvitation}><label htmlFor="invite-code">Código de invitación<input id="invite-code" autoComplete="off" required value={code} onChange={event => setCode(event.target.value.toUpperCase())} /></label><button className="button secondary" disabled={busy !== null}>{busy === "invite" ? "Validando…" : "Aceptar invitación"}</button></form>{message && <p className="auth-feedback error" role="alert">{message}</p>}</section></main>;
}

