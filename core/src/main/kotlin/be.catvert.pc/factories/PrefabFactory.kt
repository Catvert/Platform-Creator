package be.catvert.pc.factories

import be.catvert.pc.GameKeys
import be.catvert.pc.Prefab
import be.catvert.pc.Tags
import be.catvert.pc.actions.*
import be.catvert.pc.builders.GameObjectBuilder
import be.catvert.pc.components.basics.SoundComponent
import be.catvert.pc.components.graphics.AtlasComponent
import be.catvert.pc.components.graphics.AtlasRegion
import be.catvert.pc.components.logics.*
import be.catvert.pc.tweens.ResizeTween
import be.catvert.pc.utility.BoxSide
import be.catvert.pc.utility.Constants
import be.catvert.pc.utility.Size
import be.catvert.pc.utility.toFileWrapper
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.g2d.Animation

enum class PrefabType {
    All, Kenney, SMC
}

/**
 * Objet permettant la création de prefab préfait
 */
object PrefabSetup {
    fun setupSprite(region: AtlasRegion, size: Size) = Prefab("sprite", GameObjectBuilder(Tags.Sprite.tag, size)
            .withDefaultState {
                withComponent(AtlasComponent(0, AtlasComponent.AtlasData("default", region)))
            }
            .build())

    fun setupPhysicsSprite(region: AtlasRegion, size: Size) = Prefab("sprite", GameObjectBuilder(Tags.Sprite.tag, size)
            .withDefaultState {
                withComponent(AtlasComponent(0, AtlasComponent.AtlasData("default", region)))

                withComponent(PhysicsComponent(true))
            }
            .build())

    val killActionTween = MultiplexerAction(TweenAction(TweenFactory.RemoveGO(), false), TweenAction(TweenFactory.ReduceSize(), false))

    fun playerAction(action: Action) = TagAction(Tags.Player.tag, action)
}

enum class PrefabFactory(val type: PrefabType, val prefab: Prefab) {
    Empty(PrefabType.All, Prefab("empty",
            GameObjectBuilder(Tags.Empty.tag, Size(50, 50))
                    .build())),

    BlockEnemy(PrefabType.All, Prefab("block enemy",
            GameObjectBuilder(Tags.Special.tag, Size(20, 20))
                    .withDefaultState {
                        withComponent(PhysicsComponent(true, ignoreTags = arrayListOf(Tags.Player.tag)))
                    }
                    .build()
    )),

    EndLevel(PrefabType.All, Prefab("end level",
            GameObjectBuilder(Tags.Special.tag, Size(20, 20))
                    .withDefaultState {
                        withComponent(SensorComponent(SensorComponent.TagSensorData(Tags.Player.tag, LevelAction(LevelAction.LevelActions.SUCCESS_EXIT))))
                    }
                    .build()
    )),

    Sprite(PrefabType.All, PrefabSetup.setupSprite(Constants.packsKenneyDirPath.child("grassSheet.atlas").toFileWrapper() to "slice03_03", Size(50, 50))),

    PhysicsSprite(PrefabType.All, PrefabSetup.setupPhysicsSprite(Constants.packsKenneyDirPath.child("grassSheet.atlas").toFileWrapper() to "slice03_03", Size(50, 50))),

