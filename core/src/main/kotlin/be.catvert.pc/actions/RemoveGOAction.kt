package be.catvert.pc.actions

import be.catvert.pc.GameObject

/**
 * Action permettant de supprimer un gameObject
 */
class RemoveGOAction : Action() {
    override fun invoke(gameObject: GameObject) {
        gameObject.removeFromParent()
    }
}