package be.catvert.mtrktx.ecs.components

import com.badlogic.ashley.core.Entity

/**
 * Created by arno on 11/06/17.
 */

class LifeComponent(val entity: Entity, val initialHP: Int, val onAddLife: (entity: Entity, hp: Int) -> Unit = { _, _ -> }, val onRemoveLife: (entity: Entity, hp: Int) -> Unit = { _, _ -> }): BaseComponent() {
    override fun copy(target: Entity): BaseComponent {
        return LifeComponent(target, initialHP, onRemoveLife, onAddLife)
    }

    var hp = initialHP
        private set

    fun removeLife(remove: Int) {
        for(i in hp downTo 0) {
            if(hp > 0) onRemoveLife(entity, --hp) else break
        }
    }

    fun addLife(add: Int) {
        for(i in 0 until add) {
            onAddLife(entity, ++hp)
        }
    }

    fun killInstant() {
        for(i in hp downTo 0)
            removeLife(i)
    }
}