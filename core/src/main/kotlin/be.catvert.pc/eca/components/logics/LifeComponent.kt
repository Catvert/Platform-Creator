package be.catvert.pc.eca.components.logics

import be.catvert.pc.eca.Entity
import be.catvert.pc.eca.EntityState
import be.catvert.pc.eca.actions.Action
import be.catvert.pc.eca.actions.EmptyAction
import be.catvert.pc.eca.actions.LifeAction
import be.catvert.pc.eca.actions.RemoveEntityAction
import be.catvert.pc.eca.components.Component
import be.catvert.pc.eca.containers.EntityContainer
import be.catvert.pc.eca.containers.Level
import be.catvert.pc.scenes.EditorScene
import be.catvert.pc.ui.Description
import be.catvert.pc.ui.ImGuiHelper
import be.catvert.pc.ui.UIImpl
import be.catvert.pc.ui.UITextImpl
import com.badlogic.gdx.graphics.Color
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import glm_.func.common.max

/**
 * Component permettant d'ajouter des points de vie à une entité
 * Chaque point de vie à une action quand celui-ci devient actif et inactif
 */
@Description("Ajoute la possibilité d'ajouter des points de vie à une entité")
class LifeComponent(onDeathAction: Action, lifePointActions: ArrayList<Action> = arrayListOf()) : Component(), UIImpl, UITextImpl {
    @JsonCreator private constructor() : this(RemoveEntityAction(), arrayListOf())

    @JsonProperty("lpActions")
    private var lpActions = arrayListOf(onDeathAction, *lifePointActions.toTypedArray())

    @JsonProperty("lifePoint")
    private var lifePoint: Int = this.lpActions.size

    /**
     * Permet de retirer un point de vie à un entity
     */
    fun lifeAction(action: LifeAction.LifeActions) {
        if (!active || entity.container == null)
            return

        when (action) {
            LifeAction.LifeActions.ADD_LP -> {
                if (lpActions.size > lifePoint) {
                    ++lifePoint
                }
            }
            LifeAction.LifeActions.REMOVE_LP -> {
                if (lifePoint >= 1) {
                    lpActions.elementAt(lifePoint - 1).invoke(entity, entity.container!!)
                    lifePoint = (lifePoint - 1).max(1)
                }
            }
            LifeAction.LifeActions.ONE_SHOT -> {
                lpActions.elementAt(0).invoke(entity, entity.container!!)
                lifePoint = 1
            }
        }
    }

    override fun onStateActive(entity: Entity, state: EntityState, container: EntityContainer) {
        super.onStateActive(entity, state, container)

        this.entity = entity
    }

    override fun insertUI(label: String, entity: Entity, level: Level, editorUI: EditorScene.EditorUI) {
        ImGuiHelper.addImguiWidgetsArray("life points actions", lpActions, { "vie ${lpActions.indexOf(it) + 1}" }, { EmptyAction() }, entity, level, editorUI)
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