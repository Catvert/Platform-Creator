package be.catvert.pc.eca.components.logics

import be.catvert.pc.eca.Entity
import be.catvert.pc.eca.EntityState
import be.catvert.pc.eca.actions.Action
import be.catvert.pc.eca.actions.EmptyAction
import be.catvert.pc.eca.actions.LifeAction
import be.catvert.pc.eca.actions.RemoveGOAction
import be.catvert.pc.eca.components.Component
import be.catvert.pc.eca.containers.EntityContainer
import be.catvert.pc.eca.containers.Level
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
     * Permet de retirer un point de vie à un entity
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
                    lpActions.elementAt(lifePoint - 1).invoke(entity)
                    lifePoint = (lifePoint - 1).max(1)
                }
            }
            LifeAction.LifeActions.ONE_SHOT -> {
                lpActions.elementAt(0).invoke(entity)
                lifePoint = 1
            }
        }
    }

    override fun onStateActive(entity: Entity, state: EntityState, container: EntityContainer) {
        super.onStateActive(entity, state, container)

        this.entity = entity
    }

    override fun insertImgui(label: String, entity: Entity, level: Level, editorSceneUI: EditorScene.EditorSceneUI) {
        ImGuiHelper.addImguiWidgetsArray("life points actions", lpActions, { "vie ${lpActions.indexOf(it) + 1}" }, { EmptyAction() }, entity, level, editorSceneUI)
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