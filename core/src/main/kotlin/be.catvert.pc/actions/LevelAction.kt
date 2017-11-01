package be.catvert.pc.actions

import be.catvert.pc.GameObject
import be.catvert.pc.Level
import be.catvert.pc.utility.ExposeEditor
import com.fasterxml.jackson.annotation.JsonCreator

class LevelAction(@ExposeEditor var action: LevelActions = LevelActions.FAIL_EXIT) : Action {
    @JsonCreator private constructor(): this(LevelActions.FAIL_EXIT)

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