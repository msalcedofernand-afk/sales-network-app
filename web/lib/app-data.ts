import { createClient } from "./supabase";

export async function getSessionContext() {
  const supabase = createClient();
  const { data: { user } } = await supabase.auth.getUser();
  if (!user) throw new Error("Sesión no disponible");
  const { data: membership, error } = await supabase
    .from("team_members")
    .select("team_id, role, teams(id,name)")
    .eq("user_id", user.id)
    .maybeSingle();
  if (error) throw error;
  if (!membership) throw new Error("Tu cuenta todavía no pertenece a un equipo.");
  return { supabase, user, membership, teamId: membership.team_id as string };
}

export function money(cents:number,currency="PEN"){return new Intl.NumberFormat("es-PE",{style:"currency",currency}).format((cents||0)/100);}
