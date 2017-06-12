package be.catvert.mtrktx.ecs

import be.catvert.mtrktx.MtrGame
import be.catvert.mtrktx.ecs.components.*
import be.catvert.mtrktx.get
import be.catvert.mtrktx.plusAssign
import be.catvert.mtrktx.scenes.MainMenuScene
import com.badlogic.ashley.core.ComponentMapper
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.LifecycleListener
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2

/**
 * Created by arno on 03/06/17.
 */

class EntityFactory {
    enum class EntityType(val flag: Int) {
        Sprite(0), PhysicsSprite(1), Player(2), Enemy(3)
    }

    companion object {
        private val renderMapper = ComponentMapper.getFor(RenderComponent::class.java)
        private val physicsMapper = ComponentMapper.getFor(PhysicsComponent::class.java)
        private val transformMapper = ComponentMapper.getFor(TransformComponent::class.java)

        fun createSprite(rectangle: Rectangle, texture: Pair<FileHandle, Texture>): Entity {
            val entity = Entity()
            entity.flags = EntityType.Sprite.flag

            entity += TransformComponent(rectangle)
            entity += RenderComponent(texture)

            return entity
        }

        fun createPhysicsSprite(rectangle: Rectangle, texture: Pair<FileHandle, Texture>, physComp: PhysicsComponent): Entity {
            val entity = createSprite(rectangle, texture)
            entity.flags = EntityType.PhysicsSprite.flag
            entity += physComp

            return entity
        }

        fun createPlayer(game: MtrGame, pos: Vector2): Entity {
            val entity = createPhysicsSprite(Rectangle(pos.x, pos.y, 48f, 98f), game.getTexture(Gdx.files.internal("game/maryo/small/stand_right.png")), PhysicsComponent(false, 15, true))
            entity.flags = EntityType.Player.flag

            transformMapper[entity].fixedSizeEditor = true

            val renderComp = renderMapper[entity]
            renderComp.renderLayer = 1
            val physicsComp = physicsMapper[entity]
            physicsComp.jumpData = JumpData(250)
            physicsComp.onCollisionWith = { thisEntity, collisionEntity, side ->
            }

            entity += UpdateComponent(entity, { delta, e, lvl ->
                val render = renderMapper[e]
                val physics = physicsMapper[e]

                if (Gdx.input.isKeyPressed(Input.Keys.D)) {
                    render.flipX = false
                    physics.nextActions += PhysicsComponent.NextActions.GO_RIGHT
                }
                if (Gdx.input.isKeyPressed(Input.Keys.Q)) {
                    render.flipX = true
                    physics.nextActions += PhysicsComponent.NextActions.GO_LEFT
                }
                if (Gdx.input.isKeyPressed(Input.Keys.Z)) {
                    physics.nextActions += PhysicsComponent.NextActions.GO_UP
                }
                if (Gdx.input.isKeyPressed(Input.Keys.S)) {
                    physics.nextActions += PhysicsComponent.NextActions.GO_DOWN
                }
                if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE))
                    physics.nextActions += PhysicsComponent.NextActions.JUMP
            })

            entity += LifeComponent(entity, 1, { _, _ ->}, { e, hp -> // remove life event
                if (hp == 0) {
                    game.setScene(MainMenuScene(game))
                }
            })

            return entity
        }

        fun createEnemy(game: MtrGame, entityEvent: EntityEvent, enemyType: EnemyType, pos: Vector2): Entity {
            val sizeX: Int
            val sizeY: Int
            val texture: Pair<FileHandle, Texture>
            val moveSpeed: Int
            val initialHP: Int
            when(enemyType) {
                EnemyType.Turtle -> {
                    sizeX = 48
                    sizeY = 98
                    texture = game.getTexture(Gdx.files.internal("game/enemy/turtle/green/walk_0.png"))
                    moveSpeed = 10
                    initialHP = 1
                }
            }

            val entity = createPhysicsSprite(Rectangle(pos.x, pos.y, sizeX.toFloat(), sizeY.toFloat()), texture, PhysicsComponent(false, moveSpeed, true))
            entity.flags = EntityType.Enemy.flag

            val enemyComp = EnemyComponent(enemyType)
            entity += enemyComp

            val onAddLife: (entity: Entity, hp: Int) -> Unit
            val onRemoveLife: (entity: Entity, hp: Int) -> Unit

            when(enemyType) {
                EnemyType.Turtle -> {
                    onAddLife = {_, _ ->}
                    onRemoveLife = { e, hp ->
                        if (hp <= 0)
                            entityEvent.onEntityRemoved?.invoke(e)
                    }
                }
            }

            val lifeComp = LifeComponent(entity, initialHP, onAddLife, onRemoveLife)
            entity += lifeComp

            when(enemyType) {
                EnemyType.Turtle -> {
                    var goRight = false

                    entity += UpdateComponent(entity, { delta, e, lvl ->
                        val physics = physicsMapper[e]
                        val render = renderMapper[e]

                        if(goRight) {
                            physics.nextActions += PhysicsComponent.NextActions.GO_RIGHT
                            render.flipX = true
                        }
                        else {
                            physics.nextActions += PhysicsComponent.NextActions.GO_LEFT
                            render.flipX = false
                        }
                    })

                    physicsMapper[entity].onCollisionWith = { thisEntity, collisionEntity, collisionSide ->
                        if(collisionSide == CollisionSide.OnLeft)
                            goRight = true
                        else if(collisionSide == CollisionSide.OnRight)
                            goRight = false
                    }

                    enemyComp.onPlayerCollision = { thisEnemy, player, side ->
                        if(side == CollisionSide.OnUp) {
                            lifeComp.removeLife(1)
                        }
                        else {
                            player[LifeComponent::class.java].removeLife(1)
                        }
                    }
                }
            }

            return entity
        }
    }
}