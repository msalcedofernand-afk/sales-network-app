"use client";

import Link from "next/link";
import { useCallback, useEffect, useState } from "react";
import { getSessionContext, money } from "../../lib/app-data";
import { compressEvidence } from "../../lib/images";

type OrderStatus = "PENDIENTE" | "CONFIRMADO" | "COBRADO" | "ENTREGADO" | "CANCELADO" | "DEVUELTO";
type Order = {
  id: string;
  user_id: string;
  status: OrderStatus;
  total_cents: number;
  amount_paid_cents: number | null;
  payment_method: string | null;
  created_at: string;
  customer_id: string | null;
  customers: { name: string } | null;
  order_items: { product_name: string; quantity: number; unit_price_cents: number }[];
};

type Action = { order: Order; status: OrderStatus } | null;

function nextActions(status: OrderStatus): OrderStatus[] {
  switch (status) {
    case "PENDIENTE": return ["CONFIRMADO", "CANCELADO"];
    case "CONFIRMADO": return ["COBRADO", "CANCELADO"];
    case "COBRADO": return ["ENTREGADO", "CANCELADO"];
    case "ENTREGADO": return ["DEVUELTO"];
    default: return [];
  }
}

function actionLabel(status: OrderStatus) {
  return ({
    CONFIRMADO: "Confirmar",
    COBRADO: "Registrar cobro",
    ENTREGADO: "Registrar entrega",
    CANCELADO: "Cancelar pedido",
    DEVUELTO: "Registrar devolución",
    PENDIENTE: "Pendiente",
  })[status];
}

