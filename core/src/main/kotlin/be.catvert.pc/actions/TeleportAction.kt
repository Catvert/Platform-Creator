package be.catvert.pc.actions

import be.catvert.pc.GameObject
import be.catvert.pc.utility.Point

/**
 * Action permettant de téléporter le gameObject à un point précis
 */
class TeleportAction(val teleportPoint: Point) : Action {
    override fun perform(gameObject: GameObject) {
        gameObject.rectangle.position = teleportPoint
    }
}