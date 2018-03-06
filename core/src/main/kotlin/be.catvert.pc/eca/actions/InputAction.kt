package be.catvert.pc.eca.actions


import be.catvert.pc.eca.Entity
import be.catvert.pc.eca.components.logics.InputComponent
import be.catvert.pc.utility.Description
import be.catvert.pc.utility.ExposeEditor
import com.fasterxml.jackson.annotation.JsonCreator

@Description("Permet d'effectuer une action si une touche est pressée")
class InputAction(@ExposeEditor var action: InputComponent.InputData) : Action() {
    @JsonCreator private constructor() : this(InputComponent.InputData())

    override fun invoke(entity: Entity) {
        action.update(entity)
    }
}