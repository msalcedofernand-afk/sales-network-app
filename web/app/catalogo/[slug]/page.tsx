"use client";

import Link from "next/link";
import { useParams } from "next/navigation";
import { useEffect, useMemo, useState } from "react";
import { getSessionContext, money } from "../../../lib/app-data";
import { getOrCreateActiveCart, incrementCartItem } from "../../../lib/cart";
import { createClient } from "../../../lib/supabase";

type Product = {
  id: string;
  slug: string;
  sku: string;
  name: string;
  description: string;
  category: string;
  price_cents: number;
  currency: string;
  available: boolean;
  stock_quantity: number | null;
  image_url: string | null;
  updated_at: string;
  product_images: { storage_path: string; sort_order: number }[];
};

export default function ProductPage() {
  const params = useParams<{ slug: string }>();
  const [product, setProduct] = useState<Product | null>(null);
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    let active = true;
    void (async () => {
      const { data, error: requestError } = await createClient()
        .from("products")
        .select("id,slug,sku,name,description,category,price_cents,currency,available,stock_quantity,image_url,updated_at,product_images(storage_path,sort_order)")
        .eq("slug", params.slug)
        .maybeSingle();
      if (!active) return;
      if (requestError) setError("No pudimos cargar este producto.");
      else setProduct(data as unknown as Product | null);
    })();
    return () => { active = false; };
  }, [params.slug]);

  const images = useMemo(() => {
    if (!product) return [];
    const gallery = product.product_images?.toSorted((a, b) => a.sort_order - b.sort_order)
      .map(image => image.storage_path) ?? [];
    return Array.from(new Set([...gallery, ...(product.image_url ? [product.image_url] : [])]));
  }, [product]);

  async function add() {
    if (!product || !product.available || product.stock_quantity === 0) return;
    setBusy(true);
    setMessage("");
    try {
      const { supabase, teamId } = await getSessionContext();
      const cart = await getOrCreateActiveCart(supabase, teamId);
      await incrementCartItem(supabase, cart.id, product.id, 1);
      setMessage("Producto añadido al carrito.");
    } catch {
      setMessage("No pudimos añadir el producto. Comprueba el stock e inténtalo nuevamente.");
    } finally {
      setBusy(false);
    }
  }

  if (error && !product) {
    return <main><div className="state card" role="alert"><h2>Producto no encontrado</h2><p>{error}</p><Link className="button" href="/catalogo">Volver al catálogo</Link></div></main>;
  }
  if (!product) return <main><div className="state card"><div className="spinner" /><p>Cargando ficha…</p></div></main>;

  const canAdd = product.available && product.stock_quantity !== 0;
  const stock = product.stock_quantity === null ? "Disponible" : `${product.stock_quantity} unidades disponibles`;
  return (
    <main>
      <article className="card product-detail">
        <div className="detail-image">
          {images[0]
            ? <img src={images[0]} alt={product.name} width="960" height="960" />
            : <span aria-hidden="true">{product.name.slice(0, 1)}</span>}
        </div>
        {images.length > 1 ? (
          <div className="product-gallery" aria-label="Galería del producto">
            {images.map((image, index) => <img key={image} src={image} alt={`${product.name}, imagen ${index + 1}`} width="160" height="160" loading="lazy" />)}
          </div>
        ) : null}
        <span className="badge">{product.category}</span>
        <h1>{product.name}</h1>
        <p className="muted">SKU {product.sku}</p>
        <p className="product-description detail-description">{product.description || "Descripción por completar."}</p>
        <strong>{money(product.price_cents, product.currency)}</strong>
        <p className="muted">{canAdd ? stock : "Producto agotado"}</p>
        {message ? <p className="notice" role="status" aria-live="polite">{message}</p> : null}
        <button disabled={!canAdd || busy} onClick={() => void add()}>{busy ? "Añadiendo…" : canAdd ? "Añadir al carrito" : "Agotado"}</button>
        <p><Link href="/catalogo">← Volver al catálogo</Link></p>
      </article>
    </main>
  );
}
