package be.catvert.mtrktx.ecs.systems.physics

import be.catvert.mtrktx.Level
import be.catvert.mtrktx.MtrGame
import be.catvert.mtrktx.ecs.components.PhysicsComponent
import be.catvert.mtrktx.ecs.components.TransformComponent
import be.catvert.mtrktx.ecs.systems.BaseSystem
import com.badlogic.ashley.core.ComponentMapper
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family
import com.badlogic.ashley.utils.ImmutableArray
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Rectangle

/**
 * Created by arno on 04/06/17.
 */

class PhysicsSystem(private val game: MtrGame, private val level: Level, private val camera: Camera, val gravity: Int = 5) : BaseSystem() {
    private lateinit var entities: ImmutableArray<Entity>

    private val transformMapper = ComponentMapper.getFor(TransformComponent::class.java)
    private val physicsMapper = ComponentMapper.getFor(PhysicsComponent::class.java)

    init {

    }

    override fun update(deltaTime: Float) {
        super.update(deltaTime)
        entities = engine.getEntitiesFor(Family.all(PhysicsComponent::class.java, TransformComponent::class.java).get())

        entities.forEach {
            val physicsComp = physicsMapper[it]
            val transformComp = transformMapper[it]

            if (physicsComp.isStatic || !physicsComp.active)
                return@forEach

            if (physicsComp.gravity)
                physicsComp.nextActions += PhysicsComponent.NextActions.GRAVITY

            var moveSpeedX = 0
            var moveSpeedY = 0
            physicsComp.nextActions.forEach {
                when (it) {
                    PhysicsComponent.NextActions.GO_LEFT -> moveSpeedX -= physicsComp.moveSpeed
                    PhysicsComponent.NextActions.GO_RIGHT -> moveSpeedX += physicsComp.moveSpeed
                    PhysicsComponent.NextActions.GO_UP -> moveSpeedY += physicsComp.moveSpeed
                    PhysicsComponent.NextActions.GO_DOWN -> moveSpeedY -= physicsComp.moveSpeed
                    PhysicsComponent.NextActions.GRAVITY -> moveSpeedY -= gravity
                }
            }
            if (physicsComp.smoothMove != null) { // Smooth mode
                physicsComp.smoothMove.targetMoveSpeedX = moveSpeedX
                physicsComp.smoothMove.targetMoveSpeedY = moveSpeedY

                physicsComp.smoothMove.actualMoveSpeedX = MathUtils.lerp(physicsComp.smoothMove.actualMoveSpeedX, physicsComp.smoothMove.targetMoveSpeedX.toFloat(), 0.5f)
                physicsComp.smoothMove.actualMoveSpeedY = MathUtils.lerp(physicsComp.smoothMove.actualMoveSpeedY, physicsComp.smoothMove.targetMoveSpeedY.toFloat(), 0.5f)

                tryMove(physicsComp.smoothMove.actualMoveSpeedX.toInt(), physicsComp.smoothMove.actualMoveSpeedY.toInt(), it, transformComp)

            } else { // NO smooth mode
                tryMove(moveSpeedX, moveSpeedY, it, transformComp)
            }

            if (it == level.player) {
                level.playerRect.setPosition(Math.max(0f, transformComp.rectangle.x - level.playerRect.width / 2 + transformComp.rectangle.width / 2), Math.max(0f, transformComp.rectangle.y - level.playerRect.height / 2 + transformComp.rectangle.height / 2))
            }

            physicsComp.nextActions.clear()
        }
    }

    private fun tryMove(moveX: Int, moveY: Int, entity: Entity, transformTarget: TransformComponent) {
        if (moveX != 0 || moveY != 0) {
            var newMoveX = moveX
            var newMoveY = moveY

            if (!collideOnMove(moveX, 0, entity)) {
                transformTarget.rectangle.x = Math.max(0f, transformTarget.rectangle.x + moveX)
                level.setEntityGrid(entity)
                newMoveX = 0
            }
            if (!collideOnMove(0, moveY, entity)) {
                transformTarget.rectangle.y = Math.max(0f, transformTarget.rectangle.y + moveY)
                level.setEntityGrid(entity)
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

            tryMove(newMoveX, newMoveY, entity, transformTarget)
        }
    }

    private fun collideOnMove(moveX: Int, moveY: Int, entity: Entity): Boolean {
        val transformTarget = transformMapper[entity]

        val newRect = Rectangle(transformTarget.rectangle)
        newRect.setPosition(newRect.x + moveX, newRect.y + moveY)

        level.getRectCells(newRect).forEach {
           level.matrixGrid[it.x][it.y].first.forEach matrixLoop@ {
                val transformComponent = transformMapper[it]

                if (transformComponent == transformTarget)
                    return@matrixLoop

                if (newRect.overlaps(transformComponent.rectangle))
                    return true
            }
        }

        return false
    }






}