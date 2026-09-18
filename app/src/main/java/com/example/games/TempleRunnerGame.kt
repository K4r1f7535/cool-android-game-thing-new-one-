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
import com.example.ui.theme.RetroGrey
import com.example.ui.theme.RetroOrange
import com.example.ui.theme.RetroWhite
import com.example.ui.theme.RetroYellow
import kotlin.math.max
import kotlin.random.Random

data class RunnerObstacle(
    val kind: String, // "rock" or "branch"
    var x: Float,
    val y: Float,
    val w: Float,
    val h: Float
)

class TempleRunnerGame : BaseGame(GameType.RUNNER) {

    companion object {
        const val GROUND_Y = 400f
        const val PLAYER_X = 70f
        const val STAND_H = 44f
        const val DUCK_H = 22f
        const val PLAYER_W = 28f
        const val GRAVITY = 1900f
        const val JUMP_SPEED = -640f
    }

    private var py = GROUND_Y - STAND_H
    private var vy = 0f
    private var onGround = true
    private var ducking = false
    private var duckTimer = 0f
    private var speed = 240f
    private var distance = 0f
    private var scroll = 0f
    private var spawnIn = 1.0f
    private val obstacles = mutableListOf<RunnerObstacle>()
    private val stars = mutableListOf<Pair<Float, Float>>()

    init {
        for (i in 0 until 28) {
            stars.add(Pair(Random.nextFloat() * W, 20f + Random.nextFloat() * 280f))
        }
        reset(0)
    }

    override fun reset(currentHighScore: Int) {
        highScore = currentHighScore
        score = 0
        isAlive = true
        isNewRecord = false
        isDone = false
        py = GROUND_Y - STAND_H
        vy = 0f
        onGround = true
        ducking = false
        duckTimer = 0f
        speed = 240f
        distance = 0f
        scroll = 0f
        spawnIn = 1.0f
        obstacles.clear()
    }

    private fun playerRect(): RectF {
        val h = if (ducking) DUCK_H else STAND_H
        val top = if (!ducking) py else if (onGround) GROUND_Y - h else py
        return RectF(PLAYER_X, top, PLAYER_W, h)
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
            if (onGround) {
                vy = JUMP_SPEED
                onGround = false
            }
        } else if (action == GameAction.DOWN) {
            duckTimer = 0.55f
        }

        if (tapVirtual != null) {
            val (tx, ty) = tapVirtual
            if (A_BUTTON_RECT.contains(tx, ty) || ty < 470f) {
                if (onGround) {
                    vy = JUMP_SPEED
                    onGround = false
                }
            }
        }

