package be.catvert.mtrktx

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.EntitySystem

/**
 * Created by arno on 03/06/17.
 */

class Utility() {

}

operator fun Entity.plusAssign(component: Component) {
    this.add(component)
}

operator fun Entity.minusAssign(component: Class<out Component>) {
    this.remove(component)
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
