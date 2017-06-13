package be.catvert.mtrktx.ecs.components

import com.badlogic.ashley.core.Entity

/**
* Created by Catvert on 11/06/17.
*/

class LifeComponent(val entity: Entity, val initialHP: Int, val onAddLife: ((entity: Entity, hp: Int) -> Unit)? = null, val onRemoveLife: ((entity: Entity, hp: Int) -> Unit)? = null): BaseComponent() {
    override fun copy(target: Entity): BaseComponent {
        return LifeComponent(target, initialHP, onRemoveLife, onAddLife)
    }

    var hp = initialHP
        private set

    fun removeLife(remove: Int) {
        for(i in remove downTo 0) {
            --hp
            if(hp >= 0) onRemoveLife?.invoke(entity, hp) else break
        }
    }

    fun addLife(add: Int) {
        for(i in 0 until add) {
            ++hp
            onAddLife?.invoke(entity, hp)
        }
    }

    fun killInstant() {
        for(i in hp downTo 0)
            removeLife(i)
    }
}