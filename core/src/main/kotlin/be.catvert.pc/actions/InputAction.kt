package be.catvert.pc.actions

import be.catvert.pc.GameObject
import be.catvert.pc.components.logics.InputComponent
import be.catvert.pc.utility.Description
import be.catvert.pc.utility.ExposeEditor
import com.fasterxml.jackson.annotation.JsonCreator

@Description("Permet d'effectuer une action si une touche est pressée")
class InputAction(@ExposeEditor var action: InputComponent.InputData): Action() {
    @JsonCreator private constructor(): this(InputComponent.InputData())

    override fun invoke(gameObject: GameObject) {
        action.update(gameObject)
    }
}