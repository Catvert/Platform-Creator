package be.catvert.pc.eca.actions

import be.catvert.pc.eca.Entity
import be.catvert.pc.eca.containers.Level
import be.catvert.pc.utility.Description
import be.catvert.pc.utility.ExposeEditor
import be.catvert.pc.utility.cast
import com.fasterxml.jackson.annotation.JsonCreator

/**
 * Action permettant de modifier le niveau de zoom
 * @see Level
 */
@Description("Permet d'effectuer un zoom")
class ZoomAction(@ExposeEditor(max = 2f) var zoom: Float) : Action() {
    @JsonCreator private constructor() : this(1f)

    override fun invoke(entity: Entity) {
        entity.container.cast<Level>()?.zoom = zoom
    }

    override fun toString() = super.toString() + " - $zoom"
}