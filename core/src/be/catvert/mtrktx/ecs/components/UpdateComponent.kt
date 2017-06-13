package be.catvert.mtrktx.ecs.components

import be.catvert.mtrktx.Level
import com.badlogic.ashley.core.Entity

/**
* Created by Catvert on 04/06/17.
*/

class UpdateComponent(val entity: Entity, val update: (delta: Float, entity: Entity, level: Level) -> Unit) : BaseComponent() {
    override fun copy(target: Entity): BaseComponent {
        return UpdateComponent(target, update)
    }
}