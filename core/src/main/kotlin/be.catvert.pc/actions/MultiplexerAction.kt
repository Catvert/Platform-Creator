package be.catvert.pc.actions

import be.catvert.pc.GameObject
import be.catvert.pc.GameObjectState
import be.catvert.pc.utility.ExposeEditor
import com.fasterxml.jackson.annotation.JsonCreator

class MultiplexerAction(@ExposeEditor var actions: Array<Action>) : Action {
    @JsonCreator private constructor(): this(arrayOf())

    override fun perform(gameObject: GameObject) {
        actions.forEach {
            it.perform(gameObject)
        }
    }
}