package be.catvert.pc.actions

import be.catvert.pc.GameObject
import be.catvert.pc.GameObjectState
import be.catvert.pc.utility.ExposeEditor
import com.fasterxml.jackson.annotation.JsonCreator

/**
 * Représente les différentes directions possible
 */

/**
 * Action permettant de faire bouger le gameObject dans une direction et une vitesse donnée.
 */
class MoveAction(@ExposeEditor var direction: Direction, @ExposeEditor(maxInt = 100) var moveSpeed: Int) : Action {
    @JsonCreator private constructor() : this(Direction.LEFT, 0)

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