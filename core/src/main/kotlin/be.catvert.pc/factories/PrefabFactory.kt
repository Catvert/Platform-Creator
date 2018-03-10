package be.catvert.pc.factories

import be.catvert.pc.GameKeys
import be.catvert.pc.builders.EntityBuilder
import be.catvert.pc.eca.Prefab
import be.catvert.pc.eca.Tags
import be.catvert.pc.eca.actions.*
import be.catvert.pc.eca.components.basics.SoundComponent
import be.catvert.pc.eca.components.graphics.TextureComponent
import be.catvert.pc.eca.components.graphics.TextureRegion
import be.catvert.pc.eca.components.logics.*
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
    fun setupSprite(region: TextureRegion, size: Size) = Prefab("sprite", EntityBuilder(Tags.Sprite.tag, size)
            .withDefaultState {
                withComponent(TextureComponent(0, TextureComponent.TextureData("default", region)))
            }
            .build())

    fun setupPhysicsSprite(region: TextureRegion, size: Size) = Prefab("sprite", EntityBuilder(Tags.Sprite.tag, size)
            .withDefaultState {
                withComponent(TextureComponent(0, TextureComponent.TextureData("default", region)))

                withComponent(PhysicsComponent(true))
            }
            .build())

    val killActionTween = MultiplexerAction(TweenAction(TweenFactory.RemoveEntity(), false), TweenAction(TweenFactory.ReduceSize(), false))

    fun playerAction(action: Action) = TagAction(Tags.Player.tag, action)
}

/**
 * Permet d'ajouter des préfabs de base au jeu
 */
enum class PrefabFactory(val type: PrefabType, val prefab: Prefab) {
    Empty(PrefabType.All, Prefab("empty",
            EntityBuilder(Tags.Empty.tag, Size(50, 50))
                    .build())),

    BlockEnemy(PrefabType.All, Prefab("block enemy",
            EntityBuilder(Tags.Special.tag, Size(20, 20))
                    .withDefaultState {
                        withComponent(PhysicsComponent(true, ignoreTags = arrayListOf(Tags.Player.tag, Tags.Special.tag)))
                    }
                    .build()
    )),

    EndLevel(PrefabType.All, Prefab("end level",
            EntityBuilder(Tags.Special.tag, Size(20, 20))
                    .withDefaultState {
                        withComponent(SensorComponent(SensorComponent.TagSensorData(Tags.Player.tag, LevelAction(LevelAction.LevelActions.SUCCESS_EXIT))))
                    }
                    .build()
    )),

    Sprite(PrefabType.All, PrefabSetup.setupSprite(Constants.packsKenneyDirPath.child("grassSheet.atlas").toFileWrapper() to "slice03_03", Size(50, 50))),

    PhysicsSprite(PrefabType.All, PrefabSetup.setupPhysicsSprite(Constants.packsKenneyDirPath.child("grassSheet.atlas").toFileWrapper() to "slice03_03", Size(50, 50))),

