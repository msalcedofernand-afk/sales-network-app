import { adminClient, client, json, user } from "../_shared/http.ts";

type CatalogItem = { sku?: string; slug?: string; name?: string; description?: string; category?: string; price_cents?: number | null; price?: number | string | null; currency?: string; available?: boolean; source_url?: string; image_urls?: string[] };

function cents(value: CatalogItem) {
  if (Number.isInteger(value.price_cents) && Number(value.price_cents) >= 0) return Number(value.price_cents);
  if (value.price === null || value.price === undefined || value.price === "") return 0;
  const raw = String(value.price).trim().replace(/\s/g, "");
  const normalized = raw.includes(",") ? raw.replace(/\./g, "").replace(",", ".") : raw;
  const amount = Number(normalized);
  return Number.isFinite(amount) && amount >= 0 ? Math.round(amount * 100) : null;
}

Deno.serve(async req => {
  const authUser = await user(req);
  if (!authUser) return json({ error: "unauthorized" }, 401);
  let body: { team_id?: string; items?: CatalogItem[] };
  try { body = await req.json(); } catch { return json({ error: "invalid_json" }, 400); }
  if (!body.team_id || !Array.isArray(body.items) || body.items.length === 0 || body.items.length > 1000) {
    return json({ error: "invalid_payload", message: "team_id e items son obligatorios (1-1000 filas)." }, 400);
  }

  const supabase = client(req);
  const { data: member } = await supabase.from("team_members").select("role").eq("team_id", body.team_id).eq("user_id", authUser.id).maybeSingle();
  if (member?.role !== "LIDER") return json({ error: "forbidden", message: "Solo un líder puede importar el catálogo." }, 403);

  const rows: Record<string, unknown>[] = [];
  const rejected: { index: number; reason: string }[] = [];
  for (const [index, item] of body.items.entries()) {
    const sku = String(item?.sku ?? "").trim().slice(0, 80);
    const slug = String(item?.slug ?? "").trim().toLowerCase().slice(0, 120);
    const name = String(item?.name ?? "").trim().slice(0, 180);
    const priceCents = cents(item ?? {});
    if (!sku || !slug || !name || priceCents === null) {
      rejected.push({ index, reason: "sku, slug, name o precio inválido" });
      continue;
    }
    const images = Array.isArray(item.image_urls) ? item.image_urls.filter(url => typeof url === "string" && /^https:\/\//.test(url)).slice(0, 12) : [];
    rows.push({
      team_id: body.team_id, sku, slug, name,
      description: String(item.description ?? "").slice(0, 5000),
      category: String(item.category ?? "Otros").slice(0, 80),
      price_cents: priceCents,
      currency: String(item.currency ?? "PEN").toUpperCase().slice(0, 3),
      available: (item.available === undefined ? true : Boolean(item.available)) && priceCents > 0,
      source_url: item.source_url ?? null,
      image_url: images[0] ?? null,
    });
  }

  const admin = adminClient();
  const { data: event } = await admin.from("sync_events").insert({ team_id: body.team_id, source: "viveoficial.com", status: "RUNNING" }).select("id").single();
  const { data: products, error } = await admin.from("products").upsert(rows, { onConflict: "team_id,sku" }).select("id,sku");
  if (error) {
    if (event?.id) await admin.from("sync_events").update({ status: "FAILED", error_message: error.message }).eq("id", event.id);
    return json({ error: "import_failed", message: error.message, rejected }, 500);
  }
  if (products?.length) {
    const bySku = new Map(products.map(product => [product.sku, product.id]));
    const imageRows = rows.flatMap(row => {
      const productId = bySku.get(String(row.sku));
      const item = (body.items ?? []).find(candidate => candidate.sku === row.sku);
      const urls = Array.isArray(item?.image_urls) ? item.image_urls : [];
      return urls.filter(url => typeof url === "string" && /^https:\/\//.test(url)).slice(0, 12).map((url, sort_order) => ({ product_id: productId, storage_path: url, sort_order })).filter(row => row.product_id);
    });
    if (imageRows.length) await admin.from("product_images").delete().in("product_id", products.map(product => product.id));
    if (imageRows.length) await admin.from("product_images").insert(imageRows);
  }
  if (event?.id) await admin.from("sync_events").update({ status: "SUCCESS", imported_count: rows.length, error_message: rejected.length ? rejected.length + " filas rechazadas" : null }).eq("id", event.id);
  return json({ imported_count: rows.length, rejected_count: rejected.length, rejected });
});

