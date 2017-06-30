package be.catvert.plateformcreator.ecs.components

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.signals.Listener
import com.badlogic.ashley.signals.Signal

/**
 * Created by Catvert on 11/06/17.
 */

/**
 * Listener lorsque la vie de l'entité change
 * @property entity L'entité ayant le lifeComponent
 */
data class LifeListener(val entity: Entity, val hp: Int)

/**
 * Ce component permet d'ajouter un système de point de vie à l'entité
 * @property entity : l'entité possédant ce component
 * @property initialHP : Les points de vie initial de l'entité
 */
class LifeComponent(var entity: Entity, private val initialHP: Int) : BaseComponent<LifeComponent>() {
    override fun copy(): LifeComponent {
        return LifeComponent(entity, initialHP)
        TODO("Trouver une solution car si une copie a lieu, ça veut dire que c'est à destination d'une autre entité")
    }

    /**
     * Signal appelé quand l'entité reçoit de la vie
     */
    val onAddLife = Signal<LifeListener>()

    /**
     * Signal appelé quand l'entité perd de la vie
     */
    val onRemoveLife = Signal<LifeListener>()

    /**
     * Les points de vie de l'entité
     */
    var hp = initialHP
        private set

    /**
     * Permet de supprimer des points de vie à l'entité
     * @param remove Les points de vie à retirer à l'entité
     */
    fun removeLife(remove: Int) {
        for (i in remove downTo 0) {
            --hp
            if (hp >= 0) onRemoveLife.dispatch(LifeListener(entity, hp))
        }
    }

    /**
     * Permet d'ajouter des points de vie à l'entité
     * @param add Les points de vie à ajouter à l'entité
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