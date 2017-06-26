package be.catvert.plateformcreator.ecs

import be.catvert.plateformcreator.MtrGame
import be.catvert.plateformcreator.ecs.components.*
import be.catvert.plateformcreator.get
import be.catvert.plateformcreator.plusAssign
import be.catvert.plateformcreator.scenes.EndLevelScene
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.signals.Listener
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import ktx.ashley.mapperFor
import ktx.collections.gdxArrayOf
import org.lwjgl.util.Point

/**
 * Created by Catvert on 03/06/17.
 */

/**
 * Ce factory permet de créer une entité spécifique
 */
class EntityFactory(private val game: MtrGame) {
    enum class EntityType(val flag: Int) {
        Sprite(0),
        PhysicsSprite(1),
        Player(2),
        Enemy(3),
        Special(4);
    }

    private val renderMapper = mapperFor<RenderComponent>()
    private val physicsMapper = mapperFor<PhysicsComponent>()
    private val transformMapper = mapperFor<TransformComponent>()
    /**
     * Permet de copier une entité selon son flag
     */
    fun copyEntity(copy: Entity, entityEvent: EntityEvent): Entity {
        val transformComp = transformMapper[copy]
        val renderComp = renderMapper[copy]

        val type = EntityType.values().first { it.flag == copy.flags }
        when (type) {
            EntityFactory.EntityType.Sprite -> {
                return createSprite(transformComp.copy(), renderComp.copy())
            }
            EntityFactory.EntityType.PhysicsSprite -> {
                return createPhysicsSprite(transformComp.copy(), renderComp.copy(), physicsMapper[copy].copy())
            }
            EntityFactory.EntityType.Player -> {
                return createPlayer(entityEvent, transformComp.position)
            }
            EntityFactory.EntityType.Enemy -> {
                return createEnemyWithType(mapperFor<EnemyComponent>()[copy].enemyType, entityEvent, transformComp.position)
            }
            EntityFactory.EntityType.Special -> {
                return createSpecialWithType(mapperFor<SpecialComponent>()[copy].specialType, entityEvent, transformComp.position)
            }
        }
    }

    /**
     * Permet de créer un sprite
     */
    fun createSprite(transformComponent: TransformComponent, renderComponent: RenderComponent): Entity {
        val entity = game.engine.createEntity()
        entity += transformComponent
        entity += renderComponent

        entity.flags = EntityType.Sprite.flag

        return entity
    }

    /**
     * Permet de créer un sprite ayant des propriétés physique
     */
    fun createPhysicsSprite(transformComponent: TransformComponent, renderComponent: RenderComponent, physComp: PhysicsComponent): Entity {
        val entity = createSprite(transformComponent, renderComponent)
        entity.flags = EntityType.PhysicsSprite.flag

        entity += physComp

        if (!physComp.isStatic)
            renderComponent.renderLayer = Layer.LAYER_MOVABLE_ENT

        return entity
    }

