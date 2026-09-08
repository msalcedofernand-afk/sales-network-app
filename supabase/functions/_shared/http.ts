import { createClient } from "npm:@supabase/supabase-js@2";
export function client(req: Request) { return createClient(Deno.env.get("SUPABASE_URL") ?? "", Deno.env.get("SUPABASE_ANON_KEY") ?? "", { global: { headers: { Authorization: req.headers.get("Authorization") ?? "" } } }); }
export function adminClient() { return createClient(Deno.env.get("SUPABASE_URL") ?? "", Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? ""); }
export function json(body: unknown, status = 200) { return new Response(JSON.stringify(body), { status, headers: { "content-type": "application/json" } }); }
export async function user(req: Request) { const { data, error } = await client(req).auth.getUser(); return error ? null : data.user; }
