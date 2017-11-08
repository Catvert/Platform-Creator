package be.catvert.pc.actions

import be.catvert.pc.GameObject
import be.catvert.pc.components.logics.LifeComponent
import be.catvert.pc.utility.ExposeEditor

enum class LifeActions {
    ADD_LP, REMOVE_LP, ONE_SHOT
}

class LifeAction(@ExposeEditor var action: LifeActions) : Action {
    override fun invoke(gameObject: GameObject) {
        val lifeComponent = gameObject.getCurrentState().getComponent<LifeComponent>()
        when(action) {
            LifeActions.ADD_LP -> lifeComponent?.addLifePoint()
            LifeActions.REMOVE_LP -> lifeComponent?.removeLifePoint()
            LifeActions.ONE_SHOT -> lifeComponent?.kill()
        }
    }
}