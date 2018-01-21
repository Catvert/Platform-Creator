package be.catvert.pc.actions

import be.catvert.pc.GameObject
import be.catvert.pc.components.logics.PhysicsComponent
import be.catvert.pc.utility.ExposeEditor
import com.badlogic.gdx.Gdx
import com.fasterxml.jackson.annotation.JsonCreator
import kotlin.math.roundToInt

class MoveAction(@ExposeEditor(min = -100f, max = 100f) var moveX: Int, @ExposeEditor(min = -100f, max = 100f) var moveY: Int, @ExposeEditor var physics: Boolean) : Action() {
    @JsonCreator private constructor() : this(0, 0, true)

    override fun invoke(gameObject: GameObject) {
        val moveX = (moveX * Gdx.graphics.deltaTime * 60f).roundToInt()
        val moveY = (moveY * Gdx.graphics.deltaTime * 60f).roundToInt()

        if (physics) {
            gameObject.getCurrentState().getComponent<PhysicsComponent>()?.tryMove(moveX, moveY, gameObject)
        } else {
            gameObject.box.move(moveX, moveY)
        }
    }

    override fun toString() = super.toString() + " - { move x : $moveX ; move y : $moveY}"
}