"use client";

import { useEffect, useState } from "react";
import type { MouseEvent as ReactMouseEvent } from "react";

type SelectedElement = {
  id: string;
  type: string;
  element: HTMLElement;
  rect: DOMRect;
  text: string;
  role: string;
  accessibleName: string;
};

type InspectorIssue = {
  id: string;
  schemaVersion: 1;
  platform: "web";
  screen: string;
  route: string;
  elementId: string;
  issueType: string;
  severity: string;
  status: "open" | "fixed" | "verified";
  title: string;
  description: string;
  expected: string;
  actual: string;
  screenshotFile?: string;
  fullScreenshotFile?: string;
  createdAt: string;
  appVersion: string;
};

const enabled = process.env.NEXT_PUBLIC_ENABLE_DEV_INSPECTOR === "true" || process.env.NODE_ENV === "development";
const issueTypes = ["layout", "text", "image", "accessibility", "functional", "data", "performance", "other"];
const severities = ["P0", "P1", "P2", "P3"];

function makeId() {
  return globalThis.crypto?.randomUUID?.() ?? `issue-${Date.now()}-${Math.random().toString(16).slice(2)}`;
}

function safeText(value: string | null | undefined, max = 240) {
  return (value ?? "").replace(/\s+/g, " ").trim().slice(0, max);
}

function downloadBlob(blob: Blob, filename: string) {
  const url = URL.createObjectURL(blob);
  const anchor = document.createElement("a");
  anchor.href = url;
  anchor.download = filename;
  anchor.click();
  URL.revokeObjectURL(url);
}

async function capture(element: HTMLElement) {
  const { default: html2canvas } = await import("html2canvas");
  const canvas = await html2canvas(element, { useCORS: true, backgroundColor: "#ffffff", logging: false });
  return canvas.toDataURL("image/png");
}

function dataUrlToBlob(dataUrl: string) {
  const [header, data] = dataUrl.split(",");
  const bytes = Uint8Array.from(atob(data), char => char.charCodeAt(0));
  return new Blob([bytes], { type: header.match(/data:(.*);base64/)?.[1] ?? "application/octet-stream" });
}