    //region Kenney
    Player_Kenney(PrefabType.Kenney, Prefab("player",
            EntityBuilder(Tags.Player.tag, Size(48, 98))
                    .withDefaultState {
                        val pack = Constants.packsKenneyDirPath.child("aliens.atlas").toFileWrapper()

                        withComponent(TextureComponent(0,
                                TextureComponent.TextureData("stand", pack to "alienGreen_stand"),
                                TextureComponent.TextureData("walk", pack, "alienGreen_walk", 0.33f),
                                TextureComponent.TextureData("jump", pack to "alienGreen_jump"),
                                TextureComponent.TextureData("fall", pack to "alienGreen_swim_1"))
                        )

                        withComponent(InputComponent(
                                InputComponent.InputData(GameKeys.GAME_PLAYER_LEFT.key, true, MultiplexerAction(PhysicsAction(PhysicsAction.PhysicsActions.MOVE_LEFT), TextureFlipAction(true, false))),
                                InputComponent.InputData(GameKeys.GAME_PLAYER_RIGHT.key, true, MultiplexerAction(PhysicsAction(PhysicsAction.PhysicsActions.MOVE_RIGHT), TextureFlipAction(false, false))),
                                InputComponent.InputData(GameKeys.GAME_PLAYER_GOD_UP.key, true, PhysicsAction(PhysicsAction.PhysicsActions.MOVE_UP)),
                                InputComponent.InputData(GameKeys.GAME_PLAYER_GOD_DOWN.key, true, PhysicsAction(PhysicsAction.PhysicsActions.MOVE_DOWN)),
                                InputComponent.InputData(GameKeys.GAME_PLAYER_JUMP.key, false, PhysicsAction(PhysicsAction.PhysicsActions.JUMP)))
                        )

                        withComponent(PhysicsComponent(false, 10, MovementType.SMOOTH, jumpHeight = 200).apply {
                            val walkAction = TextureAction(1)
                            onRightAction = walkAction
                            onLeftAction = walkAction
                            onNothingAction = TextureAction(0)
                            onJumpAction = SoundAction(0)
                            onUpAction = TextureAction(2)
                            onDownAction = TextureAction(3)
                        })

                        withComponent(SoundComponent(SoundComponent.SoundData(Constants.soundsDirPath.child("player/jump.ogg").toFileWrapper())))

                        withComponent(LifeComponent(LevelAction(LevelAction.LevelActions.FAIL_EXIT)))
                    }
                    .withOutOfMap(LevelAction(LevelAction.LevelActions.FAIL_EXIT))
                    .build()
    )),

    Spider_Kenney(PrefabType.Kenney, Prefab("spider",
            EntityBuilder(Tags.Enemy.tag, Size(48, 48))
                    .withDefaultState {
                        withComponent(TextureComponent(0, TextureComponent.TextureData("walk", Constants.packsKenneyDirPath.child("enemies.atlas").toFileWrapper(), "spider_walk", 0.33f)))

                        withComponent(PhysicsComponent(false, 5, collisionsActions = arrayListOf(
                                CollisionAction(BoxSide.Left, action = PrefabSetup.playerAction(LifeAction(LifeAction.LifeActions.REMOVE_LP))),
                                CollisionAction(BoxSide.Right, action = PrefabSetup.playerAction(LifeAction(LifeAction.LifeActions.REMOVE_LP))),
                                CollisionAction(BoxSide.Down, action = PrefabSetup.playerAction(LifeAction(LifeAction.LifeActions.REMOVE_LP))),
                                CollisionAction(BoxSide.Up, action = MultiplexerAction(LifeAction(LifeAction.LifeActions.REMOVE_LP), PrefabSetup.playerAction(PhysicsAction(PhysicsAction.PhysicsActions.FORCE_JUMP))))
                        )))

                        withComponent(LifeComponent(PrefabSetup.killActionTween))

                        withComponent(MoverComponent(5, 0, true).apply { onReverseAction = TextureFlipAction(false, false); onUnReverseAction = TextureFlipAction(true, false) })
                    }
                    .build()
    )),

    SnakeSlime_Kenney(PrefabType.Kenney, Prefab("snake slime",
            EntityBuilder(Tags.Enemy.tag, Size(35, 120))
                    .withDefaultState {
                        withComponent(TextureComponent(0, TextureComponent.TextureData("default", Constants.packsKenneyDirPath.child("enemies.atlas").toFileWrapper(), "snakeSlime", 0.33f)))

                        withComponent(PhysicsComponent(true, collisionsActions = arrayListOf(
                                CollisionAction(BoxSide.All, action = PrefabSetup.playerAction(LifeAction(LifeAction.LifeActions.REMOVE_LP)))
                        )))

                        withComponent(LifeComponent(PrefabSetup.killActionTween))
                    }
                    .build()
    )),

