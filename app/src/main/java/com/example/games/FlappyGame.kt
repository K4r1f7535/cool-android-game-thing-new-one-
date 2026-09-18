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
import com.example.ui.theme.RetroDkGreen
import com.example.ui.theme.RetroGreen
import com.example.ui.theme.RetroOrange
import com.example.ui.theme.RetroWhite
import com.example.ui.theme.RetroYellow
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

data class FlappyPipe(
    var x: Float,
    val base: Float,
    val gap: Float,
    val amp: Float,
    val phase: Float,
    var passed: Boolean = false
)

class FlappyGame : BaseGame(GameType.FLAPPY) {

    companion object {
        const val GRAVITY = 1500f
        const val FLAP_SPEED = -430f
        const val PIPE_W = 54f
        const val GAP_START = 170f
        const val GAP_MIN = 120f
        const val GROUND_Y = 470f
        const val BIRD_X = 90f
        const val BIRD_R = 13f
        const val MAX_STEP = 110f
    }

    private var birdY = 260f
    private var birdVy = 0f
    private var started = false
    private var speed = 130f
    private var time = 0f
    private var scroll = 0f
    private val pipes = mutableListOf<FlappyPipe>()
    private val clouds = mutableListOf<Triple<Float, Float, Float>>()

    init {
        for (i in 0 until 5) {
            clouds.add(Triple(Random.nextFloat() * W, 40f + Random.nextFloat() * 260f, 30f + Random.nextFloat() * 30f))
        }
        reset(0)
    }

    override fun reset(currentHighScore: Int) {
        highScore = currentHighScore
        score = 0
        isAlive = true
        isNewRecord = false
        isDone = false
        started = false
        birdY = 260f
        birdVy = 0f
        speed = 130f
        time = 0f
        scroll = 0f
        pipes.clear()

        makePipe(W + 60f)
        makePipe(W + 60f + 190f)
    }

    private fun makePipe(x: Float) {
        val gap = max(GAP_MIN, GAP_START - score * 2f)
        val moving = score >= 5 && Random.nextFloat() < 0.5f
        val amp = if (moving) Random.nextInt(18, 30).toFloat() else 0f

        var lo = (gap / 2f + 60f + amp).toInt()
        var hi = (GROUND_Y - gap / 2f - 40f - amp).toInt()
        if (pipes.isNotEmpty()) {
            val prev = pipes.last().base
            val reach = MAX_STEP - amp - pipes.last().amp
            lo = max(lo, (prev - max(40f, reach)).toInt())
            hi = min(hi, (prev + max(40f, reach)).toInt())
        }
        if (lo > hi) {
            lo = (lo + hi) / 2
            hi = lo
        }
        val centre = if (hi > lo) Random.nextInt(lo, hi).toFloat() else lo.toFloat()
        pipes.add(
            FlappyPipe(
                x = x,
                base = centre,
                gap = gap,
                amp = amp,
                phase = Random.nextFloat() * 6.28f,
                passed = false
            )
        )
    }

    private fun gapCentre(pipe: FlappyPipe): Float {
        if (pipe.amp == 0f) return pipe.base
        val lo = pipe.gap / 2f + 20f
        val hi = GROUND_Y - pipe.gap / 2f - 20f
        val y = pipe.base + pipe.amp * sin(time * 2.5f + pipe.phase)
        return y.coerceIn(lo, hi)
    }

    private fun flap() {
        started = true
        birdVy = FLAP_SPEED
    }

    private fun birdRect(): RectF {
        return RectF(BIRD_X - BIRD_R + 3f, birdY - BIRD_R + 3f, 2f * BIRD_R - 6f, 2f * BIRD_R - 6f)
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

        if (action == GameAction.UP || action == GameAction.ACTION) {
            flap()
        }

        if (tapVirtual != null) {
            val (tx, ty) = tapVirtual
            if (!BACK_BUTTON_RECT.contains(tx, ty)) {
                flap()
            }
        }
    }

    override fun update(dt: Float, onSubmitScore: (key: String, score: Int) -> Boolean) {
        if (!isAlive) return

        time += dt
        scroll = (scroll + speed * dt) % 24f

        if (!started) {
            birdY = 260f + 8f * sin(time * 5f)
            return
        }

        birdVy += GRAVITY * dt
        birdY += birdVy * dt
        speed = min(200f, 130f + score * 2.5f)

        for (pipe in pipes) {
            pipe.x -= speed * dt
            if (!pipe.passed && pipe.x + PIPE_W < BIRD_X - BIRD_R) {
                pipe.passed = true
                score += 1
            }
        }
        pipes.removeAll { it.x < -PIPE_W - 10f }

        if (pipes.isEmpty() || pipes.last().x < W - 190f) {
            val nextX = if (pipes.isNotEmpty()) pipes.last().x + 190f else W + 40f
            makePipe(nextX)
        }

        if (birdY + BIRD_R >= GROUND_Y || birdY - BIRD_R <= 0f) {
            die(onSubmitScore)
            return
        }

        val me = birdRect()
        for (pipe in pipes) {
            val cy = gapCentre(pipe)
            val topRect = RectF(pipe.x, 0f, PIPE_W, cy - pipe.gap / 2f)
            val botRect = RectF(pipe.x, cy + pipe.gap / 2f, PIPE_W, GROUND_Y - (cy + pipe.gap / 2f))
            if (me.collides(topRect) || me.collides(botRect)) {
                die(onSubmitScore)
                return
            }
        }
    }

