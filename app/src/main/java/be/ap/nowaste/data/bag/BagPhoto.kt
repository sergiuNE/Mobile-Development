package be.ap.nowaste.data.bag

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64

/**
 * Turns photoBase64 back into a bitmap, scaled down to about [maxSize] px so a list of thumbnails stays light.
 * Slow: call it on Dispatchers.Default, never during composition. Null for an empty or broken photo.
 */
fun decodeBagPhoto(photoBase64: String, maxSize: Int): Bitmap? {
    if (photoBase64.isEmpty()) return null
    val bytes = try {
        Base64.decode(photoBase64, Base64.NO_WRAP)
    } catch (e: IllegalArgumentException) {
        return null
    }
    // First read only the size, then decode at the smallest power of 2 that is still at least maxSize
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    var sampleSize = 1
    while (bounds.outWidth / (sampleSize * 2) >= maxSize && bounds.outHeight / (sampleSize * 2) >= maxSize) sampleSize *= 2
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sampleSize })
}