    //region Kenney
    Player_Kenney(PrefabType.Kenney, Prefab("player",
            GameObjectBuilder(Tags.Player.tag, Size(48, 98))
                    .withDefaultState {
                        val pack = Constants.packsKenneyDirPath.child("aliens.atlas").toFileWrapper()

                        withComponent(AtlasComponent(0,
                                AtlasComponent.AtlasData("stand", pack to "alienGreen_stand"),
                                AtlasComponent.AtlasData("walk", pack, "alienGreen_walk", 0.33f),
                                AtlasComponent.AtlasData("jump", pack to "alienGreen_jump"),
                                AtlasComponent.AtlasData("fall", pack to "alienGreen_swim_1"))
                        )

                        withComponent(InputComponent(
                                InputComponent.InputData(GameKeys.GAME_PLAYER_LEFT.key, true, MultiplexerAction(PhysicsAction(PhysicsAction.PhysicsActions.GO_LEFT), RenderAction(RenderAction.RenderActions.FLIP_X))),
                                InputComponent.InputData(GameKeys.GAME_PLAYER_RIGHT.key, true, MultiplexerAction(PhysicsAction(PhysicsAction.PhysicsActions.GO_RIGHT), RenderAction(RenderAction.RenderActions.UNFLIP_X))),
                                InputComponent.InputData(GameKeys.GAME_PLAYER_GOD_UP.key, true, PhysicsAction(PhysicsAction.PhysicsActions.GO_UP)),
                                InputComponent.InputData(GameKeys.GAME_PLAYER_GOD_DOWN.key, true, PhysicsAction(PhysicsAction.PhysicsActions.GO_DOWN)),
                                InputComponent.InputData(GameKeys.GAME_PLAYER_JUMP.key, false, PhysicsAction(PhysicsAction.PhysicsActions.JUMP)))
                        )

                        withComponent(PhysicsComponent(false, 10, MovementType.SMOOTH, jumpHeight = 200).apply {
                            val walkAction = AtlasAction(1)
                            onRightAction = walkAction
                            onLeftAction = walkAction
                            onNothingAction = AtlasAction(0)
                            onJumpAction = SoundAction(0)
                            onUpAction = AtlasAction(2)
                            onDownAction = AtlasAction(3)
                        })

                        withComponent(SoundComponent(SoundComponent.SoundData(Constants.soundsDirPath.child("player/jump.ogg").toFileWrapper())))

                        withComponent(LifeComponent(LevelAction(LevelAction.LevelActions.FAIL_EXIT)))
                    }
                    .withOutOfMap(LevelAction(LevelAction.LevelActions.FAIL_EXIT))
                    .build()
    )),

    Spider_Kenney(PrefabType.Kenney, Prefab("spider",
            GameObjectBuilder(Tags.Enemy.tag, Size(48, 48))
                    .withDefaultState {
                        withComponent(AtlasComponent(0, AtlasComponent.AtlasData("walk", Constants.packsKenneyDirPath.child("enemies.atlas").toFileWrapper(), "spider_walk", 0.33f)))

                        withComponent(PhysicsComponent(false, 5, collisionsActions = arrayListOf(
                                CollisionAction(BoxSide.Left, action = PrefabSetup.playerAction(LifeAction(LifeAction.LifeActions.REMOVE_LP))),
                                CollisionAction(BoxSide.Right, action = PrefabSetup.playerAction(LifeAction(LifeAction.LifeActions.REMOVE_LP))),
                                CollisionAction(BoxSide.Down, action = PrefabSetup.playerAction(LifeAction(LifeAction.LifeActions.REMOVE_LP))),
                                CollisionAction(BoxSide.Up, action = MultiplexerAction(LifeAction(LifeAction.LifeActions.REMOVE_LP), PrefabSetup.playerAction(PhysicsAction(PhysicsAction.PhysicsActions.FORCE_JUMP))))
                        )))

                        withComponent(LifeComponent(PrefabSetup.killActionTween))

                        withComponent(MoverComponent(5, 0, true).apply { onReverseAction = RenderAction(RenderAction.RenderActions.UNFLIP_X); onUnReverseAction = RenderAction(RenderAction.RenderActions.FLIP_X) })
                    }
                    .build()
    )),

    SnakeSlime_Kenney(PrefabType.Kenney, Prefab("snake slime",
            GameObjectBuilder(Tags.Enemy.tag, Size(35, 120))
                    .withDefaultState {
                        withComponent(AtlasComponent(0, AtlasComponent.AtlasData("default", Constants.packsKenneyDirPath.child("enemies.atlas").toFileWrapper(), "snakeSlime", 0.33f)))

                        withComponent(PhysicsComponent(true, collisionsActions = arrayListOf(
                                CollisionAction(BoxSide.All, action = PrefabSetup.playerAction(LifeAction(LifeAction.LifeActions.REMOVE_LP)))
                        )))

                        withComponent(LifeComponent(PrefabSetup.killActionTween))
                    }
                    .build()
    )),