    /**
     * Permet de créer le joueur
     */
    fun createPlayer(entityEvent: EntityEvent, pos: Point): Entity {
        val entity = createPhysicsSprite(
                TransformComponent(Rectangle(pos.x.toFloat(), pos.y.toFloat(), 0f, 0f)),
                renderComponent { textures, animations ->
                    textures += game.getSpriteSheetTexture("alienGreen", "alienGreen_stand")
                    textures += game.getSpriteSheetTexture("alienGreen", "alienGreen_jump")

                    animations += game.getAnimation("alienGreen_walk", 0.3f)

                    resizeMode = ResizeMode.FIXED_SIZE
                },
                physicsComponent(false, { moveSpeed = 15; movementType = MovementType.SMOOTH; jumpData = JumpData(300) }))


        entity.flags = EntityType.Player.flag

        transformMapper[entity].fixedSizeEditor = true

        val renderComp = renderMapper[entity]
        val stand = renderComp.textureInfoList[0].texture
        renderComp.fixedResize = Vector2(stand.regionWidth.toFloat(), stand.regionHeight.toFloat())

        var lastState = 0

        entity += updateComponent {
            Listener<UpdateListener>({ _, (_, e, _) ->
                @Suppress("NAME_SHADOWING")
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

                if (!physics.isOnGround)
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
        }

        entity += lifeComponent(entity, 1, null, {
            // remove life
            Listener<LifeListener>({ _, _ ->
                if (hp == 0) {
                    game.setScene(EndLevelScene(game, entityEvent.levelFile, false))
                }
            })
        })

        return entity
    }

    /**
     * Permet de créer un ennemi (méthode utilisée par les sous-ennemis)
     */
    private fun createEnemy(enemyComponent: EnemyComponent, transformComponent: TransformComponent, renderComponent: RenderComponent, physicsComponent: PhysicsComponent, fixedSizeEditor: Boolean = true): Entity {
        val entity = createPhysicsSprite(transformComponent, renderComponent, physicsComponent)

        transformMapper[entity].fixedSizeEditor = fixedSizeEditor

        entity.flags = EntityType.Enemy.flag

        entity += enemyComponent

        return entity
    }

    /**
     * Permet de créer un ennemi spécifique
     */
    fun createEnemyWithType(enemyType: EnemyType, entityEvent: EntityEvent, pos: Point): Entity = when (enemyType) {
        EnemyType.Spinner -> createSpinnerEnemy(pos)
        EnemyType.Spider -> createSpiderEnemy(entityEvent, pos)
        EnemyType.SnakeSlime -> createSnakeSlime(pos)
    }
    /**/

    /**
     * Permet de créer l'ennemi spinner
     */
    private fun createSpinnerEnemy(pos: Point): Entity {
        val entity = createEnemy(
                enemyComponent(EnemyType.Spinner, {
                    Listener({ _, collision ->
                        collision.collideEntity[LifeComponent::class.java].removeLife(1)
                    })
                }),
                TransformComponent(Rectangle(pos.x.toFloat(), pos.y.toFloat(), 0f, 0f)),
                renderComponent { textures, animations ->
                    textures += game.getSpriteSheetTexture("enemies", "spinner")
                    animations += game.createAnimationFromRegions(gdxArrayOf(
                            game.getSpriteSheetTexture("enemies", "spinner").texture,
                            game.getSpriteSheetTexture("enemies", "spinner_spin").texture), 0.1f)
                    useAnimation = true
                    resizeMode = ResizeMode.ACTUAL_REGION
                },
                physicsComponent(true, {}))
        return entity
    }

    /**
     * Permet de créer l'ennemi spider
     */
    private fun createSpiderEnemy(entityEvent: EntityEvent, pos: Point): Entity {
        val entity = createEnemy(
                enemyComponent(EnemyType.Spider, {
                    Listener({ _, (entity, collideEntity, side) ->
                        if (side == CollisionSide.OnUp)
                            entityEvent.onEntityRemoved?.invoke(entity)
                        else
                            collideEntity[LifeComponent::class.java].removeLife(1)
                    })
                }),
                TransformComponent(Rectangle(pos.x.toFloat(), pos.y.toFloat(), 0f, 0f)),
                renderComponent { textures, animations ->
                    textures += game.getSpriteSheetTexture("enemies", "spider")
                    animations += game.getAnimation("spider_walk", 0.3f)
                    resizeMode = ResizeMode.ACTUAL_REGION
                    useAnimation = true
                }
                , physicsComponent(false, { moveSpeed = 10 }))

        var goRight = false

        entity += updateComponent {
            Listener<UpdateListener>({ _, (_, e, _) ->
                val phys = physicsMapper[e]
                val render = renderMapper[e]

                render.flipX = goRight

                if (!goRight)
                    phys.nextActions += NextActions.GO_LEFT
                else
                    phys.nextActions += NextActions.GO_RIGHT
            })
        }

        physicsMapper[entity].onCollisionWith.add { _, collision ->
            if (collision.side == CollisionSide.OnLeft)
                goRight = true
            else if (collision.side == CollisionSide.OnRight)
                goRight = false
        }

        return entity
    }

    /**
     * Permet de créer l'ennemi snake slime
     */
    private fun createSnakeSlime(pos: Point): Entity {
        val entity = createEnemy(
                enemyComponent(EnemyType.SnakeSlime, {
                    Listener<CollisionListener>({ _, collision -> collision.collideEntity[LifeComponent::class.java].removeLife(1) })
                }),
                TransformComponent(Rectangle(pos.x.toFloat(), pos.y.toFloat(), 0f, 0f)),
                renderComponent { textures, animations ->
                    textures += game.getSpriteSheetTexture("enemies", "snakeSlime")
                    animations += game.createAnimationFromRegions(gdxArrayOf(
                            game.getSpriteSheetTexture("enemies", "snakeSlime").texture,
                            game.getSpriteSheetTexture("enemies", "snakeSlime_ani").texture), 0.3f)
                    resizeMode = ResizeMode.ACTUAL_REGION
                    useAnimation = true
                }, physicsComponent(true, {}))

        return entity
    }

    /**
     * Permet de créer une entité spécial (méthode utilisée par les sous entités spéciaux)
     */
    private fun createSpecial(specialComponent: SpecialComponent, transformComponent: TransformComponent, physicsComponent: PhysicsComponent, fixedSizeEditor: Boolean = true): Entity {
        val entity = game.engine.createEntity()

        entity += transformComponent
        entity += physicsComponent

        transformMapper[entity].fixedSizeEditor = fixedSizeEditor

        entity.flags = EntityType.Special.flag

        entity += specialComponent

        return entity
    }

    /**
     * Permet de créer une entité spécial spécifique
     */
    fun createSpecialWithType(specialType: SpecialType, entityEvent: EntityEvent, pos: Point): Entity = when (specialType) {
        SpecialType.EXIT_LEVEL -> createSpecialExitLevel(game, entityEvent, pos)
    }

    private fun createSpecialExitLevel(game: MtrGame, entityEvent: EntityEvent, pos: Point): Entity {
        val entity = createSpecial(
                SpecialComponent(SpecialType.EXIT_LEVEL),
                TransformComponent(Rectangle(pos.x.toFloat(), pos.y.toFloat(), 20f, 20f)),
                physicsComponent(true, {
                    maskCollision = MaskCollision.SENSOR
                }, {
                    Listener<CollisionListener>({ _, collision ->
                        if (collision.collideEntity.flags == EntityFactory.EntityType.Player.flag) {
                            game.setScene(EndLevelScene(game, entityEvent.levelFile, true))
                        }
                    })
                }))

        return entity
    }
}