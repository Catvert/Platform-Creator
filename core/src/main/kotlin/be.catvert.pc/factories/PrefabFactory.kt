package be.catvert.pc.factories

import be.catvert.pc.GameKeys
import be.catvert.pc.builders.EntityBuilder
import be.catvert.pc.eca.Prefab
import be.catvert.pc.eca.Tags
import be.catvert.pc.eca.actions.*
import be.catvert.pc.eca.components.basics.SoundComponent
import be.catvert.pc.eca.components.graphics.PackRegionData
import be.catvert.pc.eca.components.graphics.TextureComponent
import be.catvert.pc.eca.components.graphics.TextureGroup
import be.catvert.pc.eca.components.logics.*
import be.catvert.pc.tweens.ResizeTween
import be.catvert.pc.utility.*
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import ktx.assets.toLocalFile


enum class PrefabType {
    All, Kenney, SMC
}

/**
 * Objet permettant la création d'entités pré-faites.
 */
object PrefabSetup {
    fun setupSprite(pack: ResourceWrapper<TextureAtlas>, region: String, size: Size) = Prefab("sprite", EntityBuilder(Tags.Sprite.tag, size)
            .withDefaultState {
                withComponent(TextureComponent(0, TextureGroup("default", PackRegionData(pack, region))))
            }
            .build())

    fun setupPhysicsSprite(pack: ResourceWrapper<TextureAtlas>, region: String, size: Size) = Prefab("physics sprite", EntityBuilder(Tags.Sprite.tag, size)
            .withDefaultState {
                withComponent(TextureComponent(0, TextureGroup("default", PackRegionData(pack, region))))

                withComponent(PhysicsComponent(true))
            }
            .build())

    val killActionTween = MultiplexerAction(TweenAction(TweenFactory.RemoveEntity(), false), TweenAction(TweenFactory.ReduceSize(), false))

    fun playerAction(action: Action) = TagAction(Tags.Player.tag, action)

    enum class PlayerStates(val index: Int, val ladderIndex: Int) {
        Small(0, 1), Big(2, 3), Fire(4, 5);

        fun stateName() = this.name.toLowerCase()
    }
}

