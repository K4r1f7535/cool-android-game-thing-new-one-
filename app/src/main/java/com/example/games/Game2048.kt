package com.example.games

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.example.engine.BaseGame
import com.example.engine.GameAction
import com.example.engine.GameType
import com.example.engine.RetroDrawing
import com.example.ui.theme.RetroBlack
import com.example.ui.theme.RetroDkGrey
import com.example.ui.theme.RetroGrey
import com.example.ui.theme.RetroWhite
import com.example.ui.theme.RetroYellow
import kotlin.math.max
import kotlin.random.Random

class Game2048 : BaseGame(GameType.GAME_2048) {

    companion object {
        const val SIZE = 4
        const val CELL = 76f
        const val GAP = 6f
        const val BOARD_X = (W - (SIZE * CELL + (SIZE + 1) * GAP)) / 2f
        const val BOARD_Y = 130f
        const val SLIDE_TIME = 0.10f

        val TILE_COLORS = mapOf(
            0 to Color(0xFF2C2E52),
            2 to Color(0xFFEEE4DA),
            4 to Color(0xFFEDE0C8),
            8 to Color(0xFFF2B179),
            16 to Color(0xFFF59563),
            32 to Color(0xFFF67C5F),
            64 to Color(0xFFF65E3B),
            128 to Color(0xFFEDCF72),
            256 to Color(0xFFEDCC61),
            512 to Color(0xFFEDC850),
            1024 to Color(0xFFEDC53F),
            2048 to Color(0xFFEDC22E)
        )
    }

    private var grid = Array(SIZE) { IntArray(SIZE) }
    private var hasWon = false
    private var pendingSpawn = false
    private var animTime = 0f
    private val animMoves = mutableListOf<Triple<Pair<Int, Int>, Pair<Int, Int>, Int>>()
    private val popAnimations = mutableMapOf<Pair<Int, Int>, Float>()

    init {
        reset(0)
    }

    override fun reset(currentHighScore: Int) {
        highScore = currentHighScore
        score = 0
        isAlive = true
        hasWon = false
        isNewRecord = false
        isDone = false
        pendingSpawn = false
        animTime = 0f
        animMoves.clear()
        popAnimations.clear()

        grid = Array(SIZE) { IntArray(SIZE) }
        addRandomTile()
        addRandomTile()
    }

    private fun addRandomTile() {
        val empty = mutableListOf<Pair<Int, Int>>()
        for (r in 0 until SIZE) {
            for (c in 0 until SIZE) {
                if (grid[r][c] == 0) empty.add(Pair(r, c))
            }
        }
        if (empty.isNotEmpty()) {
            val (r, c) = empty.random(Random.Default)
            grid[r][c] = if (Random.nextFloat() < 0.1f) 4 else 2
            popAnimations[Pair(r, c)] = 0.15f
        }
    }

    private fun slideLine(line: IntArray): Triple<IntArray, Int, List<Pair<Int, Int>>> {
        val nonZero = mutableListOf<Pair<Int, Int>>() // (origIndex, value)
        for (i in line.indices) {
            if (line[i] != 0) nonZero.add(Pair(i, line[i]))
        }

        val result = mutableListOf<Int>()
        val moves = mutableListOf<Pair<Int, Int>>()
        var points = 0
        var i = 0

        while (i < nonZero.size) {
            val (idx, value) = nonZero[i]
            if (i + 1 < nonZero.size && nonZero[i + 1].second == value) {
                val merged = value * 2
                points += merged
                moves.add(Pair(idx, result.size))
                moves.add(Pair(nonZero[i + 1].first, result.size))
                result.add(merged)
                i += 2
            } else {
                moves.add(Pair(idx, result.size))
                result.add(value)
                i += 1
            }
        }

        val resArray = IntArray(line.size)
        for (j in result.indices) {
            resArray[j] = result[j]
        }
        return Triple(resArray, points, moves)
    }

