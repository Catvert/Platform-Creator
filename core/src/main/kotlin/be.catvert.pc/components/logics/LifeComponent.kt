package be.catvert.pc.components.logics

import be.catvert.pc.GameObject
import be.catvert.pc.GameObjectState
import be.catvert.pc.actions.Action
import be.catvert.pc.actions.EmptyAction
import be.catvert.pc.actions.LifeAction
import be.catvert.pc.actions.RemoveGOAction
import be.catvert.pc.components.Component
import be.catvert.pc.containers.GameObjectContainer
import be.catvert.pc.containers.Level
import be.catvert.pc.scenes.EditorScene
import be.catvert.pc.utility.CustomEditorImpl
import be.catvert.pc.utility.CustomEditorTextImpl
import be.catvert.pc.utility.Description
import be.catvert.pc.utility.ImGuiHelper
import com.badlogic.gdx.graphics.Color
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import glm_.func.common.max

/**
 * Component permettant d'ajouter des points de vie à une entité
 * Chaque point de vie à une action quand celui-ci devient actif et inactif
 */
@Description("Ajoute la possibilité d'ajouter des points de vie à une entité")
class LifeComponent(onDeathAction: Action, lifePointActions: ArrayList<Action> = arrayListOf()) : Component(), CustomEditorImpl, CustomEditorTextImpl {
    @JsonCreator private constructor() : this(RemoveGOAction(), arrayListOf())

    @JsonProperty("lpActions")
    private var lpActions = arrayListOf(onDeathAction, *lifePointActions.toTypedArray())

    @JsonProperty("lifePoint")
    private var lifePoint: Int = this.lpActions.size

    /**
     * Permet de retirer un point de vie à un gameObject
     */
    fun lifeAction(action: LifeAction.LifeActions) {
        if (!active)
            return

        when (action) {
            LifeAction.LifeActions.ADD_LP -> {
                if (lpActions.size > lifePoint) {
                    ++lifePoint
                }
            }
            LifeAction.LifeActions.REMOVE_LP -> {
                if (lifePoint >= 1) {
                    lpActions.elementAt(lifePoint - 1).invoke(gameObject)
                    lifePoint = (lifePoint - 1).max(1)
                }
            }
            LifeAction.LifeActions.ONE_SHOT -> {
                lpActions.elementAt(0).invoke(gameObject)
                lifePoint = 1
            }
        }
    }

    override fun onStateActive(gameObject: GameObject, state: GameObjectState, container: GameObjectContainer) {
        super.onStateActive(gameObject, state, container)

        this.gameObject = gameObject
    }

    override fun insertImgui(label: String, gameObject: GameObject, level: Level, editorSceneUI: EditorScene.EditorSceneUI) {
        ImGuiHelper.addImguiWidgetsArray("life points actions", lpActions, { "vie ${lpActions.indexOf(it) + 1}" }, { EmptyAction() }, gameObject, level, editorSceneUI)
    }

    override fun insertText() {
        ImGuiHelper.textPropertyColored(Color.ORANGE, "point de vie actuel :", lifePoint.toString())

        lpActions.forEachIndexed { index, it ->
            ImGuiHelper.textColored(Color.RED, "<-->")
            ImGuiHelper.textPropertyColored(Color.ORANGE, "point de vie :", index + 1)
            ImGuiHelper.textPropertyColored(Color.ORANGE, "action :", it)
            ImGuiHelper.textColored(Color.RED, "<-->")
        }
    }
}