    Bee_Kenney(PrefabType.Kenney, Prefab("bee",
            EntityBuilder(Tags.Enemy.tag, Size(35, 35))
                    .withDefaultState {
                        withComponent(TextureComponent(0, TextureComponent.TextureData("default", Constants.packsKenneyDirPath.child("enemies.atlas").toFileWrapper() to "bee")))

                        withComponent(PhysicsComponent(false, 5, gravity = false, collisionsActions = arrayListOf(
                                CollisionAction(BoxSide.Left, action = PrefabSetup.playerAction(LifeAction(LifeAction.LifeActions.REMOVE_LP))),
                                CollisionAction(BoxSide.Right, action = PrefabSetup.playerAction(LifeAction(LifeAction.LifeActions.REMOVE_LP))),
                                CollisionAction(BoxSide.Down, action = PrefabSetup.playerAction(LifeAction(LifeAction.LifeActions.REMOVE_LP))),
                                CollisionAction(BoxSide.Up, action = MultiplexerAction(LifeAction(LifeAction.LifeActions.REMOVE_LP), PrefabSetup.playerAction(PhysicsAction(PhysicsAction.PhysicsActions.FORCE_JUMP))))
                        )))

                        withComponent(LifeComponent(PrefabSetup.killActionTween))

                        withComponent(MoverComponent(5, 0, true).apply { onReverseAction = TextureFlipAction(false, false); onUnReverseAction = TextureFlipAction(true, false) })
                    }
                    .build()
    )),

    GoldCoin_Kenney(PrefabType.Kenney, Prefab("gold coin",
            EntityBuilder(Tags.Special.tag, Size(35, 35))
                    .withDefaultState {
                        withComponent(TextureComponent(0, TextureComponent.TextureData("default", Constants.packsKenneyDirPath.child("jumper.atlas").toFileWrapper() to "coin_gold")))

                        withComponent(SoundComponent(SoundComponent.SoundData(Constants.soundsDirPath.child("coin.wav").toFileWrapper())))

                        withComponent(SensorComponent(SensorComponent.TagSensorData(sensorIn = MultiplexerAction(SoundAction(0), ScoreAction(1), TweenAction(TweenFactory.RemoveEntity(), false)))))
                    }
                    .build()
    )),
    //endregion

    //region SMC

    FireBall_Left_SMC(PrefabType.SMC, Prefab("fireball left",
            EntityBuilder(Tags.Special.tag, Size(35, 35))
                    .withDefaultState {
                        withComponent(TextureComponent(0, TextureComponent.TextureData("default", Constants.packsSMCDirPath.child("animations.atlas").toFileWrapper() to "fireball/fireball")))

                        withComponent(PhysicsComponent(false, jumpHeight = 50, collisionsActions = arrayListOf(
                                CollisionAction(BoxSide.Down, Tags.Sprite.tag, PhysicsAction(PhysicsAction.PhysicsActions.JUMP)),
                                CollisionAction(BoxSide.All, Tags.Enemy.tag, LifeAction(LifeAction.LifeActions.REMOVE_LP), true)
                        ), ignoreTags = arrayListOf(Tags.Player.tag)))

                        withComponent(MoverComponent(15, 0, true).apply { this.onUnReverseAction = RemoveEntityAction() })
                    }
                    .build())),

    FireBall_Right_SMC(PrefabType.SMC, Prefab("fireball right",
            EntityBuilder(Tags.Special.tag, Size(35, 35))
                    .withDefaultState {
                        withComponent(TextureComponent(0, TextureComponent.TextureData("default", Constants.packsSMCDirPath.child("animations.atlas").toFileWrapper() to "fireball/fireball")))

                        withComponent(PhysicsComponent(false, jumpHeight = 50, collisionsActions = arrayListOf(
                                CollisionAction(BoxSide.Down, Tags.Sprite.tag, PhysicsAction(PhysicsAction.PhysicsActions.JUMP)),
                                CollisionAction(BoxSide.All, Tags.Enemy.tag, LifeAction(LifeAction.LifeActions.REMOVE_LP), true)
                        ), ignoreTags = arrayListOf(Tags.Player.tag)))

                        withComponent(MoverComponent(15, 0).apply { this.onReverseAction = RemoveEntityAction() })
                    }
                    .build())),

