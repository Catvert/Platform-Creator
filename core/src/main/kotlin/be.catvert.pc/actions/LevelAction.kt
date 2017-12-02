package be.catvert.pc.actions

import be.catvert.pc.GameObject
import be.catvert.pc.containers.Level
import be.catvert.pc.utility.ExposeEditor
import com.fasterxml.jackson.annotation.JsonCreator

/**
 * Une action a appliqué sur le niveau en lui-même
 * Permet par exemple de terminer le niveau selon si le joueur est mort ou a réussi
 * @see Level
 */
class LevelAction(@ExposeEditor var action: LevelActions) : Action {
    @JsonCreator private constructor() : this(LevelActions.FAIL_EXIT)

    enum class LevelActions {
        SUCCESS_EXIT, FAIL_EXIT
    }

    override fun invoke(gameObject: GameObject) {
        (gameObject.container as? Level)?.apply {
            when (action) {
                LevelAction.LevelActions.SUCCESS_EXIT -> exit(true)
                LevelAction.LevelActions.FAIL_EXIT -> exit(false)
            }
        }
    }
}