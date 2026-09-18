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
import com.example.ui.theme.RetroGreen
import com.example.ui.theme.RetroNavy
import com.example.ui.theme.RetroRed
import com.example.ui.theme.RetroWhite
import com.example.ui.theme.RetroYellow
import kotlin.math.max
import kotlin.random.Random

class SnakeGame : BaseGame(GameType.SNAKE) {

    companion object {
        const val CELL = 20f
        const val COLS = 18
        const val ROWS = 20
        const val TOP = 50f
        const val START_DELAY = 0.20f
        const val MIN_DELAY = 0.06f
        const val SPEEDUP = 0.006f
    }

    private val body = mutableListOf<Pair<Int, Int>>()
    private var direction = GameAction.RIGHT
    private val queue = mutableListOf<GameAction>()
    private var delay = START_DELAY
    private var timer = 0f
    private var food: Pair<Int, Int>? = null

    init {
        reset(0)
    }

    override fun reset(currentHighScore: Int) {
        highScore = currentHighScore
        score = 0
        isAlive = true
        isNewRecord = false
        isDone = false
        delay = START_DELAY
        timer = 0f
        direction = GameAction.RIGHT
        queue.clear()

        val cx = COLS / 2
        val cy = ROWS / 2
        body.clear()
        body.add(Pair(cx, cy))
        body.add(Pair(cx - 1, cy))
        body.add(Pair(cx - 2, cy))

        placeFood()
    }

    private fun placeFood() {
        val free = mutableListOf<Pair<Int, Int>>()
        for (x in 0 until COLS) {
            for (y in 0 until ROWS) {
                val pt = Pair(x, y)
                if (pt !in body) {
                    free.add(pt)
                }
            }
        }
        food = if (free.isNotEmpty()) free.random(Random.Default) else null
    }

    private fun isOpposite(a: GameAction, b: GameAction): Boolean {
        return (a == GameAction.UP && b == GameAction.DOWN) ||
                (a == GameAction.DOWN && b == GameAction.UP) ||
                (a == GameAction.LEFT && b == GameAction.RIGHT) ||
                (a == GameAction.RIGHT && b == GameAction.LEFT)
    }

    private fun queueTurn(d: GameAction) {
        val last = if (queue.isNotEmpty()) queue.last() else direction
        if (d != last && !isOpposite(d, last) && queue.size < 3) {
            queue.add(d)
        }
    }

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

        if (action != null) {
            when (action) {
                GameAction.UP, GameAction.DOWN, GameAction.LEFT, GameAction.RIGHT -> queueTurn(action)
                else -> {}
            }
        }
    }

    override fun update(dt: Float, onSubmitScore: (key: String, score: Int) -> Boolean) {
        if (!isAlive) return

        timer += dt
        while (timer >= delay && isAlive) {
            timer -= delay
            step(onSubmitScore)
        }
    }

    private fun step(onSubmitScore: (key: String, score: Int) -> Boolean) {
        if (queue.isNotEmpty()) {
            direction = queue.removeAt(0)
        }

        val (dx, dy) = when (direction) {
            GameAction.UP -> Pair(0, -1)
            GameAction.DOWN -> Pair(0, 1)
            GameAction.LEFT -> Pair(-1, 0)
            GameAction.RIGHT -> Pair(1, 0)
            else -> Pair(1, 0)
        }

        val (hx, hy) = body[0]
        val newHead = Pair(hx + dx, hy + dy)

        val hitWall = newHead.first !in 0 until COLS || newHead.second !in 0 until ROWS
        val hitSelf = body.dropLast(1).contains(newHead)

        if (hitWall || hitSelf) {
            isAlive = false
            isNewRecord = onSubmitScore(gameType.key, score)
            if (isNewRecord) highScore = max(highScore, score)
            return
        }

        body.add(0, newHead)
        if (newHead == food) {
            score += 10
            delay = max(MIN_DELAY, delay - SPEEDUP)
            placeFood()
        } else {
            body.removeAt(body.size - 1)
        }
    }

    override fun draw(scope: DrawScope, heldDirections: Set<GameAction>) {
        // Background
        scope.drawRect(color = RetroBlack, topLeft = Offset.Zero, size = Size(W, H))

        // Header info
        RetroDrawing.drawText(scope, "SCORE $score", 10f, 16f, 22f, RetroWhite)
        RetroDrawing.drawText(scope, "HI ${max(highScore, score)}", 10f, 38f, 22f, RetroYellow)

        // Checkerboard grid
        val fieldH = ROWS * CELL
        val fieldW = COLS * CELL
        scope.drawRect(
            color = RetroNavy,
            topLeft = Offset(0f, TOP),
            size = Size(fieldW, fieldH)
        )

        for (x in 0 until COLS) {
            for (y in 0 until ROWS) {
                if ((x + y) % 2 == 0) {
                    scope.drawRect(
                        color = Color(0xFF202248),
                        topLeft = Offset(x * CELL, TOP + y * CELL),
                        size = Size(CELL, CELL)
                    )
                }
            }
        }

        // Field border
        scope.drawRect(
            color = RetroWhite,
            topLeft = Offset(0f, TOP),
            size = Size(fieldW, fieldH),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
        )

        // Food
        food?.let { (fx, fy) ->
            scope.drawRect(
                color = RetroRed,
                topLeft = Offset(fx * CELL + 3f, TOP + fy * CELL + 3f),
                size = Size(CELL - 6f, CELL - 6f)
            )
            scope.drawRect(
                color = RetroYellow,
                topLeft = Offset(fx * CELL + 5f, TOP + fy * CELL + 5f),
                size = Size(4f, 4f)
            )
        }

        // Snake body
        for ((i, part) in body.withIndex()) {
            val (bx, by) = part
            val rx = bx * CELL + 1f
            val ry = TOP + by * CELL + 1f
            val color = if (i == 0) RetroYellow else RetroGreen
            scope.drawRect(
                color = color,
                topLeft = Offset(rx, ry),
                size = Size(CELL - 2f, CELL - 2f)
            )
            // Head eyes
            if (i == 0) {
                scope.drawRect(
                    color = RetroBlack,
                    topLeft = Offset(rx + 3f, ry + 3f),
                    size = Size(3f, 3f)
                )
                scope.drawRect(
                    color = RetroBlack,
                    topLeft = Offset(rx + CELL - 7f, ry + 3f),
                    size = Size(3f, 3f)
                )
            }
        }

        // Controls
        drawControls(scope, showDpad = true, held = heldDirections)

        // Game Over
        if (!isAlive) {
            drawGameOver(scope)
        }
    }
}
