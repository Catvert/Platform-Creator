package be.catvert.pc.actions

import be.catvert.pc.GameObject
import be.catvert.pc.containers.Level
import be.catvert.pc.scenes.EditorScene
import be.catvert.pc.utility.Constants
import be.catvert.pc.utility.CustomEditorImpl
import be.catvert.pc.utility.Description
import be.catvert.pc.utility.ImGuiHelper
import com.fasterxml.jackson.annotation.JsonCreator
import imgui.ImGui
import imgui.functionalProgramming

typealias StateSwitchAction = Pair<Int, Action>

/**
 * Permet d'effectuer une action sur un game object selon le stateIndex actuel du game object
 */
@Description("Permet d'effectuer une action sur un game object selon son état.")
class StateSwitcherAction(var stateSwitchActions: ArrayList<StateSwitchAction>) : Action(), CustomEditorImpl {
    constructor(vararg stateSwitchActions: StateSwitchAction) : this(arrayListOf(*stateSwitchActions))
    @JsonCreator private constructor() : this(arrayListOf())

    override fun invoke(gameObject: GameObject) {
        stateSwitchActions.filter { it.first == gameObject.getCurrentStateIndex() }.forEach {
            it.second(gameObject)
        }
    }

    override fun insertImgui(label: String, gameObject: GameObject, level: Level, editorSceneUI: EditorScene.EditorSceneUI) {
        ImGuiHelper.addImguiWidgetsArray("state actions", stateSwitchActions, { item: StateSwitchAction -> gameObject.getStateOrDefault(item.first).name }, { 0 to EmptyAction() }, {
            val stateIndex = intArrayOf(it.obj.first)
            functionalProgramming.withItemWidth(Constants.defaultWidgetsWidth) {
                ImGui.combo("state", stateIndex, gameObject.getStates().map { it.name })
            }

            val actionItem = ImGuiHelper.Item(it.obj.second)
            ImGuiHelper.action("", actionItem, gameObject, level, editorSceneUI)

            it.obj = stateIndex[0] to actionItem.obj
        })
    }
}