    private fun move(dir: GameAction, onSubmitScore: (key: String, score: Int) -> Boolean): Boolean {
        if (animMoves.isNotEmpty()) {
            finishAnimation(onSubmitScore)
        }

        val newGrid = Array(SIZE) { IntArray(SIZE) }
        var gained = 0
        val anims = mutableListOf<Triple<Pair<Int, Int>, Pair<Int, Int>, Int>>()

        for (k in 0 until SIZE) {
            val coords = when (dir) {
                GameAction.LEFT -> (0 until SIZE).map { j -> Pair(k, j) }
                GameAction.RIGHT -> (SIZE - 1 downTo 0).map { j -> Pair(k, j) }
                GameAction.UP -> (0 until SIZE).map { j -> Pair(j, k) }
                GameAction.DOWN -> (SIZE - 1 downTo 0).map { j -> Pair(j, k) }
                else -> emptyList()
            }
            val line = IntArray(SIZE) { j ->
                val (r, c) = coords[j]
                grid[r][c]
            }

            val (newLine, pts, moves) = slideLine(line)
            gained += pts

            for (j in 0 until SIZE) {
                val (r, c) = coords[j]
                newGrid[r][c] = newLine[j]
            }

            for ((fromIdx, toIdx) in moves) {
                val valAt = line[fromIdx]
                anims.add(Triple(coords[fromIdx], coords[toIdx], valAt))
            }
        }

        var isDifferent = false
        for (r in 0 until SIZE) {
            for (c in 0 until SIZE) {
                if (newGrid[r][c] != grid[r][c]) {
                    isDifferent = true
                    break
                }
            }
        }

        if (!isDifferent) return false

        grid = newGrid
        score += gained
        animMoves.clear()
        animMoves.addAll(anims)
        animTime = 0f
        pendingSpawn = true
        return true
    }

    private fun finishAnimation(onSubmitScore: (key: String, score: Int) -> Boolean) {
        animMoves.clear()
        if (pendingSpawn) {
            pendingSpawn = false
            addRandomTile()
            checkEnd(onSubmitScore)
        }
    }

    private fun canMove(): Boolean {
        for (r in 0 until SIZE) {
            for (c in 0 until SIZE) {
                if (grid[r][c] == 0) return true
                if (c + 1 < SIZE && grid[r][c] == grid[r][c + 1]) return true
                if (r + 1 < SIZE && grid[r][c] == grid[r + 1][c]) return true
            }
        }
        return false
    }

    private fun checkEnd(onSubmitScore: (key: String, score: Int) -> Boolean) {
        for (r in 0 until SIZE) {
            for (c in 0 until SIZE) {
                if (grid[r][c] >= 2048) hasWon = true
            }
        }
        if (!canMove()) {
            isAlive = false
            isNewRecord = onSubmitScore(gameType.key, score)
            if (isNewRecord) highScore = max(highScore, score)
        }
    }

    private var queuedMove: GameAction? = null

    override fun handleInput(
        action: GameAction?,
        tapVirtual: Pair<Float, Float>?,
        isDownHeld: Boolean,
        heldDirections: Set<GameAction>
    ) {
        if (checkBack(action, tapVirtual)) return

        if (!isAlive) {
            if (action == GameAction.ACTION || tapVirtual != null) {
                reset(highScore)
            }
            return
        }

        if (action in listOf(GameAction.UP, GameAction.DOWN, GameAction.LEFT, GameAction.RIGHT)) {
            queuedMove = action
        }
    }

    override fun update(dt: Float, onSubmitScore: (key: String, score: Int) -> Boolean) {
        // Pop animations
        val it = popAnimations.entries.iterator()
        while (it.hasNext()) {
            val entry = it.next()
            val remaining = entry.value - dt
            if (remaining <= 0f) {
                it.remove()
            } else {
                entry.setValue(remaining)
            }
        }

        if (animMoves.isNotEmpty()) {
            animTime += dt
            if (animTime >= SLIDE_TIME) {
                finishAnimation(onSubmitScore)
            }
        }

        queuedMove?.let { dir ->
            queuedMove = null
            move(dir, onSubmitScore)
        }
    }

