package be.catvert.pc.components.logics

import be.catvert.pc.GameObject
import be.catvert.pc.actions.Action
import be.catvert.pc.actions.EmptyAction
import be.catvert.pc.components.LogicsComponent
import be.catvert.pc.containers.Level
import be.catvert.pc.utility.CustomEditorImpl
import be.catvert.pc.utility.CustomType
import be.catvert.pc.utility.ExposeEditorFactory
import be.catvert.pc.utility.ImguiHelper
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.fasterxml.jackson.annotation.JsonCreator
import imgui.ImGui


/**
 * Component permettant d'effectuer une action quand l'utilisateur appuie sur une touche
 */
class InputComponent(vararg inputs: InputData) : LogicsComponent(), CustomEditorImpl {
    var inputs = arrayOf(*inputs)

    data class InputData(var key: Int = Input.Keys.UNKNOWN, var justPressed: Boolean = false, var action: Action = EmptyAction()) : CustomEditorImpl {
        override fun insertImgui(labelName: String, gameObject: GameObject, level: Level) {
            with(ImGui) {
                if (treeNode(labelName)) {
                    ImguiHelper.addImguiWidget("", ::key, gameObject, level, ExposeEditorFactory.createExposeEditor(customType = CustomType.KEY_INT))
                    checkbox("Pressé", ::justPressed)
                    ImguiHelper.addImguiWidget("action", ::action, gameObject, level, ExposeEditorFactory.empty)

                    treePop()
                }
            }
        }
    }

    override fun update(gameObject: GameObject) {
        inputs.forEach {
            if (it.justPressed) {
                if (Gdx.input.isKeyJustPressed(it.key))
                    it.action(gameObject)
            } else {
                if (Gdx.input.isKeyPressed(it.key))
                    it.action(gameObject)
            }
        }
    }

    override fun insertImgui(labelName: String, gameObject: GameObject, level: Level) {
        ImguiHelper.addImguiWidgetsArray("inputs", this::inputs, { InputData() }, {
            it.obj.insertImgui(Input.Keys.toString(it.obj.key), gameObject, level)
            false
        })
    }

}