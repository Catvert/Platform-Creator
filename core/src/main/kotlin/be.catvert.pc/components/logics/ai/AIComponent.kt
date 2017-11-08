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
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnore
import com.kotcrab.vis.ui.widget.VisTable

class AIComponent(var actionOnCollisionWithPlayer: Pair<Action, List<CollisionSide>>, var actionOnPlayerCollision: Pair<Action, List<CollisionSide>>) : BasicComponent(), CustomEditorImpl {
    @JsonCreator private constructor(): this(EmptyAction() to listOf(), EmptyAction() to listOf())

    override fun insertChangeProperties(table: VisTable, editorScene: EditorScene) {
        //editorScene.addWidgetForValue(table, {actionOnPlayerCollision.first}, {actionOnPlayerCollision = it as Action to actionOnPlayerCollision.second })
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