package com.example.engine

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.RetroBlue
import com.example.ui.theme.RetroCyan
import com.example.ui.theme.RetroDkGreen
import com.example.ui.theme.RetroOrange
import com.example.ui.theme.RetroPink
import com.example.ui.theme.RetroPurple

enum class GameType(
    val key: String,
    val title: String,
    val color: Color,
    val usesDpad: Boolean,
    val showAButton: Boolean = false,
    val aButtonLabel: String = "A",
    val showFlapButton: Boolean = false,
    val description: String = ""
) {
    SNAKE(
        key = "snake",
        title = "SNAKE II",
        color = RetroDkGreen,
        usesDpad = true,
        description = "Classic nibbler with speed-ups and crunchy visuals"
    ),
    GAME_2048(
        key = "2048",
        title = "2048 RETRO",
        color = RetroOrange,
        usesDpad = true,
        description = "Slide and merge numbers to reach the mythical 2048 tile"
    ),
    RUNNER(
        key = "runner",
        title = "TEMPLE RUNNER",
        color = RetroPurple,
        usesDpad = true,
        showAButton = true,
        aButtonLabel = "JUMP",
        description = "Side-scrolling endless run: Jump over rocks & duck under branches"
    ),
    FLAPPY(
        key = "flappy",
        title = "FLAPPY BOUNCE",
        color = Color(0xFF2896AA),
        usesDpad = false,
        showFlapButton = true,
        description = "Flap between moving pipe barriers with rhythmic precision"
    ),
    SPACE_BLASTER(
        key = "blaster",
        title = "SPACE BLASTER",
        color = RetroCyan,
        usesDpad = true,
        showAButton = true,
        aButtonLabel = "FIRE",
        description = "Defend the galaxy against descending alien invaders"
    ),
    BRICK_BREAKER(
        key = "breaker",
        title = "BRICK BREAKER",
        color = RetroPink,
        usesDpad = true,
        description = "Bounce the ball, shatter neon bricks, and clear the screen"
    );

    companion object {
        val ALL = listOf(SNAKE, GAME_2048, RUNNER, FLAPPY, SPACE_BLASTER, BRICK_BREAKER)
    }
}
