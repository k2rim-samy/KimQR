package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.camera.core.ImageProxy
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import java.nio.ByteBuffer

data class ScanResultData(val content: String, val format: String)

object QRReader {

    private fun getNewReader(): MultiFormatReader {
        return MultiFormatReader().apply {
            val hints = HashMap<DecodeHintType, Any>()
            // Enlist major 1D codes and QR code for automatic scanning.
            hints[DecodeHintType.POSSIBLE_FORMATS] = listOf(
                BarcodeFormat.QR_CODE,
                BarcodeFormat.CODE_128,
                BarcodeFormat.CODE_39,
                BarcodeFormat.EAN_13,
                BarcodeFormat.UPC_A,
                BarcodeFormat.UPC_E,
                BarcodeFormat.CODABAR,
                BarcodeFormat.ITF
            )
            hints[DecodeHintType.TRY_HARDER] = true
            setHints(hints)
        }
    }

    /**
     * Helper to read image from content URI, resolve EXIF rotation, rotate, and decode.
     */
    fun decodeBitmapWithRotation(context: Context, uri: Uri): ScanResultData? {
        var inputStream: java.io.InputStream? = null
        try {
            // First pass: retrieve EXIF orientation parameters
            inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val exif = androidx.exifinterface.media.ExifInterface(inputStream)
            val orientation = exif.getAttributeInt(
                androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION,
                androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL
            )
            inputStream.close()

            // Calculate sample size for a max 1200 fraction boundary to avoid OOM and speed up grid processing
            val maxDim = 1200
            val options = android.graphics.BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            inputStream = context.contentResolver.openInputStream(uri) ?: return null
            android.graphics.BitmapFactory.decodeStream(inputStream, null, options)
            inputStream.close()

            var inSampleSize = 1
            val largestDim = Math.max(options.outWidth, options.outHeight)
            while (largestDim / inSampleSize > maxDim) {
                inSampleSize *= 2
            }

            options.inJustDecodeBounds = false
            options.inSampleSize = inSampleSize

            // Second pass: decode Bitmap with optimized size
            inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream, null, options)
            inputStream.close()

            if (bitmap == null) return null

            val rotatedBitmap = when (orientation) {
                androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_90 -> rotateBitmap(bitmap, 90f)
                androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_180 -> rotateBitmap(bitmap, 180f)
                androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_270 -> rotateBitmap(bitmap, 270f)
                else -> null
            }

            val finalBitmap = rotatedBitmap ?: bitmap
            val result = decodeBitmap(finalBitmap)

            if (rotatedBitmap != null && rotatedBitmap != bitmap) {
                rotatedBitmap.recycle()
            }
            bitmap.recycle()
            return result
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        } finally {
            try { inputStream?.close() } catch (e: Exception) {}
        }
    }

    /**
     * Decodes a code image from an Android Bitmap
     */
    fun decodeBitmap(bitmap: Bitmap): ScanResultData? {
        // Step 1: Try decoding with the original image first
        var resultText = decodeWithBinarizers(bitmap)
        if (resultText != null) return resultText

        // Step 2: Try with inverted pixels of original image
        val invertedOriginal = invertBitmapColors(bitmap)
        if (invertedOriginal != null) {
            resultText = decodeWithBinarizers(invertedOriginal)
            invertedOriginal.recycle()
            if (resultText != null) return resultText
        }

        // Step 3: Resize image to max 1024px width/height for better grid recognition
        val scaled1024 = resizeBitmap(bitmap, 1024)
        if (scaled1024 != null) {
            resultText = decodeWithBinarizers(scaled1024)
            if (resultText != null) {
                scaled1024.recycle()
                return resultText
            }

            // Inverted scaled 1024
            val inverted1024 = invertBitmapColors(scaled1024)
            if (inverted1024 != null) {
                resultText = decodeWithBinarizers(inverted1024)
                inverted1024.recycle()
                if (resultText != null) {
                    scaled1024.recycle()
                    return resultText
                }
            }

            // Try Rotations (90, 180, 270)
            for (angle in listOf(90f, 180f, 270f)) {
                val rotated = rotateBitmap(scaled1024, angle)
                if (rotated != null) {
                    resultText = decodeWithBinarizers(rotated)
                    if (resultText != null) {
                        rotated.recycle()
                        scaled1024.recycle()
                        return resultText
                    }
                    val invertedRotated = invertBitmapColors(rotated)
                    if (invertedRotated != null) {
                        resultText = decodeWithBinarizers(invertedRotated)
                        invertedRotated.recycle()
                        if (resultText != null) {
                            rotated.recycle()
                            scaled1024.recycle()
                            return resultText
                        }
                    }
                    rotated.recycle()
                }
            }
            scaled1024.recycle()
        }

        // Step 4: Try with a smaller dimension (max 600px) which helps with minor blurs/defocus
        val scaled600 = resizeBitmap(bitmap, 600)
        if (scaled600 != null) {
            resultText = decodeWithBinarizers(scaled600)
            if (resultText != null) {
                scaled600.recycle()
                return resultText
            }
            scaled600.recycle()
        }

        return null
    }

    private fun resizeBitmap(src: Bitmap, maxDimension: Int): Bitmap? {
        if (src.width <= maxDimension && src.height <= maxDimension) {
            return src.copy(src.config ?: Bitmap.Config.ARGB_8888, true)
        }
        val scale = maxDimension.toFloat() / Math.max(src.width, src.height)
        val newWidth = (src.width * scale).toInt()
        val newHeight = (src.height * scale).toInt()
        return try {
            Bitmap.createScaledBitmap(src, newWidth, newHeight, true)
        } catch (e: Exception) {
            null
        }
    }

    private fun rotateBitmap(source: Bitmap, angle: Float): Bitmap? {
        return try {
            val matrix = android.graphics.Matrix().apply { postRotate(angle) }
            Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
        } catch (e: Exception) {
            null
        }
    }

    private fun invertBitmapColors(src: Bitmap): Bitmap? {
        return try {
            val width = src.width
            val height = src.height
            val pixels = IntArray(width * height)
            src.getPixels(pixels, 0, width, 0, 0, width, height)
            for (i in pixels.indices) {
                val p = pixels[i]
                val a = p and 0xFF000000.toInt()
                val r = 255 - ((p shr 16) and 0xFF)
                val g = 255 - ((p shr 8) and 0xFF)
                val b = 255 - (p and 0xFF)
                pixels[i] = a or (r shl 16) or (g shl 8) or b
            }
            val inverted = Bitmap.createBitmap(width, height, src.config ?: Bitmap.Config.ARGB_8888)
            inverted.setPixels(pixels, 0, width, 0, 0, width, height)
            inverted
        } catch (e: Exception) {
            null
        }
    }

    private fun decodeWithBinarizers(bitmap: Bitmap): ScanResultData? {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        val source = RGBLuminanceSource(width, height, pixels)
        val localReader = getNewReader()

        // Try 1: HybridBinarizer
        try {
            val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
            val result = localReader.decodeWithState(binaryBitmap)
            return ScanResultData(result.text, result.barcodeFormat.name)
        } catch (e: Exception) {
            // Ignore
        } finally {
            localReader.reset()
        }

        // Try 2: GlobalHistogramBinarizer
        try {
            val binaryBitmap = BinaryBitmap(com.google.zxing.common.GlobalHistogramBinarizer(source))
            val result = localReader.decodeWithState(binaryBitmap)
            return ScanResultData(result.text, result.barcodeFormat.name)
        } catch (e: Exception) {
            // Ignore
        } finally {
            localReader.reset()
        }

        return null
    }

    /**
     * Decodes a code in real-time from a CameraX ImageProxy
     */
    fun decodeImageProxy(imageProxy: ImageProxy): ScanResultData? {
        val image = imageProxy.image ?: return null
        val localReader = getNewReader()
        try {
            val width = imageProxy.width
            val height = imageProxy.height
            val rotation = imageProxy.imageInfo.rotationDegrees

            // Step 1: Extract Y channel data safely (dismiss rowStride padding)
            val coherentY = getCoherentYData(image, width, height)

            // Step 2: Rotate according to camera sensor rotation degrees
            var finalWidth = width
            var finalHeight = height
            var finalBytes = coherentY

            when (rotation) {
                90 -> {
                    finalBytes = rotateYUV90(coherentY, width, height)
                    finalWidth = height
                    finalHeight = width
                }
                180 -> {
                    finalBytes = rotateYUV180(coherentY, width, height)
                }
                270 -> {
                    finalBytes = rotateYUV270(coherentY, width, height)
                    finalWidth = height
                    finalHeight = width
                }
            }

            // Step 3: Decode rotated
            var source = PlanarYUVLuminanceSource(
                finalBytes, finalWidth, finalHeight,
                0, 0, finalWidth, finalHeight,
                false
            )
            var binaryBitmap = BinaryBitmap(HybridBinarizer(source))
            try {
                val result = localReader.decodeWithState(binaryBitmap)
                return ScanResultData(result.text, result.barcodeFormat.name)
            } catch (e: Exception) {
                // If it fails, let's try with GlobalHistogramBinarizer as fallback
                try {
                    val globalBitmap = BinaryBitmap(com.google.zxing.common.GlobalHistogramBinarizer(source))
                    val result = localReader.decodeWithState(globalBitmap)
                    return ScanResultData(result.text, result.barcodeFormat.name)
                } catch (e2: Exception) {
                    // ignore and try original orientation if rotation was not zero
                }
            } finally {
                localReader.reset()
            }

            // Step 4: If rotated orientation failed, try original (unrotated) orientation with HybridBinarizer
            if (rotation != 0) {
                source = PlanarYUVLuminanceSource(
                    coherentY, width, height,
                    0, 0, width, height,
                    false
                )
                binaryBitmap = BinaryBitmap(HybridBinarizer(source))
                try {
                    val result = localReader.decodeWithState(binaryBitmap)
                    return ScanResultData(result.text, result.barcodeFormat.name)
                } catch (e: Exception) {
                    // Try GlobalHistogramBinarizer on original
                    try {
                        val globalBitmap = BinaryBitmap(com.google.zxing.common.GlobalHistogramBinarizer(source))
                        val result = localReader.decodeWithState(globalBitmap)
                        return ScanResultData(result.text, result.barcodeFormat.name)
                    } catch (e3: Exception) {
                        // ignore
                    }
                } finally {
                    localReader.reset()
                }
            }

            return null
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    private fun getCoherentYData(image: android.media.Image, width: Int, height: Int): ByteArray {
        val plane = image.planes[0]
        val buffer = plane.buffer
        val rowStride = plane.rowStride
        val bytes = ByteArray(width * height)

        val oldPosition = buffer.position()
        buffer.rewind()

        if (rowStride == width) {
            buffer.get(bytes)
        } else {
            val rowBytes = ByteArray(rowStride)
            for (row in 0 until height) {
                val length = Math.min(rowStride, buffer.remaining())
                if (length > 0) {
                    buffer.get(rowBytes, 0, length)
                    val copyLen = Math.min(width, length)
                    System.arraycopy(rowBytes, 0, bytes, row * width, copyLen)
                }
            }
        }

        buffer.position(oldPosition)
        return bytes
    }

    private fun rotateYUV90(data: ByteArray, imageWidth: Int, imageHeight: Int): ByteArray {
        val yuv = ByteArray(data.size)
        var i = 0
        for (x in 0 until imageWidth) {
            for (y in imageHeight - 1 downTo 0) {
                yuv[i++] = data[y * imageWidth + x]
            }
        }
        return yuv
    }

    private fun rotateYUV180(data: ByteArray, imageWidth: Int, imageHeight: Int): ByteArray {
        val yuv = ByteArray(data.size)
        val total = data.size
        for (i in 0 until total) {
            yuv[total - 1 - i] = data[i]
        }
        return yuv
    }

    private fun rotateYUV270(data: ByteArray, imageWidth: Int, imageHeight: Int): ByteArray {
        val yuv = ByteArray(data.size)
        var i = 0
        for (x in imageWidth - 1 downTo 0) {
            for (y in 0 until imageHeight) {
                yuv[i++] = data[y * imageWidth + x]
            }
        }
        return yuv
    }
}
