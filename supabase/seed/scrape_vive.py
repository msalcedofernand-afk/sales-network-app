#!/usr/bin/env python3
"""Importa el catálogo público de Vive en un JSON de revisión.
Uso respetuoso: una petición por ficha y sin credenciales.
"""
import html, json, re, time
from pathlib import Path
from urllib.request import Request, urlopen
from urllib.parse import urljoin

BASE = "https://viveoficial.com"
LIST_URL = BASE + "/productos"
OUT = Path(__file__).with_name("vive_catalog.json")
CATEGORY_BY_SLUG = {
    "botella-purificadora": "Tecnologica", "cafe-gold-capucchino": "Dia a dia", "inkanzable-max": "Nutrifit",
    "colageno-colafem": "Siempre joven", "colageno-del-mar": "Siempre joven", "detox-dtx-max": "Inmunologica",
    "fitburn": "Nutrifit", "quemador-bitburn": "Nutrifit", "gold-black-coffee": "Inmunologica", "cafe-black-coffee": "Inmunologica", "gold-vivelate-gourmet": "Dia a dia",
    "gomitas-vital-cbd": "CBD", "green-food-clorofila": "Inmunologica", "clorofila-green-food": "Inmunologica", "inkanzable": "Dia a dia", "energizante-inkanzable": "Dia a dia",
    "locion-analgesica": "CBD", "locion-analgesica-cbd": "CBD", "ph-max": "Inmunologica", "pureshield": "Inmunologica", "sumplementos-pureshield": "Inmunologica", "purifit": "Nutrifit", "sumplementos-purifit": "Nutrifit", "detoxificante-dtx-max": "Inmunologica",
    "superkids-nutri-gummies": "Kids", "vitaminas-nutrigummies": "Kids", "vit-aceite-de-moringa-en-capsulas-blandas": "Inmunologica", "moringa-aceite-en-capsulas-vit": "Inmunologica",
    "vit-aceite-de-moringa-en-spray": "Siempre joven", "moringa-aceite-prensado-al-frio": "Siempre joven", "vit-omega-3-6-9": "Dia a dia", "sacha-inchi-omega369": "Dia a dia", "vital-cbd": "CBD", "vital-cbd-sublingual": "CBD", "vitamoringa": "Dia a dia", "vitaminas-vitamoringa": "Dia a dia",
}


def fetch(url):
    req = Request(url, headers={"User-Agent": "SalesNetworkCatalogImporter/1.0 (catalog sync)"})
    with urlopen(req, timeout=30) as response:
        return response.read().decode("utf-8", "replace")


def text(value):
    value = re.sub(r"<[^>]+>", " ", value)
    return re.sub(r"\s+", " ", html.unescape(value)).strip()

listing = fetch(LIST_URL)
anchors = re.findall(r'<a[^>]+href="(/productos/[^"]+)"[\s\S]*?</a>', listing)
products, seen = [], set()
for path in anchors:
    if path in seen: continue
    seen.add(path)
    card = re.search(r'<a[^>]+href="' + re.escape(path) + r'"[\s\S]*?</a>', listing)
    card_html = card.group(0) if card else ""
    detail_url = urljoin(BASE, path)
    try:
        detail = fetch(detail_url)
    except Exception as exc:
        print(f"WARN {detail_url}: {exc}")
        continue
    h1 = re.search(r'<h1[^>]*>([\s\S]*?)</h1>', detail)
    name = text(h1.group(1)) if h1 else path.rsplit("/", 1)[-1].replace("-", " ").title()
    meta = re.search(r'<meta[^>]+name="description"[^>]+content="([^"]*)"', detail, re.I)
    description = html.unescape(meta.group(1)).strip() if meta else ""
    if not description:
        after = detail[h1.end():] if h1 else detail
        paragraph = re.search(r'<p[^>]*>([\s\S]*?)</p>', after)
        description = text(paragraph.group(1)) if paragraph else ""
    category = "Otros"
    cat = re.search(r'href="/productos/categoria/[^\"]+"[^>]*>([\s\S]*?)</a>', detail, re.I)
    if cat: category = text(cat.group(1))
    if category == "Otros":
        cat_card = re.search(r'<span[^>]*>(Tecnologica|Siempre joven|Inmunologica|Nutrifit|Dia a dia|CBD|Kids)</span>', card_html, re.I)
        if cat_card: category = text(cat_card.group(1))
    category = CATEGORY_BY_SLUG.get(path.rsplit("/", 1)[-1], category)
    images = []
    for src in re.findall(r'<img[^>]+src="([^"]+)"', detail, re.I):
        src = html.unescape(src)
        if "cdn.viveoficial.com" in src and src not in images: images.append(src)
    for src in re.findall(r'<img[^>]+src="([^"]+)"', card_html, re.I):
        src = html.unescape(src)
        if "cdn.viveoficial.com" in src and src not in images: images.append(src)
    products.append({
        "source_url": detail_url,
        "slug": path.rsplit("/", 1)[-1],
        "sku": "VIVE-" + path.rsplit("/", 1)[-1].upper().replace("-", "_"),
        "name": name,
        "category": category,
        "description": description,
        "price_cents": None,
        "price_status": "Precio disponible próximamente; confirmar con proveedor.",
        "currency": "PEN",
        "available": False,
        "image_urls": images,
        "scraped_at": time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime()),
    })
    print(f"OK {name} ({len(images)} images)")
    time.sleep(0.35)

OUT.write_text(json.dumps(products, ensure_ascii=False, indent=2), encoding="utf-8")
print(f"Wrote {len(products)} products to {OUT}")
