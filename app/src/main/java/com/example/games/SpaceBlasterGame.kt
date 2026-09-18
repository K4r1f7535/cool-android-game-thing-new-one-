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
import com.example.ui.theme.RetroOrange
import com.example.ui.theme.RetroRed
import com.example.ui.theme.RetroWhite
import com.example.ui.theme.RetroYellow
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

data class SpaceBullet(var x: Float, var y: Float)
data class SpaceEnemy(var x: Float, var y: Float, val speed: Float)

class SpaceBlasterGame : BaseGame(GameType.SPACE_BLASTER) {

    companion object {
        const val PW = 30f
        const val PH = 20f
        const val PY = 430f
        const val BULLET_SPEED = 380f
    }

    private var px = (W - PW) / 2f
    private val bullets = mutableListOf<SpaceBullet>()
    private val enemies = mutableListOf<SpaceEnemy>()
    private var shootCooldown = 0f
    private var spawnTimer = 0f
    private val stars = mutableListOf<Pair<Float, Float>>()

    init {
        for (i in 0 until 35) {
            stars.add(Pair(Random.nextFloat() * W, Random.nextFloat() * 460f))
        }
        reset(0)
    }

    override fun reset(currentHighScore: Int) {
        highScore = currentHighScore
        score = 0
        isAlive = true
        isNewRecord = false
        isDone = false
        px = (W - PW) / 2f
        bullets.clear()
        enemies.clear()
        shootCooldown = 0f
        spawnTimer = 0f
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

        var shooting = action == GameAction.UP || action == GameAction.ACTION
        if (tapVirtual != null) {
            val (tx, ty) = tapVirtual
            if (A_BUTTON_RECT.contains(tx, ty)) {
                shooting = true
            }
        }

        if (shooting && shootCooldown <= 0f) {
            bullets.add(SpaceBullet(px + PW / 2f - 2f, PY))
            shootCooldown = 0.18f
        }
    }

    override fun update(dt: Float, onSubmitScore: (key: String, score: Int) -> Boolean) {
        if (!isAlive) return

        shootCooldown -= dt
        spawnTimer -= dt

        if (spawnTimer <= 0f) {
            enemies.add(
                SpaceEnemy(
                    x = 20f + Random.nextFloat() * (W - 64f),
                    y = 50f,
                    speed = 90f + Random.nextFloat() * 70f
                )
            )
            spawnTimer = max(0.35f, 1.3f - (score / 150f))
        }

        // Bullets move up
        for (b in bullets) {
            b.y -= BULLET_SPEED * dt
        }
        bullets.removeAll { it.y < 0f }

        // Enemies descend
        val pRect = RectF(px, PY, PW, PH)
        for (e in enemies) {
            e.y += e.speed * dt
            val eRect = RectF(e.x, e.y, 24f, 20f)
            if (eRect.collides(pRect) || e.y > 450f) {
                isAlive = false
                isNewRecord = onSubmitScore(gameType.key, score)
                if (isNewRecord) highScore = max(highScore, score)
                return
            }
        }

        // Bullet vs enemy collisions
        val bulletsToRemove = mutableSetOf<SpaceBullet>()
        val enemiesToRemove = mutableSetOf<SpaceEnemy>()

        for (b in bullets) {
            val bRect = RectF(b.x, b.y, 4f, 10f)
            for (e in enemies) {
                if (e in enemiesToRemove) continue
                val eRect = RectF(e.x, e.y, 24f, 20f)
                if (bRect.collides(eRect)) {
                    bulletsToRemove.add(b)
                    enemiesToRemove.add(e)
                    score += 10
                    break
                }
            }
        }

        bullets.removeAll(bulletsToRemove)
        enemies.removeAll(enemiesToRemove)
    }

    override fun draw(scope: DrawScope, heldDirections: Set<GameAction>) {
        // Continuous movement while held
        if (isAlive) {
            if (heldDirections.contains(GameAction.LEFT)) {
                px = max(10f, px - 6f)
            }
            if (heldDirections.contains(GameAction.RIGHT)) {
                px = min(W - 10f - PW, px + 6f)
            }
        }

        scope.drawRect(color = RetroBlack, topLeft = Offset.Zero, size = Size(W, H))

        // Background stars
        for ((sx, sy) in stars) {
            scope.drawRect(
                color = Color(0x99FFFFFF),
                topLeft = Offset(sx, sy),
                size = Size(2f, 2f)
            )
        }

        // Player ship
        scope.drawRect(
            color = RetroCyan,
            topLeft = Offset(px, PY),
            size = Size(PW, PH)
        )
        // Cockpit
        scope.drawRect(
            color = RetroWhite,
            topLeft = Offset(px + 10f, PY - 6f),
            size = Size(10f, 6f)
        )

        // Bullets
        for (b in bullets) {
            scope.drawRect(
                color = RetroYellow,
                topLeft = Offset(b.x, b.y),
                size = Size(4f, 10f)
            )
        }

        // Enemies
        for (e in enemies) {
            scope.drawRect(
                color = RetroRed,
                topLeft = Offset(e.x, e.y),
                size = Size(24f, 20f)
            )
            scope.drawRect(
                color = RetroOrange,
                topLeft = Offset(e.x + 6f, e.y + 4f),
                size = Size(12f, 12f)
            )
        }

        // Header info
        RetroDrawing.drawText(scope, "SCORE $score", 10f, 16f, 22f, RetroWhite)
        RetroDrawing.drawText(scope, "HI ${max(highScore, score)}", 10f, 38f, 22f, RetroYellow)

        drawControls(scope, showDpad = true, showA = true, aLabel = "FIRE", held = heldDirections)

        if (!isAlive) {
            drawGameOver(scope)
        }
    }
}
