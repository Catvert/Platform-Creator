package be.catvert.plateformcreator.ecs.components

import com.badlogic.ashley.core.Entity

/**
 * Created by Catvert on 11/06/17.
 */

/**
 * Ce component permet d'ajouter un système de point de vie à l'entité
 * entity : l'entité possédant ce component
 * initialHP : Les points de vie initial de l'entité
 * onAddLife : est appelé lorsque l'on ajoute des points de vie à l'entité
 * onRemoveLife : est appelé lorsque l'on supprime des points de vie à l'entité
 */
class LifeComponent(var entity: Entity, val initialHP: Int, val onAddLife: ((entity: Entity, hp: Int) -> Unit)? = null, val onRemoveLife: ((entity: Entity, hp: Int) -> Unit)? = null) : BaseComponent<LifeComponent>() {
    override fun copy(): LifeComponent {
        return LifeComponent(entity, initialHP, onRemoveLife, onAddLife)
    }

    var hp = initialHP
        private set

    /**
     * Permet de supprimer des points de vie à l'entité
     */
    fun removeLife(remove: Int) {
        for (i in remove downTo 0) {
            --hp
            if (hp >= 0) onRemoveLife?.invoke(entity, hp) else break
        }
    }

    /**
     * Permet d'ajouter des points de vie à l'entité
     */
    fun addLife(add: Int) {
        for (i in 0 until add) {
            ++hp
            onAddLife?.invoke(entity, hp)
        }
    }

    /**
     * Permet de tuer l'entité en supprimant tout ses points de vie
     * \!/ Ne supprime pas spécialement l'entité du niveau, ne fait qu'appelé removeLife jusqu'à que l'entité aie ses points de vie à 0
     */
    fun killInstant() {
        for (i in hp downTo 0)
            removeLife(i)
    }
}