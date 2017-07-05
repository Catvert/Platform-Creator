package be.catvert.plateformcreator.ecs.components

import be.catvert.plateformcreator.getType
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.signals.Listener
import com.badlogic.ashley.signals.Signal
import ktx.ashley.mapperFor

/**
 * Created by Catvert on 11/06/17.
 */

/**
 * Listener lorsque la vie de l'entité change
 * @param entity L'entité concernée
 * @param hp Les nouveaux points de vie de l'entité
 */
data class LifeListener(val entity: Entity, val hp: Int)

/**
 * Ce component permet d'ajouter un système de point de vie à l'entité
 * @property initialHP : Les points de vie initial de l'entité
 */
class LifeComponent(private val initialHP: Int) : BaseComponent<LifeComponent>() {
    companion object {
        private val lifeMapper = mapperFor<LifeComponent>()
        /**
         * Permet de supprimer des points de vie à l'entité
         * @param entity L'entité concernée
         * @param remove Les points de vie à retirer à l'entité
         */
        fun removeLifeTo(entity: Entity, remove: Int) {
            if (lifeMapper.has(entity)) {
                val lifeComp = lifeMapper[entity]
                for (i in remove downTo 0) {
                    --lifeComp.hp
                    if (lifeComp.hp >= 0) lifeComp.onRemoveLife.dispatch(LifeListener(entity, lifeComp.hp))
                }
            } else
                throw Exception("Impossible de supprimer des points de vie à l'entité : ${entity.getType()} car elle n'a pas de lifeComponent")
        }

        /**
         * Permet d'ajouter des points de vie à l'entité
         * @param entity L'entité concernée
         * @param add Les points de vie à ajouter à l'entité
         */
        fun addLifeTo(entity: Entity, add: Int) {
            if (lifeMapper.has(entity)) {
                val lifeComp = lifeMapper[entity]
                for (i in 0 until add) {
                    ++lifeComp.hp
                    lifeComp.onAddLife.dispatch(LifeListener(entity, lifeComp.hp))
                }
            } else
                throw Exception("Impossible d'ajouter des points de vie à l'entité : ${entity.getType()} car elle n'a pas de lifeComponent")
        }

        /**
         * Permet de tuer l'entité en supprimant tout ses points de vie
         * \!/ Ne supprime pas spécialement l'entité du niveau, ne fait qu'appelé removeLife jusqu'à que l'entité aie ses points de vie à 0
         */
        fun killInstant(entity: Entity) {
            if (lifeMapper.has(entity)) {
                for (i in lifeMapper[entity].hp downTo 0)
                    removeLifeTo(entity, i)
            } else
                throw Exception("Impossible de tuer l'entité : ${entity.getType()} car elle n'a pas de lifeComponent")
        }
    }


    override fun copy(): LifeComponent {
        val lifeComp = LifeComponent(initialHP)

        lifeComp.onAddLife = onAddLife
        lifeComp.onRemoveLife = onRemoveLife

        return lifeComp
    }

    /**
     * Signal appelé quand l'entité reçoit de la vie
     */
    var onAddLife = Signal<LifeListener>()
        private set

    /**
     * Signal appelé quand l'entité perd de la vie
     */
    var onRemoveLife = Signal<LifeListener>()
        private set

    /**
     * Les points de vie de l'entité
     */
    var hp = initialHP
        private set
}

fun lifeComponent(initialHP: Int, onAddLife: (LifeComponent.() -> Listener<LifeListener>)? = null, onRemoveLife: (LifeComponent.() -> Listener<LifeListener>)? = null): LifeComponent {
    val lifeComponent = LifeComponent(initialHP)

    if (onAddLife != null) {
        lifeComponent.onAddLife.add(lifeComponent.onAddLife())
    }

    if (onRemoveLife != null) {
        lifeComponent.onRemoveLife.add(lifeComponent.onRemoveLife())
    }

    return lifeComponent
}