    Bee_Kenney(PrefabType.Kenney, Prefab("bee",
            GameObjectBuilder(Tags.Enemy.tag, Size(35, 35))
                    .withDefaultState {
                        withComponent(AtlasComponent(0, AtlasComponent.AtlasData("default", Constants.packsKenneyDirPath.child("enemies.atlas").toFileWrapper() to "bee")))

                        withComponent(PhysicsComponent(false, 5, gravity = false, collisionsActions = arrayListOf(
                                CollisionAction(BoxSide.Left, action = PrefabSetup.playerAction(LifeAction(LifeAction.LifeActions.REMOVE_LP))),
                                CollisionAction(BoxSide.Right, action = PrefabSetup.playerAction(LifeAction(LifeAction.LifeActions.REMOVE_LP))),
                                CollisionAction(BoxSide.Down, action = PrefabSetup.playerAction(LifeAction(LifeAction.LifeActions.REMOVE_LP))),
                                CollisionAction(BoxSide.Up, action = MultiplexerAction(LifeAction(LifeAction.LifeActions.REMOVE_LP), PrefabSetup.playerAction(PhysicsAction(PhysicsAction.PhysicsActions.FORCE_JUMP))))
                        )))

                        withComponent(LifeComponent(PrefabSetup.killActionTween))

                        withComponent(MoverComponent(5, 0, true).apply { onReverseAction = RenderAction(RenderAction.RenderActions.UNFLIP_X); onUnReverseAction = RenderAction(RenderAction.RenderActions.FLIP_X) })
                    }
                    .build()
    )),

    GoldCoin_Kenney(PrefabType.Kenney, Prefab("gold coin",
            GameObjectBuilder(Tags.Special.tag, Size(35, 35))
                    .withDefaultState {
                        withComponent(AtlasComponent(0, AtlasComponent.AtlasData("default", Constants.packsKenneyDirPath.child("jumper.atlas").toFileWrapper() to "coin_gold")))

                        withComponent(SoundComponent(SoundComponent.SoundData(Constants.soundsDirPath.child("coin.wav").toFileWrapper())))

                        withComponent(SensorComponent(SensorComponent.TagSensorData(sensorIn = MultiplexerAction(SoundAction(0), ScoreAction(1), TweenAction(TweenFactory.RemoveGO(), false)))))
                    }
                    .build()
    )),
    //endregion

    //region SMC

    FireBall_Left_SMC(PrefabType.SMC, Prefab("fireball left",
            GameObjectBuilder(Tags.Special.tag, Size(35, 35))
                    .withDefaultState {
                        withComponent(AtlasComponent(0, AtlasComponent.AtlasData("default", Constants.packsSMCDirPath.child("animations.atlas").toFileWrapper() to "fireball/fireball")))

                        withComponent(PhysicsComponent(false, jumpHeight = 50, collisionsActions = arrayListOf(
                                CollisionAction(BoxSide.Down, Tags.Sprite.tag, PhysicsAction(PhysicsAction.PhysicsActions.JUMP)),
                                CollisionAction(BoxSide.All, Tags.Enemy.tag, RemoveGOAction())
                        ), ignoreTags = arrayListOf(Tags.Player.tag)))

                        withComponent(MoverComponent(15, 0, true).apply { this.onReverseAction = RemoveGOAction() })
                    }
                    .build())),

    FireBall_Right_SMC(PrefabType.SMC, Prefab("fireball right",
            GameObjectBuilder(Tags.Special.tag, Size(35, 35))
                    .withDefaultState {
                        withComponent(AtlasComponent(0, AtlasComponent.AtlasData("default", Constants.packsSMCDirPath.child("animations.atlas").toFileWrapper() to "fireball/fireball")))

                        withComponent(PhysicsComponent(false, jumpHeight = 50, collisionsActions = arrayListOf(
                                CollisionAction(BoxSide.Down, Tags.Sprite.tag, PhysicsAction(PhysicsAction.PhysicsActions.JUMP)),
                                CollisionAction(BoxSide.All, Tags.Enemy.tag, RemoveGOAction())
                        ), ignoreTags = arrayListOf(Tags.Player.tag)))

                        withComponent(MoverComponent(15, 0).apply { this.onReverseAction = RemoveGOAction() })
                    }
                    .build())),

