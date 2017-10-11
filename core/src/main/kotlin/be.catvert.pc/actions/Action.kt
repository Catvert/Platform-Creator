package be.catvert.pc.actions

import be.catvert.pc.GameObject

/**
 * Représente une action qu'un gameObject peut subir selon le component utilisé
 */
interface Action {
    fun perform(gameObject: GameObject)
}