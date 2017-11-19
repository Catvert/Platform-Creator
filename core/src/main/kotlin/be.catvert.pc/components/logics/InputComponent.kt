package be.catvert.pc.components.logics

import be.catvert.pc.GameObject
import be.catvert.pc.actions.Action
import be.catvert.pc.actions.EmptyAction
import be.catvert.pc.components.UpdeatableComponent
import be.catvert.pc.scenes.EditorScene
import be.catvert.pc.serialization.PostDeserialization
import be.catvert.pc.utility.*
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.fasterxml.jackson.annotation.JsonCreator
import com.kotcrab.vis.ui.widget.VisTable
import com.kotcrab.vis.ui.widget.VisTextButton
import imgui.ImGui
import ktx.actors.onClick
import kotlin.reflect.full.findAnnotation


/**
 * Component permettant d'effectuer une action quand l'utilisateur appuie sur une touche
 * @param key La touche déclencheur
 * @param justPressed Est-ce que la touche doit-être pressé qu'une seule fois ?
 * @param action L'action à utiliser
 */
class InputComponent(var inputsData: Array<InputData>) : UpdeatableComponent(), CustomEditorImpl {
    @JsonCreator private constructor(): this(arrayOf())

    data class InputData(var key: Int = Input.Keys.UNKNOWN, var justPressed: Boolean = false, var action: Action = EmptyAction()): CustomEditorImpl {
        override fun insertImgui(gameObject: GameObject, editorScene: EditorScene) {
            with(ImGui) {
                editorScene.addImguiWidget(gameObject, "Touche", { key }, { key = it as Int }, ExposeEditorFactory.createExposeEditor(customType = CustomType.KEY_INT))
                editorScene.addImguiWidget(gameObject, "Pressé", { justPressed }, { justPressed = it as Boolean }, ExposeEditorFactory.createExposeEditor())
                editorScene.addImguiWidget(gameObject, "Action", { action }, { action = it as Action }, ExposeEditorFactory.createExposeEditor())
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

   // override fun insertChangeProperties(table: VisTable, gameObject: GameObject, editorScene: EditorScene) {
       // editorScene.addWidgetArray(table, gameObject, { "" }, { ExposeEditorFactory.createExposeEditor() }, { InputData() }, { inputsData }, { inputsData = it })
   // }

    override fun insertImgui(gameObject: GameObject, editorScene: EditorScene) {

    }

}