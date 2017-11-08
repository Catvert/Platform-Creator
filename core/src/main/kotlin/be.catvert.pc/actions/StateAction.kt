package be.catvert.pc.actions

import be.catvert.pc.GameObject
import be.catvert.pc.utility.CustomType
import be.catvert.pc.utility.ExposeEditor
import com.fasterxml.jackson.annotation.JsonCreator

class StateAction(@ExposeEditor(maxInt = 100) val stateIndex: Int) : Action {
    @JsonCreator private constructor(): this(0)

    override fun invoke(gameObject: GameObject) {
        gameObject.currentState = stateIndex
    }
}