    Player_SMC(PrefabType.SMC, Prefab("player",
            GameObjectBuilder(Tags.Player.tag, Size(48, 78))
                    // Small state
                    .withDefaultState {
                        val pack = Constants.packsSMCDirPath.child("maryo.atlas").toFileWrapper()

                        withStartAction(MultiplexerAction(TweenAction(ResizeTween(0.5f, 48, 78), false), TweenAction(TweenFactory.DisableLifeComponent(), false)))

                        withComponent(AtlasComponent(0,
                                AtlasComponent.AtlasData("stand", pack to "small/stand_right"),
                                AtlasComponent.AtlasData("walk", pack, "small/walk_right", 0.33f),
                                AtlasComponent.AtlasData("jump", pack to "small/jump_right"),
                                AtlasComponent.AtlasData("fall", pack to "small/fall_right"))
                        )

                        withComponent(InputComponent(
                                InputComponent.InputData(GameKeys.GAME_PLAYER_LEFT.key, true, MultiplexerAction(PhysicsAction(PhysicsAction.PhysicsActions.GO_LEFT), RenderAction(RenderAction.RenderActions.FLIP_X))),
                                InputComponent.InputData(GameKeys.GAME_PLAYER_RIGHT.key, true, MultiplexerAction(PhysicsAction(PhysicsAction.PhysicsActions.GO_RIGHT), RenderAction(RenderAction.RenderActions.UNFLIP_X))),
                                InputComponent.InputData(GameKeys.GAME_PLAYER_GOD_UP.key, true, PhysicsAction(PhysicsAction.PhysicsActions.GO_UP)),
                                InputComponent.InputData(GameKeys.GAME_PLAYER_GOD_DOWN.key, true, PhysicsAction(PhysicsAction.PhysicsActions.GO_DOWN)),
                                InputComponent.InputData(GameKeys.GAME_PLAYER_JUMP.key, false, PhysicsAction(PhysicsAction.PhysicsActions.JUMP)))
                        )

                        withComponent(PhysicsComponent(false, 10, MovementType.SMOOTH, jumpHeight = 200).apply {
                            val walkAction = AtlasAction(1)
                            onRightAction = walkAction
                            onLeftAction = walkAction
                            onNothingAction = AtlasAction(0)
                            onJumpAction = SoundAction(0)
                            onUpAction = AtlasAction(2)
                            onDownAction = AtlasAction(3)
                        })

                        withComponent(SoundComponent(SoundComponent.SoundData(Constants.soundsDirPath.child("player/jump.ogg").toFileWrapper())))

                        withComponent(LifeComponent(LevelAction(LevelAction.LevelActions.FAIL_EXIT)))
                    }
                    // Big state
                    .withState("big") {
                        val pack = Constants.packsSMCDirPath.child("maryo.atlas").toFileWrapper()

                        withStartAction(TweenAction(ResizeTween(0.5f, 48, 98), false))

                        withComponent(AtlasComponent(0,
                                AtlasComponent.AtlasData("stand", pack to "big/stand_right"),
                                AtlasComponent.AtlasData("walk", pack, "big/walk_right", 0.33f),
                                AtlasComponent.AtlasData("jump", pack to "big/jump_right"),
                                AtlasComponent.AtlasData("fall", pack to "big/fall_right"))
                        )

                        withComponent(InputComponent(
                                InputComponent.InputData(GameKeys.GAME_PLAYER_LEFT.key, true, MultiplexerAction(PhysicsAction(PhysicsAction.PhysicsActions.GO_LEFT), RenderAction(RenderAction.RenderActions.FLIP_X))),
                                InputComponent.InputData(GameKeys.GAME_PLAYER_RIGHT.key, true, MultiplexerAction(PhysicsAction(PhysicsAction.PhysicsActions.GO_RIGHT), RenderAction(RenderAction.RenderActions.UNFLIP_X))),
                                InputComponent.InputData(GameKeys.GAME_PLAYER_GOD_UP.key, true, PhysicsAction(PhysicsAction.PhysicsActions.GO_UP)),
                                InputComponent.InputData(GameKeys.GAME_PLAYER_GOD_DOWN.key, true, PhysicsAction(PhysicsAction.PhysicsActions.GO_DOWN)),
                                InputComponent.InputData(GameKeys.GAME_PLAYER_JUMP.key, false, PhysicsAction(PhysicsAction.PhysicsActions.JUMP)))
                        )

                        withComponent(PhysicsComponent(false, 10, MovementType.SMOOTH, jumpHeight = 200).apply {
                            val walkAction = AtlasAction(1)
                            onRightAction = walkAction
                            onLeftAction = walkAction
                            onNothingAction = AtlasAction(0)
                            onJumpAction = SoundAction(0)
                            onUpAction = AtlasAction(2)
                            onDownAction = AtlasAction(3)
                        })

                        withComponent(SoundComponent(SoundComponent.SoundData(Constants.soundsDirPath.child("player/jump.ogg").toFileWrapper())))

                        withComponent(LifeComponent(StateAction(0)))
                    }
                    .withState("fire") {
                        val pack = Constants.packsSMCDirPath.child("maryo.atlas").toFileWrapper()

                        withStartAction(TweenAction(ResizeTween(0.5f, 48, 98), false))

                        withComponent(AtlasComponent(0,
                                AtlasComponent.AtlasData("stand", pack to "fire/stand_right"),
                                AtlasComponent.AtlasData("walk", pack, "fire/walk_right", 0.33f),
                                AtlasComponent.AtlasData("jump", pack to "fire/jump_right"),
                                AtlasComponent.AtlasData("fall", pack to "fire/fall_right"))
                        )

                        withComponent(InputComponent(
                                InputComponent.InputData(GameKeys.GAME_PLAYER_LEFT.key, true, MultiplexerAction(PhysicsAction(PhysicsAction.PhysicsActions.GO_LEFT), RenderAction(RenderAction.RenderActions.FLIP_X))),
                                InputComponent.InputData(GameKeys.GAME_PLAYER_RIGHT.key, true, MultiplexerAction(PhysicsAction(PhysicsAction.PhysicsActions.GO_RIGHT), RenderAction(RenderAction.RenderActions.UNFLIP_X))),
                                InputComponent.InputData(GameKeys.GAME_PLAYER_GOD_UP.key, true, PhysicsAction(PhysicsAction.PhysicsActions.GO_UP)),
                                InputComponent.InputData(GameKeys.GAME_PLAYER_GOD_DOWN.key, true, PhysicsAction(PhysicsAction.PhysicsActions.GO_DOWN)),
                                InputComponent.InputData(GameKeys.GAME_PLAYER_JUMP.key, false, PhysicsAction(PhysicsAction.PhysicsActions.JUMP)),
                                InputComponent.InputData(Input.Keys.E, false, AtlasFlipSwitcherAction(unFlipXAction = SpawnSideAction(PrefabFactory.FireBall_Right_SMC.prefab, BoxSide.Right, false), flipXAction = SpawnSideAction(PrefabFactory.FireBall_Left_SMC.prefab, BoxSide.Left, false))))
                        )

                        withComponent(PhysicsComponent(false, 10, MovementType.SMOOTH, jumpHeight = 200).apply {
                            val walkAction = AtlasAction(1)
                            onRightAction = walkAction
                            onLeftAction = walkAction
                            onNothingAction = AtlasAction(0)
                            onJumpAction = SoundAction(0)
                            onUpAction = AtlasAction(2)
                            onDownAction = AtlasAction(3)
                        })

                        withComponent(SoundComponent(SoundComponent.SoundData(Constants.soundsDirPath.child("player/jump.ogg").toFileWrapper())))

                        withComponent(LifeComponent(StateAction(0)))
                    }
                    .withOutOfMap(LevelAction(LevelAction.LevelActions.FAIL_EXIT))
                    .build()
    )),

