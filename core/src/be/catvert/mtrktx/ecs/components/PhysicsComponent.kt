package be.catvert.mtrktx.ecs.components

import com.badlogic.ashley.core.Entity
import com.sun.xml.internal.ws.api.pipe.NextAction


/**
* Created by Catvert on 04/06/17.
*/

data class SmoothMoveData(var targetMoveSpeedX: Int = 0, var targetMoveSpeedY: Int = 0, var actualMoveSpeedX: Float = 0f, var actualMoveSpeedY: Float = 0f)

data class JumpData(var jumpHeight: Int, var isJumping: Boolean = false, var targetHeight: Int = 0, var startJumping: Boolean = false, var forceJumping: Boolean = false)

enum class CollisionSide {
    OnLeft, OnRight, OnUp, OnDown, Unknow;

    operator fun unaryMinus(): CollisionSide {
        if(this == OnLeft)
            return OnRight
        else if(this == OnRight)
            return OnLeft
        else if(this == OnUp)
            return OnDown
        else if(this == OnDown)
            return OnUp
        return Unknow
    }
}

class PhysicsComponent(var isStatic: Boolean, var moveSpeed: Int = 0, var smoothMove: Boolean = false, var gravity: Boolean = !isStatic) : BaseComponent() {
    override fun copy(target: Entity): BaseComponent {
        val physicsComp =  PhysicsComponent(isStatic, moveSpeed, smoothMove)
        physicsComp.jumpData = if(jumpData != null) JumpData(jumpData!!.jumpHeight) else null
        physicsComp.onCollisionWith = onCollisionWith
        physicsComp.onMove = onMove
        return physicsComp
    }

    enum class NextActions {
        GO_LEFT, GO_RIGHT, GO_UP, GO_DOWN, GRAVITY, JUMP
    }

    val nextActions = mutableSetOf<NextActions>()

    val smoothMoveData: SmoothMoveData?

    var jumpData: JumpData? = null

    var onCollisionWith: ((thisEntity: Entity, collisionEntity: Entity, side: CollisionSide) -> Unit)? = null

    var onMove: ((thisEntity: Entity, moveX: Int, moveY: Int) -> Unit)? = null

    init {
        if(smoothMove) this.smoothMoveData = SmoothMoveData() else this.smoothMoveData = null
    }

}