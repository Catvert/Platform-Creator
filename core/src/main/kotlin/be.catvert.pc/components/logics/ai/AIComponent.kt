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
import be.catvert.pc.utility.ExposeEditorFactory
import be.catvert.pc.utility.ImguiHelper
import be.catvert.pc.utility.ImguiHelper.enum
import com.fasterxml.jackson.annotation.JsonCreator
import imgui.ImGui

class AIComponent(var actionApplyOnPlayer: Action, var collisionsCondPlayer: Array<CollisionSide>, var actionApplyOnGO: Action, var collisionsCondGameObject: Array<CollisionSide>) : BasicComponent(), CustomEditorImpl {
    @JsonCreator private constructor() : this(EmptyAction(), arrayOf(), EmptyAction(), arrayOf())

    private var physicsComponent: PhysicsComponent? = null

    override fun onAddToContainer(gameObject: GameObject, container: GameObjectContainer) {
        super.onAddToContainer(gameObject, container)

        physicsComponent = gameObject.getCurrentState().getComponent()
        if (physicsComponent != null) {
            physicsComponent!!.onCollisionWith.register {
                if (it.collideGameObject.tag == GameObject.Tag.Player) {
                    collisionsCondPlayer.forEach { side ->
                        if (side == it.side) {
                            actionApplyOnPlayer(it.collideGameObject)
                        }
                    }

                    collisionsCondGameObject.forEach { side ->
                        if (side == it.side) {
                            actionApplyOnGO(gameObject)
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
            ImguiHelper.addImguiWidgetsArray("joueur", this@AIComponent::collisionsCondPlayer, { CollisionSide.OnLeft }, {
                enum("side", it.cast())
            }) {
                ImguiHelper.addImguiWidget("action sur le joueur", this@AIComponent::actionApplyOnPlayer, gameObject, level, ExposeEditorFactory.empty)
            }
            ImguiHelper.addImguiWidgetsArray("gameObject", this@AIComponent::collisionsCondGameObject, { CollisionSide.OnLeft }, {
                enum("side", it.cast())
            }) {
                ImguiHelper.addImguiWidget("action sur le go", this@AIComponent::actionApplyOnGO, gameObject, level, ExposeEditorFactory.empty)
            }
        }

    }
}