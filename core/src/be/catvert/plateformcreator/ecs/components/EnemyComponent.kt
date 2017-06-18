package be.catvert.plateformcreator.ecs.components

import com.badlogic.ashley.core.Entity

/**
* Created by Catvert on 12/06/17.
*/

enum class EnemyType {
    Spinner, Spider
}

/**
 * Ce component permet d'identifier l'ennemi ainsi que de permettre à l'ennemi d'avoir un callback lors d'une collision avec le joueur
 */
class EnemyComponent(var enemyType: EnemyType): BaseComponent<EnemyComponent>() {
    override fun copy(): EnemyComponent {
        val enemyComp = EnemyComponent(enemyType)

        enemyComp.onPlayerCollision = onPlayerCollision

        return enemyComp
    }

    /**
     * Fonction appelée lors d'une collision avec le joueur
     */
    var onPlayerCollision: ((thisEnemy: Entity, player: Entity, side: CollisionSide) -> Unit)? = null
}