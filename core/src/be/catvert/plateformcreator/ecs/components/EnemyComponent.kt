package be.catvert.plateformcreator.ecs.components

import com.badlogic.ashley.signals.Listener
import com.badlogic.ashley.signals.Signal

/**
 * Created by Catvert on 12/06/17.
 */

/**
 * Représente le type de l'ennemi
 */
enum class EnemyType {
    Spinner, Spider
}

/**
 * Ce component permet d'identifier l'ennemi ainsi que de permettre à l'ennemi d'avoir un callback lors d'une collision avec le joueur
 */
class EnemyComponent(var enemyType: EnemyType) : BaseComponent<EnemyComponent>() {
    override fun copy(): EnemyComponent {
        val enemyComp = EnemyComponent(enemyType)

        enemyComp.onPlayerCollision = onPlayerCollision

        return enemyComp
    }

    /**
     * Fonction appelée lors d'une collision avec le joueur
     */
    var onPlayerCollision: Signal<CollisionListener> = Signal()
}

fun enemyComponent(enemyType: EnemyType, init: (EnemyComponent.() -> Listener<CollisionListener>)? = null): EnemyComponent {
    val enemyComponent = EnemyComponent(enemyType)

    if (init != null)
        enemyComponent.onPlayerCollision.add(enemyComponent.init())

    return enemyComponent
}