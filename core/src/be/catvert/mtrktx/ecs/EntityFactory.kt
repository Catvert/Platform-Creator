package be.catvert.mtrktx.ecs

import be.catvert.mtrktx.MtrGame
import be.catvert.mtrktx.TextureInfo
import be.catvert.mtrktx.ecs.components.*
import be.catvert.mtrktx.get
import be.catvert.mtrktx.plusAssign
import be.catvert.mtrktx.scenes.MainMenuScene
import com.badlogic.ashley.core.ComponentMapper
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2

/**
* Created by Catvert on 03/06/17.
*/

class EntityFactory {
    enum class EntityType(val flag: Int) {
        Sprite(0), PhysicsSprite(1), Player(2), Enemy(3)
    }

    companion object {
        private val renderMapper = ComponentMapper.getFor(RenderComponent::class.java)
        private val physicsMapper = ComponentMapper.getFor(PhysicsComponent::class.java)
        private val transformMapper = ComponentMapper.getFor(TransformComponent::class.java)

        fun createSprite(rectangle: Rectangle, texture: TextureInfo): Entity {
            val entity = Entity()
            entity.flags = EntityType.Sprite.flag

            entity += TransformComponent(rectangle)
            entity += RenderComponent(texture)

            return entity
        }

        fun createPhysicsSprite(rectangle: Rectangle, texture: TextureInfo, physComp: PhysicsComponent): Entity {
            val entity = createSprite(rectangle, texture)
            entity.flags = EntityType.PhysicsSprite.flag
            entity += physComp

            return entity
        }

        fun createPlayer(game: MtrGame, pos: Vector2): Entity {
            val entity = createPhysicsSprite(Rectangle(pos.x, pos.y, 48f, 98f), game.getGameTexture(Gdx.files.internal("game/maryo/small/stand_right.png")), PhysicsComponent(false, 15, true))
            entity.flags = EntityType.Player.flag

            transformMapper[entity].fixedSizeEditor = true

            val renderComp = renderMapper[entity]
            renderComp.renderLayer = 1
            val physicsComp = physicsMapper[entity]
            physicsComp.jumpData = JumpData(250)

            // physicsComp.onCollisionWith = { thisEntity, collisionEntity, side -> }

            entity += UpdateComponent(entity, { _, e, _ ->
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

            entity += LifeComponent(entity, 1, { _, _ ->}, { _, hp -> // remove life event
                if (hp == 0) {
                    game.setScene(MainMenuScene(game))
                }
            })

            return entity
        }

        private fun createEnemy(enemyType: EnemyType, texture: TextureInfo, rect: Rectangle, moveSpeed: Int): Pair<Entity, EnemyComponent> {
            val entity = createPhysicsSprite(rect, texture, PhysicsComponent(false, moveSpeed, true))
            entity.flags = EntityType.Enemy.flag

            val enemyComp = EnemyComponent(enemyType)
            entity += enemyComp

            return Pair(entity, enemyComp)
        }

        fun createEnemyWithType(game: MtrGame, enemyType: EnemyType, entityEvent: EntityEvent, pos: Vector2): Entity {
            when(enemyType) {
                EnemyType.Turtle -> return createTurtleEnemy(game, entityEvent, pos)
                EnemyType.Furball -> return createFurballEnemy(game, entityEvent, pos)
            }
        }

        fun createFurballEnemy(game: MtrGame, entityEvent: EntityEvent, pos: Vector2): Entity {
            val entity = createEnemy(EnemyType.Furball, game.getGameTexture(Gdx.files.internal("game/enemy/furball/brown/walk_1.png")), Rectangle(pos.x, pos.y, 48f, 48f), 5)
            var goRight = false

            entity.first += UpdateComponent(entity.first, { _, e, _ ->
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

            val lifeComp = LifeComponent(entity.first, 2, null, { entity, hp ->
                if(hp <= 0) {
                    entityEvent.onEntityRemoved?.invoke(entity)
                }
            })
            entity.first += lifeComp

            physicsMapper[entity.first].onCollisionWith = { _, _, collisionSide ->
                if(collisionSide == CollisionSide.OnLeft)
                    goRight = true
                else if(collisionSide == CollisionSide.OnRight)
                    goRight = false
            }

            entity.second.onPlayerCollision = { thisEntity, player, side ->
                if(side == CollisionSide.OnUp) {
                    thisEntity[LifeComponent::class.java].removeLife(1)
                }
                else {
                    player[LifeComponent::class.java].removeLife(1)
                }
            }

            return entity.first
        }

        fun createTurtleEnemy(game: MtrGame, entityEvent: EntityEvent, pos: Vector2): Entity {
            val entity = createEnemy(EnemyType.Turtle, game.getGameTexture(Gdx.files.internal("game/enemy/turtle/green/walk_0.png")), Rectangle(pos.x, pos.y, 48f, 98f), 5)

            var goRight = false

            entity.first += UpdateComponent(entity.first, { _, e, _ ->
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

            var state = 0

            val lifeComp = LifeComponent(entity.first, 2, null, { entity, hp ->
               if(hp < 2) {
                   state = 1
                   renderMapper[entity].texture = game.getGameTexture(Gdx.files.internal("game/enemy/turtle/green/shell_front.png"))
                   transformMapper[entity].rectangle.setSize(48f, 48f)
                   physicsMapper[entity].moveSpeed = 10
               }
            })
            entity.first += lifeComp

            physicsMapper[entity.first].onCollisionWith = { _, collisionEntity, collisionSide ->
                if(state == 1 && collisionEntity.flags == EntityType.Enemy.flag) {
                    if(collisionEntity.getComponent(LifeComponent::class.java) != null) {
                        entityEvent.onEntityRemoved?.invoke(collisionEntity)
                    }
                }
                else {
                    if(collisionSide == CollisionSide.OnLeft)
                        goRight = true
                    else if(collisionSide == CollisionSide.OnRight)
                        goRight = false
                }
            }

            entity.second.onPlayerCollision = { _, player, side ->
                if(side == CollisionSide.OnUp) {
                    if(state == 0) {
                        lifeComp.removeLife(1)
                    }
                    else {
                        physicsMapper[player].jumpData?.forceJumping = true
                    }
                }
                else {
                    player[LifeComponent::class.java].removeLife(1)
                }
            }

            return entity.first
        }
    }
}