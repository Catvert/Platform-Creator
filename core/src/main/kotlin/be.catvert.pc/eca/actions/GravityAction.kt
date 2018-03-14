package be.catvert.pc.eca.actions

import be.catvert.pc.eca.Entity
import be.catvert.pc.eca.containers.EntityContainer
import be.catvert.pc.eca.containers.Level
import be.catvert.pc.ui.Description
import be.catvert.pc.ui.UI
import be.catvert.pc.utility.Constants
import be.catvert.pc.utility.cast
import com.fasterxml.jackson.annotation.JsonCreator

@Description("Permet de changer la gravité du niveau")
class GravityAction(@UI(min = 0f, max = 100f, customName = "gravité") var gravitySpeed: Int) : Action() {
    @JsonCreator private constructor() : this(Constants.defaultGravitySpeed)

    override fun invoke(entity: Entity, container: EntityContainer) {
        container.cast<Level>()?.gravitySpeed = gravitySpeed
    }

    override fun toString() = super.toString() + " - gravité : $gravitySpeed"
}