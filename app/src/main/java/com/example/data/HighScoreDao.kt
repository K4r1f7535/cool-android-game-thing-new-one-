package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HighScoreDao {
    @Query("SELECT * FROM high_scores")
    fun getAllHighScores(): Flow<List<HighScoreEntity>>

    @Query("SELECT highScore FROM high_scores WHERE gameKey = :key LIMIT 1")
    suspend fun getHighScore(key: String): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveHighScore(score: HighScoreEntity)
}
