package com.example.engine

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import com.example.ui.theme.RetroBlack
import com.example.ui.theme.RetroWhite

object RetroDrawing {

    private val textPaint = Paint().apply {
        isAntiAlias = false
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
    }

    fun drawText(
        scope: DrawScope,
        text: String,
        x: Float,
        y: Float,
        size: Float,
        color: Color,
        centerX: Boolean = false,
        centerY: Boolean = false
    ) {
        val nativeCanvas = scope.drawContext.canvas.nativeCanvas
        textPaint.textSize = size

        var drawX = x
        var drawY = y

        val fontMetrics = textPaint.fontMetrics
        val textHeight = fontMetrics.descent - fontMetrics.ascent
        val textWidth = textPaint.measureText(text)

        if (centerX) {
            drawX -= textWidth / 2f
        }
        if (centerY) {
            drawY += (textHeight / 2f) - fontMetrics.descent
        }

        // Draw shadow
        textPaint.color = RetroBlack.toArgb()
        nativeCanvas.drawText(text, drawX + 2f, drawY + 2f, textPaint)

        // Draw main text
        textPaint.color = color.toArgb()
        nativeCanvas.drawText(text, drawX, drawY, textPaint)
    }

    fun drawButton(
        scope: DrawScope,
        x: Float,
        y: Float,
        w: Float,
        h: Float,
        label: String,
        color: Color,
        fontSize: Float = 24f,
        isPressed: Boolean = false
    ) {
        val yOffset = if (isPressed) 2f else 0f
        val currentY = y + yOffset

        // Shadow
        if (!isPressed) {
            scope.drawRect(
                color = RetroBlack,
                topLeft = Offset(x + 3f, currentY + 4f),
                size = Size(w, h)
            )
        }

        // Button background
        scope.drawRect(
            color = color,
            topLeft = Offset(x, currentY),
            size = Size(w, h)
        )

        // White border
        scope.drawRect(
            color = RetroWhite,
            topLeft = Offset(x, currentY),
            size = Size(w, h),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
        )

        // Top highlight line
        scope.drawLine(
            color = RetroWhite,
            start = Offset(x + 4f, currentY + 4f),
            end = Offset(x + w - 5f, currentY + 4f),
            strokeWidth = 2f
        )

        // Label
        drawText(
            scope = scope,
            text = label,
            x = x + w / 2f,
            y = currentY + h / 2f,
            size = fontSize,
            color = RetroWhite,
            centerX = true,
            centerY = true
        )
    }

    fun drawScanlines(scope: DrawScope, width: Float, height: Float) {
        val scanlineColor = Color(0x28000000)
        var y = 0f
        while (y < height) {
            scope.drawLine(
                color = scanlineColor,
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 1.2f
            )
            y += 3.5f
        }
    }
}
