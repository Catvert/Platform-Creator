package be.catvert.pc.actions

import be.catvert.pc.GameObject
import be.catvert.pc.components.logics.PhysicsComponent
import be.catvert.pc.utility.Description
import be.catvert.pc.utility.ExposeEditor
import com.badlogic.gdx.Gdx
import com.fasterxml.jackson.annotation.JsonCreator
import kotlin.math.roundToInt

@Description("Permet de déplacer un game object sans utiliser le moteur physique")
class MoveAction(@ExposeEditor(min = -100f, max = 100f) var moveX: Int, @ExposeEditor(min = -100f, max = 100f) var moveY: Int, @ExposeEditor var physics: Boolean) : Action() {
    @JsonCreator private constructor() : this(0, 0, true)

    override fun invoke(gameObject: GameObject) {
        val moveX = (moveX * Gdx.graphics.deltaTime * 60f)
        val moveY = (moveY * Gdx.graphics.deltaTime * 60f)

        if (physics) {
            gameObject.getCurrentState().getComponent<PhysicsComponent>()?.move(moveX, moveY, gameObject)
        } else {
            gameObject.box.move(moveX, moveY)
        }
    }

    override fun toString() = super.toString() + " - { move x : $moveX ; move y : $moveY }"
}