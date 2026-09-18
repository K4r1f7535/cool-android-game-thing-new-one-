package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import com.example.engine.BaseGame
import com.example.engine.GameAction
import com.example.engine.RetroDrawing
import com.example.ui.theme.RetroBlack
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

@Composable
fun GameScreen(
    game: BaseGame,
    crtScanlinesEnabled: Boolean,
    onSubmitScore: (key: String, score: Int) -> Boolean,
    onReturnToMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Intercept system back button
    BackHandler {
        onReturnToMenu()
    }

    val heldDirections = remember { mutableStateListOf<GameAction>() }
    var lastTapVirtual by remember { mutableStateOf<Pair<Float, Float>?>(null) }
    var pendingAction by remember { mutableStateOf<GameAction?>(null) }
    var isDownHeld by remember { mutableStateOf(false) }

    // Repaint ticker
    var frameTick by remember { mutableFloatStateOf(0f) }

    // 60 FPS Game Loop
    LaunchedEffect(game) {
        var lastNanoTime = System.nanoTime()
        while (true) {
            withFrameNanos { now ->
                val dt = min(((now - lastNanoTime) / 1_000_000_000.0f), 0.05f)
                lastNanoTime = now

                // Dispatch any pending inputs
                val currentAction = pendingAction
                pendingAction = null
                val currentTap = lastTapVirtual
                lastTapVirtual = null

                game.handleInput(
                    action = currentAction,
                    tapVirtual = currentTap,
                    isDownHeld = isDownHeld,
                    heldDirections = heldDirections.toSet()
                )

                game.update(dt, onSubmitScore)

                if (game.isDone) {
                    onReturnToMenu()
                }

                frameTick += dt
            }
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("game_screen_container"),
        contentAlignment = Alignment.Center
    ) {
        val screenWidth = constraints.maxWidth.toFloat()
        val screenHeight = constraints.maxHeight.toFloat()

        val virtualW = BaseGame.W
        val virtualH = BaseGame.H

        val scale = min(screenWidth / virtualW, screenHeight / virtualH)
        val destW = virtualW * scale
        val destH = virtualH * scale
        val offsetX = (screenWidth - destW) / 2f
        val offsetY = (screenHeight - destH) / 2f

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .testTag("game_canvas")
                .pointerInput(game, scale, offsetX, offsetY) {
                    val swipeMin = 28f
                    val dpadHitInflate = 14f

                    awaitEachGesture {
                        val down: PointerInputChange = awaitFirstDown(requireUnconsumed = false)
                        val touchX = down.position.x
                        val touchY = down.position.y

                        val vx = (touchX - offsetX) / scale
                        val vy = (touchY - offsetY) / scale
                        val downVirtualPos = Pair(vx, vy)

                        var activeDpad = false

                        // Check Dpad touch
                        if (game.gameType.usesDpad) {
                            if (BaseGame.DPAD_UP.inflate(dpadHitInflate, dpadHitInflate).contains(vx, vy)) {
                                pendingAction = GameAction.UP
                                if (!heldDirections.contains(GameAction.UP)) heldDirections.add(GameAction.UP)
                                activeDpad = true
                            } else if (BaseGame.DPAD_DOWN.inflate(dpadHitInflate, dpadHitInflate).contains(vx, vy)) {
                                pendingAction = GameAction.DOWN
                                if (!heldDirections.contains(GameAction.DOWN)) heldDirections.add(GameAction.DOWN)
                                isDownHeld = true
                                activeDpad = true
                            } else if (BaseGame.DPAD_LEFT.inflate(dpadHitInflate, dpadHitInflate).contains(vx, vy)) {
                                pendingAction = GameAction.LEFT
                                if (!heldDirections.contains(GameAction.LEFT)) heldDirections.add(GameAction.LEFT)
                                activeDpad = true
                            } else if (BaseGame.DPAD_RIGHT.inflate(dpadHitInflate, dpadHitInflate).contains(vx, vy)) {
                                pendingAction = GameAction.RIGHT
                                if (!heldDirections.contains(GameAction.RIGHT)) heldDirections.add(GameAction.RIGHT)
                                activeDpad = true
                            }
                        }

                        // Check A Button / JUMP / FIRE
                        if (game.gameType.showAButton && BaseGame.A_BUTTON_RECT.contains(vx, vy)) {
                            pendingAction = GameAction.ACTION
                        }

                        // Check FLAP Button
                        if (game.gameType.showFlapButton && BaseGame.FLAP_BUTTON_RECT.contains(vx, vy)) {
                            pendingAction = GameAction.ACTION
                        }

                        // Check MENU Button
                        if (BaseGame.BACK_BUTTON_RECT.contains(vx, vy)) {
                            pendingAction = GameAction.BACK
                        }

                        var lastPointerPos = down.position

                        // Pointer movement & drag
                        do {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id }
                            if (change != null && change.pressed) {
                                lastPointerPos = change.position
                                val currentVx = (lastPointerPos.x - offsetX) / scale
                                val currentVy = (lastPointerPos.y - offsetY) / scale

                                // For Brick Breaker or active continuous touch in play field
                                if (currentVy in 350f..480f) {
                                    lastTapVirtual = Pair(currentVx, currentVy)
                                }
                            }
                        } while (event.changes.any { it.id == down.id && it.pressed })

                        // Release
                        heldDirections.clear()
                        isDownHeld = false

                        if (!activeDpad) {
                            val upVx = (lastPointerPos.x - offsetX) / scale
                            val upVy = (lastPointerPos.y - offsetY) / scale

                            val dx = upVx - downVirtualPos.first
                            val dy = upVy - downVirtualPos.second

                            if (max(abs(dx), abs(dy)) < swipeMin) {
                                // Tap
                                lastTapVirtual = Pair(upVx, upVy)
                            } else {
                                // Swipe gesture
                                if (abs(dx) > abs(dy)) {
                                    pendingAction = if (dx > 0) GameAction.RIGHT else GameAction.LEFT
                                } else {
                                    pendingAction = if (dy > 0) GameAction.DOWN else GameAction.UP
                                }
                            }
                        }
                    }
                }
        ) {
            // Read frameTick to trigger recomposition / repaint each frame
            val currentTick = frameTick
            if (currentTick < 0f) return@Canvas

            // Clear black letterbox background
            drawRect(
                color = RetroBlack,
                topLeft = Offset.Zero,
                size = size
            )

            // Save and transform to virtual coordinate space
            translate(left = offsetX, top = offsetY) {
                scale(scale = scale, pivot = Offset.Zero) {
                    // Draw virtual 360x640 canvas
                    game.draw(this, heldDirections.toSet())

                    // Scanlines CRT effect
                    if (crtScanlinesEnabled) {
                        RetroDrawing.drawScanlines(this, virtualW, virtualH)
                    }
                }
            }
        }
    }
}
