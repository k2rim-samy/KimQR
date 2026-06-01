package com.example.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

object QRGenerator {

    enum class QrStyle { SQUARE, DOTS, ROUNDED }
    enum class EyeStyle { SQUARE, CIRCLE, ROUNDED }
    enum class LogoType { NONE, WIFI, FACEBOOK, INSTAGRAM, TELEGRAM, WHATSAPP, GOOGLE }

    fun generate(
        content: String,
        size: Int = 512,
        primaryColorHex: String = "#0F172A",
        secondaryColorHex: String = "#1E293B",
        isGradient: Boolean = false,
        qrStyle: QrStyle = QrStyle.SQUARE,
        eyeStyle: EyeStyle = EyeStyle.SQUARE,
        logoType: LogoType = LogoType.NONE
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE) // Background is always clean white, or clear transparent

        try {
            val hints = HashMap<EncodeHintType, Any>()
            hints[EncodeHintType.ERROR_CORRECTION] = ErrorCorrectionLevel.H // High EC to support custom layouts and central logos
            hints[EncodeHintType.MARGIN] = 0 // Get raw matrix without ZXing's built-in padding
            hints[EncodeHintType.CHARACTER_SET] = "UTF-8" // Explicitly support UTF-8 for Arabic/Unicode characters

            // Encode with QR Writer
            val barcodeWriter = QRCodeWriter()
            // Request small size to get the Raw unscaled Grid Matrix!
            val rawMatrix = barcodeWriter.encode(content, BarcodeFormat.QR_CODE, 0, 0, hints)
            val matrixSize = rawMatrix.width // N x N size, e.g. 21, 25, 29, 33

            val paint = Paint(Paint.ANTI_ALIAS_FLAG)

            // Calculate exact dimensions with custom padding/quiet zone around the QR
            val paddingModules = 4
            val virtualGridSize = matrixSize + 2 * paddingModules
            val blockSize = size.toFloat() / virtualGridSize
            val offset = paddingModules * blockSize

            // Define gradients if enabled
            if (isGradient) {
                val startColor = Color.parseColor(primaryColorHex)
                val endColor = Color.parseColor(secondaryColorHex)
                val shader = LinearGradient(
                    0f, 0f, size.toFloat(), size.toFloat(),
                    startColor, endColor, Shader.TileMode.CLAMP
                )
                paint.shader = shader
            } else {
                paint.color = Color.parseColor(primaryColorHex)
            }

            // Exclude central block for logo (say 3 to 5 cells in the center depends on N)
            val logoRadiusCells = if (logoType != LogoType.NONE) 2 else -1
            val centerIndex = matrixSize / 2

            // Eye boundary checking function
            fun isEye(col: Int, row: Int): Boolean {
                val limit = 7
                if (row < limit && col < limit) return true // Top-Left Eye
                if (row < limit && col >= matrixSize - limit) return true // Top-Right Eye
                if (row >= matrixSize - limit && col < limit) return true // Bottom-Left Eye
                return false
            }

            // Central logo space checking function
            fun isInCenterLogoZone(col: Int, row: Int): Boolean {
                if (logoRadiusCells < 0) return false
                return (row >= centerIndex - logoRadiusCells && row <= centerIndex + logoRadiusCells &&
                        col >= centerIndex - logoRadiusCells && col <= centerIndex + logoRadiusCells)
            }

            // Drawing the custom matrix
            for (row in 0 until matrixSize) {
                for (col in 0 until matrixSize) {
                    if (rawMatrix.get(col, row)) {
                        // Skip Finder Patterns (designed dynamically later) and center logo zone
                        if (isEye(col, row) || isInCenterLogoZone(col, row)) {
                            continue
                        }

                        // Calculate cell rect with offset
                        val left = offset + col * blockSize
                        val top = offset + row * blockSize
                        val right = left + blockSize
                        val bottom = top + blockSize

                        when (qrStyle) {
                            QrStyle.SQUARE -> {
                                canvas.drawRect(left, top, right, bottom, paint)
                            }
                            QrStyle.DOTS -> {
                                val radius = blockSize / 2.05f
                                val cx = left + blockSize / 2f
                                val cy = top + blockSize / 2f
                                canvas.drawCircle(cx, cy, radius, paint)
                            }
                            QrStyle.ROUNDED -> {
                                val rect = RectF(
                                    left + blockSize * 0.015f,
                                    top + blockSize * 0.015f,
                                    right - blockSize * 0.015f,
                                    bottom - blockSize * 0.015f
                                )
                                val radius = blockSize * 0.20f
                                canvas.drawRoundRect(rect, radius, radius, paint)
                            }
                        }
                    }
                }
            }

            // Draw Eyes beautifully
            fun drawCustomEye(colStart: Int, rowStart: Int) {
                val left = offset + colStart * blockSize
                val top = offset + rowStart * blockSize
                val right = left + 7 * blockSize
                val bottom = top + 7 * blockSize

                // Outer frame
                val strokeWidth = blockSize
                val eyePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    this.strokeWidth = strokeWidth
                    if (isGradient) {
                        shader = paint.shader
                    } else {
                        color = paint.color
                    }
                }

                // Inner fill circle or square block
                val innerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.FILL
                    if (isGradient) {
                        shader = paint.shader
                    } else {
                        color = paint.color
                    }
                }

