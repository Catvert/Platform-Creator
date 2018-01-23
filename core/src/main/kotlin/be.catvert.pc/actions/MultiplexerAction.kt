package be.catvert.pc.actions

import be.catvert.pc.GameObject
import be.catvert.pc.containers.Level
import be.catvert.pc.scenes.EditorScene
import be.catvert.pc.utility.CustomEditorImpl
import be.catvert.pc.utility.CustomEditorTextImpl
import be.catvert.pc.utility.ImguiHelper
import com.badlogic.gdx.graphics.Color
import com.fasterxml.jackson.annotation.JsonCreator
import imgui.ImGui
import imgui.functionalProgramming

/**
 * Action permettant d'utiliser d'appliquer plusieurs actions sur un gameObject à la place d'une seule.
 */
class MultiplexerAction(var actions: ArrayList<Action>) : Action(), CustomEditorImpl, CustomEditorTextImpl {
    constructor(vararg actions: Action) : this(arrayListOf(*actions))
    @JsonCreator private constructor() : this(arrayListOf())

    override fun invoke(gameObject: GameObject) {
        actions.forEach {
            it(gameObject)
        }
    }

    override fun insertImgui(label: String, gameObject: GameObject, level: Level, editorSceneUI: EditorScene.EditorSceneUI) {
        ImguiHelper.addImguiWidgetsArray(label, actions, { "action" }, { EmptyAction() }, gameObject, level, editorSceneUI)
    }

    override fun insertText() {
        ImGui.text(super.toString())
        actions.forEach {
            functionalProgramming.withIndent {
                ImguiHelper.textColored(Color.RED, "<-->")
                ImguiHelper.textPropertyColored(Color.ORANGE, "action :", it)
                ImguiHelper.textColored(Color.RED, "<-->")
            }
        }
    }
}