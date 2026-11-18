package be.ap.nowaste.data.bag

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.util.Base64
import androidx.core.graphics.scale
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

// H4: a Firestore document holds at most 1 MiB and Base64 makes data about a third bigger
const val PHOTO_MAX_SIZE_PX = 800
private const val JPEG_QUALITY = 70
private const val MAX_BASE64_LENGTH = 700_000

/** Size that fits in [maxSize] x [maxSize] with the same aspect ratio. Never scales up. */
fun scaledSize(width: Int, height: Int, maxSize: Int): Pair<Int, Int> {
    val longest = max(width, height)
    if (longest <= maxSize) return width to height
    val factor = maxSize.toDouble() / longest
    return (width * factor).roundToInt().coerceAtLeast(1) to (height * factor).roundToInt().coerceAtLeast(1)
}

/**
 * Photo from the gallery (PickVisualMedia), already scaled down while decoding to save memory.
 * Slow: call it on Dispatchers.Default.
 */
fun loadPhoto(resolver: ContentResolver, uri: Uri): Bitmap? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        // ImageDecoder also turns the photo the right way up (EXIF orientation)
        ImageDecoder.decodeBitmap(ImageDecoder.createSource(resolver, uri)) { decoder, info, _ ->
            val (width, height) = scaledSize(info.size.width, info.size.height, PHOTO_MAX_SIZE_PX)
            decoder.setTargetSize(width, height)
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }
    } else {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sampleSize = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sampleSize * 2) >= PHOTO_MAX_SIZE_PX) sampleSize *= 2
        resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sampleSize })
        }
    }

/**
 * H4: scale down to at most 800 px, compress as JPEG (quality about 70) and turn it into Base64 text for photoBase64.
 * Slow: call it on Dispatchers.Default.
 */
fun encodeBagPhoto(photo: Bitmap): String {
    val (width, height) = scaledSize(photo.width, photo.height, PHOTO_MAX_SIZE_PX)
    val scaled = if (width == photo.width && height == photo.height) photo else photo.scale(width, height)
    var quality = JPEG_QUALITY
    while (true) {
        val bytes = ByteArrayOutputStream().use { out ->
            scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
            out.toByteArray()
        }
        val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
        // A very detailed photo can still be too big: lower the quality until it fits
        if (base64.length <= MAX_BASE64_LENGTH || quality <= 30) return base64
        quality -= 10
    }
}
