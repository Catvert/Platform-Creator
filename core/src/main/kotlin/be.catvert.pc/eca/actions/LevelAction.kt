package be.catvert.pc.eca.actions


import be.catvert.pc.eca.Entity
import be.catvert.pc.eca.containers.EntityContainer
import be.catvert.pc.eca.containers.Level
import be.catvert.pc.ui.Description
import be.catvert.pc.ui.UI
import be.catvert.pc.utility.cast
import com.fasterxml.jackson.annotation.JsonCreator

/**
 * Action permettant de terminer le niveau, selon si le joueur est mort ou a terminé le niveau
 * @see Level
 */
@Description("Permet de quitter le niveau en spécifiant si le joueur à réussi ou non")
class LevelAction(@UI var action: LevelActions) : Action() {
    @JsonCreator private constructor() : this(LevelActions.FAIL_EXIT)

    enum class LevelActions {
        SUCCESS_EXIT, FAIL_EXIT
    }

    override fun invoke(entity: Entity, container: EntityContainer) {
        container.cast<Level>()?.apply {
            when (action) {
                LevelActions.SUCCESS_EXIT -> exit(true)
                LevelActions.FAIL_EXIT -> exit(false)
            }
        }
    }

    override fun toString() = super.toString() + " - $action"
}