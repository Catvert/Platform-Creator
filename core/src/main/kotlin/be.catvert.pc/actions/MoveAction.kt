package be.catvert.pc.actions

import be.catvert.pc.GameObject
import be.catvert.pc.GameObjectState
import be.catvert.pc.utility.ExposeEditor
import com.fasterxml.jackson.annotation.JsonCreator

/**
 * Représente les différentes directions possible
 */

/**
 * Action permettant de mouvoir un gameObject dans l'espace
 */
class MoveAction(@ExposeEditor var moveX: Int, @ExposeEditor var moveY: Int) : Action {
    @JsonCreator private constructor() : this(0, 0)

    override fun invoke(gameObject: GameObject) {
        gameObject.rectangle.move(moveX, moveY)
    }
}