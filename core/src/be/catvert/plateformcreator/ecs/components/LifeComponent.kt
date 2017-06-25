package be.catvert.plateformcreator.ecs.components

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.signals.Listener
import com.badlogic.ashley.signals.Signal

/**
 * Created by Catvert on 11/06/17.
 */

/**
 * Listener lorsque la vie de l'entité change
 */
data class LifeListener(val entity: Entity, val hp: Int)

/**
 * Ce component permet d'ajouter un système de point de vie à l'entité
 * entity : l'entité possédant ce component
 * initialHP : Les points de vie initial de l'entité
 * onAddLife : est appelé lorsque l'on ajoute des points de vie à l'entité
 * onRemoveLife : est appelé lorsque l'on supprime des points de vie à l'entité
 */
class LifeComponent(var entity: Entity, initialHP: Int) : BaseComponent<LifeComponent>() {
    override fun copy(): LifeComponent {
        /*val lifeComponent = LifeComponent(entity, initialHP)

        lifeComponent.onAddLife = onAddLife
        lifeComponent.onRemoveLife = onRemoveLife

        return lifeComponent
        */

        TODO("Trouver une solution car si une copie a lieu, ça veut dire que c'est à destination d'une autre entité")
    }

    val onAddLife = Signal<LifeListener>()
    val onRemoveLife = Signal<LifeListener>()

    var hp = initialHP
        private set

    /**
     * Permet de supprimer des points de vie à l'entité
     */
    fun removeLife(remove: Int) {
        for (i in remove downTo 0) {
            --hp
            if (hp >= 0) onRemoveLife.dispatch(LifeListener(entity, hp))
        }
    }

    /**
     * Permet d'ajouter des points de vie à l'entité
     */
    fun addLife(add: Int) {
        for (i in 0 until add) {
            ++hp
            onAddLife.dispatch(LifeListener(entity, hp))
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

fun lifeComponent(entity: Entity, initialHP: Int, onAddLife: (LifeComponent.() -> Listener<LifeListener>)? = null, onRemoveLife: (LifeComponent.() -> Listener<LifeListener>)? = null): LifeComponent {
    val lifeComponent = LifeComponent(entity, initialHP)

    if (onAddLife != null) {
        lifeComponent.onAddLife.add(lifeComponent.onAddLife())
    }

    if (onRemoveLife != null) {
        lifeComponent.onRemoveLife.add(lifeComponent.onRemoveLife())
    }

    return lifeComponent
}