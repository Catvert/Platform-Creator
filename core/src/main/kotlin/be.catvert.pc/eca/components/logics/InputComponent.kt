package be.catvert.pc.eca.components.logics

import be.catvert.pc.eca.Entity
import be.catvert.pc.eca.actions.Action
import be.catvert.pc.eca.actions.EmptyAction
import be.catvert.pc.eca.components.Component
import be.catvert.pc.eca.containers.Level
import be.catvert.pc.scenes.EditorScene
import be.catvert.pc.utility.*
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.fasterxml.jackson.annotation.JsonCreator
import imgui.ImGui
import imgui.functionalProgramming


/**
 * Component permettant d'effectuer une action quand l'utilisateur appuie sur une touche
 */
@Description("Ajoute la possibilité d'effectuer une action sur une entité quand une touche est pressée")
class InputComponent(var inputs: ArrayList<InputData>) : Component(), Updeatable, CustomEditorImpl, CustomEditorTextImpl {
    constructor(vararg inputs: InputData) : this(arrayListOf(*inputs))
    @JsonCreator private constructor() : this(arrayListOf())

    data class InputData(var key: Int = Input.Keys.UNKNOWN, var pressed: Boolean = true, @ExposeEditor var action: Action = EmptyAction()) : CustomEditorImpl {
        override fun insertImgui(label: String, entity: Entity, level: Level, editorSceneUI: EditorScene.EditorSceneUI) {
            with(ImGui) {
                checkbox("", ::pressed)
                if (ImGui.isItemHovered()) {
                    functionalProgramming.withTooltip {
                        text("Pressé en continu")
                    }
                }
                sameLine(0f, style.itemInnerSpacing.x)
                ImGuiHelper.gdxKey(::key)
            }
        }

        fun update(entity: Entity) {
            if (pressed) {
                if (Gdx.input.isKeyPressed(key))
                    action(entity)
            } else {
                if (Gdx.input.isKeyJustPressed(key))
                    action(entity)
            }
        }
    }

    override fun update() {
        inputs.forEach {
            it.update(entity)
        }
    }

    override fun insertImgui(label: String, entity: Entity, level: Level, editorSceneUI: EditorScene.EditorSceneUI) {
        ImGuiHelper.addImguiWidgetsArray("inputs", inputs, { item -> Input.Keys.toString(item.key) }, { InputData() }, entity, level, editorSceneUI)
    }

    override fun insertText() {
        inputs.forEach {
            ImGuiHelper.textColored(Color.RED, "<-->")
            ImGuiHelper.textPropertyColored(Color.ORANGE, "key :", Input.Keys.toString(it.key))
            ImGuiHelper.textPropertyColored(Color.ORANGE, "action :", it.action)
            ImGuiHelper.textPropertyColored(Color.ORANGE, "pressed :", it.pressed)
            ImGui.sameLine()
            ImGuiHelper.textColored(Color.ORANGE, if (it.pressed) Gdx.input.isKeyJustPressed(it.key) else Gdx.input.isKeyPressed(it.key))
            ImGuiHelper.textColored(Color.RED, "<-->")
        }
    }
}