    private fun die(onSubmitScore: (key: String, score: Int) -> Boolean) {
        isAlive = false
        isNewRecord = onSubmitScore(gameType.key, score)
        if (isNewRecord) highScore = max(highScore, score)
    }

    private fun drawPipePart(scope: DrawScope, rect: RectF, capAtBottom: Boolean) {
        if (rect.h <= 0f) return
        scope.drawRect(
            color = RetroDkGreen,
            topLeft = Offset(rect.x, rect.y),
            size = Size(rect.w, rect.h)
        )
        scope.drawRect(
            color = RetroGreen,
            topLeft = Offset(rect.x + 4f, rect.y),
            size = Size(8f, rect.h)
        )
        scope.drawRect(
            color = RetroBlack,
            topLeft = Offset(rect.x, rect.y),
            size = Size(rect.w, rect.h),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
        )
        val capY = if (capAtBottom) rect.bottom - 16f else rect.y
        val capRect = RectF(rect.x - 4f, capY, rect.w + 8f, 16f)
        scope.drawRect(
            color = RetroGreen,
            topLeft = Offset(capRect.x, capRect.y),
            size = Size(capRect.w, capRect.h)
        )
        scope.drawRect(
            color = RetroBlack,
            topLeft = Offset(capRect.x, capRect.y),
            size = Size(capRect.w, capRect.h),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
        )
    }

    override fun draw(scope: DrawScope, heldDirections: Set<GameAction>) {
        // Sky
        scope.drawRect(color = Color(0xFF58BEE6), topLeft = Offset.Zero, size = Size(W, H))

        // Clouds
        for ((cx, cy, cw) in clouds) {
            scope.drawRect(
                color = Color(0xFFEBF5FF),
                topLeft = Offset(cx, cy),
                size = Size(cw, 14f)
            )
            scope.drawRect(
                color = Color(0xFFEBF5FF),
                topLeft = Offset(cx + 8f, cy - 8f),
                size = Size(cw - 16f, 10f)
            )
        }

        // Pipes
        for (pipe in pipes) {
            val cy = gapCentre(pipe)
            val top = RectF(pipe.x, 0f, PIPE_W, cy - pipe.gap / 2f)
            val bottom = RectF(pipe.x, cy + pipe.gap / 2f, PIPE_W, GROUND_Y - (cy + pipe.gap / 2f))
            drawPipePart(scope, top, capAtBottom = true)
            drawPipePart(scope, bottom, capAtBottom = false)
        }

        // Ground
        scope.drawRect(
            color = Color(0xFFDEC882),
            topLeft = Offset(0f, GROUND_Y),
            size = Size(W, H - GROUND_Y)
        )
        scope.drawLine(
            color = Color(0xFF5AA03C),
            start = Offset(0f, GROUND_Y),
            end = Offset(W, GROUND_Y),
            strokeWidth = 6f
        )
        var gx = -24f
        while (gx < W + 24f) {
            scope.drawRect(
                color = Color(0xFFC8B069),
                topLeft = Offset(gx - scroll, GROUND_Y + 12f),
                size = Size(12f, 6f)
            )
            gx += 24f
        }

        // Bird
        val tilt = (birdVy / 500f).coerceIn(-1f, 1f)
        val bx = BIRD_X
        val by = birdY
        scope.drawRect(
            color = RetroYellow,
            topLeft = Offset(bx - 13f, by - 10f),
            size = Size(26f, 20f)
        )
        scope.drawRect(
            color = RetroBlack,
            topLeft = Offset(bx - 13f, by - 10f),
            size = Size(26f, 20f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
        )
        // Eye
        scope.drawRect(
            color = RetroWhite,
            topLeft = Offset(bx + 3f, by - 8f),
            size = Size(9f, 9f)
        )
        scope.drawRect(
            color = RetroBlack,
            topLeft = Offset(bx + 7f, by - 6f),
            size = Size(4f, 4f)
        )
        // Beak
        scope.drawRect(
            color = RetroOrange,
            topLeft = Offset(bx + 12f, by - 1f + tilt * 3f),
            size = Size(9f, 6f)
        )
        // Flapping Wing
        val wingY = if ((time * 12).toInt() % 2 == 0) -4f else 3f
        scope.drawRect(
            color = RetroOrange,
            topLeft = Offset(bx - 12f, by + wingY),
            size = Size(12f, 7f)
        )

        // Large Score in center
        RetroDrawing.drawText(scope, "$score", W / 2f, 70f, 56f, RetroWhite, centerX = true, centerY = true)
        RetroDrawing.drawText(scope, "HI ${max(highScore, score)}", 10f, 18f, 22f, RetroYellow)

        if (!started && isAlive) {
            RetroDrawing.drawText(scope, "TAP / SPACE TO FLAP", W / 2f, 190f, 24f, RetroWhite, centerX = true, centerY = true)
        }
        RetroDrawing.drawText(scope, "tap anywhere to flap", W / 2f, 498f, 18f, Color(0xFF5A3C1E), centerX = true, centerY = true)

        drawControls(scope, showDpad = false, showA = false, showFlap = true)

        if (!isAlive) {
            drawGameOver(scope)
        }
    }
}
