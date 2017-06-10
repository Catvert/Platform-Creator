package be.catvert.mtrktx.ecs.components

import be.catvert.mtrktx.ecs.systems.physics.GridCell


/**
 * Created by arno on 04/06/17.
 */

data class SmoothMove(var targetMoveSpeedX: Int = 0, var targetMoveSpeedY: Int = 0, var actualMoveSpeedX: Float = 0f, var actualMoveSpeedY: Float = 0f)

class PhysicsComponent(var isStatic: Boolean, var moveSpeed: Int = 0, smoothMove: Boolean = false, var gravity: Boolean = !isStatic) : BaseComponent() {
    enum class NextActions {
        GO_LEFT, GO_RIGHT, GO_UP, GO_DOWN, GRAVITY
    }

    val nextActions = mutableSetOf<NextActions>()

    val gridCell: MutableList<GridCell> = mutableListOf()

    val smoothMove: SmoothMove?

    init {
        if(smoothMove) this.smoothMove = SmoothMove() else this.smoothMove = null
    }

}