export default function DevInspector() {
  const [active, setActive] = useState(false);
  const [selected, setSelected] = useState<SelectedElement | null>(null);
  const [issues, setIssues] = useState<InspectorIssue[]>([]);
  const [type, setType] = useState("layout");
  const [severity, setSeverity] = useState("P2");
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [expected, setExpected] = useState("");
  const [actual, setActual] = useState("");
  const [screenshot, setScreenshot] = useState<string | null>(null);
  const [fullScreenshot, setFullScreenshot] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [notice, setNotice] = useState("");

  const [inspectableCount, setInspectableCount] = useState(0);

  useEffect(() => {
    if (active) setInspectableCount(document.querySelectorAll("[data-inspectable='true']").length);
  }, [active, selected]);

  useEffect(() => {
    if (!enabled) return;
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.ctrlKey && event.shiftKey && event.key.toLowerCase() === "i") {
        event.preventDefault();
        setActive(value => !value);
      }
    };
    window.addEventListener("keydown", onKeyDown);
    return () => window.removeEventListener("keydown", onKeyDown);
  }, []);

  useEffect(() => {
    if (!enabled || !active) return;
    const onClick = (event: MouseEvent) => {
      const target = event.target instanceof Element ? event.target.closest<HTMLElement>("[data-inspectable='true']") : null;
      if (!target) return;
      event.preventDefault();
      event.stopPropagation();
      const rect = target.getBoundingClientRect();
      setSelected({
        id: target.dataset.inspectorId ?? target.id ?? target.tagName.toLowerCase(),
        type: target.dataset.inspectorType ?? target.tagName.toLowerCase(),
        element: target,
        rect,
        text: safeText(target.textContent),
        role: target.getAttribute("role") ?? target.tagName.toLowerCase(),
        accessibleName: safeText(target.getAttribute("aria-label") ?? target.textContent, 120),
      });
      setScreenshot(null);
      setFullScreenshot(null);
      setNotice("");
    };
    document.addEventListener("click", onClick, true);
    return () => document.removeEventListener("click", onClick, true);
  }, [active]);

  if (!enabled) return null;

  async function prepareEvidence() {
    if (!selected) return;
    setBusy(true);
    setNotice("");
    try {
      setScreenshot(await capture(selected.element));
      setFullScreenshot(await capture(document.body));
      setNotice("Evidencia capturada localmente.");
    } catch {
      setNotice("No se pudo capturar el elemento. Revisa imágenes externas o contenido dinámico.");
    } finally {
      setBusy(false);
    }
  }

  function addIssue(event: ReactMouseEvent<HTMLButtonElement>) {
    event.preventDefault();
    if (!selected || !title.trim() || !description.trim()) return;
    const issue: InspectorIssue = {
      id: makeId(), schemaVersion: 1, platform: "web", screen: document.title || location.pathname,
      route: location.pathname, elementId: selected.id, issueType: type, severity, status: "open",
      title: title.trim().slice(0, 160), description: description.trim().slice(0, 2000),
      expected: expected.trim().slice(0, 1000), actual: actual.trim().slice(0, 1000),
      screenshotFile: screenshot ? `screenshots/web/${selected.id}-${Date.now()}.png` : undefined,
      fullScreenshotFile: fullScreenshot ? `screenshots/web/full-${Date.now()}.png` : undefined,
      createdAt: new Date().toISOString(), appVersion: process.env.NEXT_PUBLIC_APP_VERSION ?? "unknown",
    };
    setIssues(current => [...current, issue]);
    setTitle(""); setDescription(""); setExpected(""); setActual("");
    setNotice(`Incidencia ${issue.id} guardada en memoria.`);
  }

  async function exportAll() {
    if (!issues.length) {
      setNotice("Registra al menos una incidencia antes de exportar.");
      return;
    }
    const { default: JSZip } = await import("jszip");
    const zip = new JSZip();
    const lines = issues.map(issue => JSON.stringify(issue)).join("\n") + "\n";
    zip.file("issues.jsonl", lines);
    zip.file("manifest.json", JSON.stringify({ schemaVersion: 1, exportedAt: new Date().toISOString(), platform: "web", route: location.pathname, issueCount: issues.length }, null, 2));
    zip.file("README.txt", "Reporte local del Dev Inspector. Revisar y sanitizar antes de compartir fuera del equipo.");
    if (screenshot) zip.file("screenshots/web/current-element.png", dataUrlToBlob(screenshot));
    if (fullScreenshot) zip.file("screenshots/web/current-page.png", dataUrlToBlob(fullScreenshot));
    downloadBlob(await zip.generateAsync({ type: "blob" }), `sales-network-inspector-${new Date().toISOString().slice(0, 10)}.zip`);
    setNotice("Reporte exportado.");
  }

  return (
    <>
      <button className="dev-inspector-toggle" type="button" onClick={() => setActive(value => !value)} aria-pressed={active}>
        {active ? "Cerrar inspector" : "Inspector beta"}
      </button>
      {active ? <div className="dev-inspector-bar" role="status">Modo inspector activo · {inspectableCount} elementos · Ctrl + Shift + I para cerrar</div> : null}
      {selected ? <aside className="dev-inspector-panel" aria-label="Panel Dev Inspector">
        <div className="dev-inspector-head"><div><strong>Dev Inspector</strong><span>{selected.type} · {selected.id}</span></div><button type="button" className="button ghost" onClick={() => setSelected(null)}>Cerrar</button></div>
        <dl className="dev-inspector-meta"><div><dt>Ruta</dt><dd>{location.pathname}</dd></div><div><dt>Dimensiones</dt><dd>{Math.round(selected.rect.width)} × {Math.round(selected.rect.height)} px</dd></div><div><dt>Rol</dt><dd>{selected.role}</dd></div><div><dt>Contenido</dt><dd>{selected.text || "(sin texto)"}</dd></div><div><dt>Nombre accesible</dt><dd>{selected.accessibleName || "(sin nombre)"}</dd></div></dl>
        <button type="button" className="button secondary" onClick={() => void prepareEvidence()} disabled={busy}>{busy ? "Capturando…" : "Capturar evidencia"}</button>
        <div className="dev-inspector-form"><label>Tipo<select value={type} onChange={event => setType(event.target.value)}>{issueTypes.map(item => <option key={item}>{item}</option>)}</select></label><label>Prioridad<select value={severity} onChange={event => setSeverity(event.target.value)}>{severities.map(item => <option key={item}>{item}</option>)}</select></label><label>Título<input value={title} onChange={event => setTitle(event.target.value)} placeholder="Qué debe corregirse" /></label><label>Descripción<textarea value={description} onChange={event => setDescription(event.target.value)} placeholder="Qué ocurre y cómo reproducirlo" /></label><label>Resultado esperado<textarea value={expected} onChange={event => setExpected(event.target.value)} /></label><label>Resultado actual<textarea value={actual} onChange={event => setActual(event.target.value)} /></label><button type="button" onClick={addIssue} disabled={!title.trim() || !description.trim()}>Guardar incidencia</button></div>
        {notice ? <p className="dev-inspector-notice" role="status">{notice}</p> : null}
        <div className="dev-inspector-footer"><span>{issues.length} incidencia(s)</span><button type="button" className="button ghost" onClick={() => void exportAll()}>Exportar todo</button></div>
      </aside> : null}
    </>
  );
}
