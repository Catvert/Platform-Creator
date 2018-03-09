package be.catvert.pc.eca.actions


import be.catvert.pc.eca.Entity
import be.catvert.pc.ui.Description
import be.catvert.pc.ui.UI
import be.catvert.pc.utility.Constants
import be.catvert.pc.utility.Size
import com.fasterxml.jackson.annotation.JsonCreator

/**
 * Action permettant de redimensionner une entité
 */
@Description("Permet de redimensionner une entité")
class ResizeAction(@UI(min = 1f, max = Constants.maxEntitySize.toFloat()) var newSize: Size) : Action() {
    @JsonCreator private constructor() : this(Size(1, 1))

    override fun invoke(entity: Entity) {
        entity.box.size = newSize
    }

    override fun toString() = super.toString() + " - $newSize"
}