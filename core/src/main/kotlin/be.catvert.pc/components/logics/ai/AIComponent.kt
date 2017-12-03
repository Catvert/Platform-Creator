package be.catvert.pc.components.logics.ai

import be.catvert.pc.GameObject
import be.catvert.pc.Log
import be.catvert.pc.actions.Action
import be.catvert.pc.actions.EmptyAction
import be.catvert.pc.components.BasicComponent
import be.catvert.pc.components.logics.CollisionSide
import be.catvert.pc.components.logics.PhysicsComponent
import be.catvert.pc.containers.GameObjectContainer
import be.catvert.pc.containers.Level
import be.catvert.pc.utility.CustomEditorImpl
import be.catvert.pc.utility.ImguiHelper
import be.catvert.pc.utility.ImguiHelper.enum
import com.fasterxml.jackson.annotation.JsonCreator
import imgui.ImGui

class AIComponent(var target: GameObject.Tag, var actionTarget: Action, var actionCondTarget: ArrayList<CollisionSide>, var actionOnThis: Action, var actionCondThis: ArrayList<CollisionSide>) : BasicComponent(), CustomEditorImpl {
    @JsonCreator private constructor() : this(GameObject.Tag.Player, EmptyAction(), arrayListOf(), EmptyAction(), arrayListOf())

    private var physicsComponent: PhysicsComponent? = null

    override fun onAddToContainer(gameObject: GameObject, container: GameObjectContainer) {
        super.onAddToContainer(gameObject, container)

        physicsComponent = gameObject.getCurrentState().getComponent()
        if (physicsComponent != null) {
            physicsComponent!!.onCollisionWith.register {
                if (it.collideGameObject.tag == target) {
                    actionCondTarget.forEach { side ->
                        if (side == it.side) {
                            actionTarget(it.collideGameObject)
                        }
                    }

                    actionCondThis.forEach { side ->
                        if (side == it.side) {
                            actionOnThis(gameObject)
                        }
                    }
                }
            }
        } else {
            Log.error { "Impossible de d'utiliser un AIComponent si l'objet n'a pas de PhysicsComponent" }
        }
    }

    override fun insertImgui(labelName: String, gameObject: GameObject, level: Level) {
        with(ImGui) {
            val targetItem: ImguiHelper.Item<Enum<*>> = ImguiHelper.Item(target)
            if (ImguiHelper.enum("cible", targetItem))
                target = targetItem.obj as GameObject.Tag
            ImguiHelper.addImguiWidgetsArray("conditions cible", actionCondTarget, { CollisionSide.OnLeft }, {
                enum("side", it.cast())
            }) {
                ImguiHelper.action("action cible ", ::actionTarget, gameObject, level)
            }
            ImguiHelper.addImguiWidgetsArray("conditions gameObject", actionCondThis, { CollisionSide.OnLeft }, {
                enum("side", it.cast())
            }) {
                ImguiHelper.action("action go", ::actionOnThis, gameObject, level)
            }
        }
    }
}