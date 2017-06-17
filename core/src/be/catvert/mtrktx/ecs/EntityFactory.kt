package be.catvert.mtrktx.ecs

import be.catvert.mtrktx.MtrGame
import be.catvert.mtrktx.ecs.components.*
import be.catvert.mtrktx.ecs.components.RenderComponent
import be.catvert.mtrktx.get
import be.catvert.mtrktx.plusAssign
import be.catvert.mtrktx.scenes.MainMenuScene
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.LifecycleListener
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import ktx.ashley.mapperFor
import ktx.collections.gdxArrayOf
import kotlin.coroutines.experimental.EmptyCoroutineContext.plus

/**
 * Created by Catvert on 03/06/17.
 */

class EntityFactory(private val game: MtrGame) {
    enum class EntityType(val flag: Int) {
        Sprite(0), PhysicsSprite(1), Player(2), Enemy(3)
    }

    private val renderMapper = mapperFor<RenderComponent>()
    private val physicsMapper = mapperFor<PhysicsComponent>()
    private val transformMapper = mapperFor<TransformComponent>()

    fun createSprite(rectangle: Rectangle, renderComponent: RenderComponent): Entity {
        val entity = game.engine.createEntity()
        entity += TransformComponent(rectangle)
        entity += renderComponent

        entity.flags = EntityType.Sprite.flag

        return entity
    }

    fun createPhysicsSprite(rectangle: Rectangle, renderComponent: RenderComponent, physComp: PhysicsComponent): Entity {
        val entity = createSprite(rectangle, renderComponent)
        entity.flags = EntityType.PhysicsSprite.flag
        entity += physComp

        return entity
    }

    fun createPlayer(game: MtrGame, pos: Vector2): Entity {
        val entity = createPhysicsSprite(Rectangle(pos.x, pos.y, 0f, 0f),
                RenderComponent(
                        listOf(
                                game.getSpriteSheetTexture("alienGreen", "alienGreen_stand"),
                                game.getSpriteSheetTexture("alienGreen", "alienGreen_jump")),
                        listOf(
                                game.getAnimation("alienGreen_walk", 0.3f)), autoResizeWithAtlas = true),
                PhysicsComponent(false, 15, true))
        entity.flags = EntityType.Player.flag

        transformMapper[entity].fixedSizeEditor = true

        val renderComp = renderMapper[entity]
        renderComp.renderLayer = 1
        val physicsComp = physicsMapper[entity]
        physicsComp.jumpData = JumpData(250)

        var lastState = 0

        entity += UpdateComponent(entity, { _, e, _ ->
            val renderComp = renderMapper[e]
            val physics = physicsMapper[e]

            var state: Int = 0

            if (Gdx.input.isKeyPressed(Input.Keys.D)) {
                renderComp.flipX = false
                physics.nextActions += PhysicsComponent.NextActions.GO_RIGHT
                state = 1
            }
            if (Gdx.input.isKeyPressed(Input.Keys.Q)) {
                renderComp.flipX = true
                physics.nextActions += PhysicsComponent.NextActions.GO_LEFT
                state = 1
            }
            if (Gdx.input.isKeyPressed(Input.Keys.Z)) {
                physics.nextActions += PhysicsComponent.NextActions.GO_UP
                state = 1
            }
            if (Gdx.input.isKeyPressed(Input.Keys.S)) {
                physics.nextActions += PhysicsComponent.NextActions.GO_DOWN
                state = 1
            }

            if (Gdx.input.isKeyPressed(Input.Keys.SPACE)) {
                physics.nextActions += PhysicsComponent.NextActions.JUMP
            }

            if (physics.jumpData?.isJumping ?: false)
                state = 2

            if (state != lastState) {
                when (state) {
                    0 -> {
                        renderComp.actualTextureInfoIndex = 0
                        renderComp.useAnimation = false
                    }
                    1 -> {
                        renderComp.useAnimation = true
                    }
                    2 -> {
                        renderComp.actualTextureInfoIndex = 1
                    }
                }
            }

            lastState = state
        })

        entity += LifeComponent(entity, 1, { _, _ -> }, { _, hp ->
            // remove life event
            if (hp == 0) {
                game.setScene(MainMenuScene(game))
            }
        })

        return entity
    }

    private fun createEnemy(enemyType: EnemyType, renderComponent: RenderComponent, rect: Rectangle, moveSpeed: Int): Pair<Entity, EnemyComponent> {
        val entity = createPhysicsSprite(rect, renderComponent, PhysicsComponent(false, moveSpeed, true))

        entity.flags = EntityType.Enemy.flag

        val enemyComp = EnemyComponent(enemyType)
        entity += enemyComp

        return Pair(entity, enemyComp)
    }

    fun createEnemyWithType(game: MtrGame, enemyType: EnemyType, entityEvent: EntityEvent, pos: Vector2): Entity {
        when (enemyType) {
            EnemyType.Spinner -> return createSpinner(game, pos)
            EnemyType.Spider -> return createSpider(game, entityEvent, pos)
        }
    }

    fun createSpinner(game: MtrGame, pos: Vector2): Entity {
        val entity = createEnemy(EnemyType.Spinner, RenderComponent(
                listOf(
                        game.getSpriteSheetTexture("enemies", "spinner")),
                listOf(game.createAnimationFromRegions(gdxArrayOf(game.getSpriteSheetTexture("enemies", "spinner").texture, game.getSpriteSheetTexture("enemies", "spinner_spin").texture), 0.1f)), useAnimation = true, autoResizeWithAtlas = true), Rectangle(pos.x, pos.y, 0f, 0f), 0)


        physicsMapper[entity.first].gravity = false

        entity.second.onPlayerCollision = { _, player, _ ->
            player[LifeComponent::class.java].removeLife(1)
        }

        return entity.first
    }

    fun createSpider(game: MtrGame, entityEvent: EntityEvent, pos: Vector2): Entity {
        val entity = createEnemy(EnemyType.Spider, RenderComponent(
                listOf(game.getSpriteSheetTexture("enemies", "spider")),
                listOf(game.getAnimation("spider_walk", 0.3f)), useAnimation = true, autoResizeWithAtlas = true), Rectangle(pos.x, pos.y, 0f, 0f), 10)

        entity.second.onPlayerCollision = { thisEntity, player, side ->
            if(side == CollisionSide.OnUp)
                entityEvent.onEntityRemoved?.invoke(thisEntity)
            else
                player[LifeComponent::class.java].removeLife(1)
        }

        var goRight = false

        entity.first += UpdateComponent(entity.first, { _, entity, _ ->
            val phys = physicsMapper[entity]
            val render = renderMapper[entity]

            render.flipX = goRight

            if(!goRight)
                phys.nextActions += PhysicsComponent.NextActions.GO_LEFT
            else
                phys.nextActions += PhysicsComponent.NextActions.GO_RIGHT
        } )

        physicsMapper[entity.first].onCollisionWith = { thisEntity, collisionEntity, side ->
            if(side == CollisionSide.OnLeft)
                goRight = true
            else if(side == CollisionSide.OnRight)
                goRight = false
        }

        return entity.first
    }
}