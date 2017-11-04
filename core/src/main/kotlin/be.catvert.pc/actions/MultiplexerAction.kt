package be.catvert.pc.actions

import be.catvert.pc.GameObject
import be.catvert.pc.GameObjectState
import be.catvert.pc.scenes.EditorScene
import be.catvert.pc.utility.CustomEditorImpl
import be.catvert.pc.utility.ExposeEditor
import be.catvert.pc.utility.ReflectionUtility
import com.fasterxml.jackson.annotation.JsonCreator
import com.kotcrab.vis.ui.widget.VisList
import com.kotcrab.vis.ui.widget.VisScrollPane
import com.kotcrab.vis.ui.widget.VisTable
import ktx.actors.onClick
import ktx.collections.toGdxArray
import ktx.vis.verticalGroup

class MultiplexerAction(var actions: Array<Action>) : Action, CustomEditorImpl {
    @JsonCreator private constructor(): this(arrayOf())

    override fun perform(gameObject: GameObject) {
        actions.forEach {
            it.perform(gameObject)
        }
    }

    override fun insertChangeProperties(table: VisTable, editorScene: EditorScene) {
        data class ActionItem(val action: Action) {
            override fun toString(): String {
                return ReflectionUtility.simpleNameOf(action)
            }
        }

        val actionsList = VisList<ActionItem>()

        val scrollPane = VisScrollPane(actionsList)
        table.add(scrollPane).height(100f)

        fun updateActions() {
            actionsList.setItems((actions).map { ActionItem(it) }.toGdxArray())
        }
        updateActions()

        table.add(verticalGroup {
            space(10f)

            textButton("Modifier") {
                onClick {
                    if (actionsList.selected != null) {
                        editorScene.showEditActionWindow(actionsList.selected.action, {
                            this@MultiplexerAction.actions[actionsList.selectedIndex] = it
                            updateActions()
                        })
                    }
                }
            }
            textButton("Supprimer") {
                onClick {
                    if (actionsList.selected != null) {
                        this@MultiplexerAction.actions = this@MultiplexerAction.actions.toGdxArray().apply { this.removeIndex(actionsList.selectedIndex) }.toArray()
                        updateActions()
                    }
                }
            }
            textButton("Ajouter") {
                onClick {
                    editorScene.showEditActionWindow(null) { addAction ->
                        this@MultiplexerAction.actions = this@MultiplexerAction.actions.toGdxArray().apply { this.add(addAction) }.toArray()
                        updateActions()
                    }
                }
            }
        })
    }
}