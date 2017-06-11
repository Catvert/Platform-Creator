package be.catvert.mtrktx.ecs.components

import com.badlogic.ashley.core.Entity

/**
 * Created by arno on 11/06/17.
 */

class LifeComponent(val entity: Entity, val initialHP: Int, val removeLifeEvent: (hp: Int, entity: Entity) -> Unit = { hp, e -> }, val addLifeEvent: (hp: Int, entity: Entity) -> Unit = {hp, e -> }): BaseComponent() {
    override fun copy(target: Entity): BaseComponent {
        return LifeComponent(target, initialHP, removeLifeEvent, addLifeEvent)
    }

    var hp = initialHP
        private set

    fun removeLife(remove: Int) {
        for(i in hp downTo 0) {
            if(hp > 0) removeLifeEvent(--hp, entity) else break
        }
    }

    fun addLife(add: Int) {
        for(i in 0 until add) {
            addLifeEvent(++hp, entity)
        }
    }

    fun killInstant() {
        for(i in hp downTo 0)
            removeLife(i)
    }
}