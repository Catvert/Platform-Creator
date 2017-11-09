package be.catvert.pc.components.logics.ai

import be.catvert.pc.GameObject
import be.catvert.pc.GameObjectState
import be.catvert.pc.Log
import be.catvert.pc.actions.Action
import be.catvert.pc.actions.EmptyAction
import be.catvert.pc.components.BasicComponent
import be.catvert.pc.components.logics.CollisionSide
import be.catvert.pc.components.logics.PhysicsComponent
import be.catvert.pc.scenes.EditorScene
import be.catvert.pc.utility.CustomEditorImpl
import be.catvert.pc.utility.ExposeEditorFactory
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnore
import com.kotcrab.vis.ui.widget.VisTable

class AIComponent(var actionOnCollisionWithPlayer: Pair<Action, Array<CollisionSide>>, var actionOnPlayerCollision: Pair<Action, Array<CollisionSide>>) : BasicComponent(), CustomEditorImpl {
    @JsonCreator private constructor(): this(EmptyAction() to arrayOf(), EmptyAction() to arrayOf())

    override fun insertChangeProperties(table: VisTable, gameObject: GameObject, editorScene: EditorScene) {
        editorScene.addWidgetValue(table, gameObject,"Action sur le joueur : ", {actionOnCollisionWithPlayer.first}, {actionOnCollisionWithPlayer = it as Action to actionOnCollisionWithPlayer.second }, ExposeEditorFactory.createExposeEditor())
        editorScene.addWidgetArray(table.add(VisTable()).actor, gameObject, { actionOnCollisionWithPlayer.second.elementAt(it).name }, { ExposeEditorFactory.createExposeEditor() }, { CollisionSide.OnLeft }, { actionOnCollisionWithPlayer.second }, { actionOnCollisionWithPlayer = actionOnCollisionWithPlayer.first to it })
        table.row()

        editorScene.addWidgetValue(table, gameObject,"Action sur ce gameObject : ", {actionOnCollisionWithPlayer.first}, {actionOnCollisionWithPlayer = it as Action to actionOnCollisionWithPlayer.second }, ExposeEditorFactory.createExposeEditor())
        editorScene.addWidgetArray(table.add(VisTable()).actor, gameObject, { actionOnCollisionWithPlayer.second.elementAt(it).name }, { ExposeEditorFactory.createExposeEditor() }, { CollisionSide.OnLeft }, { actionOnCollisionWithPlayer.second }, { actionOnCollisionWithPlayer = actionOnCollisionWithPlayer.first to it })
        table.row()
    }

    @JsonIgnore private var physicsComponent: PhysicsComponent? = null

    override fun onGOAddToContainer(state: GameObjectState, gameObject: GameObject) {
        super.onGOAddToContainer(state, gameObject)

        physicsComponent = state.getComponent()
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
}