    Player_SMC(PrefabType.SMC, Prefab("player",
            EntityBuilder(Tags.Player.tag, Size(48, 78))
                    // Small state
                    .withDefaultState {
                        val pack = Constants.packsSMCDirPath.child("maryo.atlas").toFileWrapper()

                        withStartAction(MultiplexerAction(TweenAction(ResizeTween(0.5f, 48, 78), false), TweenAction(TweenFactory.DisableLifeComponent(), false)))

                        withComponent(TextureComponent(0,
                                TextureComponent.TextureData("stand", pack to "small/stand_right"),
                                TextureComponent.TextureData("walk", pack, "small/walk_right", 0.33f),
                                TextureComponent.TextureData("jump", pack to "small/jump_right"),
                                TextureComponent.TextureData("fall", pack to "small/fall_right"))
                        )

                        withComponent(InputComponent(
                                InputComponent.InputData(GameKeys.GAME_PLAYER_LEFT.key, true, MultiplexerAction(PhysicsAction(PhysicsAction.PhysicsActions.MOVE_LEFT), TextureFlipAction(true, false))),
                                InputComponent.InputData(GameKeys.GAME_PLAYER_RIGHT.key, true, MultiplexerAction(PhysicsAction(PhysicsAction.PhysicsActions.MOVE_RIGHT), TextureFlipAction(false, false))),
                                InputComponent.InputData(GameKeys.GAME_PLAYER_GOD_UP.key, true, PhysicsAction(PhysicsAction.PhysicsActions.MOVE_UP)),
                                InputComponent.InputData(GameKeys.GAME_PLAYER_GOD_DOWN.key, true, PhysicsAction(PhysicsAction.PhysicsActions.MOVE_DOWN)),
                                InputComponent.InputData(GameKeys.GAME_PLAYER_JUMP.key, false, PhysicsAction(PhysicsAction.PhysicsActions.JUMP)))
                        )

                        withComponent(PhysicsComponent(false, 10, MovementType.SMOOTH, jumpHeight = 200).apply {
                            val walkAction = TextureAction(1)
                            onRightAction = walkAction
                            onLeftAction = walkAction
                            onNothingAction = TextureAction(0)
                            onJumpAction = SoundAction(0)
                            onUpAction = TextureAction(2)
                            onDownAction = TextureAction(3)
                        })

                        withComponent(SoundComponent(SoundComponent.SoundData(Constants.soundsDirPath.child("player/jump.ogg").toFileWrapper())))

                        withComponent(LifeComponent(LevelAction(LevelAction.LevelActions.FAIL_EXIT)))
                    }
                    // Big state
                    .withState("big") {
                        val pack = Constants.packsSMCDirPath.child("maryo.atlas").toFileWrapper()

                        withStartAction(TweenAction(ResizeTween(0.5f, 48, 98), false))

                        withComponent(TextureComponent(0,
                                TextureComponent.TextureData("stand", pack to "big/stand_right"),
                                TextureComponent.TextureData("walk", pack, "big/walk_right", 0.33f),
                                TextureComponent.TextureData("jump", pack to "big/jump_right"),
                                TextureComponent.TextureData("fall", pack to "big/fall_right"))
                        )

                        withComponent(InputComponent(
                                InputComponent.InputData(GameKeys.GAME_PLAYER_LEFT.key, true, MultiplexerAction(PhysicsAction(PhysicsAction.PhysicsActions.MOVE_LEFT), TextureFlipAction(true, false))),
                                InputComponent.InputData(GameKeys.GAME_PLAYER_RIGHT.key, true, MultiplexerAction(PhysicsAction(PhysicsAction.PhysicsActions.MOVE_RIGHT), TextureFlipAction(false, false))),
                                InputComponent.InputData(GameKeys.GAME_PLAYER_GOD_UP.key, true, PhysicsAction(PhysicsAction.PhysicsActions.MOVE_UP)),
                                InputComponent.InputData(GameKeys.GAME_PLAYER_GOD_DOWN.key, true, PhysicsAction(PhysicsAction.PhysicsActions.MOVE_DOWN)),
                                InputComponent.InputData(GameKeys.GAME_PLAYER_JUMP.key, false, PhysicsAction(PhysicsAction.PhysicsActions.JUMP)))
                        )

                        withComponent(PhysicsComponent(false, 10, MovementType.SMOOTH, jumpHeight = 200).apply {
                            val walkAction = TextureAction(1)
                            onRightAction = walkAction
                            onLeftAction = walkAction
                            onNothingAction = TextureAction(0)
                            onJumpAction = SoundAction(0)
                            onUpAction = TextureAction(2)
                            onDownAction = TextureAction(3)
                        })

                        withComponent(SoundComponent(SoundComponent.SoundData(Constants.soundsDirPath.child("player/jump.ogg").toFileWrapper())))

                        withComponent(LifeComponent(StateAction(0)))
                    }
                    // Fire state
                    .withState("fire") {
                        val pack = Constants.packsSMCDirPath.child("maryo.atlas").toFileWrapper()

                        withStartAction(TweenAction(ResizeTween(0.5f, 48, 98), false))

                        withComponent(TextureComponent(0,
                                TextureComponent.TextureData("stand", pack to "fire/stand_right"),
                                TextureComponent.TextureData("walk", pack, "fire/walk_right", 0.33f),
                                TextureComponent.TextureData("jump", pack to "fire/jump_right"),
                                TextureComponent.TextureData("fall", pack to "fire/fall_right"))
                        )

                        withComponent(InputComponent(
                                InputComponent.InputData(GameKeys.GAME_PLAYER_LEFT.key, true, MultiplexerAction(PhysicsAction(PhysicsAction.PhysicsActions.MOVE_LEFT), TextureFlipAction(true, false))),
                                InputComponent.InputData(GameKeys.GAME_PLAYER_RIGHT.key, true, MultiplexerAction(PhysicsAction(PhysicsAction.PhysicsActions.MOVE_RIGHT), TextureFlipAction(false, false))),
                                InputComponent.InputData(GameKeys.GAME_PLAYER_GOD_UP.key, true, PhysicsAction(PhysicsAction.PhysicsActions.MOVE_UP)),
                                InputComponent.InputData(GameKeys.GAME_PLAYER_GOD_DOWN.key, true, PhysicsAction(PhysicsAction.PhysicsActions.MOVE_DOWN)),
                                InputComponent.InputData(GameKeys.GAME_PLAYER_JUMP.key, false, PhysicsAction(PhysicsAction.PhysicsActions.JUMP)),
                                InputComponent.InputData(Input.Keys.E, false, TextureFlipSwitcherAction(unFlipXAction = SpawnSideAction(FireBall_Right_SMC.prefab, BoxSide.Right, false), flipXAction = SpawnSideAction(FireBall_Left_SMC.prefab, BoxSide.Left, false))))
                        )

                        withComponent(PhysicsComponent(false, 10, MovementType.SMOOTH, jumpHeight = 200).apply {
                            val walkAction = TextureAction(1)
                            onRightAction = walkAction
                            onLeftAction = walkAction
                            onNothingAction = TextureAction(0)
                            onJumpAction = SoundAction(0)
                            onUpAction = TextureAction(2)
                            onDownAction = TextureAction(3)
                        })

                        withComponent(SoundComponent(SoundComponent.SoundData(Constants.soundsDirPath.child("player/jump.ogg").toFileWrapper())))

                        withComponent(LifeComponent(StateAction(0)))
                    }
                    .withOutOfMap(LevelAction(LevelAction.LevelActions.FAIL_EXIT))
                    .build()
    )),

