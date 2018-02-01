package be.catvert.pc.actions

import be.catvert.pc.GameObject
import com.sun.org.glassfish.gmbal.Description

/**
 * Action permettant de supprimer un gameObject
 */
@Description("Permet de supprimer un game object")
class RemoveGOAction : Action() {
    override fun invoke(gameObject: GameObject) {
        gameObject.removeFromParent()
    }
}