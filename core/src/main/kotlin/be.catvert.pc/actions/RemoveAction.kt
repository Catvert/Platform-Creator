package be.catvert.pc.actions

import be.catvert.pc.GameObject
import be.catvert.pc.GameObjectState

/**
 * Action permettant de supprimer le gameObject
 */
class RemoveAction : Action {
    override fun perform(gameObject: GameObject) {
        gameObject.removeFromParent()
    }
}