    Furball_SMC(PrefabType.SMC, Prefab("furball",
            EntityBuilder(Tags.Enemy.tag, Size(48, 48))
                    .withDefaultState {
                        withComponent(TextureComponent(0, TextureComponent.TextureData("walk", Constants.packsSMCDirPath.child("enemies.atlas").toFileWrapper(), "furball/brown/walk", 0.1f)))

                        withComponent(PhysicsComponent(false, 5, collisionsActions = arrayListOf(
                                CollisionAction(BoxSide.Left, action = PrefabSetup.playerAction(LifeAction(LifeAction.LifeActions.REMOVE_LP))),
                                CollisionAction(BoxSide.Right, action = PrefabSetup.playerAction(LifeAction(LifeAction.LifeActions.REMOVE_LP))),
                                CollisionAction(BoxSide.Down, action = PrefabSetup.playerAction(LifeAction(LifeAction.LifeActions.REMOVE_LP))),
                                CollisionAction(BoxSide.Up, action = MultiplexerAction(LifeAction(LifeAction.LifeActions.REMOVE_LP), PrefabSetup.playerAction(PhysicsAction(PhysicsAction.PhysicsActions.FORCE_JUMP)))))
                        ))

                        withComponent(LifeComponent(PrefabSetup.killActionTween))

                        withComponent(MoverComponent(5, 0, true).apply { onReverseAction = TextureFlipAction(false, false); onUnReverseAction = TextureFlipAction(true, false) })
                    }
                    .build()
    )),

