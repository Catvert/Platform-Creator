package be.catvert.pc.factories

import be.catvert.pc.*
import be.catvert.pc.actions.*
import be.catvert.pc.components.basics.SoundComponent
import be.catvert.pc.components.basics.TweenComponent
import be.catvert.pc.components.graphics.AtlasComponent
import be.catvert.pc.components.graphics.AtlasRegion
import be.catvert.pc.components.logics.*
import be.catvert.pc.utility.*
import com.badlogic.gdx.graphics.g2d.Animation

enum class PrefabType {
    All, Kenney, SMC
}

/**
 * Objet permettant la création de prefab préfait
 */
object PrefabSetup {
    fun setupSprite(region: AtlasRegion, sizeBox: Size = Size(50, 50)) = Prefab("sprite", GameObject(Tags.Sprite.tag, box = Rect(Point(), sizeBox), initDefaultState = {
        this += AtlasComponent(0, AtlasComponent.AtlasData("default", region))
    }))

    fun setupPhysicsSprite(region: AtlasRegion, sizeBox: Size = Size(50, 50)) = Prefab("physics sprite", GameObject(Tags.Sprite.tag, box = Rect(Point(), sizeBox), initDefaultState = {
        this += AtlasComponent(0, AtlasComponent.AtlasData("default", region))
        this += PhysicsComponent(true)
    }))
}

enum class PrefabFactory(val type: PrefabType, val prefab: Prefab) {
    Empty(PrefabType.All, Prefab("empty", GameObject(Tags.Empty.tag, box = Rect(size = Size(50, 50))))),

