package be.catvert.plateformcreator.ecs

import be.catvert.plateformcreator.*
import be.catvert.plateformcreator.ecs.components.*
import be.catvert.plateformcreator.scenes.EndLevelScene
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.signals.Listener
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import ktx.ashley.mapperFor
import ktx.collections.gdxArrayOf

/**
 * Created by Catvert on 03/06/17.
 */

/**
 * Cette classe permet de créer une entité spécifique
 * Le compagnion object permet de créer les entités les plus basiques
 * Pour pouvoir créer une entité plus spécifique(joueur, ennemis..), il faut instancier ce factory
 * @property game L'objet du jeu
 * @property levelFile Le fichier utilisé pour charger le niveau
 */
class EntityFactory(private val game: MtrGame, private val levelFile: FileHandle) {
    /**
     * Représente le type de l'entité
     * Le type est sauvegardé dans le flag de l'entité
     */
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
     * @param copy L'entité à copier
     */
    fun copyEntity(copy: Entity): Entity {
        val transformComp = transformMapper[copy]
        val renderComp = renderMapper[copy]

        when (copy.getType()) {
            EntityFactory.EntityType.Sprite -> {
                return createSprite(transformComp.copy(), renderComp.copy())
            }
            EntityFactory.EntityType.PhysicsSprite -> {
                return createPhysicsSprite(transformComp.copy(), renderComp.copy(), physicsMapper[copy].copy())
            }
            EntityFactory.EntityType.Player -> {
                return createPlayer(transformComp.position())
            }
            EntityFactory.EntityType.Enemy -> {
                return createEnemyWithType(mapperFor<EnemyComponent>()[copy].enemyType, transformComp.position())
            }
            EntityFactory.EntityType.Special -> {
                return createSpecialWithType(mapperFor<SpecialComponent>()[copy].specialType, transformComp.position())
            }
        }
    }

    companion object {
        /**
         * Permet de créer un sprite
         */
        fun createSprite(transformComponent: TransformComponent, renderComponent: RenderComponent) = entity {
            this += transformComponent
            this += renderComponent
            setType(EntityType.Sprite)
        }

        /**
         * Permet de créer un sprite ayant des propriétés physique
         */
        fun createPhysicsSprite(transformComponent: TransformComponent, renderComponent: RenderComponent, physicsComponent: PhysicsComponent): Entity {
            val entity = createSprite(transformComponent, renderComponent)
            entity.setType(EntityType.PhysicsSprite)

            entity += physicsComponent

            return entity
        }
    }

