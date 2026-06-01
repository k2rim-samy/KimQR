package com.example.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter

object BarcodeGenerator {

    enum class BarcodeType {
        CODE_128,
        CODE_39,
        EAN_13,
        UPC_A,
        ISBN
    }

    /**
     * Helper to validate user inputs based on barcode type specifications
     */
    fun validateInput(content: String, type: BarcodeType): String? {
        val trimmed = content.trim()
        if (trimmed.isEmpty()) {
            return "المحتوى لا يمكن أن يكون فارغاً"
        }

        return when (type) {
            BarcodeType.CODE_128 -> {
                // Code 128 supports standard ASCII character set
                val isAscii = trimmed.all { it.code in 0..127 }
                if (!isAscii) "رمز Code 128 يدعم حروف ASCII الإنجليزية فقط" else null
            }
            BarcodeType.CODE_39 -> {
                val allowedChars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ -.$/+%"
                val upperStr = trimmed.uppercase()
                val isSupported = upperStr.all { it in allowedChars }
                if (!isSupported) "رمز Code 39 يدعم الأرقام، الحروف الإنجليزية الكبيرة والرموز (- . $ / + % مسافة)" else null
            }
            BarcodeType.EAN_13 -> {
                val digitsOnly = trimmed.filter { it.isDigit() }
                if (digitsOnly.length != 12 && digitsOnly.length != 13) {
                    "رمز EAN-13 يتطلب 12 أو 13 رقماً فقط"
                } else if (trimmed.any { !it.isDigit() }) {
                    "رمز EAN-13 يدعم الأرقام فقط"
                } else {
                    null
                }
            }
            BarcodeType.UPC_A -> {
                val digitsOnly = trimmed.filter { it.isDigit() }
                if (digitsOnly.length != 11 && digitsOnly.length != 12) {
                    "رمز UPC-A يتطلب 11 أو 12 رقماً فقط"
                } else if (trimmed.any { !it.isDigit() }) {
                    "رمز UPC-A يدعم الأرقام فقط"
                } else {
                    null
                }
            }
            BarcodeType.ISBN -> {
                val cleaned = trimmed.replace("-", "").replace(" ", "")
                val digitsOnly = cleaned.all { it.isDigit() }
                if (!digitsOnly) {
                    "رمز ISBN يدعم الأرقام والشرطات فقط"
                } else if (cleaned.length != 10 && cleaned.length != 13) {
                    "رمز ISBN يجب أن يتكون من 10 أو 13 رقماً"
                } else {
                    null
                }
            }
        }
    }

    /**
     * Generates a high-resolution, pixel-perfect customized barcode Bitmap.
     */
    fun generate(
        content: String,
        type: BarcodeType,
        width: Int = 800,
        height: Int = 400,
        primaryHex: String = "#000000",
        backgroundHex: String = "#FFFFFF",
        showText: Boolean = true
    ): Bitmap {
        val trimmed = content.trim()
        val format = when (type) {
            BarcodeType.CODE_128 -> BarcodeFormat.CODE_128
            BarcodeType.CODE_39 -> BarcodeFormat.CODE_39
            BarcodeType.EAN_13 -> BarcodeFormat.EAN_13
            BarcodeType.UPC_A -> BarcodeFormat.UPC_A
            BarcodeType.ISBN -> BarcodeFormat.EAN_13 // ISBN is typically encoded as EAN_13
        }

        // Adjust string for EAN_13 / UPC_A / ISBN standard encoding
        var encodeStr = trimmed
        if (type == BarcodeType.ISBN) {
            val cleaned = trimmed.replace("-", "").replace(" ", "")
            if (cleaned.length == 10) {
                // Convert ancient ISBN 10 to ISBN 13 format
                val isbn13WithoutCheckDigest = "978" + cleaned.substring(0, 9)
                encodeStr = isbn13WithoutCheckDigest + calculateEan13CheckDigit(isbn13WithoutCheckDigest)
            } else {
                encodeStr = cleaned
            }
        } else if (type == BarcodeType.CODE_39) {
            encodeStr = trimmed.uppercase()
        }

        // Setup dimensions
        val contentHeight = if (showText) (height * 0.75f).toInt() else height
        val textHeight = height - contentHeight

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val barColor = try { Color.parseColor(primaryHex) } catch (e: Exception) { Color.BLACK }
        val bgColor = try { Color.parseColor(backgroundHex) } catch (e: Exception) { Color.WHITE }

        canvas.drawColor(bgColor)

        try {
            val hints = HashMap<EncodeHintType, Any>()
            hints[EncodeHintType.MARGIN] = 2 // Small neat margins inside ZXing
            
            val multiFormatWriter = MultiFormatWriter()
            val bitMatrix = multiFormatWriter.encode(encodeStr, format, width, contentHeight, hints)

            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = barColor
            }

            // Draw bitMatrix
            for (x in 0 until width) {
                for (y in 0 until contentHeight) {
                    if (bitMatrix.get(x, y)) {
                        canvas.drawPoint(x.toFloat(), y.toFloat(), paint)
                    }
                }
            }

            // Draw clear label underneath the barcode
            if (showText) {
                val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = barColor
                    textSize = textHeight * 0.45f
                    textAlign = Paint.Align.CENTER
                    typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                }

                // Smooth background white cover underneath reading label
                val textBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = bgColor
                    style = Paint.Style.FILL
                }

                // Draw solid background strip for text
                canvas.drawRect(0f, contentHeight.toFloat(), width.toFloat(), height.toFloat(), textBgPaint)

                // Render content text elegantly
                val displayText = if (type == BarcodeType.ISBN && trimmed.contains("-")) trimmed else encodeStr
                val xCenter = width / 2f
                val yBaseline = contentHeight + (textHeight * 0.65f)

                canvas.drawText(displayText, xCenter, yBaseline, textPaint)
            }

        } catch (e: Exception) {
            e.printStackTrace()
            // If encoding fails (e.g. check digit error or wrong length before validation catches it)
            // draw a beautiful placeholder showing the error
            val errPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.RED
                textSize = height * 0.08f
                textAlign = Paint.Align.CENTER
            }
            canvas.drawColor(bgColor)
            canvas.drawText("فشل توليد الباركود!", width / 2f, height / 2f - 10f, errPaint)
            canvas.drawText("يرجى التحقق من صحة المدخلات وصيغتها", width / 2f, height / 2f + 30f, errPaint.apply { textSize = height * 0.05f })
        }

        return bitmap
    }

    private fun calculateEan13CheckDigit(input: String): Int {
        var sum = 0
        for (i in 0..11) {
            val digit = Character.getNumericValue(input[i])
            sum += if (i % 2 == 0) digit else digit * 3
        }
        val remainder = sum % 10
        return if (remainder == 0) 0 else 10 - remainder
    }
}
