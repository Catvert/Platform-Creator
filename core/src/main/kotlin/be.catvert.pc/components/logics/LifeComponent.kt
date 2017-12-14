package be.catvert.pc.components.logics

import be.catvert.pc.GameObject
import be.catvert.pc.GameObjectState
import be.catvert.pc.actions.Action
import be.catvert.pc.actions.EmptyAction
import be.catvert.pc.actions.RemoveGOAction
import be.catvert.pc.components.BasicComponent
import be.catvert.pc.containers.GameObjectContainer
import be.catvert.pc.containers.Level
import be.catvert.pc.utility.CustomEditorImpl
import be.catvert.pc.utility.ExposeEditor
import be.catvert.pc.utility.ExposeEditorFactory
import be.catvert.pc.utility.ImguiHelper
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import imgui.ImGui

/**
 * Component permettant d'ajouter des points de vie à un gameObject
 * Chaque point de vie à une action quand celui-ci devient actif et inactif
 */
class LifeComponent(onDeathAction: Action, lifePointActions: ArrayList<Action> = arrayListOf()) : BasicComponent(), CustomEditorImpl {
    @JsonCreator private constructor() : this(RemoveGOAction(), arrayListOf())

    private lateinit var gameObject: GameObject

    @JsonProperty("lpActions")
    private var lpActions = arrayListOf(onDeathAction, *lifePointActions.toTypedArray())

    @JsonProperty("lifePoint")
    private var lifePoint: Int = this.lpActions.size

    /**
     * Permet de retirer un point de vie à un gameObject
     */
    fun removeLifePoint() {
        if (lifePoint > 1) {
            lpActions.elementAt(lifePoint - 1).invoke(gameObject)
            --lifePoint
        } else if (lifePoint != -1) {
            lpActions.elementAt(0).invoke(gameObject)
            lifePoint = -1
        }
    }

    /**
     * Permet de supprimer tout les points de vie à un gameObject
     */
    fun kill() {
        lpActions.elementAt(0).invoke(gameObject)
        lifePoint = -1
    }

    /**
     * Permet de rajouter un point de vie au gameObject
     */
    fun addLifePoint() {
        if (lpActions.size > lifePoint) {
            ++lifePoint
        }
    }

    override fun onStateActive(gameObject: GameObject, state: GameObjectState, container: GameObjectContainer) {
        super.onStateActive(gameObject, state, container)

        this.gameObject = gameObject
    }

    override fun insertImgui(label: String, gameObject: GameObject, level: Level) {
        ImguiHelper.addImguiWidgetsArray("life actions", lpActions, { "vie ${lpActions.indexOf(it) + 1}" }, { EmptyAction() }, gameObject, level)
    }

}