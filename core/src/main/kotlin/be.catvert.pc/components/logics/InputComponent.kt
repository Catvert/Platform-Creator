package be.catvert.pc.components.logics

import be.catvert.pc.GameObject
import be.catvert.pc.actions.Action
import be.catvert.pc.actions.EmptyAction
import be.catvert.pc.components.UpdeatableComponent
import be.catvert.pc.scenes.EditorScene
import be.catvert.pc.utility.CustomEditorImpl
import be.catvert.pc.utility.CustomType
import be.catvert.pc.utility.ExposeEditorFactory
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.fasterxml.jackson.annotation.JsonCreator
import imgui.ImGui


/**
 * Component permettant d'effectuer une action quand l'utilisateur appuie sur une touche
 */
class InputComponent(var inputsData: Array<InputData>) : UpdeatableComponent(), CustomEditorImpl {
    @JsonCreator private constructor(): this(arrayOf())

    data class InputData(var key: Int = Input.Keys.UNKNOWN, var justPressed: Boolean = false, var action: Action = EmptyAction()): CustomEditorImpl {
        override fun insertImgui(gameObject: GameObject, labelName: String, editorScene: EditorScene) {
            with(ImGui) {
                if (treeNode(labelName)) {
                    editorScene.addImguiWidget(gameObject, "Touche", { key }, { key = it }, ExposeEditorFactory.createExposeEditor(customType = CustomType.KEY_INT))
                    editorScene.addImguiWidget(gameObject, "Pressé", { justPressed }, { justPressed = it }, ExposeEditorFactory.createExposeEditor())
                    editorScene.addImguiWidget(gameObject, "Action", { action }, { action = it }, ExposeEditorFactory.createExposeEditor())

                    treePop()
                }
            }
        }
    }

    override fun update(gameObject: GameObject) {
        inputsData.forEach {
            if (it.justPressed) {
                if (Gdx.input.isKeyJustPressed(it.key))
                    it.action(gameObject)
            } else {
                if (Gdx.input.isKeyPressed(it.key))
                    it.action(gameObject)
            }
        }
    }

    override fun insertImgui(gameObject: GameObject, labelName: String, editorScene: EditorScene) {
        editorScene.addImguiWidgetsArray(gameObject, "inputs", { inputsData }, { inputsData = it }, { InputData() }, { Input.Keys.toString(inputsData[it].key) }, { ExposeEditorFactory.createExposeEditor() })
    }

}