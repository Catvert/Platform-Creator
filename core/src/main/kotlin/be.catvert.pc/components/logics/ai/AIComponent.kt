package be.catvert.pc.components.logics.ai

import be.catvert.pc.GameObject
import be.catvert.pc.GameObjectState
import be.catvert.pc.Log
import be.catvert.pc.actions.Action
import be.catvert.pc.actions.EmptyAction
import be.catvert.pc.components.BasicComponent
import be.catvert.pc.components.logics.CollisionSide
import be.catvert.pc.components.logics.PhysicsComponent
import be.catvert.pc.containers.GameObjectContainer
import be.catvert.pc.scenes.EditorScene
import be.catvert.pc.utility.CustomEditorImpl
import be.catvert.pc.utility.ExposeEditorFactory
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnore
import imgui.ImGui

class AIComponent(var actionOnCollisionWithPlayer: Pair<Action, Array<CollisionSide>>, var actionOnPlayerCollision: Pair<Action, Array<CollisionSide>>) : BasicComponent(), CustomEditorImpl {
    @JsonCreator private constructor(): this(EmptyAction() to arrayOf(), EmptyAction() to arrayOf())

    @JsonIgnore private var physicsComponent: PhysicsComponent? = null

    override fun onAddToContainer(gameObject: GameObject, container: GameObjectContainer) {
        super.onAddToContainer(gameObject, container)

        physicsComponent = gameObject.getCurrentState().getComponent()
        if(physicsComponent != null) {
            physicsComponent!!.onCollisionWith.register {
                if(it.collideGameObject.tag == GameObject.Tag.Player) {
                    actionOnCollisionWithPlayer.second.forEach { side ->
                        if(side == it.side) {
                            actionOnCollisionWithPlayer.first(it.collideGameObject)
                        }
                    }

                    actionOnPlayerCollision.second.forEach { side ->
                        if(side == it.side) {
                            actionOnPlayerCollision.first(gameObject)
                        }
                    }
                }
            }
        }
        else {
            Log.error { "Impossible de d'utiliser un AIComponent si l'objet n'a pas de PhysicsComponent" }
        }
    }

    override fun insertImgui(gameObject: GameObject, labelName: String, editorScene: EditorScene) {
        with(ImGui) {
            editorScene.addImguiWidget(gameObject,"Action sur le joueur", {actionOnCollisionWithPlayer.first}, {actionOnCollisionWithPlayer = it to actionOnCollisionWithPlayer.second }, ExposeEditorFactory.empty)
            editorScene.addImguiWidgetsArray(gameObject, "Side action pour joueur", { actionOnCollisionWithPlayer.second }, { actionOnCollisionWithPlayer = actionOnCollisionWithPlayer.first to it }, { CollisionSide.OnLeft }, { actionOnCollisionWithPlayer.second.elementAt(it).name }, { ExposeEditorFactory.empty })
            separator()
            editorScene.addImguiWidget(gameObject,"Action sur ce gameObject", {actionOnPlayerCollision.first}, {actionOnPlayerCollision = it to actionOnPlayerCollision.second }, ExposeEditorFactory.empty)
            editorScene.addImguiWidgetsArray(gameObject, "Side action pour ce gameObject", { actionOnPlayerCollision.second }, { actionOnPlayerCollision = actionOnPlayerCollision.first to it }, { CollisionSide.OnLeft }, { actionOnPlayerCollision.second.elementAt(it).name }, { ExposeEditorFactory.empty })
        }
    }
}