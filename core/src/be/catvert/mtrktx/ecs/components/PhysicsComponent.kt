package be.catvert.mtrktx.ecs.components


/**
 * Created by arno on 04/06/17.
 */

data class SmoothMoveData(var targetMoveSpeedX: Int = 0, var targetMoveSpeedY: Int = 0, var actualMoveSpeedX: Float = 0f, var actualMoveSpeedY: Float = 0f)

data class JumpData(var jumpHeight: Int, var isJumping: Boolean = false, var targetHeight: Int = 0, var startJumping: Boolean = false)

class PhysicsComponent(var isStatic: Boolean, var moveSpeed: Int = 0, smoothMove: Boolean = false, val collisionCallback: Boolean = false, var gravity: Boolean = !isStatic) : BaseComponent() {
    enum class NextActions {
        GO_LEFT, GO_RIGHT, GO_UP, GO_DOWN, GRAVITY, JUMP
    }

    val nextActions = mutableSetOf<NextActions>()

    val smoothMove: SmoothMoveData?

    var jumpData: JumpData? = null

    init {
        if(smoothMove) this.smoothMove = SmoothMoveData() else this.smoothMove = null
    }

}