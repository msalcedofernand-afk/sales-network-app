"use client";

import { ChangeEvent, useState } from "react";
import { getSessionContext } from "../../../lib/app-data";

type CatalogRow = {
  sku?: string;
  slug?: string;
  name?: string;
  price?: string | number;
  currency?: string;
  category?: string;
  description?: string;
  available?: boolean | string;
  image_urls?: string[];
};

function parseCsvLine(line: string) {
  const cells: string[] = [];
  let value = "";
  let quoted = false;
  for (let i = 0; i < line.length; i += 1) {
    const char = line[i];
    if (char === '"') quoted = !quoted;
    else if (char === "," && !quoted) { cells.push(value.trim()); value = ""; }
    else value += char;
  }
  cells.push(value.trim());
  return cells.map(cell => cell.replace(/^"|"$/g, "").replace(/""/g, '"'));
}

function parseCsv(text: string): CatalogRow[] {
  const lines = text.split(/\r?\n/).filter(Boolean);
  if (lines.length < 2) return [];
  const headers = parseCsvLine(lines[0]).map(header => header.toLowerCase().replace(/\s+/g, "_"));
  return lines.slice(1).map(line => {
    const values = parseCsvLine(line);
    const row: Record<string, string> = {};
    headers.forEach((header, index) => { row[header] = values[index] ?? ""; });
    return {
      ...row,
      price: row.price || row.precio,
      currency: row.currency || row.moneda || "PEN",
      available: row.available !== "false" && row.available !== "0",
      image_urls: (row.image_urls || row.imagen || "").split("|").map(value => value.trim()).filter(Boolean),
    };
  });
}

export default function AdminCatalogPage() {
  const [file, setFile] = useState<File | null>(null);
  const [preview, setPreview] = useState<CatalogRow[]>([]);
  const [result, setResult] = useState("");
  const [busy, setBusy] = useState(false);

  async function chooseFile(event: ChangeEvent<HTMLInputElement>) {
    const selected = event.target.files?.[0] ?? null;
    setFile(selected);
    setResult("");
    setPreview([]);
    if (!selected) return;
    if (selected.size > 2 * 1024 * 1024) { setResult("El archivo supera el límite de 2 MB."); return; }
    try {
      const text = await selected.text();
      const parsed = selected.name.toLowerCase().endsWith(".csv") ? parseCsv(text) : JSON.parse(text);
      const rows = Array.isArray(parsed) ? parsed : parsed.items;
      if (!Array.isArray(rows)) throw new Error("El archivo debe contener una lista de productos.");
      setPreview(rows.slice(0, 8));
      setResult("Vista previa lista. Se muestran las primeras " + Math.min(rows.length, 8) + " filas.");
    } catch { setResult("No pudimos leer el archivo. Revisa el CSV o JSON."); }
  }

  async function importCatalog() {
    if (!file || file.size > 2 * 1024 * 1024) return;
    setBusy(true);
    try {
      const text = await file.text();
      const parsed = file.name.toLowerCase().endsWith(".csv") ? parseCsv(text) : JSON.parse(text);
      const items = Array.isArray(parsed) ? parsed : parsed.items;
      const { supabase, teamId } = await getSessionContext();
      const response = await supabase.functions.invoke("sync-catalog", { body: { team_id: teamId, items } });
      if (response.error) throw response.error;
      setResult("Importados: " + response.data.imported_count + ". Rechazados: " + response.data.rejected_count + ".");
    } catch (error) {
      setResult(error instanceof Error ? error.message : "El archivo no se pudo importar.");
    } finally { setBusy(false); }
  }

  return <main><section className="hero"><p className="eyebrow">VV / Administración</p><h1>Publica tu catálogo.</h1><p className="lede">Valida un CSV o JSON antes de enviar productos a tu equipo.</p></section><section className="card"><h2>Importar productos</h2><p className="muted">CSV: sku, slug, name, price, currency, category, description, available, image_urls. Usa barra vertical para varias imágenes.</p><label htmlFor="catalog-file">Archivo de catálogo<input id="catalog-file" type="file" accept=".json,.csv,application/json,text/csv" onChange={chooseFile} /></label>{preview.length > 0 && <div className="table-wrap" aria-label="Vista previa"><table className="table"><thead><tr><th>SKU</th><th>Nombre</th><th>Precio</th><th>Categoría</th></tr></thead><tbody>{preview.map((row, index) => <tr key={index}><td>{row.sku || "—"}</td><td>{row.name || "—"}</td><td>{String(row.price || "—")}</td><td>{row.category || "—"}</td></tr>)}</tbody></table></div>}<button style={{ marginTop: 14 }} disabled={!file || busy || file.size > 2 * 1024 * 1024} onClick={importCatalog}>{busy ? "Importando…" : "Validar e importar"}</button>{result && <p className="notice" role="status">{result}</p>}</section></main>;
}
