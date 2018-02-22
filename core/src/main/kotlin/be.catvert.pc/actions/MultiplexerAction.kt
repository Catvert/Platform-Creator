package be.catvert.pc.actions

import be.catvert.pc.GameObject
import be.catvert.pc.containers.Level
import be.catvert.pc.scenes.EditorScene
import be.catvert.pc.utility.CustomEditorImpl
import be.catvert.pc.utility.CustomEditorTextImpl
import be.catvert.pc.utility.Description
import be.catvert.pc.utility.ImGuiHelper
import com.badlogic.gdx.graphics.Color
import com.fasterxml.jackson.annotation.JsonCreator
import imgui.ImGui
import imgui.functionalProgramming

/**
 * Action permettant d'appeler plusieurs actions à la place d'une seule.
 */
@Description("Permet d'effectuer plusieurs actions sur une entité")
class MultiplexerAction(var actions: ArrayList<Action>) : Action(), CustomEditorImpl, CustomEditorTextImpl {
    constructor(vararg actions: Action) : this(arrayListOf(*actions))
    @JsonCreator private constructor() : this(arrayListOf())

    override fun invoke(gameObject: GameObject) {
        actions.forEach {
            it(gameObject)
        }
    }

    override fun insertImgui(label: String, gameObject: GameObject, level: Level, editorSceneUI: EditorScene.EditorSceneUI) {
        ImGuiHelper.addImguiWidgetsArray(label, actions, { "action" }, { EmptyAction() }, gameObject, level, editorSceneUI)
    }

    override fun insertText() {
        ImGui.text(super.toString())
        actions.forEach {
            functionalProgramming.withIndent {
                ImGuiHelper.textColored(Color.RED, "<-->")
                ImGuiHelper.textPropertyColored(Color.ORANGE, "action :", it)
                ImGuiHelper.textColored(Color.RED, "<-->")
            }
        }
    }
}