"use client";

import Link from "next/link";
import { useCallback, useMemo, useRef, useState, useEffect } from "react";
import { getSessionContext, money } from "../../lib/app-data";
import { getOrCreateActiveCart, incrementCartItem } from "../../lib/cart";

type Item = {
  id: string;
  quantity: number;
  product_id: string;
  products: {
    name: string;
    sku: string;
    price_cents: number;
    currency: string;
    available: boolean;
    stock_quantity: number | null;
  } | null;
};
type Customer = { id: string; name: string };

export default function CartPage() {
  const [items, setItems] = useState<Item[]>([]);
  const [customers, setCustomers] = useState<Customer[]>([]);
  const [customerId, setCustomerId] = useState("");
  const [cartId, setCartId] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState("");
  const checkoutKey = useRef<string | null>(null);

  const load = useCallback(async () => {
    try {
      const { supabase, teamId } = await getSessionContext();
      const cart = await getOrCreateActiveCart(supabase, teamId);
      setCartId(cart.id);
      const [cartResult, customerResult] = await Promise.all([
        supabase.from("cart_items")
          .select("id,quantity,product_id,products(name,sku,price_cents,currency,available,stock_quantity)")
          .eq("cart_id", cart.id),
        supabase.from("customers").select("id,name")
          .eq("team_id", teamId).eq("archived", false).order("name"),
      ]);
      if (cartResult.error) throw cartResult.error;
      if (customerResult.error) throw customerResult.error;
      setItems((cartResult.data ?? []) as unknown as Item[]);
      setCustomers((customerResult.data ?? []) as Customer[]);
    } catch {
      setMessage("No pudimos cargar el carrito. Revisa tu conexión e inténtalo nuevamente.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { void load(); }, [load]);

  async function change(item: Item, delta: number) {
    if (!cartId || busy) return;
    setBusy(true);
    setMessage("");
    try {
      const { supabase } = await getSessionContext();
      await incrementCartItem(supabase, cartId, item.product_id, delta);
      await load();
    } catch {
      setMessage("No pudimos cambiar la cantidad. Comprueba las existencias disponibles.");
    } finally {
      setBusy(false);
    }
  }

  async function checkout() {
    if (!cartId || !customerId) {
      setMessage("Selecciona un cliente antes de confirmar.");
      return;
    }
    setBusy(true);
    setMessage("");
    try {
      const { supabase } = await getSessionContext();
      checkoutKey.current ??= `web-${crypto.randomUUID()}`;
      const result = await supabase.functions.invoke("checkout-cart", {
        body: { cart_id: cartId, customer_id: customerId, idempotency_key: checkoutKey.current },
      });
      if (result.error) throw result.error;
      checkoutKey.current = null;
      setMessage("Pedido confirmado correctamente.");
      await load();
    } catch {
      setMessage("No pudimos confirmar el pedido. Comprueba tu conexión, precio y stock.");
    } finally {
      setBusy(false);
    }
  }

  const total = useMemo(
    () => items.reduce((sum, item) => sum + (item.products?.price_cents ?? 0) * item.quantity, 0),
    [items],
  );
  const canCheckout = items.length > 0 && items.every(item => item.products?.available &&
    (item.products.stock_quantity === null || item.products.stock_quantity >= item.quantity));

  if (loading) return <main><div className="state card"><div className="spinner" /><p>Cargando tu carrito…</p></div></main>;
  return (
    <main>
      <section className="hero"><p className="eyebrow">VV / Compra</p><h1>Tu carrito.</h1><p className="lede">Revisa cantidades y confirma el pedido con precios y stock del servidor.</p></section>
      {message ? <div className="notice" role="status" aria-live="polite">{message}</div> : null}
      {items.length === 0 ? (
        <div className="state card"><h2>Tu carrito está vacío</h2><p className="muted">Explora el catálogo y añade productos para comenzar.</p><Link className="button" href="/catalogo">Explorar catálogo</Link></div>
      ) : (
        <>
          <section className="list" aria-label="Productos del carrito">
            {items.map(item => {
              const available = item.products?.available && (item.products.stock_quantity === null || item.products.stock_quantity >= item.quantity);
              return (
                <article className="list-row" key={item.id}>
                  <div>
                    <strong>{item.products?.name ?? "Producto"}</strong>
                    <p className="muted">SKU {item.products?.sku} · {available ? "Disponible" : "Sin stock suficiente"}</p>
                  </div>
                  <div className="quantity-controls">
                    <button className="button ghost" disabled={busy} onClick={() => void change(item, -1)} aria-label={`Disminuir ${item.products?.name}`}>−</button>
                    <strong aria-live="polite">{item.quantity}</strong>
                    <button className="button ghost" disabled={busy || !available} onClick={() => void change(item, 1)} aria-label={`Aumentar ${item.products?.name}`}>+</button>
                    <strong>{money((item.products?.price_cents ?? 0) * item.quantity, item.products?.currency)}</strong>
                  </div>
                </article>
              );
            })}
          </section>
          <section className="card cart-summary">
            <div className="section-head"><div><span className="muted">Total confirmado al crear el pedido</span><h2>{money(total)}</h2></div><Link className="button ghost" href="/pedidos">Ver pedidos</Link></div>
            <div className="form-grid">
              <label className="full">Cliente
                <select name="customer" value={customerId} onChange={event => setCustomerId(event.target.value)}>
                  <option value="">Selecciona un cliente</option>
                  {customers.map(customer => <option key={customer.id} value={customer.id}>{customer.name}</option>)}
                </select>
              </label>
              <button className="full" disabled={busy || !customerId || !canCheckout} onClick={() => void checkout()}>
                {busy ? "Confirmando…" : canCheckout ? "Confirmar pedido" : "Revisa el stock"}
              </button>
            </div>
          </section>
        </>
      )}
    </main>
  );
}
