package com.example.myapplication.tetris

import kotlin.math.max

enum class GameState {
    NOT_STARTED,
    PLAYING,
    PAUSED,
    GAME_OVER
}

class TetrisGame(
    val soundManager: SoundManager = SoundManager()
) {
    companion object {
        const val BOARD_WIDTH = 10
        const val BOARD_HEIGHT = 20
    }

    var grid = Array(BOARD_HEIGHT) { IntArray(BOARD_WIDTH) { 0 } }
        private set

    var gameState = GameState.NOT_STARTED
        private set

    var score = 0
        private set

    var highScore = 0

    var level = 1
        private set

    var linesCleared = 0
        private set

    var currentPiece: Piece? = null
        private set

    var holdPiece: TetrominoType? = null
        private set

    var canHold = true
        private set

    private val bag = mutableListOf<TetrominoType>()
    val nextPieces = mutableListOf<TetrominoType>()

    var clearingLines = listOf<Int>()
        private set

    var onStateChanged: (() -> Unit)? = null
    var onHapticFeedback: ((HapticType) -> Unit)? = null

    enum class HapticType {
        MOVE, ROTATE, DROP, LINE_CLEAR, GAME_OVER
    }

    init {
        fillNextQueue()
    }

    fun startNewGame() {
        grid = Array(BOARD_HEIGHT) { IntArray(BOARD_WIDTH) { 0 } }
        score = 0
        level = 1
        linesCleared = 0
        holdPiece = null
        canHold = true
        clearingLines = emptyList()
        bag.clear()
        nextPieces.clear()
        fillNextQueue()
        spawnNextPiece()
        gameState = GameState.PLAYING
        onStateChanged?.invoke()
    }

    fun togglePause() {
        if (gameState == GameState.PLAYING) {
            gameState = GameState.PAUSED
        } else if (gameState == GameState.PAUSED) {
            gameState = GameState.PLAYING
        }
        onStateChanged?.invoke()
    }

    private fun fillNextQueue() {
        while (nextPieces.size < 4) {
            if (bag.isEmpty()) {
                bag.addAll(TetrominoType.values().toList().shuffled())
            }
            nextPieces.add(bag.removeAt(0))
        }
    }

    private fun spawnNextPiece() {
        fillNextQueue()
        val type = nextPieces.removeAt(0)
        fillNextQueue()

        val newPiece = Piece(type = type, x = (BOARD_WIDTH - 2) / 2, y = 0)
        if (!isValidPosition(newPiece)) {
            // Game Over
            gameState = GameState.GAME_OVER
            if (score > highScore) {
                highScore = score
            }
            soundManager.playGameOver()
            onHapticFeedback?.invoke(HapticType.GAME_OVER)
        } else {
            currentPiece = newPiece
        }
        canHold = true
        onStateChanged?.invoke()
    }

    fun tick() {
        if (gameState != GameState.PLAYING) return

        if (clearingLines.isNotEmpty()) {
            // Finish line clearing animation step
            removeClearedLines()
            spawnNextPiece()
            return
        }

        val piece = currentPiece ?: return

        val nextPiece = piece.clone()
        nextPiece.y += 1

        if (isValidPosition(nextPiece)) {
            piece.y += 1
        } else {
            lockPiece()
        }
        onStateChanged?.invoke()
    }

    fun moveLeft() {
        if (gameState != GameState.PLAYING || clearingLines.isNotEmpty()) return
        val piece = currentPiece ?: return
        val nextPiece = piece.clone()
        nextPiece.x -= 1
        if (isValidPosition(nextPiece)) {
            piece.x -= 1
            soundManager.playMove()
            onHapticFeedback?.invoke(HapticType.MOVE)
            onStateChanged?.invoke()
        }
    }

    fun moveRight() {
        if (gameState != GameState.PLAYING || clearingLines.isNotEmpty()) return
        val piece = currentPiece ?: return
        val nextPiece = piece.clone()
        nextPiece.x += 1
        if (isValidPosition(nextPiece)) {
            piece.x += 1
            soundManager.playMove()
            onHapticFeedback?.invoke(HapticType.MOVE)
            onStateChanged?.invoke()
        }
    }

    fun softDrop() {
        if (gameState != GameState.PLAYING || clearingLines.isNotEmpty()) return
        val piece = currentPiece ?: return
        val nextPiece = piece.clone()
        nextPiece.y += 1
        if (isValidPosition(nextPiece)) {
            piece.y += 1
            score += 1 // Soft drop bonus
            soundManager.playMove()
            onStateChanged?.invoke()
        } else {
            lockPiece()
        }
    }

    fun hardDrop() {
        if (gameState != GameState.PLAYING || clearingLines.isNotEmpty()) return
        val piece = currentPiece ?: return
        var dropDistance = 0
        while (true) {
            val nextPiece = piece.clone()
            nextPiece.y += 1
            if (isValidPosition(nextPiece)) {
                piece.y += 1
                dropDistance++
            } else {
                break
            }
        }
        score += dropDistance * 2 // Hard drop bonus
        soundManager.playHardDrop()
        onHapticFeedback?.invoke(HapticType.DROP)
        lockPiece()
        onStateChanged?.invoke()
    }

    fun rotateClockwise() {
        if (gameState != GameState.PLAYING || clearingLines.isNotEmpty()) return
        val piece = currentPiece ?: return
        val nextRotation = (piece.rotation + 1) % 4

        if (tryRotation(piece, nextRotation)) {
            soundManager.playRotate()
            onHapticFeedback?.invoke(HapticType.ROTATE)
            onStateChanged?.invoke()
        }
    }

    fun rotateCounterClockwise() {
        if (gameState != GameState.PLAYING || clearingLines.isNotEmpty()) return
        val piece = currentPiece ?: return
        val nextRotation = (piece.rotation + 3) % 4

        if (tryRotation(piece, nextRotation)) {
            soundManager.playRotate()
            onHapticFeedback?.invoke(HapticType.ROTATE)
            onStateChanged?.invoke()
        }
    }

    private fun tryRotation(piece: Piece, nextRotation: Int): Boolean {
        // SRS Wall kick offset vectors
        val offsets = arrayOf(
            Pair(0, 0), Pair(-1, 0), Pair(1, 0), Pair(0, -1),
            Pair(-1, -1), Pair(1, -1), Pair(-2, 0), Pair(2, 0)
        )

        for ((dx, dy) in offsets) {
            val testPiece = piece.clone()
            testPiece.rotation = nextRotation
            testPiece.x += dx
            testPiece.y += dy

            if (isValidPosition(testPiece)) {
                piece.rotation = nextRotation
                piece.x += dx
                piece.y += dy
                return true
            }
        }
        return false
    }

    fun hold() {
        if (gameState != GameState.PLAYING || !canHold || clearingLines.isNotEmpty()) return
        val piece = currentPiece ?: return

        val currentType = piece.type
        val held = holdPiece

        soundManager.playHold()
        onHapticFeedback?.invoke(HapticType.ROTATE)

        if (held == null) {
            holdPiece = currentType
            spawnNextPiece()
        } else {
            holdPiece = currentType
            currentPiece = Piece(type = held, x = (BOARD_WIDTH - 2) / 2, y = 0)
        }
        canHold = false
        onStateChanged?.invoke()
    }

    fun getGhostPiece(): Piece? {
        val piece = currentPiece ?: return null
        val ghost = piece.clone()
        while (true) {
            val nextGhost = ghost.clone()
            nextGhost.y += 1
            if (isValidPosition(nextGhost)) {
                ghost.y += 1
            } else {
                break
            }
        }
        return ghost
    }

    private fun lockPiece() {
        val piece = currentPiece ?: return
        for (b in piece.getBlocks()) {
            if (b.y in 0 until BOARD_HEIGHT && b.x in 0 until BOARD_WIDTH) {
                grid[b.y][b.x] = piece.type.color
            }
        }
        currentPiece = null

        checkLineClears()
    }

    private fun checkLineClears() {
        val fullLines = mutableListOf<Int>()
        for (y in 0 until BOARD_HEIGHT) {
            if (grid[y].all { it != 0 }) {
                fullLines.add(y)
            }
        }

        if (fullLines.isNotEmpty()) {
            clearingLines = fullLines
            val linesCount = fullLines.size

            val basePoints = when (linesCount) {
                1 -> 100
                2 -> 300
                3 -> 500
                4 -> 800 // Tetris!
                else -> 100 * linesCount
            }
            score += basePoints * level
            linesCleared += linesCount

            level = (linesCleared / 10) + 1

            if (score > highScore) {
                highScore = score
            }

            soundManager.playLineClear(linesCount)
            onHapticFeedback?.invoke(HapticType.LINE_CLEAR)
        } else {
            spawnNextPiece()
        }
    }

    private fun removeClearedLines() {
        if (clearingLines.isEmpty()) return

        val newGrid = Array(BOARD_HEIGHT) { IntArray(BOARD_WIDTH) { 0 } }
        var newY = BOARD_HEIGHT - 1

        for (y in BOARD_HEIGHT - 1 downTo 0) {
            if (!clearingLines.contains(y)) {
                newGrid[newY] = grid[y].clone()
                newY--
            }
        }

        grid = newGrid
        clearingLines = emptyList()
    }

    private fun isValidPosition(piece: Piece): Boolean {
        for (b in piece.getBlocks()) {
            if (b.x < 0 || b.x >= BOARD_WIDTH || b.y >= BOARD_HEIGHT) {
                return false
            }
            if (b.y >= 0 && grid[b.y][b.x] != 0) {
                return false
            }
        }
        return true
    }

    fun getDropIntervalMs(): Long {
        // Speed curve: level 1 = 800ms, drops faster down to 100ms
        val interval = 800L - (level - 1) * 70L
        return max(100L, interval)
    }
}
