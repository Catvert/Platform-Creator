package be.catvert.pc.actions

import be.catvert.pc.GameObject
import be.catvert.pc.GameObjectState

class EmptyAction : Action {
    override fun invoke(gameObject: GameObject) {}
}