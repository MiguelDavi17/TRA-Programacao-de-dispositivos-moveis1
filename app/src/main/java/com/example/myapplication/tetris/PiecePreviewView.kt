package com.example.myapplication.tetris

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import kotlin.math.min

class PiecePreviewView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var pieceType: TetrominoType? = null
        set(value) {
            field = value
            invalidate()
        }

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#0A0E1A")
        style = Paint.Style.FILL
    }

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#3D5AFE")
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }

    private val blockPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()

        val rect = RectF(4f, 4f, w - 4f, h - 4f)
        canvas.drawRoundRect(rect, 8f, 8f, bgPaint)
        canvas.drawRoundRect(rect, 8f, 8f, borderPaint)

        val currentPiece = pieceType ?: return

        // Pattern points for rotation 0
        val pattern = currentPiece.shape[0]

        val minX = pattern.minOf { it.x }
        val maxX = pattern.maxOf { it.x }
        val minY = pattern.minOf { it.y }
        val maxY = pattern.maxOf { it.y }

        val gridW = maxX - minX + 1
        val gridH = maxY - minY + 1

        val cellSize = min((w - 24f) / 4f, (h - 24f) / 4f)

        val offsetX = (w - gridW * cellSize) / 2f - minX * cellSize
        val offsetY = (h - gridH * cellSize) / 2f - minY * cellSize

        blockPaint.color = currentPiece.color
        highlightPaint.color = currentPiece.lightColor
        shadowPaint.color = currentPiece.darkColor

        for (pt in pattern) {
            val px = offsetX + pt.x * cellSize
            val py = offsetY + pt.y * cellSize
            val blockRect = RectF(px + 1f, py + 1f, px + cellSize - 1f, py + cellSize - 1f)

            canvas.drawRoundRect(blockRect, 3f, 3f, blockPaint)

            val bevelSize = cellSize * 0.12f

            // Top-Left Light Bevel
            val pathTopLeft = Path().apply {
                moveTo(blockRect.left, blockRect.bottom)
                lineTo(blockRect.left, blockRect.top)
                lineTo(blockRect.right, blockRect.top)
                lineTo(blockRect.right - bevelSize, blockRect.top + bevelSize)
                lineTo(blockRect.left + bevelSize, blockRect.top + bevelSize)
                lineTo(blockRect.left + bevelSize, blockRect.bottom - bevelSize)
                close()
            }
            canvas.drawPath(pathTopLeft, highlightPaint)

            // Bottom-Right Dark Bevel
            val pathBottomRight = Path().apply {
                moveTo(blockRect.right, blockRect.top)
                lineTo(blockRect.right, blockRect.bottom)
                lineTo(blockRect.left, blockRect.bottom)
                lineTo(blockRect.left + bevelSize, blockRect.bottom - bevelSize)
                lineTo(blockRect.right - bevelSize, blockRect.bottom - bevelSize)
                lineTo(blockRect.right - bevelSize, blockRect.top + bevelSize)
                close()
            }
            canvas.drawPath(pathBottomRight, shadowPaint)
        }
    }
}
