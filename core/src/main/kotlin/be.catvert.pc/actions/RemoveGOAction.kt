package be.catvert.pc.actions

import be.catvert.pc.GameObject

class RemoveGOAction : Action {
    override fun invoke(gameObject: GameObject) {
        gameObject.removeFromParent()
    }
}