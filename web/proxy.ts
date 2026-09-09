import { createServerClient } from "@supabase/ssr";
import { NextResponse, type NextRequest } from "next/server";

const protectedPrefixes = ["/catalogo", "/carrito", "/pedidos", "/clientes", "/equipo", "/admin", "/onboarding"];

function loginRedirect(request: NextRequest, reason?: string) {
  const destination = new URL("/login", request.url);
  destination.searchParams.set("next", request.nextUrl.pathname);
  if (reason) destination.searchParams.set("reason", reason);
  return NextResponse.redirect(destination);
}

export async function proxy(request: NextRequest) {
  const protectedPath = protectedPrefixes.some(path => request.nextUrl.pathname.startsWith(path));
  if (!protectedPath) return NextResponse.next({ request });

  const url = process.env.NEXT_PUBLIC_SUPABASE_URL;
  const key = process.env.NEXT_PUBLIC_SUPABASE_PUBLISHABLE_KEY;
  // Fail closed: an unconfigured deployment must never publish a private catalogue.
  if (!url || !key) return loginRedirect(request, "config");

  let response = NextResponse.next({ request });
  const supabase = createServerClient(url, key, {
    cookies: {
      getAll: () => request.cookies.getAll(),
      setAll: cookies => cookies.forEach(({ name, value, options }) => response.cookies.set(name, value, options)),
    },
  });

  try {
    const { data: { user }, error } = await supabase.auth.getUser();
    if (error || !user) return loginRedirect(request);

    if (request.nextUrl.pathname.startsWith("/admin")) {
      const { data: membership } = await supabase
        .from("team_members")
        .select("role")
        .eq("user_id", user.id)
        .eq("role", "LIDER")
        .limit(1)
        .maybeSingle();
      if (!membership) return NextResponse.redirect(new URL("/catalogo", request.url));
    }
    return response;
  } catch {
    return loginRedirect(request);
  }
}

export const config = { matcher: ["/catalogo/:path*", "/carrito/:path*", "/pedidos/:path*", "/clientes/:path*", "/equipo/:path*", "/admin/:path*", "/onboarding/:path*"] };
