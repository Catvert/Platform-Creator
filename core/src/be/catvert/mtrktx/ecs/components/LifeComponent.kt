package be.catvert.mtrktx.ecs.components

/**
 * Created by arno on 11/06/17.
 */

class LifeComponent(initialHP: Int, val removeLifeEvent: (hp: Int) -> Unit = {}, val addLifeEvent: (hp: Int) -> Unit = {}): BaseComponent() {
    var hp = initialHP
        private set

    fun removeLife(remove: Int) {
        for(i in hp downTo 0) {
            if(hp > 0) removeLifeEvent(--hp) else break
        }
    }

    fun addLife(add: Int) {
        for(i in 0 until add) {
            addLifeEvent(++hp)
        }
    }

    fun killInstant() {
        for(i in hp downTo 0)
            removeLife(i)
    }
}