package be.catvert.mtrktx.ecs.components

import be.catvert.mtrktx.ecs.IUpdateable
import com.badlogic.ashley.core.Entity

/**
 * Created by arno on 04/06/17.
 */

class UpdateComponent(val entity: Entity, val update: (delta: Float, entity: Entity) -> Unit) : BaseComponent() {
    override fun copy(target: Entity): BaseComponent {
        return UpdateComponent(target, update)
    }
}