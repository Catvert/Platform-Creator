package be.catvert.plateformcreator.ecs.systems.physics

import be.catvert.plateformcreator.Level
import be.catvert.plateformcreator.ecs.EntityFactory
import be.catvert.plateformcreator.ecs.components.*
import be.catvert.plateformcreator.get
import com.badlogic.ashley.core.ComponentMapper
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.EntitySystem
import com.badlogic.ashley.core.Family
import com.badlogic.ashley.utils.ImmutableArray
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Rectangle

/**
 * Created by Catvert on 04/06/17.
 */

/**
 * Ce système permet de gérer le système physique du jeu en mettant à jour les entités ayant un physicsComponent et un transformComponent
 * level : Le niveau chargé au préalable
 * gravity : La gravité à appliquée au entité implémentant un physicsComponent, permet aussi de spécifier la vitesse du jump(inverse de la gravité)
 */
class PhysicsSystem(private val level: Level, val gravity: Int = 15) : EntitySystem() {
    private lateinit var entities: ImmutableArray<Entity>

    private val transformMapper = ComponentMapper.getFor(TransformComponent::class.java)
    private val physicsMapper = ComponentMapper.getFor(PhysicsComponent::class.java)

    override fun update(deltaTime: Float) {
        super.update(deltaTime)
        entities = engine.getEntitiesFor(Family.all(PhysicsComponent::class.java, TransformComponent::class.java).get())

        entities.forEach { entity ->
            val physicsComp = physicsMapper[entity]
            val transformComp = transformMapper[entity]

            if (physicsComp.isStatic || !physicsComp.active)
                return@forEach

            if (physicsComp.gravity && level.applyGravity)
                physicsComp.nextActions += NextActions.GRAVITY

            if (physicsComp.jumpData?.forceJumping ?: false) {
                physicsComp.nextActions += NextActions.JUMP
            }

            var moveSpeedX = 0f
            var moveSpeedY = 0f
            var addJumpAfterClear = false

            physicsComp.nextActions.forEach action@ {
                when (it) {
                    NextActions.GO_LEFT -> moveSpeedX -= physicsComp.moveSpeed
                    NextActions.GO_RIGHT -> moveSpeedX += physicsComp.moveSpeed
                    NextActions.GO_UP -> moveSpeedY += physicsComp.moveSpeed
                    NextActions.GO_DOWN -> moveSpeedY -= physicsComp.moveSpeed
                    NextActions.GRAVITY -> if (physicsComp.gravity) moveSpeedY -= gravity
                    NextActions.JUMP -> {
                        if (physicsComp.jumpData == null) {
                            println("L'entité ne contient pas de jumpData")
                            return@action
                        }

                        val jumpData = physicsComp.jumpData!!

                        if (!jumpData.isJumping) {
                            if (!jumpData.forceJumping) {
                                if (!checkIsOnGround(entity)) {
                                    return@action
                                }
                            } else {
                                jumpData.forceJumping = false
                            }
                            jumpData.isJumping = true
                            jumpData.targetHeight = transformComp.rectangle.y.toInt() + jumpData.jumpHeight
                            jumpData.startJumping = true

                            physicsComp.gravity = false

                            moveSpeedY = gravity.toFloat()
                            addJumpAfterClear = true
                        } else {
                            if (transformComp.rectangle.y >= jumpData.targetHeight || collideOnMove(0, gravity, entity)) {
                                physicsComp.gravity = true
                                jumpData.isJumping = false
                            } else {

                                moveSpeedY = gravity.toFloat()
                                addJumpAfterClear = true
                            }
                            jumpData.startJumping = false
                        }
                    }
                }
            }

            if (physicsComp.movementType == MovementType.SMOOTH) {
                moveSpeedX = MathUtils.lerp(physicsComp.actualMoveSpeedX, moveSpeedX, 0.2f)
                moveSpeedY = MathUtils.lerp(physicsComp.actualMoveSpeedY, moveSpeedY, 0.2f)
            }

            physicsComp.actualMoveSpeedX = moveSpeedX
            physicsComp.actualMoveSpeedY = moveSpeedY

            tryMove(moveSpeedX.toInt(), moveSpeedY.toInt(), entity) // move l'entité

            physicsComp.nextActions.clear()

            if (addJumpAfterClear)
                physicsComp.nextActions += NextActions.JUMP

            physicsComp.isOnGround = checkIsOnGround(entity)
        }
    }

