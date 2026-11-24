package com.example.myapplication.tetris

import android.graphics.Color

enum class TetrominoType(
    val color: Int,
    val lightColor: Int,
    val darkColor: Int,
    val shape: Array<Array<Point>>
) {
    I(
        color = Color.parseColor("#00E5FF"), // Cyan
        lightColor = Color.parseColor("#80D8FF"),
        darkColor = Color.parseColor("#00B0FF"),
        shape = arrayOf(
            arrayOf(Point(0, 1), Point(1, 1), Point(2, 1), Point(3, 1)),
            arrayOf(Point(2, 0), Point(2, 1), Point(2, 2), Point(2, 3)),
            arrayOf(Point(0, 2), Point(1, 2), Point(2, 2), Point(3, 2)),
            arrayOf(Point(1, 0), Point(1, 1), Point(1, 2), Point(1, 3))
        )
    ),
    J(
        color = Color.parseColor("#2979FF"), // Blue
        lightColor = Color.parseColor("#82B1FF"),
        darkColor = Color.parseColor("#2962FF"),
        shape = arrayOf(
            arrayOf(Point(0, 0), Point(0, 1), Point(1, 1), Point(2, 1)),
            arrayOf(Point(1, 0), Point(2, 0), Point(1, 1), Point(1, 2)),
            arrayOf(Point(0, 1), Point(1, 1), Point(2, 1), Point(2, 2)),
            arrayOf(Point(1, 0), Point(1, 1), Point(0, 2), Point(1, 2))
        )
    ),
    L(
        color = Color.parseColor("#FF9100"), // Orange
        lightColor = Color.parseColor("#FFD180"),
        darkColor = Color.parseColor("#FF6D00"),
        shape = arrayOf(
            arrayOf(Point(2, 0), Point(0, 1), Point(1, 1), Point(2, 1)),
            arrayOf(Point(1, 0), Point(1, 1), Point(1, 2), Point(2, 2)),
            arrayOf(Point(0, 1), Point(1, 1), Point(2, 1), Point(0, 2)),
            arrayOf(Point(0, 0), Point(1, 0), Point(1, 1), Point(1, 2))
        )
    ),
    O(
        color = Color.parseColor("#FFEA00"), // Yellow
        lightColor = Color.parseColor("#FFFF8D"),
        darkColor = Color.parseColor("#FFD600"),
        shape = arrayOf(
            arrayOf(Point(1, 0), Point(2, 0), Point(1, 1), Point(2, 1)),
            arrayOf(Point(1, 0), Point(2, 0), Point(1, 1), Point(2, 1)),
            arrayOf(Point(1, 0), Point(2, 0), Point(1, 1), Point(2, 1)),
            arrayOf(Point(1, 0), Point(2, 0), Point(1, 1), Point(2, 1))
        )
    ),
    S(
        color = Color.parseColor("#00E676"), // Green
        lightColor = Color.parseColor("#B9F6CA"),
        darkColor = Color.parseColor("#00C853"),
        shape = arrayOf(
            arrayOf(Point(1, 0), Point(2, 0), Point(0, 1), Point(1, 1)),
            arrayOf(Point(1, 0), Point(1, 1), Point(2, 1), Point(2, 2)),
            arrayOf(Point(1, 1), Point(2, 1), Point(0, 2), Point(1, 2)),
            arrayOf(Point(0, 0), Point(0, 1), Point(1, 1), Point(1, 2))
        )
    ),
    T(
        color = Color.parseColor("#D500F9"), // Purple
        lightColor = Color.parseColor("#EA80FC"),
        darkColor = Color.parseColor("#AA00FF"),
        shape = arrayOf(
            arrayOf(Point(1, 0), Point(0, 1), Point(1, 1), Point(2, 1)),
            arrayOf(Point(1, 0), Point(1, 1), Point(2, 1), Point(1, 2)),
            arrayOf(Point(0, 1), Point(1, 1), Point(2, 1), Point(1, 2)),
            arrayOf(Point(1, 0), Point(0, 1), Point(1, 1), Point(1, 2))
        )
    ),
    Z(
        color = Color.parseColor("#FF1744"), // Red
        lightColor = Color.parseColor("#FF8A80"),
        darkColor = Color.parseColor("#D50000"),
        shape = arrayOf(
            arrayOf(Point(0, 0), Point(1, 0), Point(1, 1), Point(2, 1)),
            arrayOf(Point(2, 0), Point(1, 1), Point(2, 1), Point(1, 2)),
            arrayOf(Point(0, 1), Point(1, 1), Point(1, 2), Point(2, 2)),
            arrayOf(Point(1, 0), Point(0, 1), Point(1, 1), Point(0, 2))
        )
    )
}

data class Point(val x: Int, val y: Int)

class Piece(
    val type: TetrominoType,
    var x: Int = 3,
    var y: Int = 0,
    var rotation: Int = 0
) {
    fun getBlocks(currentRotation: Int = rotation, posX: Int = x, posY: Int = y): List<Point> {
        val pattern = type.shape[currentRotation % type.shape.size]
        return pattern.map { Point(posX + it.x, posY + it.y) }
    }

    fun clone(): Piece {
        return Piece(type, x, y, rotation)
    }
}
