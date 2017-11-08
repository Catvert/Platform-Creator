package be.catvert.pc.actions

import be.catvert.pc.GameObject
import be.catvert.pc.utility.Constants
import be.catvert.pc.utility.ExposeEditor
import be.catvert.pc.utility.Size
import com.fasterxml.jackson.annotation.JsonCreator

class ResizeAction(@ExposeEditor(minInt = 1, maxInt = Constants.maxGameObjectSize) var newSize: Size) : Action {
    @JsonCreator private constructor(): this(Size(1, 1))

    override fun invoke(gameObject: GameObject) {
        gameObject.rectangle.size = newSize
    }
}