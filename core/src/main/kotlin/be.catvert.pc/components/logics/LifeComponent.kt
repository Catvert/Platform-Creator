package be.catvert.pc.components.logics

import be.catvert.pc.GameObject
import be.catvert.pc.GameObjectState
import be.catvert.pc.actions.Action
import be.catvert.pc.actions.EmptyAction
import be.catvert.pc.actions.RemoveGOAction
import be.catvert.pc.components.BasicComponent
import be.catvert.pc.containers.GameObjectContainer
import be.catvert.pc.scenes.EditorScene
import be.catvert.pc.utility.CustomEditorImpl
import be.catvert.pc.utility.ExposeEditorFactory
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import imgui.ImGui

class LifeComponent(onDeathAction: Action, vararg lifePointActions: LifePointActions) : BasicComponent(), CustomEditorImpl {
    @JsonCreator private constructor(): this(RemoveGOAction())

    data class LifePointActions(var onStartAction: Action, var onEndAction: Action) : CustomEditorImpl {
        override fun insertImgui(gameObject: GameObject, labelName: String, editorScene: EditorScene) {
            with(ImGui) {
                if(treeNode(labelName)) {
                    editorScene.addImguiWidget(gameObject, "start", { onStartAction }, { onStartAction = it }, ExposeEditorFactory.empty)
                    editorScene.addImguiWidget(gameObject, "end", { onEndAction }, { onEndAction = it }, ExposeEditorFactory.empty)
                    treePop()
                }
            }
        }
    }

    @JsonIgnore
    private lateinit var gameObject: GameObject

    @JsonProperty("lpActions")
    private var lpActions = lifePointActions.toMutableSet().apply { add(LifePointActions(EmptyAction(), onDeathAction)) }

    @JsonProperty("lifePoint")
    private var lifePoint: Int = this.lpActions.size

    fun removeLifePoint() {
        if(lifePoint > 1)
            --lifePoint
        lpActions.elementAt(lifePoint - 1).onEndAction(gameObject)
    }

    fun kill() {
        lifePoint = 1
        lpActions.elementAt(0).onEndAction(gameObject)
    }

    fun addLifePoint() {
        if(lpActions.size > lifePoint) {
            ++lifePoint
            lpActions.elementAt(lifePoint - 1).onStartAction(gameObject)
        }
    }

    override fun onAddToContainer(gameObject: GameObject, container: GameObjectContainer) {
        super.onAddToContainer(gameObject, container)

        this.gameObject = gameObject

        lpActions.elementAt(lifePoint - 1).onStartAction(gameObject)
    }

    override fun insertImgui(gameObject: GameObject, labelName: String, editorScene: EditorScene) {
        with(ImGui) {
            editorScene.addImguiWidgetsArray(gameObject, "life actions", { lpActions.toTypedArray() }, { lpActions = it.toMutableSet() }, { LifePointActions(EmptyAction(), EmptyAction()) }, { "Point de vie : ${it + 1}" }, { ExposeEditorFactory.empty })
        }
    }

}