package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.theme.RetroBlack

@Composable
fun GameHubApp(
    viewModel: GameHubViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val activeGame by viewModel.activeGame.collectAsStateWithLifecycle()
    val highScores by viewModel.highScores.collectAsStateWithLifecycle()
    val selectedIndex by viewModel.selectedMenuIndex.collectAsStateWithLifecycle()
    val crtScanlinesEnabled by viewModel.crtScanlinesEnabled.collectAsStateWithLifecycle()

    Surface(
        modifier = modifier
            .fillMaxSize()
            .background(RetroBlack)
            .windowInsetsPadding(WindowInsets.safeDrawing),
        color = RetroBlack
    ) {
        val currentGame = activeGame
        if (currentGame != null) {
            GameScreen(
                game = currentGame,
                crtScanlinesEnabled = crtScanlinesEnabled,
                onSubmitScore = { key, score ->
                    viewModel.submitScore(key, score)
                },
                onReturnToMenu = {
                    viewModel.returnToMenu()
                }
            )
        } else {
            GameHubMenu(
                highScores = highScores,
                selectedIndex = selectedIndex,
                crtScanlinesEnabled = crtScanlinesEnabled,
                onToggleScanlines = {
                    viewModel.toggleCrtScanlines()
                },
                onSelectGame = { gameType ->
                    viewModel.launchGame(gameType)
                }
            )
        }
    }
}