    /**
     * Permet d'essayer de déplacer une entité ayant un physicsComponent
     * moveX : Le déplacement x
     * moveY : Le déplacement y
     */
    private fun tryMove(moveX: Int, moveY: Int, entity: Entity) {
        val transformTarget = transformMapper[entity]
        val physicsTarget = physicsMapper[entity]

        if (moveX != 0 || moveY != 0) {
            var newMoveX = moveX
            var newMoveY = moveY

            if (!collideOnMove(moveX, 0, entity)) {
                transformTarget.rectangle.x = Math.max(0f, transformTarget.rectangle.x + moveX)
                level.setEntityGrid(entity)
                physicsTarget.onMove?.invoke(entity, if (transformTarget.rectangle.x == 0f) 0 else moveX, moveY) // TODO voir ici pour amélioration
                newMoveX = 0
            }
            if (!collideOnMove(0, moveY, entity)) {
                transformTarget.rectangle.y += moveY
                level.setEntityGrid(entity)
                physicsTarget.onMove?.invoke(entity, moveX, moveY)
                newMoveY = 0
            }

            if (newMoveX > 0)
                newMoveX -= 1
            else if (newMoveX < 0)
                newMoveX += 1

            if (newMoveY > 0)
                newMoveY -= 1
            else if (newMoveY < 0)
                newMoveY += 1
            tryMove(newMoveX, newMoveY, entity)
        }
    }

    /**
     * Permet de vérifier si l'entité est sur le sol ou pas
     */
    fun checkIsOnGround(entity: Entity) = collideOnMove(0, -1, entity)

    /**
     * Permet de tester si un déplacement est possible ou non
     * moveX : Le déplacement x
     * moveY : Le déplacement y
     * entity : L'entité à tester
     *
     */
    private fun collideOnMove(moveX: Int, moveY: Int, entity: Entity): Boolean {
        val physicsTarget = physicsMapper[entity]
        val transformTarget = transformMapper[entity]

        val newRect = Rectangle(transformTarget.rectangle)
        newRect.setPosition(newRect.x + moveX, newRect.y + moveY)

        level.getRectCells(newRect).forEach {
            level.matrixGrid[it.x][it.y].first.filter { // On parcourt toute les entités avec la condition en-dessous
                transformMapper[it] != transformTarget && physicsMapper.has(it) && (physicsMapper[it].maskCollision == physicsTarget.maskCollision)
            }.forEach {
                val transformComponent = transformMapper[it]

                if (newRect.overlaps(transformComponent.rectangle)) {
                    val side = if (moveX > 0) CollisionSide.OnRight else if (moveX < 0) CollisionSide.OnLeft else if (moveY > 0) CollisionSide.OnUp else if (moveY < 0) CollisionSide.OnDown else CollisionSide.Unknow

                    if (entity.flags == EntityFactory.EntityType.Enemy.flag && it.flags == EntityFactory.EntityType.Player.flag)
                        entity[EnemyComponent::class.java].onPlayerCollision?.invoke(entity, it, side)
                    else if (entity.flags == EntityFactory.EntityType.Player.flag && it.flags == EntityFactory.EntityType.Enemy.flag)
                        it[EnemyComponent::class.java].onPlayerCollision?.invoke(it, entity, -side) // - side to inverse the side
                    else
                        physicsMapper[entity].onCollisionWith?.invoke(entity, it, side)

                    return true
                }
            }
        }
        return false
    }
}