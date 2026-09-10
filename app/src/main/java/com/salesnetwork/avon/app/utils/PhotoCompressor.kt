package com.salesnetwork.avon.app.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max

data class CompressedPhoto(
    val file: File,
    val mimeType: String,
    val width: Int,
    val height: Int,
    val bytes: Long
)

/**
 * Compresses proof photos before upload. Decoding and re-encoding also strips
 * EXIF location/device metadata. The original URI is never modified.
 */
object PhotoCompressor {
    private const val MAX_SIDE = 1600
    private const val JPEG_QUALITY = 84

    fun compress(context: Context, source: Uri): Result<CompressedPhoto> = runCatching {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(source).use { input ->
            requireNotNull(input) { "No se pudo abrir la imagen." }
            BitmapFactory.decodeStream(input, null, bounds)
        }
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "La imagen no es válida." }

        val sample = calculateSample(bounds.outWidth, bounds.outHeight)
        val options = BitmapFactory.Options().apply { inSampleSize = sample; inPreferredConfig = Bitmap.Config.ARGB_8888 }
        val bitmap = resolver.openInputStream(source).use { input ->
            requireNotNull(input) { "No se pudo leer la imagen." }
            requireNotNull(BitmapFactory.decodeStream(input, null, options)) { "No se pudo decodificar la imagen." }
        }
        val scaled = scaleDown(bitmap)
        if (scaled !== bitmap) bitmap.recycle()

        val output = File(context.cacheDir, "proof-${System.currentTimeMillis()}.jpg")
        val outputWidth = scaled.width
        val outputHeight = scaled.height
        FileOutputStream(output).use { stream ->
            check(scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, stream)) { "No se pudo comprimir la imagen." }
        }
        scaled.recycle()
        CompressedPhoto(output, "image/jpeg", outputWidth, outputHeight, output.length())
    }

    private fun calculateSample(width: Int, height: Int): Int {
        var sample = 1
        while (max(width / sample, height / sample) > MAX_SIDE * 2) sample *= 2
        return sample
    }

    private fun scaleDown(bitmap: Bitmap): Bitmap {
        val largest = max(bitmap.width, bitmap.height)
        if (largest <= MAX_SIDE) return bitmap
        val ratio = MAX_SIDE.toFloat() / largest
        return Bitmap.createScaledBitmap(bitmap, (bitmap.width * ratio).toInt(), (bitmap.height * ratio).toInt(), true)
    }

}