    private fun cellOffset(r: Int, c: Int): Pair<Float, Float> {
        val x = BOARD_X + GAP + c * (CELL + GAP)
        val y = BOARD_Y + GAP + r * (CELL + GAP)
        return Pair(x, y)
    }

    private fun drawTile(scope: DrawScope, x: Float, y: Float, value: Int, scale: Float = 1.0f) {
        val color = TILE_COLORS[value] ?: Color(0xFF3C3A32)
        val size = CELL * scale
        val off = (CELL - size) / 2f
        val rx = x + off
        val ry = y + off

        scope.drawRect(
            color = color,
            topLeft = Offset(rx, ry),
            size = Size(size, size)
        )

        if (value > 0) {
            scope.drawRect(
                color = RetroWhite,
                topLeft = Offset(rx, ry),
                size = Size(size, size),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1f)
            )
            val txtColor = if (value <= 4) RetroBlack else RetroWhite
            val fsize = if (value < 100) 36f else if (value < 1000) 28f else 22f
            RetroDrawing.drawText(
                scope = scope,
                text = value.toString(),
                x = rx + size / 2f,
                y = ry + size / 2f,
                size = fsize * scale,
                color = txtColor,
                centerX = true,
                centerY = true
            )
        }
    }

    override fun draw(scope: DrawScope, heldDirections: Set<GameAction>) {
        scope.drawRect(color = RetroBlack, topLeft = Offset.Zero, size = Size(W, H))

        RetroDrawing.drawText(scope, "2048", 14f, 14f, 40f, RetroYellow)
        RetroDrawing.drawText(scope, "SCORE $score", 14f, 58f, 22f, RetroWhite)
        RetroDrawing.drawText(scope, "HI ${max(highScore, score)}", 14f, 82f, 22f, RetroYellow)

        // Board background
        val bw = SIZE * CELL + (SIZE + 1) * GAP
        scope.drawRect(
            color = RetroDkGrey,
            topLeft = Offset(BOARD_X, BOARD_Y),
            size = Size(bw, bw)
        )
        scope.drawRect(
            color = RetroWhite,
            topLeft = Offset(BOARD_X, BOARD_Y),
            size = Size(bw, bw),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
        )

        // Empty cells background
        for (r in 0 until SIZE) {
            for (c in 0 until SIZE) {
                val (x, y) = cellOffset(r, c)
                drawTile(scope, x, y, 0)
            }
        }

        if (animMoves.isNotEmpty()) {
            var t = (animTime / SLIDE_TIME).coerceIn(0f, 1f)
            t = 1f - (1f - t) * (1f - t) // ease out
            for ((from, to, value) in animMoves) {
                val (x0, y0) = cellOffset(from.first, from.second)
                val (x1, y1) = cellOffset(to.first, to.second)
                drawTile(scope, x0 + (x1 - x0) * t, y0 + (y1 - y0) * t, value)
            }
        } else {
            for (r in 0 until SIZE) {
                for (c in 0 until SIZE) {
                    val v = grid[r][c]
                    if (v > 0) {
                        val (x, y) = cellOffset(r, c)
                        val popDuration = popAnimations[Pair(r, c)]
                        val scale = if (popDuration != null) {
                            1f - (popDuration / 0.15f) * 0.4f
                        } else 1.0f
                        drawTile(scope, x, y, v, scale)
                    }
                }
            }
        }

        if (hasWon && isAlive) {
            RetroDrawing.drawText(scope, "YOU MADE 2048!", W / 2f, 96f, 22f, RetroYellow, centerX = true, centerY = true)
        }

        RetroDrawing.drawText(scope, "swipe or use buttons", W / 2f, 474f, 16f, RetroGrey, centerX = true, centerY = true)

        drawControls(scope, showDpad = true, held = heldDirections)

        if (!isAlive) {
            drawGameOver(scope)
        }
    }
}