/**
 * Usines de fabrication d'entités pré-faites
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

    Sprite(PrefabType.All, PrefabSetup.setupSprite(resourceWrapperOf(Constants.packsKenneyDirPath.child("grassSheet.atlas").toFileWrapper()), "slice03_03", Size(50, 50))),

    PhysicsSprite(PrefabType.All, PrefabSetup.setupPhysicsSprite(resourceWrapperOf(Constants.packsKenneyDirPath.child("grassSheet.atlas").toFileWrapper()), "slice03_03", Size(50, 50))),

    //region Kenney
    Spider_Kenney(PrefabType.Kenney, Prefab("spider",
            EntityBuilder(Tags.Enemy.tag, Size(48, 48))
                    .withDefaultState {
                        withComponent(TextureComponent(0, TextureGroup("walk", Constants.packsKenneyDirPath.child("enemies.atlas").toFileWrapper(), "spider_walk", 0.33f)))

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
                        withComponent(TextureComponent(0, TextureGroup("default", Constants.packsKenneyDirPath.child("enemies.atlas").toFileWrapper(), "snakeSlime", 0.33f)))

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
                        withComponent(TextureComponent(0, TextureGroup("default", PackRegionData(resourceWrapperOf(Constants.packsKenneyDirPath.child("enemies.atlas").toFileWrapper()), "bee"))))

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
                        withComponent(TextureComponent(0, TextureGroup("default", PackRegionData(resourceWrapperOf(Constants.packsKenneyDirPath.child("jumper.atlas").toFileWrapper()), "coin_gold"))))

                        withComponent(SoundComponent(SoundComponent.SoundData(resourceWrapperOf(Constants.soundsDirPath.child("coin.wav").toFileWrapper()))))

                        withComponent(SensorComponent(SensorComponent.TagSensorData(onIn = MultiplexerAction(SoundAction(0), ScoreAction(1), TweenAction(TweenFactory.RemoveEntity(), false)))))
                    }
                    .build()
    )),

    Bumper_Kenney(PrefabType.Kenney, Prefab("bumper",
            EntityBuilder(Tags.Special.tag, Size(50, 50))
                    .withDefaultState {
                        withComponent(TextureComponent(0, TextureGroup("default", PackRegionData(resourceWrapperOf(Constants.packsKenneyDirPath.child("jumper.atlas").toFileWrapper()), "spring_out"))))

                        withComponent(PhysicsComponent(true, collisionsActions = arrayListOf(
                               CollisionAction(BoxSide.Up, action = PrefabSetup.playerAction(PhysicsAction(PhysicsAction.PhysicsActions.FORCE_JUMP)))
                        )))
                    }.build())),
    Ladder_Kenney(PrefabType.Kenney, Prefab("ladder",
            EntityBuilder(Tags.Special.tag, Size(50, 150))
                    .withDefaultState {
                        withComponent(TextureComponent(0, TextureGroup("default", PackRegionData(resourceWrapperOf(Constants.packsKenneyDirPath.child("abstract_platform.atlas").toFileWrapper()), "ladderNarrow_mid")).apply {
                            repeatRegion = true
                            repeatRegionSize = Size(50, 50)
                        }))

                        withComponent(PhysicsComponent(true, isPlatform = true))

                        withComponent(SensorComponent(SensorComponent.TagSensorData(Tags.Player.tag,
                                insideUpdate = InputAction(InputComponent.InputData(Input.Keys.Z, action = PrefabSetup.playerAction(StateSwitcherAction(
                                        *PrefabSetup.PlayerStates.values().map { it.index to PrefabSetup.playerAction(StateAction(it.ladderIndex)) }.toTypedArray()
                                )))), onOut = PrefabSetup.playerAction(StateSwitcherAction(
                                *PrefabSetup.PlayerStates.values().map { it.ladderIndex to PrefabSetup.playerAction(StateAction(it.index)) }.toTypedArray())))))
                    }.build())),
    //endregion

    //region SMC

    FireBall_Left_SMC(PrefabType.SMC, Prefab("fireball left",
            EntityBuilder(Tags.Special.tag, Size(35, 35))
                    .withDefaultState {
                        withComponent(TextureComponent(0, TextureGroup("default", PackRegionData(resourceWrapperOf(Constants.packsSMCDirPath.child("animations.atlas").toFileWrapper()), "fireball/fireball"))))

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
                        withComponent(TextureComponent(0, TextureGroup("default", PackRegionData(resourceWrapperOf(Constants.packsSMCDirPath.child("animations.atlas").toFileWrapper()), "fireball/fireball"))))

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
                                TextureGroup("stand", PackRegionData(resourceWrapperOf(pack), "small/stand_right")),
                                TextureGroup("walk", pack, "small/walk_right", 0.33f),
                                TextureGroup("jump", PackRegionData(resourceWrapperOf(pack), "small/jump_right")),
                                TextureGroup("fall", PackRegionData(resourceWrapperOf(pack), "small/fall_right")))
                        )

                        withComponent(InputComponent(
                                InputComponent.InputData(GameKeys.GAME_PLAYER_LEFT.key, true, MultiplexerAction(PhysicsAction(PhysicsAction.PhysicsActions.MOVE_LEFT), TextureFlipAction(true, false))),
                                InputComponent.InputData(GameKeys.GAME_PLAYER_RIGHT.key, true, MultiplexerAction(PhysicsAction(PhysicsAction.PhysicsActions.MOVE_RIGHT), TextureFlipAction(false, false))),
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

                        withComponent(SoundComponent(SoundComponent.SoundData(resourceWrapperOf(Constants.soundsDirPath.child("player/jump.ogg").toFileWrapper()))))

                        withComponent(LifeComponent(LevelAction(LevelAction.LevelActions.FAIL_EXIT)))
                    }
                    .withOutOfMap(LevelAction(LevelAction.LevelActions.FAIL_EXIT))
                    .apply {
                        val pack = Constants.packsSMCDirPath.child("maryo.atlas").toFileWrapper()

                        PrefabSetup.PlayerStates.values().forEach {
                            if(it != PrefabSetup.PlayerStates.Small) {
                                withState(it.stateName()) {
                                    withStartAction(TweenAction(ResizeTween(0.5f, 48, 98), false))

                                    withComponent(TextureComponent(0,
                                            TextureGroup("stand", PackRegionData(resourceWrapperOf(pack), "${it.stateName()}/stand_right")),
                                            TextureGroup("walk", pack, "${it.stateName()}/walk_right", 0.33f),
                                            TextureGroup("jump", PackRegionData(resourceWrapperOf(pack), "${it.stateName()}/jump_right")),
                                            TextureGroup("fall", PackRegionData(resourceWrapperOf(pack), "${it.stateName()}/fall_right")))
                                    )

                                    withComponent(InputComponent(
                                            InputComponent.InputData(GameKeys.GAME_PLAYER_LEFT.key, true, MultiplexerAction(PhysicsAction(PhysicsAction.PhysicsActions.MOVE_LEFT), TextureFlipAction(true, false))),
                                            InputComponent.InputData(GameKeys.GAME_PLAYER_RIGHT.key, true, MultiplexerAction(PhysicsAction(PhysicsAction.PhysicsActions.MOVE_RIGHT), TextureFlipAction(false, false))),
                                            InputComponent.InputData(GameKeys.GAME_PLAYER_JUMP.key, false, PhysicsAction(PhysicsAction.PhysicsActions.JUMP))).apply {
                                        if (it == PrefabSetup.PlayerStates.Fire)
                                            inputs.add(InputComponent.InputData(Input.Keys.E, false, TextureFlipSwitcherAction(unFlipXAction = SpawnSideAction(FireBall_Right_SMC.prefab, BoxSide.Right, false), flipXAction = SpawnSideAction(FireBall_Left_SMC.prefab, BoxSide.Left, false))))
                                    })

                                    withComponent(PhysicsComponent(false, 10, MovementType.SMOOTH, jumpHeight = 200).apply {
                                        val walkAction = TextureAction(1)
                                        onRightAction = walkAction
                                        onLeftAction = walkAction
                                        onNothingAction = TextureAction(0)
                                        onJumpAction = SoundAction(0)
                                        onUpAction = TextureAction(2)
                                        onDownAction = TextureAction(3)
                                    })

                                    withComponent(SoundComponent(SoundComponent.SoundData(resourceWrapperOf(Constants.soundsDirPath.child("player/jump.ogg").toFileWrapper()))))

                                    withComponent(LifeComponent(StateAction(PrefabSetup.PlayerStates.Small.index)))
                                }
                            }
                            withState("ladder-${it.stateName()}") {
                                withComponent(TextureComponent(0,
                                        TextureGroup("climb", PackRegionData(resourceWrapperOf(pack), "${it.stateName()}/climb_left"), PackRegionData(resourceWrapperOf(pack), "${it.stateName()}/climb_right")))
                                )

                                withComponent(InputComponent(
                                        InputComponent.InputData(GameKeys.GAME_PLAYER_LEFT.key, true, PhysicsAction(PhysicsAction.PhysicsActions.MOVE_LEFT)),
                                        InputComponent.InputData(GameKeys.GAME_PLAYER_RIGHT.key, true, PhysicsAction(PhysicsAction.PhysicsActions.MOVE_RIGHT)),
                                        InputComponent.InputData(GameKeys.GAME_PLAYER_UP.key, true, PhysicsAction(PhysicsAction.PhysicsActions.MOVE_UP)),
                                        InputComponent.InputData(GameKeys.GAME_PLAYER_DOWN.key, true, PhysicsAction(PhysicsAction.PhysicsActions.MOVE_DOWN)))
                                )

                                withComponent(PhysicsComponent(false, 5, MovementType.SMOOTH, false))

                                withComponent(LifeComponent(StateAction(PrefabSetup.PlayerStates.Small.index)))
                            }
                        }
                    }
                    .build()
    )),

    Furball_SMC(PrefabType.SMC, Prefab("furball",
            EntityBuilder(Tags.Enemy.tag, Size(48, 48))
                    .withDefaultState {
                        withComponent(TextureComponent(0, TextureGroup("walk", Constants.packsSMCDirPath.child("enemies.atlas").toFileWrapper(), "furball/brown/walk", 0.1f)))

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
                        withComponent(TextureComponent(0, TextureGroup("walk", Constants.packsSMCDirPath.child("enemies.atlas").toFileWrapper(), "turtle/green/walk", 0.33f)))

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

                        withComponent(TextureComponent(0, TextureGroup("walk", Constants.packsSMCDirPath.child("enemies.atlas").toFileWrapper(), "turtle/green/shell_move", 0.33f)))

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
                        withComponent(TextureComponent(0, TextureGroup("default", Constants.packsSMCDirPath.child("enemies.atlas").toFileWrapper(), "eato/green/eato", 0.15f, Animation.PlayMode.LOOP_PINGPONG)))

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
                        withComponent(TextureComponent(0, TextureGroup("default", PackRegionData(resourceWrapperOf(Constants.packsSMCDirPath.child("items.atlas").toFileWrapper()), "mushroom_red"))))

                        withComponent(PhysicsComponent(false, 5, collisionsActions = arrayListOf(
                                CollisionAction(BoxSide.All, action = MultiplexerAction(PrefabSetup.playerAction(
                                        StateSwitcherAction(
                                                PrefabSetup.PlayerStates.Small.index to StateAction(PrefabSetup.PlayerStates.Big.index),
                                                PrefabSetup.PlayerStates.Small.ladderIndex to StateAction(PrefabSetup.PlayerStates.Big.ladderIndex))), TweenAction(TweenFactory.RemoveEntity(), false))))
                        ))

                        withComponent(MoverComponent(5, 0))
                    }
                    .build()
    )),

    FirePlant_SMC(PrefabType.SMC, Prefab("fire plant",
            EntityBuilder(Tags.Special.tag, Size(45, 45))
                    .withDefaultState {
                        withComponent(TextureComponent(0, TextureGroup("default", PackRegionData(resourceWrapperOf(Constants.packsSMCDirPath.child("items.atlas").toFileWrapper()), "fireplant"))))

                        withComponent(PhysicsComponent(false, 0, collisionsActions = arrayListOf(
                                CollisionAction(BoxSide.All, action = MultiplexerAction(PrefabSetup.playerAction(StateAction(PrefabSetup.PlayerStates.Fire.index)), TweenAction(TweenFactory.RemoveEntity(), false))))
                        ))
                    }
                    .build()
    )),

    BoxSpawner_SMC(PrefabType.SMC,
            Prefab("box spawner", EntityBuilder(Tags.Special.tag, Size(48, 48))
                    .withDefaultState {
                        withComponent(TextureComponent(0, TextureGroup("default", PackRegionData(resourceWrapperOf(Constants.packsSMCDirPath.child("box.atlas").toFileWrapper()), "yellow/default"))))

                        withComponent(PhysicsComponent(true, collisionsActions = arrayListOf(
                                CollisionAction(BoxSide.Down, action = StateAction(1)))
                        ))
                    }
                    .withState("pop") {
                        withStartAction(SpawnSideAction(MushroomRed_SMC.prefab, BoxSide.Up, true))

                        withComponent(TextureComponent(0, TextureGroup("pop", PackRegionData(resourceWrapperOf(Constants.packsSMCDirPath.child("box.atlas").toFileWrapper()), "brown1_1"))))
                        withComponent(PhysicsComponent(true))
                    }
                    .withLayer(6)
                    .build()
            ));
    //endregion
}