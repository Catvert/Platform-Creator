package be.catvert.pc.eca.actions


import be.catvert.pc.eca.Entity
import be.catvert.pc.eca.containers.EntityContainer
import be.catvert.pc.eca.containers.Level
import be.catvert.pc.scenes.EditorScene
import be.catvert.pc.ui.Description
import be.catvert.pc.ui.ImGuiHelper
import be.catvert.pc.ui.UIImpl
import be.catvert.pc.ui.UITextImpl
import com.badlogic.gdx.graphics.Color
import com.fasterxml.jackson.annotation.JsonCreator
import imgui.ImGui
import imgui.functionalProgramming

/**
 * Action permettant d'appeler plusieurs actions à la place d'une seule.
 */
@Description("Permet d'effectuer plusieurs actions sur une entité")
class MultiplexerAction(var actions: ArrayList<Action>) : Action(), UIImpl, UITextImpl {
    constructor(vararg actions: Action) : this(arrayListOf(*actions))
    @JsonCreator private constructor() : this(arrayListOf())

    override fun invoke(entity: Entity, container: EntityContainer) {
        actions.forEach {
            it(entity, container)
        }
    }

    override fun insertUI(label: String, entity: Entity, level: Level, editorSceneUI: EditorScene.EditorSceneUI) {
        ImGuiHelper.addImguiWidgetsArray(label, actions, { "action" }, { EmptyAction() }, entity, level, editorSceneUI)
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