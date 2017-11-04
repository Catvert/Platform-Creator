package be.catvert.pc.components.logics.ai

import be.catvert.pc.GameObject
import be.catvert.pc.GameObjectState
import be.catvert.pc.Log
import be.catvert.pc.actions.Action
import be.catvert.pc.components.UpdeatableComponent
import be.catvert.pc.components.logics.CollisionSide
import be.catvert.pc.components.logics.PhysicsComponent
import be.catvert.pc.utility.SignalListener
import com.fasterxml.jackson.annotation.JsonIgnore

abstract class AIComponent(val applyActionCollisionToPlayer: Pair<Action, List<CollisionSide>>, val applyActionCollisionToEnemy: Pair<Action, List<CollisionSide>>) : UpdeatableComponent() {
    @JsonIgnore protected var physicsComponent: PhysicsComponent? = null

    override fun onGOAddToContainer(state: GameObjectState, gameObject: GameObject) {
        super.onGOAddToContainer(state, gameObject)

        physicsComponent = state.getComponent()
        if(physicsComponent != null) {
            physicsComponent!!.onCollisionWith.register {
                if(it.collideGameObject.tag == GameObject.Tag.Player) {
                    for(i in 0 until applyActionCollisionToPlayer.second.size) {
                        if(applyActionCollisionToPlayer.second[i] == it.side) {
                            applyActionCollisionToPlayer.first(it.collideGameObject)
                            break;
                        }
                    }

                    for(i in 0 until applyActionCollisionToEnemy.second.size) {
                        if(applyActionCollisionToEnemy.second[i] == it.side) {
                            applyActionCollisionToEnemy.first(gameObject)
                            break;
                        }
                    }
                }
            }
        }
        else {
            Log.error { "Impossible de d'utiliser un SimpleAIComponent si l'objet n'a pas de PhysicsComponent" }
        }
    }
}