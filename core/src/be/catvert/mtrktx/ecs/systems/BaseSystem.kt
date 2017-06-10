package be.catvert.mtrktx.ecs.systems

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.EntitySystem

/**
 * Created by arno on 10/06/17.
 */

abstract class BaseSystem() : EntitySystem() {
    init {
    }

    fun processEntities(entities: List<Entity>) {
        engine.removeAllEntities()
        entities.forEach {
            if(!engine.entities.contains(it))
                engine.addEntity(it)
        }
    }
}