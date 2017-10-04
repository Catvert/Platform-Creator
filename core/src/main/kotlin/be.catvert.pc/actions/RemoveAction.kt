package be.catvert.pc.actions

import be.catvert.pc.GameObject

class RemoveAction : Action {
    override fun perform(gameObject: GameObject) {
        gameObject.removeFromParent()
    }
}