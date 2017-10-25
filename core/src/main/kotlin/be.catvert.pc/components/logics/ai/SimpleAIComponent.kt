package be.catvert.pc.components.logics.ai

import be.catvert.pc.GameObject
import be.catvert.pc.Log
import be.catvert.pc.actions.*
import be.catvert.pc.components.UpdeatableComponent
import be.catvert.pc.components.logics.CollisionSide
import be.catvert.pc.components.logics.PhysicsComponent
import be.catvert.pc.utility.SignalListener
import com.fasterxml.jackson.annotation.JsonIgnore

class SimpleAIComponent(var goRight: Boolean = true) : AIComponent(RemoveAction() to listOf(CollisionSide.OnLeft, CollisionSide.OnRight, CollisionSide.OnDown), RemoveAction() to listOf(CollisionSide.OnUp)) {
    @JsonIgnore val goLeftAction = MultiplexerAction(listOf(PhysicsAction(NextPhysicsActions.GO_LEFT), RenderAction(RenderAction.RenderActions.UNFLIP_X)))
    @JsonIgnore val goRightAction = MultiplexerAction(listOf(PhysicsAction(NextPhysicsActions.GO_RIGHT), RenderAction(RenderAction.RenderActions.FLIP_X)))

    override fun onGOAddToContainer(gameObject: GameObject) {
        super.onGOAddToContainer(gameObject)
        physicsComponent?.onCollisionWith?.register {
            if(it.side == CollisionSide.OnLeft)
                goRight = true
            else if(it.side == CollisionSide.OnRight)
                goRight = false
        }
    }

    override fun update() {
        if(goRight)
            goRightAction.perform(gameObject)
        else
            goLeftAction.perform(gameObject)
    }
}