package be.catvert.pc.actions

import be.catvert.pc.GameObject
import be.catvert.pc.components.SoundComponent


class SoundAction(val soundIndex: Int) : Action {
    override fun perform(gameObject: GameObject) {
        gameObject.getComponent<SoundComponent>(soundIndex)?.playSound()
    }
}