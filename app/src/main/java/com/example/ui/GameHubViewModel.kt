package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.HighScoreRepository
import com.example.engine.BaseGame
import com.example.engine.GameType
import com.example.games.BrickBreakerGame
import com.example.games.FlappyGame
import com.example.games.Game2048
import com.example.games.SnakeGame
import com.example.games.SpaceBlasterGame
import com.example.games.TempleRunnerGame
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class GameHubViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: HighScoreRepository

    val highScores: StateFlow<Map<String, Int>>

    private val _activeGame = MutableStateFlow<BaseGame?>(null)
    val activeGame: StateFlow<BaseGame?> = _activeGame.asStateFlow()

    private val _selectedMenuIndex = MutableStateFlow(0)
    val selectedMenuIndex: StateFlow<Int> = _selectedMenuIndex.asStateFlow()

    private val _crtScanlinesEnabled = MutableStateFlow(true)
    val crtScanlinesEnabled: StateFlow<Boolean> = _crtScanlinesEnabled.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = HighScoreRepository(database.highScoreDao())
        highScores = repository.highScores.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyMap()
        )
    }

    fun toggleCrtScanlines() {
        _crtScanlinesEnabled.value = !_crtScanlinesEnabled.value
    }

    fun selectMenuIndex(index: Int) {
        _selectedMenuIndex.value = index.coerceIn(0, GameType.ALL.size - 1)
    }

    fun launchGame(type: GameType) {
        val currentHigh = highScores.value[type.key] ?: 0
        val game: BaseGame = when (type) {
            GameType.SNAKE -> SnakeGame()
            GameType.GAME_2048 -> Game2048()
            GameType.RUNNER -> TempleRunnerGame()
            GameType.FLAPPY -> FlappyGame()
            GameType.SPACE_BLASTER -> SpaceBlasterGame()
            GameType.BRICK_BREAKER -> BrickBreakerGame()
        }
        game.reset(currentHigh)
        _activeGame.value = game
    }

    fun returnToMenu() {
        _activeGame.value = null
    }

    fun submitScore(gameKey: String, score: Int): Boolean {
        var isNewRecord = false
        // Submit asynchronously to Room DB
        viewModelScope.launch {
            val record = repository.submitScore(gameKey, score)
            if (record) {
                isNewRecord = true
            }
        }
        // Immediate local evaluation for game over screen display
        val current = highScores.value[gameKey] ?: 0
        return score > current
    }
}
