package be.catvert.mtrktx.ecs

import com.badlogic.ashley.core.Entity

/**
 * Created by arno on 12/06/17.
 */

class EntityEvent() {
    var onEntityRemoved: ((entity: Entity) -> Unit)? = null
    var onEntityAdded: ((entity: Entity) -> Unit)? = null
}
