package be.catvert.mtrktx.ecs.systems

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.EntitySystem

/**
 * Created by arno on 04/06/17.
 */

abstract class EntityEventSystem() : EntitySystem() {
    open fun onEntityAdded(entity: Entity) {}
    open fun onEntityRemoved(entity: Entity) {}
}