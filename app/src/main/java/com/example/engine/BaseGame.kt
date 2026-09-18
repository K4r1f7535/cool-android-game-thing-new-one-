package com.example.engine

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.example.ui.theme.RetroCyan
import com.example.ui.theme.RetroDkGrey
import com.example.ui.theme.RetroGrey
import com.example.ui.theme.RetroRed
import com.example.ui.theme.RetroWhite
import com.example.ui.theme.RetroYellow

abstract class BaseGame(val gameType: GameType) {
    companion object {
        const val W = 360f
        const val H = 640f
        const val BTN = 46f
        const val PAD_CX = 180f
        const val PAD_CY = 560f

        // Controls Bounding Boxes
        val DPAD_UP = RectF(PAD_CX - BTN / 2f, PAD_CY - BTN - BTN / 2f - 2f, BTN, BTN)
        val DPAD_DOWN = RectF(PAD_CX - BTN / 2f, PAD_CY + BTN / 2f + 2f, BTN, BTN)
        val DPAD_LEFT = RectF(PAD_CX - BTN - BTN / 2f - 2f, PAD_CY - BTN / 2f, BTN, BTN)
        val DPAD_RIGHT = RectF(PAD_CX + BTN / 2f + 2f, PAD_CY - BTN / 2f, BTN, BTN)

        val A_BUTTON_RECT = RectF(W - 96f, PAD_CY - 28f, 76f, 56f)
        val BACK_BUTTON_RECT = RectF(W - 78f, 8f, 68f, 30f)
        val FLAP_BUTTON_RECT = RectF(40f, PAD_CY - 46f, W - 80f, 92f)
    }

    var isDone: Boolean = false
    var score: Int = 0
    var highScore: Int = 0
    var isAlive: Boolean = true
    var isNewRecord: Boolean = false

    abstract fun reset(currentHighScore: Int)
    abstract fun handleInput(
        action: GameAction?,
        tapVirtual: Pair<Float, Float>?,
        isDownHeld: Boolean,
        heldDirections: Set<GameAction>
    )
    abstract fun update(dt: Float, onSubmitScore: (key: String, score: Int) -> Boolean)
    abstract fun draw(scope: DrawScope, heldDirections: Set<GameAction>)

    fun checkBack(action: GameAction?, tapVirtual: Pair<Float, Float>?): Boolean {
        if (action == GameAction.BACK) {
            isDone = true
            return true
        }
        if (tapVirtual != null && BACK_BUTTON_RECT.contains(tapVirtual.first, tapVirtual.second)) {
            isDone = true
            return true
        }
        return false
    }

    fun drawControls(
        scope: DrawScope,
        showDpad: Boolean = true,
        showA: Boolean = false,
        aLabel: String = "A",
        held: Set<GameAction> = emptySet(),
        showFlap: Boolean = false
    ) {
        if (showDpad) {
            val pressedColor = Color(0xFF5A5A82)
            // UP
            RetroDrawing.drawButton(
                scope, DPAD_UP.x, DPAD_UP.y, DPAD_UP.w, DPAD_UP.h,
                "^", if (held.contains(GameAction.UP)) pressedColor else RetroDkGrey,
                fontSize = 28f, isPressed = held.contains(GameAction.UP)
            )
            // DOWN
            RetroDrawing.drawButton(
                scope, DPAD_DOWN.x, DPAD_DOWN.y, DPAD_DOWN.w, DPAD_DOWN.h,
                "v", if (held.contains(GameAction.DOWN)) pressedColor else RetroDkGrey,
                fontSize = 24f, isPressed = held.contains(GameAction.DOWN)
            )
            // LEFT
            RetroDrawing.drawButton(
                scope, DPAD_LEFT.x, DPAD_LEFT.y, DPAD_LEFT.w, DPAD_LEFT.h,
                "<", if (held.contains(GameAction.LEFT)) pressedColor else RetroDkGrey,
                fontSize = 28f, isPressed = held.contains(GameAction.LEFT)
            )
            // RIGHT
            RetroDrawing.drawButton(
                scope, DPAD_RIGHT.x, DPAD_RIGHT.y, DPAD_RIGHT.w, DPAD_RIGHT.h,
                ">", if (held.contains(GameAction.RIGHT)) pressedColor else RetroDkGrey,
                fontSize = 28f, isPressed = held.contains(GameAction.RIGHT)
            )
        }

        if (showA) {
            RetroDrawing.drawButton(
                scope, A_BUTTON_RECT.x, A_BUTTON_RECT.y, A_BUTTON_RECT.w, A_BUTTON_RECT.h,
                aLabel, RetroRed, fontSize = 22f
            )
        }

        if (showFlap) {
            RetroDrawing.drawButton(
                scope, FLAP_BUTTON_RECT.x, FLAP_BUTTON_RECT.y, FLAP_BUTTON_RECT.w, FLAP_BUTTON_RECT.h,
                "FLAP", RetroRed, fontSize = 36f
            )
        }

        // MENU button at top right
        RetroDrawing.drawButton(
            scope, BACK_BUTTON_RECT.x, BACK_BUTTON_RECT.y, BACK_BUTTON_RECT.w, BACK_BUTTON_RECT.h,
            "MENU", RetroDkGrey, fontSize = 18f
        )
    }

    fun drawGameOver(scope: DrawScope, extra: String = "") {
        // Dim overlay
        scope.drawRect(
            color = Color(0xAA000000),
            topLeft = Offset.Zero,
            size = Size(W, H)
        )

        RetroDrawing.drawText(scope, "GAME OVER", W / 2f, 200f, 44f, RetroRed, centerX = true, centerY = true)
        RetroDrawing.drawText(scope, "SCORE $score", W / 2f, 260f, 30f, RetroWhite, centerX = true, centerY = true)
        if (isNewRecord) {
            RetroDrawing.drawText(scope, "NEW RECORD!", W / 2f, 300f, 28f, RetroYellow, centerX = true, centerY = true)
        }
        if (extra.isNotEmpty()) {
            RetroDrawing.drawText(scope, extra, W / 2f, 336f, 20f, RetroGrey, centerX = true, centerY = true)
        }
        RetroDrawing.drawText(scope, "TAP OR PRESS SPACE", W / 2f, 390f, 22f, RetroCyan, centerX = true, centerY = true)
        RetroDrawing.drawText(scope, "TO PLAY AGAIN", W / 2f, 416f, 22f, RetroCyan, centerX = true, centerY = true)
    }
}

data class RectF(val x: Float, val y: Float, val w: Float, val h: Float) {
    val right: Float get() = x + w
    val bottom: Float get() = y + h
    val centerx: Float get() = x + w / 2f
    val centery: Float get() = y + h / 2f

    fun contains(px: Float, py: Float): Boolean {
        return px in x..right && py in y..bottom
    }

    fun collides(other: RectF): Boolean {
        return x < other.right && right > other.x && y < other.bottom && bottom > other.y
    }

    fun inflate(dx: Float, dy: Float): RectF {
        return RectF(x - dx, y - dy, w + dx * 2f, h + dy * 2f)
    }
}
