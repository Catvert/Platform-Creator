package be.catvert.pc.eca.actions

import be.catvert.pc.eca.Entity
import be.catvert.pc.eca.containers.EntityContainer
import be.catvert.pc.eca.containers.Level
import be.catvert.pc.scenes.EditorScene
import be.catvert.pc.ui.Description
import be.catvert.pc.ui.ImGuiHelper
import be.catvert.pc.ui.UIImpl
import be.catvert.pc.utility.Constants
import com.fasterxml.jackson.annotation.JsonCreator
import imgui.ImGui
import imgui.functionalProgramming

typealias StateSwitchAction = Pair<Int, Action>

/**
 * Permet d'effectuer une action sur une entité selon l'état actuel de l'entité
 */
@Description("Permet d'effectuer une action sur une entité selon son état.")
class StateSwitcherAction(var stateSwitchActions: ArrayList<StateSwitchAction>) : Action(), UIImpl {
    constructor(vararg stateSwitchActions: StateSwitchAction) : this(arrayListOf(*stateSwitchActions))
    @JsonCreator private constructor() : this(arrayListOf())

    override fun invoke(entity: Entity, container: EntityContainer) {
        stateSwitchActions.filter { it.first == entity.getCurrentStateIndex() }.forEach {
            it.second(entity, container)
        }
    }

    override fun insertUI(label: String, entity: Entity, level: Level, editorSceneUI: EditorScene.EditorSceneUI) {
        ImGuiHelper.addImguiWidgetsArray("états actions", stateSwitchActions, { item: StateSwitchAction -> entity.getStateOrDefault(item.first).name }, { 0 to EmptyAction() }, {
            val stateIndex = intArrayOf(it.obj.first)
            functionalProgramming.withItemWidth(Constants.defaultWidgetsWidth) {
                ImGui.combo("état", stateIndex, entity.getStates().map { it.name })
            }

            val actionItem = ImGuiHelper.Item(it.obj.second)
            ImGuiHelper.action("", actionItem, entity, level, editorSceneUI)

            it.obj = stateIndex[0] to actionItem.obj
        })
    }
}