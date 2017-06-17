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
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import ktx.ashley.mapperFor
import ktx.collections.gdxArrayOf

/**
* Created by Catvert on 03/06/17.
*/

/**
 * Ce factory permet de créer une entité spécifique
 */
class EntityFactory(private val game: MtrGame) {
    enum class EntityType(val flag: Int) {
        Sprite(0), PhysicsSprite(1), Player(2), Enemy(3)
    }

    private val renderMapper = mapperFor<RenderComponent>()
    private val physicsMapper = mapperFor<PhysicsComponent>()
    private val transformMapper = mapperFor<TransformComponent>()

    /**
     * Permet de copier une entité
     */
    fun copyEntity(copy: Entity, entityEvent: EntityEvent): Entity {
        val transformComp = transformMapper[copy]
        val renderComp = renderMapper[copy]

        when(EntityType.values().first { it.flag == copy.flags }) {
            EntityFactory.EntityType.Sprite -> {
                return createSprite(Rectangle(transformComp.rectangle), renderComp.copy())
            }
            EntityFactory.EntityType.PhysicsSprite -> {
                return createPhysicsSprite(Rectangle(transformComp.rectangle), renderComp.copy(), physicsMapper[copy].copy())
            }
            EntityFactory.EntityType.Player -> {
                return createPlayer(game, Vector2(transformComp.rectangle.x, transformComp.rectangle.y))
            }
            EntityFactory.EntityType.Enemy -> {
                return createEnemyWithType(game, mapperFor<EnemyComponent>()[copy].enemyType, entityEvent, Vector2(transformComp.rectangle.x, transformComp.rectangle.y))
            }
        }
    }

    /**
     * Permet de créer un sprite
     */
    fun createSprite(rectangle: Rectangle, renderComponent: RenderComponent): Entity {
        val entity = game.engine.createEntity()
        entity += TransformComponent(rectangle)
        entity += renderComponent

        entity.flags = EntityType.Sprite.flag

        return entity
    }

    /**
     * Permet de créer un sprite ayant des propriétés physique
     */
    fun createPhysicsSprite(rectangle: Rectangle, renderComponent: RenderComponent, physComp: PhysicsComponent): Entity {
        val entity = createSprite(rectangle, renderComponent)
        entity.flags = EntityType.PhysicsSprite.flag
        entity += physComp

        return entity
    }

    /**
     * Permet de créer le joueur
     */
    fun createPlayer(game: MtrGame, pos: Vector2): Entity {
        val entity = createPhysicsSprite(Rectangle(pos.x, pos.y, 0f, 0f),
                RenderComponent(
                        listOf(
                                game.getSpriteSheetTexture("alienGreen", "alienGreen_stand"),
                                game.getSpriteSheetTexture("alienGreen", "alienGreen_jump")),
                        listOf(
                                game.getAnimation("alienGreen_walk", 0.3f)), resizeMode = ResizeMode.FIXED_SIZE),
                PhysicsComponent(false, 15, true))
        entity.flags = EntityType.Player.flag

        transformMapper[entity].fixedSizeEditor = true

        val renderComp = renderMapper[entity]
        val stand = renderComp.textureInfoList[0].texture
        renderComp.fixedResize = Vector2(stand.regionWidth.toFloat(), stand.regionHeight.toFloat())

        renderComp.renderLayer = 1

        val physicsComp = physicsMapper[entity]
        physicsComp.jumpData = JumpData(300)

        var lastState = 0

        entity += UpdateComponent({ _, e, _ ->
            val renderComp = renderMapper[e]
            val physics = physicsMapper[e]

            var state: Int = 0

            if (Gdx.input.isKeyPressed(Input.Keys.D)) {
                renderComp.flipX = false
                physics.nextActions += NextActions.GO_RIGHT
                state = 1
            }
            if (Gdx.input.isKeyPressed(Input.Keys.Q)) {
                renderComp.flipX = true
                physics.nextActions += NextActions.GO_LEFT
                state = 1
            }
            if (Gdx.input.isKeyPressed(Input.Keys.Z)) {
                physics.nextActions += NextActions.GO_UP
                state = 1
            }
            if (Gdx.input.isKeyPressed(Input.Keys.S)) {
                physics.nextActions += NextActions.GO_DOWN
                state = 1
            }

            if (Gdx.input.isKeyPressed(Input.Keys.SPACE)) {
                physics.nextActions += NextActions.JUMP
            }

            if(!physics.isOnGround)
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

    /**
     * Permet de créer un ennemi (méthode utilisée par les sous-ennemis)
     */
    private fun createEnemy(enemyType: EnemyType, renderComponent: RenderComponent, rect: Rectangle, isStatic: Boolean, moveSpeed: Int, fixedSizeEditor: Boolean = true): Pair<Entity, EnemyComponent> {
        val entity = createPhysicsSprite(rect, renderComponent, PhysicsComponent(isStatic, moveSpeed, true))

        transformMapper[entity].fixedSizeEditor = fixedSizeEditor

        entity.flags = EntityType.Enemy.flag

        val enemyComp = EnemyComponent(enemyType)
        entity += enemyComp

        return Pair(entity, enemyComp)
    }

    /**
     * Permet de créer un ennemi spécifique
     */
    fun createEnemyWithType(game: MtrGame, enemyType: EnemyType, entityEvent: EntityEvent, pos: Vector2): Entity {
        when (enemyType) {
            EnemyType.Spinner -> return createSpinner(game, pos)
            EnemyType.Spider -> return createSpider(game, entityEvent, pos)
        }
    }

    /**
     * Permet de créer l'ennemi spinner
     */
    fun createSpinner(game: MtrGame, pos: Vector2): Entity {
        val entity = createEnemy(EnemyType.Spinner, RenderComponent(
                listOf(
                        game.getSpriteSheetTexture("enemies", "spinner")),
                listOf(game.createAnimationFromRegions(gdxArrayOf(game.getSpriteSheetTexture("enemies", "spinner").texture, game.getSpriteSheetTexture("enemies", "spinner_spin").texture), 0.1f)), useAnimation = true, resizeMode = ResizeMode.ACTUAL_REGION), Rectangle(pos.x, pos.y, 0f, 0f), true, 0)

        entity.second.onPlayerCollision = { _, player, _ ->
            player[LifeComponent::class.java].removeLife(1)
        }

        return entity.first
    }

    /**
     * Permet de créer l'ennemi spider
     */
    fun createSpider(game: MtrGame, entityEvent: EntityEvent, pos: Vector2): Entity {
        val entity = createEnemy(EnemyType.Spider, RenderComponent(
                listOf(game.getSpriteSheetTexture("enemies", "spider")),
                listOf(game.getAnimation("spider_walk", 0.3f)), useAnimation = true, resizeMode = ResizeMode.ACTUAL_REGION), Rectangle(pos.x, pos.y, 0f, 0f), false, 10)

        entity.second.onPlayerCollision = { thisEntity, player, side ->
            if(side == CollisionSide.OnUp)
                entityEvent.onEntityRemoved?.invoke(thisEntity)
            else
                player[LifeComponent::class.java].removeLife(1)
        }

        var goRight = false

        entity.first += UpdateComponent({ _, entity, _ ->
            val phys = physicsMapper[entity]
            val render = renderMapper[entity]

            render.flipX = goRight

            if(!goRight)
                phys.nextActions +=NextActions.GO_LEFT
            else
                phys.nextActions += NextActions.GO_RIGHT
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