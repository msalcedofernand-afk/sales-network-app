const MAX_SIDE = 1600;
const JPEG_QUALITY = 0.84;

export async function compressEvidence(file: File) {
  if (!file.type.startsWith("image/")) throw new Error("Selecciona una imagen válida.");
  const bitmap = await createImageBitmap(file);
  const scale = Math.min(1, MAX_SIDE / Math.max(bitmap.width, bitmap.height));
  const width = Math.max(1, Math.round(bitmap.width * scale));
  const height = Math.max(1, Math.round(bitmap.height * scale));
  const canvas = document.createElement("canvas");
  canvas.width = width;
  canvas.height = height;
  const context = canvas.getContext("2d");
  if (!context) throw new Error("No pudimos procesar la imagen.");
  context.drawImage(bitmap, 0, 0, width, height);
  bitmap.close();
  const blob = await new Promise<Blob>((resolve, reject) => {
    canvas.toBlob(result => result ? resolve(result) : reject(new Error("No pudimos comprimir la imagen.")), "image/jpeg", JPEG_QUALITY);
  });
  if (blob.size > 2_500_000) throw new Error("La imagen continúa siendo demasiado grande.");
  return blob;
}
