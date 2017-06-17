package be.catvert.mtrktx.ecs.components

import be.catvert.mtrktx.ecs.systems.physics.GridCell
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.math.Rectangle

/**
* Created by Catvert on 03/06/17.
*/

class TransformComponent(var rectangle: Rectangle = Rectangle(), val gridCell: MutableList<GridCell> = mutableListOf(), var fixedSizeEditor: Boolean = false) : BaseComponent() {
    override fun copy(target: Entity): BaseComponent {
        return TransformComponent(Rectangle(rectangle), mutableListOf(), fixedSizeEditor)
    }
}