export default function OrdersPage() {
  const [rows, setRows] = useState<Order[]>([]);
  const [message, setMessage] = useState("");
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [action, setAction] = useState<Action>(null);
  const [reason, setReason] = useState("");
  const [paymentMethod, setPaymentMethod] = useState("YAPE");
  const [evidence, setEvidence] = useState<File | null>(null);

  const load = useCallback(async () => {
    try {
      const { supabase, user, teamId, membership } = await getSessionContext();
      let query = supabase.from("orders")
        .select("id,user_id,status,total_cents,amount_paid_cents,payment_method,created_at,customer_id,customers(name),order_items(product_name,quantity,unit_price_cents)")
        .eq("team_id", teamId)
        .order("created_at", { ascending: false });
      if (!["LIDER", "ROOT_ADMIN"].includes(membership.role)) query = query.eq("user_id", user.id);
      const { data, error } = await query;
      if (error) throw error;
      setRows((data ?? []) as unknown as Order[]);
    } catch {
      setMessage("No pudimos cargar los pedidos. Revisa tu sesión y conexión.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { void load(); }, [load]);

  function begin(order: Order, status: OrderStatus) {
    if (status === "CONFIRMADO") {
      void submit(order, status);
      return;
    }
    setReason("");
    setEvidence(null);
    setPaymentMethod("YAPE");
    setAction({ order, status });
  }

  async function uploadEvidence(order: Order, status: OrderStatus, file: File) {
    const { supabase, user } = await getSessionContext();
    const blob = await compressEvidence(file);
    const kind = status === "COBRADO" ? "payment" : "delivery";
    const path = `${user.id}/${order.id}-${kind}-${crypto.randomUUID()}.jpg`;
    const { error } = await supabase.storage.from("order-proofs").upload(path, blob, {
      contentType: "image/jpeg",
      upsert: false,
    });
    if (error) throw error;
    return { supabase, path };
  }

  async function submit(order: Order, status: OrderStatus) {
    if (["CANCELADO", "DEVUELTO"].includes(status) && reason.trim().length < 3) {
      setMessage("Escribe un motivo de al menos 3 caracteres.");
      return;
    }
    if (status === "COBRADO" && paymentMethod !== "EFECTIVO" && !evidence) {
      setMessage("Selecciona la foto del comprobante.");
      return;
    }

    setBusy(true);
    setMessage("");
    let uploadedPath: string | null = null;
    try {
      const upload = evidence ? await uploadEvidence(order, status, evidence) : null;
      uploadedPath = upload?.path ?? null;
      const { supabase } = upload ?? await getSessionContext();
      const { error } = await supabase.rpc("transition_order_status_v2", {
        input_order_id: order.id,
        next_status: status,
        reason: reason.trim() || null,
        payment_method: status === "COBRADO" ? paymentMethod : null,
        input_amount_paid_cents: status === "COBRADO" ? order.total_cents : null,
        proof_path: status === "COBRADO" ? uploadedPath : null,
        input_delivery_proof_path: status === "ENTREGADO" ? uploadedPath : null,
        event_source: "WEB",
      });
      if (error) throw error;
      setAction(null);
      setMessage("Estado actualizado y registrado en el historial.");
      await load();
    } catch {
      if (uploadedPath) {
        const { supabase } = await getSessionContext();
        await supabase.storage.from("order-proofs").remove([uploadedPath]);
      }
      setMessage("No pudimos actualizar el pedido. Revisa los datos e inténtalo nuevamente.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <main>
      <section className="hero"><p className="eyebrow">VV / Ventas</p><h1>Tu negocio en movimiento.</h1><p className="lede">Cada cobro, entrega, cancelación y devolución queda registrado.</p></section>
      {message ? <div className="notice" role="status" aria-live="polite">{message}</div> : null}
      {loading ? (
        <div className="state card"><div className="spinner" /><p>Cargando pedidos…</p></div>
      ) : rows.length === 0 ? (
        <div className="state card"><h2>Aún no tienes pedidos</h2><p className="muted">Agrega productos al carrito para crear el primero.</p><Link className="button" href="/catalogo">Ir al catálogo</Link></div>
      ) : (
        <section className="list">
          {rows.map(order => (
            <article className="card" key={order.id}>
              <div className="section-head">
                <div><span className="badge">{order.status}</span><h2>{order.customers?.name ?? "Cliente archivado"}</h2><p className="muted">{new Date(order.created_at).toLocaleDateString("es-PE")}</p></div>
                <strong>{money(order.total_cents)}</strong>
              </div>
              <ul>{order.order_items?.map((item, index) => <li key={`${item.product_name}-${index}`}>{item.quantity} × {item.product_name} · {money(item.unit_price_cents * item.quantity)}</li>)}</ul>
              {order.payment_method ? <p className="muted">Cobrado con {order.payment_method} · {money(order.amount_paid_cents ?? 0)}</p> : null}
              <div className="button-row">
                {nextActions(order.status).map(status => <button key={status} className={status === "CANCELADO" || status === "DEVUELTO" ? "button ghost" : "button"} disabled={busy} onClick={() => begin(order, status)}>{actionLabel(status)}</button>)}
              </div>
            </article>
          ))}
        </section>
      )}

      {action ? (
        <div className="modal-overlay" role="presentation" onMouseDown={event => { if (event.target === event.currentTarget && !busy) setAction(null); }}>
          <section className="modal" role="dialog" aria-modal="true" aria-labelledby="order-action-title">
            <h2 id="order-action-title">{actionLabel(action.status)}</h2>
            <p className="muted">Pedido de {action.order.customers?.name ?? "cliente"} por {money(action.order.total_cents)}.</p>
            <div className="auth-form">
              {action.status === "COBRADO" ? <label>Método de pago<select name="payment-method" value={paymentMethod} onChange={event => setPaymentMethod(event.target.value)}><option>YAPE</option><option>PLIN</option><option>TRANSFERENCIA</option><option>EFECTIVO</option><option>OTRO</option></select></label> : null}
              {["CANCELADO", "DEVUELTO"].includes(action.status) ? <label>Motivo<textarea name="reason" value={reason} onChange={event => setReason(event.target.value)} placeholder="Explica brevemente el motivo…" autoFocus /></label> : null}
              {["COBRADO", "ENTREGADO"].includes(action.status) ? <label>{action.status === "COBRADO" ? "Comprobante de pago" : "Evidencia de entrega (opcional)"}<input name="evidence" type="file" accept="image/*" onChange={event => setEvidence(event.target.files?.[0] ?? null)} /></label> : null}
              <div className="button-row"><button disabled={busy} onClick={() => void submit(action.order, action.status)}>{busy ? "Guardando…" : "Guardar cambio"}</button><button className="button ghost" disabled={busy} onClick={() => setAction(null)}>Volver</button></div>
            </div>
          </section>
        </div>
      ) : null}
    </main>
  );
}