    Furball_SMC(PrefabType.SMC, Prefab("furball",
            GameObjectBuilder(Tags.Enemy.tag, Size(48, 48))
                    .withDefaultState {
                        withComponent(AtlasComponent(0, AtlasComponent.AtlasData("walk", Constants.packsSMCDirPath.child("enemies.atlas").toFileWrapper(), "furball/brown/walk", 0.1f)))

                        withComponent(PhysicsComponent(false, 5, collisionsActions = arrayListOf(
                                CollisionAction(BoxSide.Left, action = PrefabSetup.playerAction(LifeAction(LifeAction.LifeActions.REMOVE_LP))),
                                CollisionAction(BoxSide.Right, action = PrefabSetup.playerAction(LifeAction(LifeAction.LifeActions.REMOVE_LP))),
                                CollisionAction(BoxSide.Down, action = PrefabSetup.playerAction(LifeAction(LifeAction.LifeActions.REMOVE_LP))),
                                CollisionAction(BoxSide.Up, action = MultiplexerAction(LifeAction(LifeAction.LifeActions.REMOVE_LP), PrefabSetup.playerAction(PhysicsAction(PhysicsAction.PhysicsActions.FORCE_JUMP)))))
                        ))

                        withComponent(LifeComponent(PrefabSetup.killActionTween))

                        withComponent(MoverComponent(5, 0, true).apply { onReverseAction = RenderAction(RenderAction.RenderActions.UNFLIP_X); onUnReverseAction = RenderAction(RenderAction.RenderActions.FLIP_X) })
                    }
                    .build()
    )),

