package be.catvert.pc.actions

import be.catvert.pc.GameObject

class MultiplexerAction(val actions: List<Action>) : Action {
    override fun perform(gameObject: GameObject) {
        actions.forEach {
            it.perform(gameObject)
        }
    }
}