package be.catvert.pc.actions

import aurelienribon.tweenengine.Tween
import be.catvert.pc.GameObject
import be.catvert.pc.GameObjectTweenAccessor
import be.catvert.pc.PCGame

/**
 * Action permettant de supprimer un gameObject
 */
class RemoveGOAction : Action {
    override fun invoke(gameObject: GameObject) {
        gameObject.removeFromParent()
    }
}