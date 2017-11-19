package be.catvert.pc.actions

import be.catvert.pc.GameObject
import be.catvert.pc.GameObjectState
import be.catvert.pc.scenes.EditorScene
import be.catvert.pc.utility.CustomEditorImpl
import be.catvert.pc.utility.ExposeEditor
import be.catvert.pc.utility.ExposeEditorFactory
import be.catvert.pc.utility.ReflectionUtility
import com.fasterxml.jackson.annotation.JsonCreator
import com.kotcrab.vis.ui.widget.VisList
import com.kotcrab.vis.ui.widget.VisScrollPane
import com.kotcrab.vis.ui.widget.VisTable
import ktx.actors.onClick
import ktx.collections.toGdxArray
import ktx.vis.verticalGroup

/**
 * Action permettant d'utiliser d'appliquer plusieurs actions sur un gameObject à la place d'une seule.
 */
class MultiplexerAction(var actions: Array<Action>) : Action, CustomEditorImpl {
    override fun insertImgui(gameObject: GameObject, editorScene: EditorScene) {

    }

    @JsonCreator private constructor(): this(arrayOf())

    override fun invoke(gameObject: GameObject) {
        actions.forEach {
            it(gameObject)
        }
    }

    //override fun insertChangeProperties(table: VisTable, gameObject: GameObject, editorScene: EditorScene) {
       // editorScene.addWidgetArray(table, gameObject, { ReflectionUtility.simpleNameOf(actions[it]) }, { ExposeEditorFactory.createExposeEditor() }, { EmptyAction() }, { actions }, { actions = it })
    //}
}