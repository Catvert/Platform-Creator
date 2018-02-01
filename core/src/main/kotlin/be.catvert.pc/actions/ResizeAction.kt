package be.catvert.pc.actions

import be.catvert.pc.GameObject
import be.catvert.pc.utility.Constants
import be.catvert.pc.utility.Description
import be.catvert.pc.utility.ExposeEditor
import be.catvert.pc.utility.Size
import com.fasterxml.jackson.annotation.JsonCreator

/**
 * Action permettant de redimensionner un gameObject
 */
@Description("Permet de redimensionner un game object")
class ResizeAction(@ExposeEditor(min = 1f, max = Constants.maxGameObjectSize.toFloat()) var newSize: Size) : Action() {
    @JsonCreator private constructor() : this(Size(1, 1))

    override fun invoke(gameObject: GameObject) {
        gameObject.box.size = newSize
    }

    override fun toString() = super.toString() + " - $newSize"
}