package be.catvert.mtrktx.ecs.components

import be.catvert.mtrktx.ecs.systems.physics.GridCell
import com.badlogic.gdx.math.Rectangle

/**
 * Created by arno on 03/06/17.
 */

class TransformComponent(val rectangle: Rectangle) : BaseComponent() {
    val gridCell: MutableList<GridCell> = mutableListOf()
}