    /**
     * Permet de créer le joueur
     * @param pos La position de l'entité
     */
    fun createPlayer(pos: Point): Entity {
        val entity = createPhysicsSprite(
                TransformComponent(Rectangle(pos.x.toFloat(), pos.y.toFloat(), 0f, 0f), true),
                renderComponent { textures, animations ->
                    textures += game.getSpriteSheetTexture("alienGreen", "alienGreen_stand")
                    textures += game.getSpriteSheetTexture("alienGreen", "alienGreen_jump")

                    animations += game.getAnimation("alienGreen_walk", 0.3f)

                    fixedResize = Vector2(textures[0].texture.regionWidth.toFloat(), textures[0].texture.regionHeight.toFloat())
                    resizeMode = ResizeMode.FIXED_SIZE
                    renderLayer = Layer.LAYER_MOVABLE_ENT
                },
                physicsComponent(false, { moveSpeed = 15; movementType = MovementType.SMOOTH; jumpData = JumpData(300) }))

        entity.setType(EntityType.Player)

        var lastState = 0

        val jumpSound = game.getGameSound(Gdx.files.internal("sounds/player/jump.ogg"))

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
                    if (physics.jumpData!!.startJumping)
                        jumpSound.play()
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
                    EntityEvent.onEndLevel.invoke(false)
                }
            })
        })

        return entity
    }

    /**
     * Permet de créer un ennemi (méthode utilisée par les sous-ennemis)
     * @param fixedSizeEditor Spécifie si oui ou non l'entité peut-être redimensionner dans l'éditeur
     */
    private fun createEnemy(enemyComponent: EnemyComponent, transformComponent: TransformComponent, renderComponent: RenderComponent, physicsComponent: PhysicsComponent, fixedSizeEditor: Boolean = true): Entity {
        val entity = createPhysicsSprite(transformComponent, renderComponent, physicsComponent)

        transformMapper[entity].fixedSizeEditor = fixedSizeEditor
        renderMapper[entity].renderLayer = Layer.LAYER_MOVABLE_ENT

        entity.setType(EntityType.Enemy)

        entity += enemyComponent

        return entity
    }

    /**
     * Permet de créer un ennemi spécifique
     * @param enemyType Le type de l'ennemi
     * @param pos La position de l'entité
     */
    fun createEnemyWithType(enemyType: EnemyType, pos: Point): Entity = when (enemyType) {
        EnemyType.Spinner -> createSpinnerEnemy(pos)
        EnemyType.Spider -> createSpiderEnemy(pos)
        EnemyType.SnakeSlime -> createSnakeSlime(pos)
    }

    /**
     * Permet de créer l'ennemi spinner
     * @param pos La position de l'entité
     */
    private fun createSpinnerEnemy(pos: Point) = createEnemy(
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

    /**
     * Permet de créer l'ennemi spider
     * @param pos La position de l'entité
     */
    private fun createSpiderEnemy(pos: Point): Entity {
        var goRight = false

        val dieSound = game.getGameSound(Gdx.files.internal("sounds/enemy/die.wav"))

        val entity = createEnemy(
                enemyComponent(EnemyType.Spider, {
                    Listener({ _, (entity, collideEntity, side) ->
                        if (side == CollisionSide.OnUp) {
                            EntityEvent.onAddScore.invoke(2)
                            EntityEvent.onEntityRemoved.invoke(entity) // remove this entity
                            dieSound.play()
                        } else
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
                , physicsComponent(false, { moveSpeed = 10 }, {
            Listener<CollisionListener>({ _, collision ->
                if (collision.side == CollisionSide.OnLeft)
                    goRight = true
                else if (collision.side == CollisionSide.OnRight)
                    goRight = false
            })
        }))

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

        return entity
    }

    /**
     * Permet de créer l'ennemi snake slime
     * @param pos La position de l'entité
     */
    private fun createSnakeSlime(pos: Point) = createEnemy(
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

    /**
     * Permet de créer une entité spécial (méthode utilisée par les sous entités spéciaux)
     * @param fixedSizeEditor Spécifie si oui ou non l'entité peut-être redimensionner dans l'éditeur
     */
    private fun createSpecial(specialComponent: SpecialComponent, transformComponent: TransformComponent, renderComponent: RenderComponent? = null, physicsComponent: PhysicsComponent, fixedSizeEditor: Boolean = true) = entity {
        this += transformComponent
        this += physicsComponent

        if (renderComponent != null)
            this += renderComponent

        transformMapper[this].fixedSizeEditor = fixedSizeEditor

        setType(EntityType.Special)

        this += specialComponent
    }

    /**
     * Permet de créer une entité spécial spécifique
     * @param specialType Le type spécial de l'entité
     * @param pos La position de l'entité
     */
    fun createSpecialWithType(specialType: SpecialType, pos: Point) = when (specialType) {
        SpecialType.ExitLevel -> createSpecialExitLevel(pos)
        SpecialType.BlockEnemy -> createSpecialBlockEnemy(pos)
        SpecialType.GoldCoin -> createSpecialGoldCoin(pos)
    }

    /**
     * Permet de créer une entité spécial, qui permet de sortir du niveau
     * @param pos La position de l'entité
     */
    private fun createSpecialExitLevel(pos: Point) = createSpecial(
            SpecialComponent(SpecialType.ExitLevel),
            TransformComponent(Rectangle(pos.x.toFloat(), pos.y.toFloat(), 20f, 20f)), null,
            physicsComponent(true, {
                maskCollision = MaskCollision.ONLY_PLAYER
                isSensor = true
            }, {
                Listener<CollisionListener>({ _, collision ->
                    if (collision.collideEntity isType EntityFactory.EntityType.Player) {
                        EntityEvent.onEndLevel.invoke(true)
                    }
                })
            }))

    /**
     * Permet de créer une entité spécial, qui permet de bloquer les ennemis avançant vers cette direction par un bloc invisible
     * @param pos La position de l'entité
     */
    private fun createSpecialBlockEnemy(pos: Point) = createSpecial(
            SpecialComponent(SpecialType.BlockEnemy),
            TransformComponent(Rectangle(pos.x.toFloat(), pos.y.toFloat(), 20f, 20f)), null,
            physicsComponent(true, {
                maskCollision = MaskCollision.ONLY_ENEMY
            }))

    /**
     * Permet de créer une entité spécial, la pièce d'or.
     * @param pos La position de l'entité
     */
    private fun createSpecialGoldCoin(pos: Point): Entity {
        val coinSound = game.getGameSound(Gdx.files.internal("sounds/coin.wav"))

        val entity = createSpecial(
                SpecialComponent(SpecialType.GoldCoin),
                TransformComponent(Rectangle(pos.x.toFloat(), pos.y.toFloat(), 50f, 50f)),
                renderComponent({ textures, _ ->
                    textures += game.getSpriteSheetTexture("spritesheet_jumper", "coin_gold")
                }),
                physicsComponent(true, {
                    isSensor = true
                    maskCollision = MaskCollision.ONLY_PLAYER
                }, {
                    Listener<CollisionListener>({ _, (entity, collideEntity) ->
                        if (collideEntity isType EntityType.Player) {
                            EntityEvent.onAddScore.invoke(1)
                            EntityEvent.onEntityRemoved.invoke(entity)
                            coinSound.play()
                        }
                    })
                }))

        return entity
    }
}