    Turtle_SMC(PrefabType.SMC, Prefab("turtle",
            GameObjectBuilder(Tags.Enemy.tag, Size(48, 98))
                    .withDefaultState {
                        withComponent(AtlasComponent(0, AtlasComponent.AtlasData("walk", Constants.packsSMCDirPath.child("enemies.atlas").toFileWrapper(), "turtle/green/walk", 0.33f)))

                        withComponent(PhysicsComponent(false, 5, collisionsActions = arrayListOf(
                                CollisionAction(BoxSide.Left, action = PrefabSetup.playerAction(LifeAction(LifeAction.LifeActions.REMOVE_LP))),
                                CollisionAction(BoxSide.Right, action = PrefabSetup.playerAction(LifeAction(LifeAction.LifeActions.REMOVE_LP))),
                                CollisionAction(BoxSide.Down, action = PrefabSetup.playerAction(LifeAction(LifeAction.LifeActions.REMOVE_LP))),
                                CollisionAction(BoxSide.Up, action = MultiplexerAction(LifeAction(LifeAction.LifeActions.REMOVE_LP), PrefabSetup.playerAction(PhysicsAction(PhysicsAction.PhysicsActions.FORCE_JUMP)))))
                        ))

                        withComponent(LifeComponent(StateAction(1, true)))

                        withComponent(MoverComponent(5, 0, true).apply { onReverseAction = RenderAction(RenderAction.RenderActions.UNFLIP_X); onUnReverseAction = RenderAction(RenderAction.RenderActions.FLIP_X) })
                    }
                    .withState("shell") {
                        withStartAction(ResizeAction(Size(45, 45)))

                        withComponent(AtlasComponent(0, AtlasComponent.AtlasData("walk", Constants.packsSMCDirPath.child("enemies.atlas").toFileWrapper(), "turtle/green/shell_move", 0.33f)))

                        withComponent(PhysicsComponent(false, 10, collisionsActions = arrayListOf(
                                CollisionAction(BoxSide.Left, action = PrefabSetup.playerAction(LifeAction(LifeAction.LifeActions.REMOVE_LP))),
                                CollisionAction(BoxSide.Right, action = PrefabSetup.playerAction(LifeAction(LifeAction.LifeActions.REMOVE_LP))),
                                CollisionAction(BoxSide.Up, action = PrefabSetup.playerAction(PhysicsAction(PhysicsAction.PhysicsActions.FORCE_JUMP))),
                                CollisionAction(BoxSide.Right, Tags.Enemy.tag, action = TagAction(Tags.Enemy.tag, LifeAction(LifeAction.LifeActions.ONE_SHOT))),
                                CollisionAction(BoxSide.Left, Tags.Enemy.tag, action = TagAction(Tags.Enemy.tag, PrefabSetup.playerAction(LifeAction(LifeAction.LifeActions.ONE_SHOT)))))
                        ))

                        withComponent(MoverComponent(10, 0))
                    }
                    .build()
    )),

