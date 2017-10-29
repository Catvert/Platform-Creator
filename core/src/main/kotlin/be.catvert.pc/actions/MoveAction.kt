package be.catvert.pc.actions

import be.catvert.pc.GameObject
import be.catvert.pc.utility.ExposeEditor

/**
 * Représente les différentes directions possible
 */

/**
 * Action permettant de faire bouger le gameObject dans une direction et une vitesse donnée.
 */
class MoveAction(val direction: Direction, @ExposeEditor(maxInt = 100) val moveSpeed: Int) : Action{
    enum class Direction {
        LEFT, RIGHT, UP, DOWN
    }

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