    Turtle_SMC(PrefabType.SMC, Prefab("turtle",
            EntityBuilder(Tags.Enemy.tag, Size(48, 98))
                    .withDefaultState {
                        withComponent(TextureComponent(0, TextureComponent.TextureData("walk", Constants.packsSMCDirPath.child("enemies.atlas").toFileWrapper(), "turtle/green/walk", 0.33f)))

                        withComponent(PhysicsComponent(false, 5, collisionsActions = arrayListOf(
                                CollisionAction(BoxSide.Left, action = PrefabSetup.playerAction(LifeAction(LifeAction.LifeActions.REMOVE_LP))),
                                CollisionAction(BoxSide.Right, action = PrefabSetup.playerAction(LifeAction(LifeAction.LifeActions.REMOVE_LP))),
                                CollisionAction(BoxSide.Down, action = PrefabSetup.playerAction(LifeAction(LifeAction.LifeActions.REMOVE_LP))),
                                CollisionAction(BoxSide.Up, action = MultiplexerAction(LifeAction(LifeAction.LifeActions.REMOVE_LP), PrefabSetup.playerAction(PhysicsAction(PhysicsAction.PhysicsActions.FORCE_JUMP)))))
                        ))

                        withComponent(LifeComponent(StateAction(1, true)))

                        withComponent(MoverComponent(5, 0, true).apply { onReverseAction = TextureFlipAction(false, false); onUnReverseAction = TextureFlipAction(true, false) })
                    }
                    .withState("shell") {
                        withStartAction(ResizeAction(Size(45, 45)))

                        withComponent(TextureComponent(0, TextureComponent.TextureData("walk", Constants.packsSMCDirPath.child("enemies.atlas").toFileWrapper(), "turtle/green/shell_move", 0.33f)))

                        withComponent(PhysicsComponent(false, 10, collisionsActions = arrayListOf(
                                CollisionAction(BoxSide.Left, action = PrefabSetup.playerAction(LifeAction(LifeAction.LifeActions.REMOVE_LP))),
                                CollisionAction(BoxSide.Right, action = PrefabSetup.playerAction(LifeAction(LifeAction.LifeActions.REMOVE_LP))),
                                CollisionAction(BoxSide.Up, action = PrefabSetup.playerAction(PhysicsAction(PhysicsAction.PhysicsActions.FORCE_JUMP))),
                                CollisionAction(BoxSide.Right, Tags.Enemy.tag, LifeAction(LifeAction.LifeActions.ONE_SHOT), true),
                                CollisionAction(BoxSide.Left, Tags.Enemy.tag, LifeAction(LifeAction.LifeActions.ONE_SHOT), true))
                        ))

                        withComponent(MoverComponent(10, 0))
                    }
                    .build()
    )),

