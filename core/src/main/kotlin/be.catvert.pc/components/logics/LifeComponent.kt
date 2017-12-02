package be.catvert.pc.components.logics

import be.catvert.pc.GameObject
import be.catvert.pc.actions.Action
import be.catvert.pc.actions.EmptyAction
import be.catvert.pc.actions.RemoveGOAction
import be.catvert.pc.components.BasicComponent
import be.catvert.pc.containers.GameObjectContainer
import be.catvert.pc.containers.Level
import be.catvert.pc.utility.CustomEditorImpl
import be.catvert.pc.utility.ExposeEditor
import be.catvert.pc.utility.ExposeEditorFactory
import be.catvert.pc.utility.ImguiHelper
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import imgui.ImGui

class LifeComponent(onDeathAction: Action, vararg lifePointActions: LifePointActions) : BasicComponent(), CustomEditorImpl {
    @JsonCreator private constructor() : this(RemoveGOAction())

    data class LifePointActions(@ExposeEditor var onStartAction: Action, @ExposeEditor var onEndAction: Action): CustomEditorImpl  {
       override fun insertImgui(labelName: String, gameObject: GameObject, level: Level) {
            with(ImGui) {
                if (treeNode(labelName)) {
                    ImguiHelper.addImguiWidget("start", ::onStartAction, gameObject, level, ExposeEditorFactory.empty)
                    ImguiHelper.addImguiWidget("end", ::onEndAction, gameObject, level, ExposeEditorFactory.empty)
                    treePop()
                }
            }
        }
    }

    private lateinit var gameObject: GameObject

    @JsonProperty("lpActions")
    private var lpActions = arrayOf(LifePointActions(EmptyAction(), onDeathAction)) + lifePointActions

    @JsonProperty("lifePoint")
    private var lifePoint: Int = this.lpActions.size

    fun removeLifePoint() {
        if (lifePoint > 1) {
            lpActions.elementAt(lifePoint - 1).onEndAction(gameObject)
            --lifePoint
            lpActions.elementAt(lifePoint - 1).onStartAction(gameObject)
        }
        else if(lifePoint != -1) {
            lpActions.elementAt(0).onEndAction(gameObject)
            lifePoint = -1
        }
    }

    fun kill() {
        lpActions.elementAt(0).onEndAction(gameObject)
        lifePoint = -1
    }

    fun addLifePoint() {
        if (lpActions.size > lifePoint) {
            ++lifePoint
            lpActions.elementAt(lifePoint - 1).onStartAction(gameObject)
        }
    }

    override fun onAddToContainer(gameObject: GameObject, container: GameObjectContainer) {
        super.onAddToContainer(gameObject, container)

        this.gameObject = gameObject

        lpActions.elementAt(lifePoint - 1).onStartAction(gameObject)
    }

    override fun insertImgui(labelName: String, gameObject: GameObject, level: Level) {
        var index = 0
        ImguiHelper.addImguiWidgetsArray("life actions", this::lpActions,  { LifePointActions(EmptyAction(), EmptyAction()) }, {
            it.obj.insertImgui("vie ${++index}", gameObject, level)
            false
        })
    }

}