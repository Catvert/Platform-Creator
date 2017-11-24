package be.catvert.pc.actions

import be.catvert.pc.GameObject
import be.catvert.pc.scenes.EditorScene
import be.catvert.pc.utility.CustomEditorImpl
import be.catvert.pc.utility.ExposeEditorFactory
import be.catvert.pc.utility.ReflectionUtility
import com.fasterxml.jackson.annotation.JsonCreator

/**
 * Action permettant d'utiliser d'appliquer plusieurs actions sur un gameObject à la place d'une seule.
 */
class MultiplexerAction(var actions: Array<Action>) : Action, CustomEditorImpl {
    @JsonCreator private constructor() : this(arrayOf())

    override fun invoke(gameObject: GameObject) {
        actions.forEach {
            it(gameObject)
        }
    }

    override fun insertImgui(gameObject: GameObject, labelName: String, editorScene: EditorScene) {
        editorScene.addImguiWidgetsArray(gameObject, "actions", { actions }, { actions = it }, { EmptyAction() }, { ReflectionUtility.simpleNameOf(actions[it]) }, { ExposeEditorFactory.empty })
    }
}