    Eato_SMC(PrefabType.SMC, Prefab("eato",
            EntityBuilder(Tags.Enemy.tag, Size(45, 45))
                    .withDefaultState {
                        withComponent(TextureComponent(0, TextureComponent.TextureData("default", Constants.packsSMCDirPath.child("enemies.atlas").toFileWrapper(), "eato/green/eato", 0.15f, Animation.PlayMode.LOOP_PINGPONG)))

                        withComponent(PhysicsComponent(true, collisionsActions = arrayListOf(
                                CollisionAction(BoxSide.All, action = PrefabSetup.playerAction(LifeAction(LifeAction.LifeActions.REMOVE_LP))))
                        ))

                        withComponent(LifeComponent(PrefabSetup.killActionTween))
                    }
                    .build()
    )),

    MushroomRed_SMC(PrefabType.SMC, Prefab("mushroom red",
            EntityBuilder(Tags.Special.tag, Size(45, 45))
                    .withDefaultState {
                        withComponent(TextureComponent(0, TextureComponent.TextureData("default", Constants.packsSMCDirPath.child("items.atlas").toFileWrapper() to "mushroom_red")))

                        withComponent(PhysicsComponent(false, 5, collisionsActions = arrayListOf(
                                CollisionAction(BoxSide.All, action = MultiplexerAction(PrefabSetup.playerAction(StateSwitcherAction(0 to StateAction(1))), TweenAction(TweenFactory.RemoveEntity(), false))))
                        ))

                        withComponent(MoverComponent(5, 0))
                    }
                    .build()
    )),

    FirePlant_SMC(PrefabType.SMC, Prefab("fire plant",
            EntityBuilder(Tags.Special.tag, Size(45, 45))
                    .withDefaultState {
                        withComponent(TextureComponent(0, TextureComponent.TextureData("default", Constants.packsSMCDirPath.child("items.atlas").toFileWrapper() to "fireplant")))

                        withComponent(PhysicsComponent(false, 0, collisionsActions = arrayListOf(
                                CollisionAction(BoxSide.All, action = MultiplexerAction(PrefabSetup.playerAction(StateAction(2)), TweenAction(TweenFactory.RemoveEntity(), false))))
                        ))
                    }
                    .build()
    )),

    BoxSpawner_SMC(PrefabType.SMC,
            Prefab("box spawner", EntityBuilder(Tags.Special.tag, Size(48, 48))
                    .withDefaultState {
                        withComponent(TextureComponent(0, TextureComponent.TextureData("default", Constants.packsSMCDirPath.child("box.atlas").toFileWrapper() to "yellow/default")))

                        withComponent(PhysicsComponent(true, collisionsActions = arrayListOf(
                                CollisionAction(BoxSide.Down, action = StateAction(1)))
                        ))
                    }
                    .withState("pop") {
                        withStartAction(SpawnSideAction(MushroomRed_SMC.prefab, BoxSide.Up, true))

                        withComponent(TextureComponent(0, TextureComponent.TextureData("pop", Constants.packsSMCDirPath.child("box.atlas").toFileWrapper() to "brown1_1")))
                        withComponent(PhysicsComponent(true))
                    }
                    .withLayer(1)
                    .build()
            )),
    //endregion
}