    Eato_SMC(PrefabType.SMC, Prefab("eato",
            GameObjectBuilder(Tags.Enemy.tag, Size(45, 45))
                    .withDefaultState {
                        withComponent(AtlasComponent(0, AtlasComponent.AtlasData("default", Constants.packsSMCDirPath.child("enemies.atlas").toFileWrapper(), "eato/green/eato", 0.15f, Animation.PlayMode.LOOP_PINGPONG)))

                        withComponent(PhysicsComponent(true, collisionsActions = arrayListOf(
                                CollisionAction(BoxSide.All, action = PrefabSetup.playerAction(LifeAction(LifeAction.LifeActions.REMOVE_LP))))
                        ))

                        withComponent(LifeComponent(PrefabSetup.killActionTween))
                    }
                    .build()
    )),

    MushroomRed_SMC(PrefabType.SMC, Prefab("mushroom red",
            GameObjectBuilder(Tags.Special.tag, Size(45, 45))
                    .withDefaultState {
                        withComponent(AtlasComponent(0, AtlasComponent.AtlasData("default", Constants.packsSMCDirPath.child("items.atlas").toFileWrapper() to "mushroom_red")))

                        withComponent(PhysicsComponent(false, 5, collisionsActions = arrayListOf(
                                CollisionAction(BoxSide.All, action = MultiplexerAction(PrefabSetup.playerAction(StateSwitcherAction(0 to StateAction(1))), TweenAction(TweenFactory.RemoveGO(), false))))
                        ))

                        withComponent(MoverComponent(5, 0))
                    }
                    .build()
    )),

    FirePlant_SMC(PrefabType.SMC, Prefab("fire plant",
            GameObjectBuilder(Tags.Special.tag, Size(45, 45))
                    .withDefaultState {
                        withComponent(AtlasComponent(0, AtlasComponent.AtlasData("default", Constants.packsSMCDirPath.child("items.atlas").toFileWrapper() to "fireplant")))

                        withComponent(PhysicsComponent(false, 0, collisionsActions = arrayListOf(
                                CollisionAction(BoxSide.All, action = MultiplexerAction(PrefabSetup.playerAction(StateAction(2)), TweenAction(TweenFactory.RemoveGO(), false))))
                        ))
                    }
                    .build()
    )),

    BoxSpawner_SMC(PrefabType.SMC,
            Prefab("box spawner", GameObjectBuilder(Tags.Special.tag, Size(48, 48))
                    .withDefaultState {
                        withComponent(AtlasComponent(0, AtlasComponent.AtlasData("default", Constants.packsSMCDirPath.child("box.atlas").toFileWrapper() to "yellow/default")))

                        withComponent(PhysicsComponent(true, collisionsActions = arrayListOf(
                                CollisionAction(BoxSide.Down, action = StateAction(1)))
                        ))
                    }
                    .withState("pop") {
                        withStartAction(SpawnSideAction(PrefabFactory.MushroomRed_SMC.prefab, BoxSide.Up, true))

                        withComponent(AtlasComponent(0, AtlasComponent.AtlasData("pop", Constants.packsSMCDirPath.child("box.atlas").toFileWrapper() to "brown1_1")))
                        withComponent(PhysicsComponent(true))
                    }
                    .withLayer(1)
                    .build()
            )),
    //endregion
}