package com.example.games

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.example.engine.BaseGame
import com.example.engine.GameAction
import com.example.engine.GameType
import com.example.engine.RectF
import com.example.engine.RetroDrawing
import com.example.ui.theme.RetroBlack
import com.example.ui.theme.RetroCyan
import com.example.ui.theme.RetroGreen
import com.example.ui.theme.RetroOrange
import com.example.ui.theme.RetroRed
import com.example.ui.theme.RetroWhite
import com.example.ui.theme.RetroYellow
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

data class BreakerBrick(val rect: RectF, val color: Color)

class BrickBreakerGame : BaseGame(GameType.BRICK_BREAKER) {

    companion object {
        const val PW = 64f
        const val PH = 12f
        const val PY = 440f
    }

    private var px = (W - PW) / 2f
    private var bx = W / 2f
    private var by = 300f
    private var bvx = 150f * if (Random.nextBoolean()) 1f else -1f
    private var bvy = -180f
    private val bricks = mutableListOf<BreakerBrick>()

    init {
        reset(0)
    }

    override fun reset(currentHighScore: Int) {
        highScore = currentHighScore
        score = 0
        isAlive = true
        isNewRecord = false
        isDone = false
        px = (W - PW) / 2f
        bx = W / 2f
        by = 300f
        bvx = 150f * if (Random.nextBoolean()) 1f else -1f
        bvy = -180f
        buildBricks()
    }

    private fun buildBricks() {
        bricks.clear()
        val cols = 6
        val rows = 5
        val bw = (W - 30f) / cols
        val bh = 16f
        val colors = listOf(RetroRed, RetroOrange, RetroYellow, RetroGreen, RetroCyan)

        for (r in 0 until rows) {
            val color = colors[r % colors.size]
            for (c in 0 until cols) {
                val rect = RectF(15f + c * bw, 60f + r * (bh + 4f), bw - 2f, bh)
                bricks.add(BreakerBrick(rect, color))
            }
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

        // Tap drag or tap position to position paddle
        if (tapVirtual != null) {
            val (tx, ty) = tapVirtual
            if (ty in 380f..490f) {
                px = (tx - PW / 2f).coerceIn(5f, W - 5f - PW)
            }
        }
    }

    override fun update(dt: Float, onSubmitScore: (key: String, score: Int) -> Boolean) {
        if (!isAlive) return

        bx += bvx * dt
        by += bvy * dt

        if (bx <= 8f) {
            bx = 8f
            bvx = abs(bvx)
        } else if (bx >= W - 8f) {
            bx = W - 8f
            bvx = -abs(bvx)
        }

        if (by <= 50f) {
            by = 50f
            bvy = abs(bvy)
        }

        if (by >= 470f) {
            isAlive = false
            isNewRecord = onSubmitScore(gameType.key, score)
            if (isNewRecord) highScore = max(highScore, score)
            return
        }

        val ballRect = RectF(bx - 5f, by - 5f, 10f, 10f)
        val paddleRect = RectF(px, PY, PW, PH)

        if (ballRect.collides(paddleRect) && bvy > 0f) {
            bvy = -abs(bvy)
            val offset = (bx - (px + PW / 2f)) / (PW / 2f)
            bvx = offset.coerceIn(-1.2f, 1.2f) * 220f
        }

        val it = bricks.iterator()
        while (it.hasNext()) {
            val brick = it.next()
            if (ballRect.collides(brick.rect)) {
                it.remove()
                bvy = -bvy
                score += 10
                break
            }
        }

        if (bricks.isEmpty()) {
            buildBricks()
            bvy *= 1.1f
        }
    }

    override fun draw(scope: DrawScope, heldDirections: Set<GameAction>) {
        if (isAlive) {
            if (heldDirections.contains(GameAction.LEFT)) {
                px = max(5f, px - 7f)
            }
            if (heldDirections.contains(GameAction.RIGHT)) {
                px = min(W - 5f - PW, px + 7f)
            }
        }

        scope.drawRect(color = RetroBlack, topLeft = Offset.Zero, size = Size(W, H))

        // Header info
        RetroDrawing.drawText(scope, "SCORE $score", 10f, 16f, 22f, RetroWhite)
        RetroDrawing.drawText(scope, "HI ${max(highScore, score)}", 10f, 38f, 22f, RetroYellow)

        // Bricks
        for (b in bricks) {
            scope.drawRect(
                color = b.color,
                topLeft = Offset(b.rect.x, b.rect.y),
                size = Size(b.rect.w, b.rect.h)
            )
            scope.drawRect(
                color = RetroWhite,
                topLeft = Offset(b.rect.x, b.rect.y),
                size = Size(b.rect.w, b.rect.h),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1f)
            )
        }

        // Paddle
        scope.drawRect(
            color = RetroWhite,
            topLeft = Offset(px, PY),
            size = Size(PW, PH)
        )

        // Ball
        scope.drawCircle(
            color = RetroYellow,
            radius = 5f,
            center = Offset(bx, by)
        )

        drawControls(scope, showDpad = true, held = heldDirections)

        if (!isAlive) {
            drawGameOver(scope)
        }
    }
}
