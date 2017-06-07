package be.catvert.mtrktx.ecs.components

import be.catvert.mtrktx.ecs.systems.physics.GridCell
import com.badlogic.ashley.core.Component


/**
 * Created by arno on 04/06/17.
 */

class PhysicsComponent(var isStatic: Boolean, var moveSpeed: Int = 0, var gravity: Boolean = !isStatic) : Component {
    enum class NextActions {
        GO_LEFT, GO_RIGHT, GO_UP, GO_DOWN, GRAVITY
    }

    val nextActions = mutableSetOf<NextActions>()

    val gridCell: MutableList<GridCell> = mutableListOf()
}