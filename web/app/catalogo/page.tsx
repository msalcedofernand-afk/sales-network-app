"use client";

import Link from "next/link";
import { useDeferredValue, useEffect, useMemo, useState } from "react";
import { getSessionContext, money } from "../../lib/app-data";
import { getOrCreateActiveCart, incrementCartItem } from "../../lib/cart";
import { createClient } from "../../lib/supabase";

type ProductImage = { storage_path: string; sort_order: number };
type Product = {
  id: string;
  slug: string;
  sku: string;
  name: string;
  description: string | null;
  category: string | null;
  price_cents: number;
  currency: string;
  available: boolean;
  stock_quantity: number | null;
  image_url: string | null;
  updated_at: string;
  product_images: ProductImage[];
};

function productImage(product: Product) {
  return product.product_images?.toSorted((a, b) => a.sort_order - b.sort_order)[0]?.storage_path
    ?? product.image_url;
}

function stockLabel(product: Product) {
  if (!product.available || product.stock_quantity === 0) return "Agotado";
  if (product.stock_quantity === null) return "Disponible";
  return `${product.stock_quantity} disponibles`;
}

export default function CatalogPage() {
  const [products, setProducts] = useState<Product[]>([]);
  const [query, setQuery] = useState("");
  const [category, setCategory] = useState("Todas");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");
  const [addingId, setAddingId] = useState<string | null>(null);
  const deferredQuery = useDeferredValue(query.trim().toLocaleLowerCase("es-PE"));

  useEffect(() => {
    let active = true;
    void (async () => {
      try {
        const { data, error: requestError } = await createClient()
          .from("products")
          .select("id,slug,sku,name,description,category,price_cents,currency,available,stock_quantity,image_url,updated_at,product_images(storage_path,sort_order)")
          .order("name");
        if (requestError) throw requestError;
        if (active) setProducts((data ?? []) as unknown as Product[]);
      } catch {
        if (active) setError("No pudimos cargar el catálogo. Revisa tu conexión e inténtalo nuevamente.");
      } finally {
        if (active) setLoading(false);
      }
    })();
    return () => { active = false; };
  }, []);

  const categories = useMemo(
    () => ["Todas", ...Array.from(new Set(products.map(product => product.category).filter(Boolean) as string[]))],
    [products],
  );
  const visible = useMemo(() => products.filter(product => {
    const matchesCategory = category === "Todas" || product.category === category;
    const searchable = `${product.name} ${product.sku} ${product.category ?? ""}`.toLocaleLowerCase("es-PE");
    return matchesCategory && searchable.includes(deferredQuery);
  }), [category, deferredQuery, products]);

  async function addToCart(product: Product) {
    if (!product.available || product.stock_quantity === 0) return;
    setAddingId(product.id);
    setMessage("");
    try {
      const { supabase, teamId } = await getSessionContext();
      const cart = await getOrCreateActiveCart(supabase, teamId);
      await incrementCartItem(supabase, cart.id, product.id, 1);
      setMessage(`${product.name} se añadió al carrito.`);
    } catch {
      setMessage("No pudimos añadir el producto. Comprueba el stock y vuelve a intentarlo.");
    } finally {
      setAddingId(null);
    }
  }

  return (
    <main>
      <section className="hero hero-grid">
        <div>
          <p className="eyebrow">VV / Colecciones</p>
          <h1>Encuentra tu próxima venta.</h1>
          <p className="lede">Consulta el mismo catálogo, precio y stock disponible desde la web y la app.</p>
        </div>
        <div className="hero-stat" aria-label={`${products.length} productos en el catálogo`}>
          <strong>{products.length}</strong><span>productos en catálogo</span>
        </div>
      </section>

      {message ? <div className="notice" role="status" aria-live="polite">{message}</div> : null}

      <section className="toolbar" aria-label="Filtros del catálogo">
        <label className="search">
          <span className="sr-only">Buscar productos</span>
          <input
            name="catalog-search"
            type="search"
            value={query}
            onChange={event => setQuery(event.target.value)}
            placeholder="Buscar por nombre, SKU o categoría…"
          />
        </label>
        <label>
          <span className="sr-only">Categoría</span>
          <select name="catalog-category" value={category} onChange={event => setCategory(event.target.value)}>
            {categories.map(item => <option key={item}>{item}</option>)}
          </select>
        </label>
      </section>

      {loading ? (
        <div className="state card"><div className="spinner" /><p>Cargando catálogo…</p></div>
      ) : error ? (
        <div className="state card" role="alert">
          <h2>No pudimos cargar el catálogo</h2><p>{error}</p>
          <button onClick={() => location.reload()}>Reintentar</button>
        </div>
      ) : visible.length === 0 ? (
        <div className="state card"><h2>No encontramos productos</h2><p className="muted">Prueba otra búsqueda o categoría.</p></div>
      ) : (
        <section className="grid" aria-label="Productos">
          {visible.map(product => {
            const image = productImage(product);
            const canAdd = product.available && product.stock_quantity !== 0;
            return (
              <article className="product-card" key={product.id}>
                <div className="product-image">
                  {image
                    ? <img src={image} alt={product.name} loading="lazy" width="640" height="640" />
                    : <span aria-hidden="true">{product.name.slice(0, 1)}</span>}
                </div>
                <div className="product-content">
                  <div className="section-head">
                    <span className="badge">{product.category ?? "Sin categoría"}</span>
                    <span className="muted">{stockLabel(product)}</span>
                  </div>
                  <h2>{product.name}</h2>
                  <p className="muted">SKU {product.sku}</p>
                  <p className="product-description">{product.description || "Descripción por completar."}</p>
                  <strong>{money(product.price_cents, product.currency)}</strong>
                  <div className="button-row">
                    <Link className="button secondary" href={`/catalogo/${product.slug}`}>Ver ficha</Link>
                    <button disabled={!canAdd || addingId === product.id} onClick={() => void addToCart(product)}>
                      {addingId === product.id ? "Añadiendo…" : canAdd ? "Añadir" : "Agotado"}
                    </button>
                  </div>
                </div>
              </article>
            );
          })}
        </section>
      )}
    </main>
  );
}
