package be.catvert.pc.actions

import be.catvert.pc.GameObject

/**
 * Représente les différentes directions possible
 */
enum class Direction {
    LEFT, RIGHT, UP, DOWN
}

/**
 * Action permettant de faire bouger le gameObject dans une direction et une vitesse donnée.
 */
class MoveAction(val direction: Direction,val moveSpeed: Int) : Action {
    override fun perform(gameObject: GameObject) {
        var xSpeed = 0
        var ySpeed = 0

        when (direction) {
            Direction.LEFT -> xSpeed = -moveSpeed
            Direction.RIGHT -> xSpeed = moveSpeed
            Direction.UP -> ySpeed = moveSpeed
            Direction.DOWN -> ySpeed = -moveSpeed
        }

        gameObject.rectangle.x += xSpeed
        gameObject.rectangle.y += ySpeed
    }
}