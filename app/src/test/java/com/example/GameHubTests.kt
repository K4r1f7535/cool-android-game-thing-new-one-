package com.example

import com.example.engine.GameAction
import com.example.engine.GameType
import com.example.games.BrickBreakerGame
import com.example.games.FlappyGame
import com.example.games.Game2048
import com.example.games.SnakeGame
import com.example.games.SpaceBlasterGame
import com.example.games.TempleRunnerGame
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class GameHubTests {

    @Test
    fun testAllGameTypesDefined() {
        assertEquals(6, GameType.ALL.size)
        val keys = GameType.ALL.map { it.key }
        assertTrue(keys.contains("snake"))
        assertTrue(keys.contains("2048"))
        assertTrue(keys.contains("runner"))
        assertTrue(keys.contains("flappy"))
        assertTrue(keys.contains("blaster"))
        assertTrue(keys.contains("breaker"))
    }

    @Test
    fun testSnakeInitializationAndTurn() {
        val snake = SnakeGame()
        snake.reset(50)
        assertEquals(50, snake.highScore)
        assertEquals(0, snake.score)
        assertTrue(snake.isAlive)

        // Queue turn
        snake.handleInput(GameAction.UP, null, false, emptySet())
        snake.update(0.25f) { _, _ -> false }
        assertTrue(snake.isAlive)
    }

    @Test
    fun test2048SlideAndSpawn() {
        val g2048 = Game2048()
        g2048.reset(100)
        assertEquals(100, g2048.highScore)
        assertTrue(g2048.isAlive)

        // Perform moves
        g2048.handleInput(GameAction.LEFT, null, false, emptySet())
        g2048.update(0.15f) { _, _ -> false }
        g2048.handleInput(GameAction.UP, null, false, emptySet())
        g2048.update(0.15f) { _, _ -> false }
        assertTrue(g2048.isAlive)
    }

    @Test
    fun testTempleRunnerJumpAndDuck() {
        val runner = TempleRunnerGame()
        runner.reset(20)
        assertTrue(runner.isAlive)

        // Jump
        runner.handleInput(GameAction.UP, null, false, emptySet())
        runner.update(0.1f) { _, _ -> false }
        assertTrue(runner.isAlive)

        // Duck
        runner.handleInput(GameAction.DOWN, null, true, setOf(GameAction.DOWN))
        runner.update(0.1f) { _, _ -> false }
        assertTrue(runner.isAlive)
    }

    @Test
    fun testFlappyBounceFlap() {
        val flappy = FlappyGame()
        flappy.reset(10)
        assertTrue(flappy.isAlive)

        // Flap
        flappy.handleInput(GameAction.ACTION, null, false, emptySet())
        flappy.update(0.05f) { _, _ -> false }
        assertTrue(flappy.isAlive)
    }

    @Test
    fun testSpaceBlasterFire() {
        val blaster = SpaceBlasterGame()
        blaster.reset(30)
        assertTrue(blaster.isAlive)

        // Fire bullet
        blaster.handleInput(GameAction.ACTION, null, false, emptySet())
        blaster.update(0.1f) { _, _ -> false }
        assertTrue(blaster.isAlive)
    }

    @Test
    fun testBrickBreakerPaddleMove() {
        val breaker = BrickBreakerGame()
        breaker.reset(80)
        assertTrue(breaker.isAlive)

        // Move paddle
        breaker.handleInput(GameAction.RIGHT, null, false, setOf(GameAction.RIGHT))
        breaker.update(0.05f) { _, _ -> false }
        assertTrue(breaker.isAlive)
    }
}
