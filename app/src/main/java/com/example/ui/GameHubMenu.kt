package com.example.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.TvOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.GameType
import com.example.engine.RetroDrawing
import com.example.ui.theme.RetroBlack
import com.example.ui.theme.RetroCyan
import com.example.ui.theme.RetroGrey
import com.example.ui.theme.RetroNavy
import com.example.ui.theme.RetroPink
import com.example.ui.theme.RetroWhite
import com.example.ui.theme.RetroYellow
import kotlin.math.sin

@Composable
fun GameHubMenu(
    highScores: Map<String, Int>,
    selectedIndex: Int,
    crtScanlinesEnabled: Boolean,
    onToggleScanlines: () -> Unit,
    onSelectGame: (GameType) -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "menu_anim")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "time"
    )

    val bobOffset = (sin(Math.toRadians(time.toDouble() * 3.0)) * 6.0).dp

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(RetroBlack)
            .drawWithContent {
                // Draw retro animated horizontal lines
                val h = size.height
                val w = size.width
                val scroll = (time * 15f) % 40f
                var y = scroll
                while (y < h) {
                    drawLine(
                        color = RetroNavy,
                        start = Offset(0f, y),
                        end = Offset(w, y),
                        strokeWidth = 2f
                    )
                    y += 40f
                }
                drawContent()
                if (crtScanlinesEnabled) {
                    RetroDrawing.drawScanlines(this, w, h)
                }
            }
    ) {
        // CRT Scanline Toggle in Top-Right
        IconButton(
            onClick = onToggleScanlines,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)
                .testTag("crt_toggle_button")
        ) {
            Icon(
                imageVector = if (crtScanlinesEnabled) Icons.Default.Tv else Icons.Default.TvOff,
                contentDescription = "Toggle CRT Scanlines",
                tint = if (crtScanlinesEnabled) RetroCyan else RetroGrey,
                modifier = Modifier.size(24.dp)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(18.dp))

            // Animated Bobbing Title matching Python:
            // COOL-LIL / GAME HUB / * * * YAY * * *
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.offset(y = bobOffset)
            ) {
                Text(
                    text = "COOL-LIL",
                    color = RetroPink,
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center,
                    letterSpacing = 2.sp
                )
                Text(
                    text = "GAME HUB",
                    color = RetroYellow,
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center,
                    letterSpacing = 2.sp
                )
                Text(
                    text = "* * *  YAY  * * *",
                    color = RetroCyan,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center,
                    letterSpacing = 3.sp
                )
            }

            Spacer(modifier = Modifier.height(26.dp))

            // 6 Retro Game Buttons
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                GameType.ALL.forEachIndexed { index, gameType ->
                    val highScore = highScores[gameType.key] ?: 0
                    RetroGameButton(
                        gameType = gameType,
                        highScore = highScore,
                        isSelected = index == selectedIndex,
                        onClick = { onSelectGame(gameType) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Footer instructions
            Text(
                text = "TAP A GAME TO PLAY",
                color = RetroGrey,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "ON-SCREEN D-PAD & GESTURE CONTROLS",
                color = Color(0xFF5A5A78),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun RetroGameButton(
    gameType: GameType,
    highScore: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .testTag("game_button_${gameType.key}")
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = RetroWhite),
                onClick = onClick
            )
    ) {
        // 3D Shadow offset block
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = 3.dp, y = 4.dp)
                .background(Color(0xFF06060C))
        )

        // Button Face
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(gameType.color)
                .border(2.dp, RetroWhite)
                .padding(vertical = 12.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // White highlight line at top
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(Color(0x88FFFFFF))
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isSelected) {
                    Text(
                        text = "> ",
                        color = RetroYellow,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Text(
                    text = gameType.title,
                    color = RetroWhite,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )

                if (isSelected) {
                    Text(
                        text = " <",
                        color = RetroYellow,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "HI $highScore",
                color = RetroYellow,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
