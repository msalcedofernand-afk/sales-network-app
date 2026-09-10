import { createClient } from "npm:@supabase/supabase-js@2";

const localOrigins = new Set([
  "http://localhost:3000",
  "http://127.0.0.1:3000",
]);

function configuredOrigins() {
  return new Set(
    (Deno.env.get("WEB_ALLOWED_ORIGINS") ?? "https://sales-network-app.vercel.app")
      .split(",")
      .map(origin => origin.trim())
      .filter(Boolean),
  );
}

function isAllowedOrigin(origin: string) {
  if (localOrigins.has(origin) || configuredOrigins().has(origin)) return true;
  try {
    const url = new URL(origin);
    return url.protocol === "https:" &&
      /^sales-network-[a-z0-9-]+-fms7w7\.vercel\.app$/.test(url.hostname);
  } catch {
    return false;
  }
}

function corsHeaders(req: Request) {
  const origin = req.headers.get("origin");
  const headers: Record<string, string> = {
    "access-control-allow-headers": "authorization, apikey, content-type, x-client-info",
    "access-control-allow-methods": "POST, OPTIONS",
    "access-control-max-age": "86400",
    "vary": "Origin",
  };
  if (origin && isAllowedOrigin(origin)) headers["access-control-allow-origin"] = origin;
  return headers;
}

export function preflight(req: Request) {
  if (req.method !== "OPTIONS") return null;
  const origin = req.headers.get("origin");
  if (origin && !isAllowedOrigin(origin)) {
    return new Response(null, { status: 403, headers: corsHeaders(req) });
  }
  return new Response(null, { status: 204, headers: corsHeaders(req) });
}

export function client(req: Request) {
  const key = Deno.env.get("SUPABASE_ANON_KEY") ?? Deno.env.get("SUPABASE_PUBLISHABLE_KEY") ?? "";
  return createClient(Deno.env.get("SUPABASE_URL") ?? "", key, {
    global: { headers: { Authorization: req.headers.get("Authorization") ?? "" } },
  });
}

export function adminClient() {
  return createClient(
    Deno.env.get("SUPABASE_URL") ?? "",
    Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? "",
  );
}

export function json(req: Request, body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "content-type": "application/json", ...corsHeaders(req) },
  });
}

export async function user(req: Request) {
  const { data, error } = await client(req).auth.getUser();
  return error ? null : data.user;
}
