package be.catvert.pc.actions

import be.catvert.pc.GameObject
import be.catvert.pc.GameObjectState
import be.catvert.pc.scenes.EditorScene
import be.catvert.pc.utility.CustomEditorImpl
import be.catvert.pc.utility.CustomType
import be.catvert.pc.utility.ExposeEditor
import com.fasterxml.jackson.annotation.JsonCreator
import com.kotcrab.vis.ui.widget.VisSelectBox
import com.kotcrab.vis.ui.widget.VisTable
import ktx.actors.onChange
import ktx.collections.toGdxArray

/**
 * Action permettant de changer l'état d'un gameObject
 */
class StateAction(var stateIndex: Int) : Action, CustomEditorImpl {
    @JsonCreator private constructor(): this(0)

    override fun invoke(gameObject: GameObject) {
        gameObject.currentState = stateIndex
    }


    override fun insertChangeProperties(table: VisTable, gameObject: GameObject, editorScene: EditorScene) {
        table.add(VisSelectBox<GameObjectState>().apply {
            items = gameObject.getStates().toGdxArray()

            if(stateIndex in gameObject.getStates().indices)
                selectedIndex = stateIndex

            onChange {
                stateIndex = selectedIndex
            }
        })
        table.row()
    }

}