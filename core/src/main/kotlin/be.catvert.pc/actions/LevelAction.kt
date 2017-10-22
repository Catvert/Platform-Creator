package be.catvert.pc.actions

import be.catvert.pc.GameObject
import be.catvert.pc.Level

class LevelAction(val action: LevelActions) : Action {
    enum class LevelActions {
        SUCCESS_EXIT, FAIL_EXIT
    }

    override fun perform(gameObject: GameObject) {
        if(gameObject.container != null && gameObject.container is Level) {
            val level = gameObject.container as Level
            when(action) {
                LevelAction.LevelActions.SUCCESS_EXIT -> level.exitLevel(true)
                LevelAction.LevelActions.FAIL_EXIT -> level.exitLevel(false)
            }
        }
    }
}