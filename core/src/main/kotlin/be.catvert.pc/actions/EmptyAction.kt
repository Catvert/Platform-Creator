package be.catvert.pc.actions

import be.catvert.pc.GameObject
import be.catvert.pc.GameObjectState

/**
 * Une action qui ne fais rien
 */
class EmptyAction : Action {
    override fun invoke(gameObject: GameObject) {}
}