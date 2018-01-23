package be.catvert.pc.actions

import be.catvert.pc.GameObject

/**
 * Une action qui ne fait rien
 */
class EmptyAction : Action() {
    override fun invoke(gameObject: GameObject) {}
}