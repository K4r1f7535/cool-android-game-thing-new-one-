package com.example.engine

enum class GameAction {
    UP, DOWN, LEFT, RIGHT, ACTION, BACK
}

data class InputSnapshot(
    val actions: Set<GameAction> = emptySet(),
    val heldDirections: Set<GameAction> = emptySet(),
    val tapPosition: Pair<Float, Float>? = null,
    val isDownHeld: Boolean = false
)
