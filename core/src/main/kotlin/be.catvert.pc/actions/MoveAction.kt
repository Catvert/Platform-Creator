package be.catvert.pc.actions

import be.catvert.pc.GameObject
import be.catvert.pc.components.logics.PhysicsComponent
import be.catvert.pc.utility.ExposeEditor
import com.fasterxml.jackson.annotation.JsonCreator

class MoveAction(@ExposeEditor var moveX: Int, @ExposeEditor var moveY: Int, @ExposeEditor var physics: Boolean) : Action {
    @JsonCreator private constructor(): this(0, 0, true)

    override fun invoke(gameObject: GameObject) {
        if(physics) {
            gameObject.getCurrentState().getComponent<PhysicsComponent>()?.tryMove(moveX, moveY, gameObject)
        }
        else {
            gameObject.box.move(moveX, moveY)
        }
    }
}