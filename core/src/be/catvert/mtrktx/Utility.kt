package be.catvert.mtrktx

import be.catvert.mtrktx.ecs.components.BaseComponent
import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.EntitySystem

/**
 * Created by arno on 03/06/17.
 */

class Utility() {

}

operator fun Entity.plusAssign(component: BaseComponent) {
    this.add(component)
}

operator fun Entity.minusAssign(component: Class<out BaseComponent>) {
    this.remove(component)
}

fun Entity.copy(): Entity {
    val entityCopy = Entity()
    this.components.forEach {
        if(it is BaseComponent) {
            entityCopy += it.copy(entityCopy)
        }
    }
    return entityCopy
}

operator fun <T: BaseComponent> Entity.get(component: Class<T>): T {
    return this.getComponent(component)
}

operator fun Engine.plusAssign(entity: Entity) {
    this.addEntity(entity)
}

operator fun Engine.minusAssign(entity: Entity) {
    this.removeEntity(entity)
}

operator fun Engine.plusAssign(system: EntitySystem) {
    this.addSystem(system)
}

operator fun Engine.minusAssign(system: EntitySystem) {
    this.removeSystem(system)
}
