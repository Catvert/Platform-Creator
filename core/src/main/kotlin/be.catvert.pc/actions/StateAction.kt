package be.catvert.pc.actions

import be.catvert.pc.GameObject
import be.catvert.pc.scenes.EditorScene
import be.catvert.pc.utility.CustomEditorImpl
import be.catvert.pc.utility.CustomType
import be.catvert.pc.utility.ExposeEditor
import com.fasterxml.jackson.annotation.JsonCreator
import com.kotcrab.vis.ui.widget.VisSelectBox
import com.kotcrab.vis.ui.widget.VisTable
import ktx.actors.onChange
import ktx.collections.toGdxArray

class StateAction(var stateIndex: Int) : Action, CustomEditorImpl {
    @JsonCreator private constructor(): this(0)

    override fun invoke(gameObject: GameObject) {
        gameObject.currentState = stateIndex
    }


    override fun insertChangeProperties(table: VisTable, gameObject: GameObject, editorScene: EditorScene) {
        table.add(VisSelectBox<String>().apply {
            items = gameObject.getStates().map { it.name }.toGdxArray()

            onChange {
                stateIndex = selectedIndex
            }
        })
        table.row()
    }

}