package be.catvert.pc.actions

import be.catvert.pc.GameObject
import be.catvert.pc.GameObjectState

/**
 * Action permettant de supprimer le gameObject
 */
class RemoveStateAction : Action {
    override fun perform(gameObject: GameObject) {
        gameObject.getCurrentState().isRemoving = true
    }
}