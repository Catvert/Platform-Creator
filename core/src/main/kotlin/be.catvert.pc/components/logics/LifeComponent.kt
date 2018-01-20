package be.catvert.pc.components.logics

import be.catvert.pc.GameObject
import be.catvert.pc.GameObjectState
import be.catvert.pc.actions.Action
import be.catvert.pc.actions.EmptyAction
import be.catvert.pc.actions.RemoveGOAction
import be.catvert.pc.components.Component
import be.catvert.pc.containers.GameObjectContainer
import be.catvert.pc.containers.Level
import be.catvert.pc.scenes.EditorScene
import be.catvert.pc.utility.CustomEditorImpl
import be.catvert.pc.utility.CustomEditorTextImpl
import be.catvert.pc.utility.ImguiHelper
import com.badlogic.gdx.graphics.Color
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import glm_.func.common.max
import glm_.func.common.min

/**
 * Component permettant d'ajouter des points de vie à un gameObject
 * Chaque point de vie à une action quand celui-ci devient actif et inactif
 */
class LifeComponent(onDeathAction: Action, lifePointActions: ArrayList<Action> = arrayListOf()) : Component(), CustomEditorImpl, CustomEditorTextImpl {
    @JsonCreator private constructor() : this(RemoveGOAction(), arrayListOf())

    @JsonProperty("lpActions")
    private var lpActions = arrayListOf(onDeathAction, *lifePointActions.toTypedArray())

    @JsonProperty("lifePoint")
    private var lifePoint: Int = this.lpActions.size

    /**
     * Permet de retirer un point de vie à un gameObject
     */
    fun removeLifePoint() {
        if (lifePoint >= 1) {
            lpActions.elementAt(lifePoint - 1).invoke(gameObject)
            lifePoint = (lifePoint - 1).max(1)
        }
    }

    /**
     * Permet de supprimer tout les points de vie à un gameObject
     */
    fun kill() {
        lpActions.elementAt(0).invoke(gameObject)
        lifePoint = 1
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

    override fun insertImgui(label: String, gameObject: GameObject, level: Level, editorSceneUI: EditorScene.EditorSceneUI) {
        ImguiHelper.addImguiWidgetsArray("life actions", lpActions, { "vie ${lpActions.indexOf(it) + 1}" }, { EmptyAction() }, gameObject, level, editorSceneUI)
    }

    override fun insertText() {
        ImguiHelper.textPropertyColored(Color.ORANGE, "point de vie actuel :", lifePoint.toString())

        lpActions.forEachIndexed { index, it ->
            ImguiHelper.textColored(Color.RED, "<-->")
            ImguiHelper.textPropertyColored(Color.ORANGE, "point de vie :", index + 1)
            ImguiHelper.textPropertyColored(Color.ORANGE, "action :", it)
            ImguiHelper.textColored(Color.RED, "<-->")
        }
    }
}