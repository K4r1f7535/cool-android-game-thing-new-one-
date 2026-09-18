package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class HighScoreRepository(private val dao: HighScoreDao) {

    val highScores: Flow<Map<String, Int>> = dao.getAllHighScores().map { list ->
        list.associate { it.gameKey to it.highScore }
    }

    suspend fun getHighScore(key: String): Int = withContext(Dispatchers.IO) {
        dao.getHighScore(key) ?: 0
    }

    suspend fun submitScore(key: String, score: Int): Boolean = withContext(Dispatchers.IO) {
        val current = dao.getHighScore(key) ?: 0
        if (score > current) {
            dao.saveHighScore(HighScoreEntity(key, score))
            true
        } else {
            false
        }
    }
}
