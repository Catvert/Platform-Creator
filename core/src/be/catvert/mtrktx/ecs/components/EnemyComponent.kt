package be.catvert.mtrktx.ecs.components

import com.badlogic.ashley.core.Entity

/**
 * Created by arno on 12/06/17.
 */

enum class EnemyType {
    Turtle
}

class EnemyComponent(val enemyType: EnemyType): BaseComponent() {
    override fun copy(target: Entity): BaseComponent {
        val enemyComp = EnemyComponent(enemyType)
        enemyComp.onPlayerCollision = onPlayerCollision
        return enemyComp
    }

    var onPlayerCollision: ((thisEnemy: Entity, player: Entity, side: CollisionSide) -> Unit)? = null
}