    BlockEnemy(PrefabType.All,
            Prefab("block enemy", GameObject(Tags.Special.tag, box = Rect(size = Size(20, 20)), initDefaultState = {
                this += PhysicsComponent(true, ignoreTags = arrayListOf(Tags.Player.tag))
            }))
    ),
    EndLevel(PrefabType.All,
            Prefab("end level", GameObject(Tags.Special.tag, box = Rect(size = Size(20, 20)), initDefaultState = {
                this += PhysicsComponent(true, sensor = SensorData(true, Tags.Player.tag, LevelAction(LevelAction.LevelActions.SUCCESS_EXIT)))
            }))
    ),
    //region Kenney
    Sprite_Kenney(PrefabType.Kenney, PrefabSetup.setupSprite(Constants.packsKenneyDirPath.child("grassSheet.atlas").toFileWrapper() to "slice03_03")),
    PhysicsSprite_Kenney(PrefabType.Kenney, PrefabSetup.setupPhysicsSprite(Constants.packsKenneyDirPath.child("grassSheet.atlas").toFileWrapper() to "slice03_03")),
    Player_Kenney(PrefabType.Kenney,
            Prefab("player", GameObject("player", box = Rect(size = Size(48, 98)), initDefaultState = {
                val jumpSoundIndex = 0

                val atlas = Constants.packsKenneyDirPath.child("aliens.atlas").toFileWrapper()

                this += AtlasComponent(0,
                        AtlasComponent.AtlasData("stand", atlas to "alienGreen_stand"),
                        AtlasComponent.AtlasData("walk", atlas, "alienGreen_walk", 0.33f),
                        AtlasComponent.AtlasData("jump", atlas to "alienGreen_jump"),
                        AtlasComponent.AtlasData("fall", atlas to "alienGreen_swim_1"))

                this += InputComponent(
                        InputComponent.InputData(GameKeys.GAME_PLAYER_LEFT.key, false, MultiplexerAction(PhysicsAction(PhysicsAction.PhysicsActions.GO_LEFT), RenderAction(RenderAction.RenderActions.FLIP_X))),
                        InputComponent.InputData(GameKeys.GAME_PLAYER_RIGHT.key, false, MultiplexerAction(PhysicsAction(PhysicsAction.PhysicsActions.GO_RIGHT), RenderAction(RenderAction.RenderActions.UNFLIP_X))),
                        InputComponent.InputData(GameKeys.GAME_PLAYER_GOD_UP.key, false, PhysicsAction(PhysicsAction.PhysicsActions.GO_UP)),
                        InputComponent.InputData(GameKeys.GAME_PLAYER_GOD_DOWN.key, false, PhysicsAction(PhysicsAction.PhysicsActions.GO_DOWN)),
                        InputComponent.InputData(GameKeys.GAME_PLAYER_JUMP.key, true, PhysicsAction(PhysicsAction.PhysicsActions.JUMP))
                )

                this += PhysicsComponent(false, 10, MovementType.SMOOTH, jumpHeight = 200).apply {
                    val walkAction = AtlasAction(1)
                    onRightAction = walkAction
                    onLeftAction = walkAction
                    onNothingAction = AtlasAction(0)
                    onJumpAction = MultiplexerAction(SoundAction(jumpSoundIndex), AtlasAction(2))
                    onFallAction = AtlasAction(3)
                }

                this += SoundComponent(SoundComponent.SoundData(Constants.soundsDirPath.child("player/jump.ogg").toFileWrapper()))

                this += LifeComponent(LevelAction(LevelAction.LevelActions.FAIL_EXIT))
            }).apply {
                onOutOfMapAction = LevelAction(LevelAction.LevelActions.FAIL_EXIT)
            })
    ),
    Spider_Kenney(PrefabType.Kenney,
            Prefab("spider", GameObject(Tags.Enemy.tag, box = Rect(size = Size(48, 48)), initDefaultState = {
                this += AtlasComponent(0, AtlasComponent.AtlasData("walk", Constants.packsKenneyDirPath.child("enemies.atlas").toFileWrapper(), "spider_walk", 0.33f))
                this += PhysicsComponent(false, 5, collisionsActions = arrayListOf(
                        CollisionAction(BoxSide.Left).apply { tagAction.action = LifeAction(LifeAction.LifeActions.REMOVE_LP) },
                        CollisionAction(BoxSide.Right).apply { tagAction.action = LifeAction(LifeAction.LifeActions.REMOVE_LP) },
                        CollisionAction(BoxSide.Down).apply { tagAction.action = LifeAction(LifeAction.LifeActions.REMOVE_LP) },
                        CollisionAction(BoxSide.Up, action = LifeAction(LifeAction.LifeActions.REMOVE_LP))
                ))
                this += TweenComponent(TweenFactory.RemoveGOTween())
                this += LifeComponent(TweenAction(0))
                this += MoverComponent(MoverComponent.SimpleMoverOrientation.HORIZONTAL, false).apply { onReverseAction = RenderAction(RenderAction.RenderActions.FLIP_X); onUnReverseAction = RenderAction(RenderAction.RenderActions.UNFLIP_X) }
            }))
    ),
    SnakeSlime_Kenney(PrefabType.Kenney,
            Prefab("snake slime", GameObject(Tags.Enemy.tag, box = Rect(size = Size(35, 120)), initDefaultState = {
                this += AtlasComponent(0, AtlasComponent.AtlasData("default", Constants.packsKenneyDirPath.child("enemies.atlas").toFileWrapper(), "snakeSlime", 0.33f))
                this += PhysicsComponent(true, collisionsActions = arrayListOf(
                        CollisionAction(BoxSide.All).apply { tagAction.action = LifeAction(LifeAction.LifeActions.REMOVE_LP) }
                ))
                this += LifeComponent(RemoveGOAction())
            }))
    ),
    Bee_Kenney(PrefabType.Kenney,
            Prefab("bee", GameObject(Tags.Enemy.tag, box = Rect(size = Size(35, 35)), initDefaultState = {
                this += AtlasComponent(0, AtlasComponent.AtlasData("default", Constants.packsKenneyDirPath.child("enemies.atlas").toFileWrapper() to "bee"))
                this += PhysicsComponent(false, 5, gravity = false, collisionsActions = arrayListOf(
                        CollisionAction(BoxSide.Left).apply { tagAction.action = LifeAction(LifeAction.LifeActions.REMOVE_LP) },
                        CollisionAction(BoxSide.Right).apply { tagAction.action = LifeAction(LifeAction.LifeActions.REMOVE_LP) },
                        CollisionAction(BoxSide.Down).apply { tagAction.action = LifeAction(LifeAction.LifeActions.REMOVE_LP) },
                        CollisionAction(BoxSide.Up, action = LifeAction(LifeAction.LifeActions.REMOVE_LP))
                ))
                this += LifeComponent(RemoveGOAction())
                this += MoverComponent(MoverComponent.SimpleMoverOrientation.HORIZONTAL, false).apply { onReverseAction = RenderAction(RenderAction.RenderActions.FLIP_X); onUnReverseAction = RenderAction(RenderAction.RenderActions.UNFLIP_X) }
            }))
    ),
    GoldCoin_Kenney(PrefabType.Kenney,
            Prefab("gold coin", GameObject(Tags.Special.tag, box = Rect(size = Size(35, 35)), initDefaultState = {
                this += AtlasComponent(0, AtlasComponent.AtlasData("default", Constants.packsKenneyDirPath.child("jumper.atlas").toFileWrapper() to "coin_gold"))
                this += SoundComponent(SoundComponent.SoundData(Constants.soundsDirPath.child("coin.wav").toFileWrapper()))
                this += PhysicsComponent(true, sensor = SensorData(true, sensorIn = MultiplexerAction(SoundAction(0), ScoreAction(1), RemoveGOAction())))
            }))
    ),
    //endregion
    //region SMC
    Sprite_SMC(PrefabType.SMC, PrefabSetup.setupSprite(Constants.packsSMCDirPath.child("blocks.atlas").toFileWrapper() to "blocks/brick/Brick Blue")),
    PhysicsSprite_SMC(PrefabType.SMC, PrefabSetup.setupPhysicsSprite(Constants.packsSMCDirPath.child("blocks.atlas").toFileWrapper() to "blocks/brick/Brick Blue")),
    Player_SMC(PrefabType.SMC,
            Prefab("player", GameObject(Tags.Player.tag, box = Rect(size = Size(48, 98)), initDefaultState = {
                val jumpSoundIndex = 0

                val atlas = Constants.packsSMCDirPath.child("maryo.atlas").toFileWrapper()

                this += AtlasComponent(0,
                        AtlasComponent.AtlasData("stand", atlas to "big/stand_right"),
                        AtlasComponent.AtlasData("walk", atlas, "big/walk_right", 0.33f),
                        AtlasComponent.AtlasData("jump", atlas to "big/jump_right"),
                        AtlasComponent.AtlasData("fall", atlas to "big/fall_right"))

                this += InputComponent(
                        InputComponent.InputData(GameKeys.GAME_PLAYER_LEFT.key, false, MultiplexerAction(PhysicsAction(PhysicsAction.PhysicsActions.GO_LEFT), RenderAction(RenderAction.RenderActions.FLIP_X))),
                        InputComponent.InputData(GameKeys.GAME_PLAYER_RIGHT.key, false, MultiplexerAction(PhysicsAction(PhysicsAction.PhysicsActions.GO_RIGHT), RenderAction(RenderAction.RenderActions.UNFLIP_X))),
                        InputComponent.InputData(GameKeys.GAME_PLAYER_GOD_UP.key, false, PhysicsAction(PhysicsAction.PhysicsActions.GO_UP)),
                        InputComponent.InputData(GameKeys.GAME_PLAYER_GOD_DOWN.key, false, PhysicsAction(PhysicsAction.PhysicsActions.GO_DOWN)),
                        InputComponent.InputData(GameKeys.GAME_PLAYER_JUMP.key, true, PhysicsAction(PhysicsAction.PhysicsActions.JUMP))
                )

                this += PhysicsComponent(false, 10, MovementType.SMOOTH, jumpHeight = 200).apply {
                    val walkAction = AtlasAction(1)
                    onRightAction = walkAction
                    onLeftAction = walkAction
                    onNothingAction = AtlasAction(0)
                    onJumpAction = MultiplexerAction(SoundAction(jumpSoundIndex), AtlasAction(2))
                    onFallAction = AtlasAction(3)
                    collisionsActions.add(CollisionAction())
                }

                this += SoundComponent(SoundComponent.SoundData(Constants.soundsDirPath.child("player/jump.ogg").toFileWrapper()))

                this += LifeComponent(LevelAction(LevelAction.LevelActions.FAIL_EXIT))
            }, otherStates = *arrayOf(GameObjectState("small") {
                val jumpSoundIndex = 0

                val atlas = Constants.packsSMCDirPath.child("maryo.atlas").toFileWrapper()

                this += AtlasComponent(0,
                        AtlasComponent.AtlasData("stand", atlas to "small/stand_right"),
                        AtlasComponent.AtlasData("walk", atlas, "small/walk_right", 0.33f),
                        AtlasComponent.AtlasData("jump", atlas to "small/jump_right"),
                        AtlasComponent.AtlasData("fall", atlas to "small/fall_right"))

                this += InputComponent(
                        InputComponent.InputData(GameKeys.GAME_PLAYER_LEFT.key, false, MultiplexerAction(PhysicsAction(PhysicsAction.PhysicsActions.GO_LEFT), RenderAction(RenderAction.RenderActions.FLIP_X))),
                        InputComponent.InputData(GameKeys.GAME_PLAYER_RIGHT.key, false, MultiplexerAction(PhysicsAction(PhysicsAction.PhysicsActions.GO_RIGHT), RenderAction(RenderAction.RenderActions.UNFLIP_X))),
                        InputComponent.InputData(GameKeys.GAME_PLAYER_GOD_UP.key, false, PhysicsAction(PhysicsAction.PhysicsActions.GO_UP)),
                        InputComponent.InputData(GameKeys.GAME_PLAYER_GOD_DOWN.key, false, PhysicsAction(PhysicsAction.PhysicsActions.GO_DOWN)),
                        InputComponent.InputData(GameKeys.GAME_PLAYER_JUMP.key, true, PhysicsAction(PhysicsAction.PhysicsActions.JUMP))
                )

                this += PhysicsComponent(false, 10, MovementType.SMOOTH, jumpHeight = 150).apply {
                    val walkAction = AtlasAction(1)
                    onRightAction = walkAction
                    onLeftAction = walkAction
                    onNothingAction = AtlasAction(0)
                    onJumpAction = MultiplexerAction(SoundAction(jumpSoundIndex), AtlasAction(2))
                    onFallAction = AtlasAction(3)
                }

                this += SoundComponent(SoundComponent.SoundData(Constants.soundsDirPath.child("player/jump.ogg").toFileWrapper()))

                this += LifeComponent(LevelAction(LevelAction.LevelActions.FAIL_EXIT))
            })).apply {
                onOutOfMapAction = LevelAction(LevelAction.LevelActions.FAIL_EXIT)
            })
    ),
    Furball_SMC(PrefabType.SMC,
            Prefab("furball", GameObject(Tags.Enemy.tag, box = Rect(size = Size(48, 48)), initDefaultState = {
                this += AtlasComponent(0, AtlasComponent.AtlasData("walk", Constants.packsSMCDirPath.child("enemies.atlas").toFileWrapper(), "furball/brown/walk", 0.1f))
                this += PhysicsComponent(false, 5, collisionsActions = arrayListOf(
                        CollisionAction(BoxSide.Left).apply { tagAction.action = LifeAction(LifeAction.LifeActions.REMOVE_LP) },
                        CollisionAction(BoxSide.Right).apply { tagAction.action = LifeAction(LifeAction.LifeActions.REMOVE_LP) },
                        CollisionAction(BoxSide.Down).apply { tagAction.action = LifeAction(LifeAction.LifeActions.REMOVE_LP) },
                        CollisionAction(BoxSide.Up, action = LifeAction(LifeAction.LifeActions.REMOVE_LP))
                ))
                this += TweenComponent(TweenFactory.RemoveGOTween())
                this += LifeComponent(TweenAction(0))
                this += MoverComponent(MoverComponent.SimpleMoverOrientation.HORIZONTAL, false).apply { onReverseAction = RenderAction(RenderAction.RenderActions.FLIP_X); onUnReverseAction = RenderAction(RenderAction.RenderActions.UNFLIP_X) }
            }))
    ),
    Turtle_SMC(PrefabType.SMC,
            Prefab("turtle", GameObject(Tags.Enemy.tag, box = Rect(size = Size(48, 98)), initDefaultState = {
                this += AtlasComponent(0, AtlasComponent.AtlasData("walk", Constants.packsSMCDirPath.child("enemies.atlas").toFileWrapper(), "turtle/green/walk", 0.33f))
                this += PhysicsComponent(false, 5, collisionsActions = arrayListOf(
                        CollisionAction(BoxSide.Left).apply { tagAction.action = LifeAction(LifeAction.LifeActions.REMOVE_LP) },
                        CollisionAction(BoxSide.Right).apply { tagAction.action = LifeAction(LifeAction.LifeActions.REMOVE_LP) },
                        CollisionAction(BoxSide.Down).apply { tagAction.action = LifeAction(LifeAction.LifeActions.REMOVE_LP) },
                        CollisionAction(BoxSide.Up).apply { action = LifeAction(LifeAction.LifeActions.REMOVE_LP) }
                ))
                this += LifeComponent(MultiplexerAction(StateAction(1), ResizeAction(Size(45, 45))))
                this += MoverComponent(MoverComponent.SimpleMoverOrientation.HORIZONTAL, false).apply { onReverseAction = RenderAction(RenderAction.RenderActions.FLIP_X); onUnReverseAction = RenderAction(RenderAction.RenderActions.UNFLIP_X) }
            }, otherStates = *arrayOf(GameObjectState("shell") {
                this += AtlasComponent(0, AtlasComponent.AtlasData("walk", Constants.packsSMCDirPath.child("enemies.atlas").toFileWrapper(), "turtle/green/shell_move", 0.33f))
                this += PhysicsComponent(false, 10, collisionsActions = arrayListOf(
                        CollisionAction(BoxSide.Left).apply { tagAction.action = LifeAction(LifeAction.LifeActions.REMOVE_LP) },
                        CollisionAction(BoxSide.Right).apply { tagAction.action = LifeAction(LifeAction.LifeActions.REMOVE_LP) },
                        CollisionAction(BoxSide.Up).apply { tagAction.action = PhysicsAction(PhysicsAction.PhysicsActions.JUMP) },
                        CollisionAction(BoxSide.Right, Tags.Enemy.tag).apply { tagAction = TagAction(Tags.Enemy.tag, LifeAction(LifeAction.LifeActions.ONE_SHOT)) },
                        CollisionAction(BoxSide.Left, Tags.Enemy.tag).apply { tagAction = TagAction(Tags.Enemy.tag, LifeAction(LifeAction.LifeActions.ONE_SHOT)) }
                ))
                this += MoverComponent(MoverComponent.SimpleMoverOrientation.HORIZONTAL, false)
            })))
    ),
    Eato_SMC(PrefabType.SMC,
            Prefab("eato", GameObject(Tags.Enemy.tag, box = Rect(size = Size(45, 45)), initDefaultState = {
                this += AtlasComponent(0, AtlasComponent.AtlasData("default", Constants.packsSMCDirPath.child("enemies.atlas").toFileWrapper(), "eato/green/eato", 0.15f, Animation.PlayMode.LOOP_PINGPONG))
                this += PhysicsComponent(true, collisionsActions = arrayListOf(
                        CollisionAction(BoxSide.Left).apply { tagAction.action = LifeAction(LifeAction.LifeActions.REMOVE_LP) },
                        CollisionAction(BoxSide.Right).apply { tagAction.action = LifeAction(LifeAction.LifeActions.REMOVE_LP) },
                        CollisionAction(BoxSide.Down).apply { tagAction.action = LifeAction(LifeAction.LifeActions.REMOVE_LP) },
                        CollisionAction(BoxSide.Up).apply { tagAction.action = LifeAction(LifeAction.LifeActions.REMOVE_LP) }
                ))
                this += LifeComponent(RemoveGOAction())
            }))
    ),
    MushroomRed_SMC(PrefabType.SMC,
            Prefab("mushroom red", GameObject(Tags.Special.tag, box = Rect(size = Size(45, 45)), initDefaultState = {
                this += AtlasComponent(0, AtlasComponent.AtlasData("default", Constants.packsSMCDirPath.child("items.atlas").toFileWrapper() to "mushroom_red"))
                this += PhysicsComponent(false, 5, collisionsActions = arrayListOf(
                        CollisionAction(BoxSide.All).apply { action = RemoveGOAction(); tagAction = TagAction(Tags.Player.tag, EmptyAction()) }
                ))
                this += MoverComponent(MoverComponent.SimpleMoverOrientation.HORIZONTAL, false).apply { onReverseAction = RenderAction(RenderAction.RenderActions.FLIP_X); onUnReverseAction = RenderAction(RenderAction.RenderActions.UNFLIP_X) }
            }))
    ),
    BoxSpawner_SMC(PrefabType.SMC,
            Prefab("box spawner", GameObject(Tags.Special.tag, box = Rect(size = Size(48, 48)), initDefaultState = {
                this += AtlasComponent(0,
                        AtlasComponent.AtlasData("default", Constants.packsSMCDirPath.child("box.atlas").toFileWrapper() to "yellow/default"))
                this += PhysicsComponent(true, collisionsActions = arrayListOf(
                        CollisionAction(BoxSide.Down).apply { action = MultiplexerAction(StateAction(1), SpawnAction(MushroomRed_SMC.prefab, BoxSide.Up)) }
                ))
                //this += TweenComponent(TweenFactory.RemoveGOTween())
            }, otherStates = *arrayOf(GameObjectState("pop") {
                this += AtlasComponent(0, AtlasComponent.AtlasData("pop", Constants.packsSMCDirPath.child("box.atlas").toFileWrapper() to "brown1_1"))
                this += PhysicsComponent(true)
            })))
    ),
    //endregion
}