                when (eyeStyle) {
                    EyeStyle.SQUARE -> {
                        // Draw outer 7x7 square outline (offset by strokeWidth/2 for exact outline rendering)
                        val offsetStroke = strokeWidth / 2f
                        canvas.drawRect(left + offsetStroke, top + offsetStroke, right - offsetStroke, bottom - offsetStroke, eyePaint)
                        // Draw inner 3x3 square fill (at 2dx, 2dy of size 3dx)
                        val innerLeft = left + 2 * blockSize
                        val innerTop = top + 2 * blockSize
                        val innerRight = right - 2 * blockSize
                        val innerBottom = bottom - 2 * blockSize
                        canvas.drawRect(innerLeft, innerTop, innerRight, innerBottom, innerPaint)
                    }
                    EyeStyle.CIRCLE -> {
                        // Draw outer circle outline
                        val radiusOuter = 3 * blockSize
                        val cx = left + 3.5f * blockSize
                        val cy = top + 3.5f * blockSize
                        canvas.drawCircle(cx, cy, radiusOuter, eyePaint)

                        // Draw inner circle fill (radius 1.5 blocks)
                        val radiusInner = 1.5f * blockSize
                        canvas.drawCircle(cx, cy, radiusInner, innerPaint)
                    }
                    EyeStyle.ROUNDED -> {
                        // Draw outer rounded square outline
                        val offsetStroke = strokeWidth / 2f
                        val rectOuter = RectF(left + offsetStroke, top + offsetStroke, right - offsetStroke, bottom - offsetStroke)
                        val rxOuter = 2 * blockSize
                        canvas.drawRoundRect(rectOuter, rxOuter, rxOuter, eyePaint)

                        // Draw inner rounded square fill
                        val rectInner = RectF(
                            left + 2.2f * blockSize,
                            top + 2.2f * blockSize,
                            right - 2.2f * blockSize,
                            bottom - 2.2f * blockSize
                        )
                        val rxInner = 1f * blockSize
                        canvas.drawRoundRect(rectInner, rxInner, rxInner, innerPaint)
                    }
                }
            }

            // Draw Top-Left, Top-Right, and Bottom-Left Eyes
            drawCustomEye(0, 0)
            drawCustomEye(matrixSize - 7, 0)
            drawCustomEye(0, matrixSize - 7)

            // Draw Logo in the clean center
            if (logoType != LogoType.NONE) {
                val cx = size / 2f
                val cy = size / 2f
                val logoSize = (logoRadiusCells * 2 + 1) * blockSize

                // Draw solid white circular badge background for the logo
                val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.WHITE
                    style = Paint.Style.FILL
                }
                // Constrain the background white circle radius to exactly 2.3 * blockSize.
                // Since the cleared area has a radius of 2.5 * blockSize, this leaves a perfect 0.2 * blockSize white safety margin inside the clear zone.
                val maxBgRadius = logoRadiusCells * blockSize + blockSize * 0.3f
                canvas.drawCircle(cx, cy, maxBgRadius, bgPaint)

                // Optional: thin custom boundary ring for aesthetic luxury look
                val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = 2f
                    color = Color.parseColor(primaryColorHex)
                    alpha = 50
                }
                canvas.drawCircle(cx, cy, maxBgRadius, borderPaint)

                // Draw actual logo artwork
                val logoSymbolPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor(primaryColorHex)
                    style = Paint.Style.FILL
                }

                val symbolHalfSize = logoSize * 0.35f
                val leftSymbol = cx - symbolHalfSize
                val topSymbol = cy - symbolHalfSize
                val rightSymbol = cx + symbolHalfSize
                val bottomSymbol = cy + symbolHalfSize

                when (logoType) {
                    LogoType.WIFI -> {
                        // Draw stylized WiFi icon
                        logoSymbolPaint.style = Paint.Style.STROKE
                        logoSymbolPaint.strokeWidth = logoSize * 0.08f
                        logoSymbolPaint.strokeCap = Paint.Cap.ROUND
                        // Dot
                        logoSymbolPaint.style = Paint.Style.FILL
                        canvas.drawCircle(cx, cy + symbolHalfSize * 0.6f, logoSize * 0.08f, logoSymbolPaint)
                        // Arcs
                        logoSymbolPaint.style = Paint.Style.STROKE
                        val rect1 = RectF(cx - symbolHalfSize * 0.6f, cy - symbolHalfSize * 0.2f, cx + symbolHalfSize * 0.6f, cy + symbolHalfSize * 1.0f)
                        canvas.drawArc(rect1, 220f, 100f, false, logoSymbolPaint)
                        val rect2 = RectF(cx - symbolHalfSize, cy - symbolHalfSize * 0.6f, cx + symbolHalfSize, cy + symbolHalfSize * 1.4f)
                        canvas.drawArc(rect2, 220f, 100f, false, logoSymbolPaint)
                    }
                    LogoType.FACEBOOK -> {
                        // Stylized 'f'
                        logoSymbolPaint.apply {
                            textSize = logoSize * 0.8f
                            textAlign = Paint.Align.CENTER
                            typeface = android.graphics.Typeface.create("serif", android.graphics.Typeface.BOLD)
                        }
                        canvas.drawText("f", cx, cy + symbolHalfSize * 0.8f, logoSymbolPaint)
                    }
                    LogoType.INSTAGRAM -> {
                        // Stylized Instagram camera icon
                        logoSymbolPaint.style = Paint.Style.STROKE
                        logoSymbolPaint.strokeWidth = logoSize * 0.09f
                        val cameraRect = RectF(leftSymbol, topSymbol, rightSymbol, bottomSymbol)
                        canvas.drawRoundRect(cameraRect, symbolHalfSize * 0.4f, symbolHalfSize * 0.4f, logoSymbolPaint)
                        canvas.drawCircle(cx, cy, symbolHalfSize * 0.4f, logoSymbolPaint)
                        logoSymbolPaint.style = Paint.Style.FILL
                        canvas.drawCircle(cx + symbolHalfSize * 0.45f, cy - symbolHalfSize * 0.45f, logoSize * 0.04f, logoSymbolPaint)
                    }
                    LogoType.TELEGRAM -> {
                        // Stylized paper plane representation
                        val path = android.graphics.Path()
                        path.moveTo(cx - symbolHalfSize * 0.7f, cy)
                        path.lineTo(cx + symbolHalfSize * 0.8f, cy - symbolHalfSize * 0.6f)
                        path.lineTo(cx + symbolHalfSize * 0.2f, cy + symbolHalfSize * 0.8f)
                        path.lineTo(cx - symbolHalfSize * 0.1f, cy + symbolHalfSize * 0.3f)
                        path.lineTo(cx - symbolHalfSize * 0.7f, cy)
                        path.close()
                        canvas.drawPath(path, logoSymbolPaint)
                    }
                    LogoType.WHATSAPP -> {
                        val bubblePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            if (isGradient) {
                                shader = paint.shader
                            } else {
                                color = Color.parseColor(primaryColorHex)
                            }
                            style = Paint.Style.FILL
                        }

                        val r = symbolHalfSize * 0.9f
                        val path = android.graphics.Path()
                        // Speech bubble circle
                        path.addCircle(cx, cy, r, android.graphics.Path.Direction.CW)
                        
                        // Triangular tail pointing bottom-left
                        val tailPath = android.graphics.Path().apply {
                            moveTo(cx - r * 0.45f, cy + r * 0.75f)
                            lineTo(cx - r * 0.95f, cy + r * 0.95f)
                            lineTo(cx - r * 0.75f, cy + r * 0.45f)
                            close()
                        }
                        path.addPath(tailPath)
                        canvas.drawPath(path, bubblePaint)

                        // White phone handset inside, rotated to look exactly like the brand asset
                        canvas.save()
                        canvas.translate(cx, cy)
                        canvas.rotate(-45f)

                        val phonePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = Color.WHITE
                            style = Paint.Style.FILL
                        }

                        val phonePath = android.graphics.Path()
                        val scale = symbolHalfSize * 0.10f // slightly larger scale for bold professional branding

                        // Highly precise handset geometry
                        phonePath.moveTo(-2.5f * scale, -4.0f * scale)
                        // Top speaker cap
                        phonePath.cubicTo(-1.5f * scale, -4.8f * scale, 1.8f * scale, -3.8f * scale, 1.2f * scale, -1.8f * scale)
                        // Inner upper line
                        phonePath.lineTo(0.3f * scale, -1.2f * scale)
                        // Smooth inner grip curve
                        phonePath.quadTo(-1.2f * scale, 0.0f, 0.3f * scale, 1.2f * scale)
                        // Inner lower line
                        phonePath.lineTo(1.2f * scale, 1.8f * scale)
                        // Bottom microphone cap
                        phonePath.cubicTo(1.8f * scale, 3.8f * scale, -1.5f * scale, 4.8f * scale, -2.5f * scale, 4.0f * scale)
                        // Beautiful outer handle arc
                        phonePath.quadTo(-4.5f * scale, 0.0f, -2.5f * scale, -4.0f * scale)
                        phonePath.close()

                        canvas.drawPath(phonePath, phonePaint)
                        canvas.restore()
                    }
                    LogoType.GOOGLE -> {
                        // Stylized high weight 'G' label
                        logoSymbolPaint.apply {
                            textSize = logoSize * 0.75f
                            textAlign = Paint.Align.CENTER
                            typeface = android.graphics.Typeface.DEFAULT_BOLD
                        }
                        canvas.drawText("G", cx, cy + symbolHalfSize * 0.7f, logoSymbolPaint)
                    }
                    else -> {}
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }

        return bitmap
    }
}
