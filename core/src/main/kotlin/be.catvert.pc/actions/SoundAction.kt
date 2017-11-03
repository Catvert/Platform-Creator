package be.catvert.pc.actions

import be.catvert.pc.GameObject
import be.catvert.pc.GameObjectState
import be.catvert.pc.components.SoundComponent
import be.catvert.pc.utility.ExposeEditor
import com.fasterxml.jackson.annotation.JsonCreator


class SoundAction(@ExposeEditor var soundIndex: Int) : Action {
    @JsonCreator private constructor(): this(0)

    override fun perform(gameObject: GameObject) {
        gameObject.getCurrentState().getComponent<SoundComponent>(soundIndex)?.playSound()
    }
}