        ducking = (isDownHeld || heldDirections.contains(GameAction.DOWN) || duckTimer > 0f) && onGround
    }

    override fun update(dt: Float, onSubmitScore: (key: String, score: Int) -> Boolean) {
        if (!isAlive) return

        duckTimer = max(0f, duckTimer - dt)

        if (!onGround) {
            vy += GRAVITY * dt
            py += vy * dt
            val floor = GROUND_Y - STAND_H
            if (py >= floor) {
                py = floor
                vy = 0f
                onGround = true
            }
        }

        speed = (speed + 6.0f * dt).coerceAtMost(560f)
        distance += speed * dt
        score = (distance / 10f).toInt()
        scroll = (scroll + speed * dt) % 40f

        spawnIn -= dt
        if (spawnIn <= 0f) {
            if (Random.nextFloat() < 0.55f) {
                val h = listOf(26f, 34f, 42f).random()
                obstacles.add(RunnerObstacle("rock", W + 20f, GROUND_Y - h, 26f, h))
            } else {
                obstacles.add(RunnerObstacle("branch", W + 20f, GROUND_Y - 62f, 44f, 26f))
            }
            val gap = (Random.nextFloat() * 0.7f + 0.9f) * (300f / speed) * 1.25f
            spawnIn = max(0.55f, gap)
        }

        for (ob in obstacles) {
            ob.x -= speed * dt
        }
        obstacles.removeAll { it.x < -60f }

        val me = playerRect().inflate(-3f, -2f)
        for (ob in obstacles) {
            val obRect = RectF(ob.x, ob.y, ob.w, ob.h).inflate(-2f, -1f)
            if (me.collides(obRect)) {
                isAlive = false
                isNewRecord = onSubmitScore(gameType.key, score)
                if (isNewRecord) highScore = max(highScore, score)
                break
            }
        }
    }

    override fun draw(scope: DrawScope, heldDirections: Set<GameAction>) {
        // Night sky background
        scope.drawRect(color = Color(0xFF1C1634), topLeft = Offset.Zero, size = Size(W, H))

        // Stars
        for ((sx, sy) in stars) {
            scope.drawRect(
                color = Color(0xFFC8C8E6),
                topLeft = Offset(sx, sy),
                size = Size(2f, 2f)
            )
        }

        // Parallax temple ruins in background
        for (i in 0 until 4) {
            val bx = ((i * 120 - (distance * 0.1f).toInt()) % (W.toInt() + 120)) - 60f
            scope.drawRect(
                color = Color(0xFF2C244E),
                topLeft = Offset(bx, GROUND_Y - 70f),
                size = Size(70f, 70f)
            )
            scope.drawRect(
                color = Color(0xFF2C244E),
                topLeft = Offset(bx + 12f, GROUND_Y - 96f),
                size = Size(46f, 26f)
            )
        }

        // Ground
        scope.drawRect(
            color = Color(0xFF5A3C28),
            topLeft = Offset(0f, GROUND_Y),
            size = Size(W, 90f)
        )
        scope.drawLine(
            color = RetroOrange,
            start = Offset(0f, GROUND_Y),
            end = Offset(W, GROUND_Y),
            strokeWidth = 3f
        )
        var x = -40f
        while (x < W + 40f) {
            scope.drawRect(
                color = Color(0xFF462E1E),
                topLeft = Offset(x - scroll, GROUND_Y + 14f),
                size = Size(20f, 6f)
            )
            x += 40f
        }

        // Obstacles
        for (ob in obstacles) {
            if (ob.kind == "rock") {
                scope.drawRect(
                    color = Color(0xFF9696AA),
                    topLeft = Offset(ob.x, ob.y),
                    size = Size(ob.w, ob.h)
                )
                scope.drawRect(
                    color = RetroWhite,
                    topLeft = Offset(ob.x, ob.y),
                    size = Size(ob.w, ob.h),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
                )
            } else {
                // Branch
                scope.drawRect(
                    color = RetroDkGreen,
                    topLeft = Offset(ob.x, ob.y),
                    size = Size(ob.w, ob.h)
                )
                scope.drawRect(
                    color = RetroGreen,
                    topLeft = Offset(ob.x, ob.y),
                    size = Size(ob.w, ob.h),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
                )
                scope.drawRect(
                    color = Color(0xFF6E4628),
                    topLeft = Offset(ob.x + ob.w / 2f - 3f, ob.y + ob.h),
                    size = Size(6f, 30f)
                )
            }
        }

        // Player
        val p = playerRect()
        scope.drawRect(
            color = RetroOrange,
            topLeft = Offset(p.x, p.y),
            size = Size(p.w, p.h)
        )
        scope.drawRect(
            color = RetroWhite,
            topLeft = Offset(p.x, p.y),
            size = Size(p.w, p.h),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
        )
        // Headband
        scope.drawRect(
            color = RetroYellow,
            topLeft = Offset(p.x, p.y - 4f),
            size = Size(p.w, 6f)
        )
        // Eye
        scope.drawRect(
            color = RetroBlack,
            topLeft = Offset(p.right - 8f, p.y + 5f),
            size = Size(4f, 4f)
        )

        // Header info
        RetroDrawing.drawText(scope, "DIST $score", 10f, 16f, 22f, RetroWhite)
        RetroDrawing.drawText(scope, "HI ${max(highScore, score)}", 10f, 38f, 22f, RetroYellow)
        RetroDrawing.drawText(scope, "tap/A/UP = jump   DOWN = duck", W / 2f, 464f, 16f, RetroGrey, centerX = true, centerY = true)

        drawControls(scope, showDpad = true, showA = true, aLabel = "JUMP", held = heldDirections)

        if (!isAlive) {
            drawGameOver(scope)
        }
    }
}
