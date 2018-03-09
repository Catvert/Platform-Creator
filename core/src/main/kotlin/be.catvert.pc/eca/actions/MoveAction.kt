package be.catvert.pc.eca.actions

import be.catvert.pc.eca.Entity
import be.catvert.pc.eca.components.logics.PhysicsComponent
import be.catvert.pc.utility.Constants
import be.catvert.pc.ui.Description
import be.catvert.pc.ui.UI
import be.catvert.pc.utility.Utility
import com.fasterxml.jackson.annotation.JsonCreator

@Description("Permet de déplacer une entité en utilisant ou non le moteur physique")
class MoveAction(@UI(min = -100f, max = 100f) var moveX: Int, @UI(min = -100f, max = 100f) var moveY: Int, @UI var physics: Boolean) : Action() {
    @JsonCreator private constructor() : this(0, 0, true)

    override fun invoke(entity: Entity) {
        val moveX = moveX * Utility.getDeltaTime() * Constants.physicsDeltaSpeed
        val moveY = moveY * Utility.getDeltaTime() * Constants.physicsDeltaSpeed

        if (physics) {
            entity.getCurrentState().getComponent<PhysicsComponent>()?.move(true, moveX, moveY)
        } else {
            entity.box.move(moveX, moveY)
        }
    }

    override fun toString() = super.toString() + " - { move x : $moveX ; move y : $moveY }"
}