package be.catvert.pc.eca.actions

import be.catvert.pc.eca.Entity
import be.catvert.pc.eca.containers.EntityContainer
import be.catvert.pc.eca.containers.Level
import be.catvert.pc.ui.Description
import be.catvert.pc.ui.UI
import be.catvert.pc.utility.cast
import com.fasterxml.jackson.annotation.JsonCreator

/**
 * Action permettant de modifier le niveau de zoom
 * @see Level
 */
@Description("Permet d'effectuer un zoom")
class ZoomAction(@UI(max = 2f) var zoom: Float) : Action() {
    @JsonCreator private constructor() : this(1f)

    override fun invoke(entity: Entity, container: EntityContainer) {
        container.cast<Level>()?.zoom = zoom
    }

    override fun toString() = super.toString() + " - $zoom"
}