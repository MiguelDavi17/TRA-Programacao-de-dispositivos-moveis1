package com.example.myapplication.tetris

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs
import kotlin.math.min

class TetrisView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var game: TetrisGame? = null
        set(value) {
            field = value
            value?.onStateChanged = { invalidate() }
            invalidate()
        }

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1A237E") // Deep blue grid line
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#0A0E1A") // Dark arcade background
        style = Paint.Style.FILL
    }

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#00E5FF") // Neon cyan border
        style = Paint.Style.STROKE
        strokeWidth = 6f
    }

    private val blockPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val ghostPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }

    private val flashPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 48f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }

    private var cellSize = 0f
    private var startX = 0f
    private var startY = 0f

    // Touch gesture detection
    private var touchX = 0f
    private var touchY = 0f

    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            game?.rotateClockwise()
            return true
        }

        override fun onFling(
            e1: MotionEvent?,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            if (e1 == null) return false
            val diffY = e2.y - e1.y
            val diffX = e2.x - e1.x

            if (abs(diffY) > abs(diffX)) {
                if (diffY > 100 && velocityY > 200) {
                    game?.hardDrop()
                    return true
                }
            } else {
                if (abs(diffX) > 100 && abs(velocityX) > 200) {
                    if (diffX > 0) {
                        game?.moveRight()
                    } else {
                        game?.moveLeft()
                    }
                    return true
                }
            }
            return false
        }
    })

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        calculateDimensions(w.toFloat(), h.toFloat())
    }

    private fun calculateDimensions(width: Float, height: Float) {
        val availableWidth = width - paddingLeft - paddingRight
        val availableHeight = height - paddingTop - paddingBottom

        val cellW = availableWidth / TetrisGame.BOARD_WIDTH
        val cellH = availableHeight / TetrisGame.BOARD_HEIGHT

        cellSize = min(cellW, cellH)

        val boardW = cellSize * TetrisGame.BOARD_WIDTH
        val boardH = cellSize * TetrisGame.BOARD_HEIGHT

        startX = paddingLeft + (availableWidth - boardW) / 2f
        startY = paddingTop + (availableHeight - boardH) / 2f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val currentGame = game ?: return

        // Draw outer board background
        val boardW = cellSize * TetrisGame.BOARD_WIDTH
        val boardH = cellSize * TetrisGame.BOARD_HEIGHT
        val boardRect = RectF(startX, startY, startX + boardW, startY + boardH)

        canvas.drawRoundRect(boardRect, 12f, 12f, bgPaint)

        // Draw Grid Lines
        for (x in 0..TetrisGame.BOARD_WIDTH) {
            val posX = startX + x * cellSize
            canvas.drawLine(posX, startY, posX, startY + boardH, gridPaint)
        }
        for (y in 0..TetrisGame.BOARD_HEIGHT) {
            val posY = startY + y * cellSize
            canvas.drawLine(startX, posY, startX + boardW, posY, gridPaint)
        }

        // Draw Locked Grid Blocks
        val grid = currentGame.grid
        for (y in 0 until TetrisGame.BOARD_HEIGHT) {
            for (x in 0 until TetrisGame.BOARD_WIDTH) {
                val colorHex = grid[y][x]
                if (colorHex != 0) {
                    drawBlock(canvas, x, y, colorHex)
                }
            }
        }

        // Draw Line Clear Flashing Effect
        val clearingLines = currentGame.clearingLines
        if (clearingLines.isNotEmpty()) {
            flashPaint.alpha = ((System.currentTimeMillis() / 50) % 2 * 200 + 55).toInt()
            for (y in clearingLines) {
                val rect = RectF(
                    startX,
                    startY + y * cellSize,
                    startX + boardW,
                    startY + (y + 1) * cellSize
                )
                canvas.drawRect(rect, flashPaint)
            }
        }

        // Draw Ghost Piece
        val ghost = currentGame.getGhostPiece()
        if (ghost != null && currentGame.currentPiece != null && clearingLines.isEmpty()) {
            val type = ghost.type
            ghostPaint.color = type.color
            ghostPaint.alpha = 100
            for (b in ghost.getBlocks()) {
                if (b.y >= 0) {
                    drawGhostBlock(canvas, b.x, b.y)
                }
            }
        }

        // Draw Falling Piece
        val piece = currentGame.currentPiece
        if (piece != null && clearingLines.isEmpty()) {
            for (b in piece.getBlocks()) {
                if (b.y >= 0) {
                    drawBlock(canvas, b.x, b.y, piece.type.color, piece.type.lightColor, piece.type.darkColor)
                }
            }
        }

        // Draw Neon Border around board
        canvas.drawRoundRect(boardRect, 12f, 12f, borderPaint)

        // Overlay Game State message if needed
        when (currentGame.gameState) {
            GameState.PAUSED -> {
                canvas.drawRect(boardRect, Paint().apply {
                    color = Color.parseColor("#B0000000")
                })
                textPaint.color = Color.WHITE
                textPaint.textSize = cellSize * 1.5f
                canvas.drawText("PAUSADO", boardRect.centerX(), boardRect.centerY(), textPaint)
            }
            GameState.GAME_OVER -> {
                canvas.drawRect(boardRect, Paint().apply {
                    color = Color.parseColor("#D0000000")
                })
                textPaint.color = Color.parseColor("#FF1744")
                textPaint.textSize = cellSize * 1.5f
                canvas.drawText("FIM DE JOGO!", boardRect.centerX(), boardRect.centerY() - cellSize, textPaint)

                textPaint.color = Color.WHITE
                textPaint.textSize = cellSize * 0.9f
                canvas.drawText("Pontos: ${currentGame.score}", boardRect.centerX(), boardRect.centerY() + cellSize, textPaint)
            }
            GameState.NOT_STARTED -> {
                canvas.drawRect(boardRect, Paint().apply {
                    color = Color.parseColor("#C0000000")
                })
                textPaint.color = Color.parseColor("#00E5FF")
                textPaint.textSize = cellSize * 1.5f
                canvas.drawText("TETRIS", boardRect.centerX(), boardRect.centerY() - cellSize, textPaint)

                textPaint.color = Color.WHITE
                textPaint.textSize = cellSize * 0.8f
                canvas.drawText("Toque em INICIAR", boardRect.centerX(), boardRect.centerY() + cellSize, textPaint)
            }
            GameState.PLAYING -> {
                // Game running normally
            }
        }
    }

    private fun drawBlock(
        canvas: Canvas,
        x: Int,
        y: Int,
        color: Int,
        lightColor: Int? = null,
        darkColor: Int? = null
    ) {
        val px = startX + x * cellSize
        val py = startY + y * cellSize
        val margin = 1f
        val rect = RectF(px + margin, py + margin, px + cellSize - margin, py + cellSize - margin)

        // Main block body
        blockPaint.color = color
        canvas.drawRoundRect(rect, 4f, 4f, blockPaint)

        // 3D Bevel highlight
        val bevelSize = cellSize * 0.12f

        highlightPaint.color = lightColor ?: blendColor(color, Color.WHITE, 0.4f)
        shadowPaint.color = darkColor ?: blendColor(color, Color.BLACK, 0.4f)

        // Top-Left Light Bevel
        val pathTopLeft = Path().apply {
            moveTo(rect.left, rect.bottom)
            lineTo(rect.left, rect.top)
            lineTo(rect.right, rect.top)
            lineTo(rect.right - bevelSize, rect.top + bevelSize)
            lineTo(rect.left + bevelSize, rect.top + bevelSize)
            lineTo(rect.left + bevelSize, rect.bottom - bevelSize)
            close()
        }
        canvas.drawPath(pathTopLeft, highlightPaint)

        // Bottom-Right Dark Bevel
        val pathBottomRight = Path().apply {
            moveTo(rect.right, rect.top)
            lineTo(rect.right, rect.bottom)
            lineTo(rect.left, rect.bottom)
            lineTo(rect.left + bevelSize, rect.bottom - bevelSize)
            lineTo(rect.right - bevelSize, rect.bottom - bevelSize)
            lineTo(rect.right - bevelSize, rect.top + bevelSize)
            close()
        }
        canvas.drawPath(pathBottomRight, shadowPaint)
    }

    private fun drawGhostBlock(canvas: Canvas, x: Int, y: Int) {
        val px = startX + x * cellSize
        val py = startY + y * cellSize
        val margin = 2f
        val rect = RectF(px + margin, py + margin, px + cellSize - margin, py + cellSize - margin)

        canvas.drawRoundRect(rect, 4f, 4f, ghostPaint)
    }

    private fun blendColor(c1: Int, c2: Int, ratio: Float): Int {
        val inverseRatio = 1f - ratio
        val r = (Color.red(c1) * inverseRatio + Color.red(c2) * ratio).toInt()
        val g = (Color.green(c1) * inverseRatio + Color.green(c2) * ratio).toInt()
        val b = (Color.blue(c1) * inverseRatio + Color.blue(c2) * ratio).toInt()
        return Color.rgb(r, g, b)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (gestureDetector.onTouchEvent(event)) {
            return true
        }

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                touchX = event.x
                touchY = event.y
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - touchX
                val dy = event.y - touchY

                if (abs(dx) > cellSize * 0.8f) {
                    if (dx > 0) {
                        game?.moveRight()
                    } else {
                        game?.moveLeft()
                    }
                    touchX = event.x
                } else if (dy > cellSize * 1.2f) {
                    game?.softDrop()
                    touchY = event.y
                }
                return true
            }
        }

        return super.onTouchEvent(event)
    }
}
