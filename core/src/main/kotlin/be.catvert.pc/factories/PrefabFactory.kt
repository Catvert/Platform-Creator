package be.catvert.pc.factories

import be.catvert.pc.GameKeys
import be.catvert.pc.GameObject
import be.catvert.pc.Prefab
import be.catvert.pc.actions.*
import be.catvert.pc.components.basics.SoundComponent
import be.catvert.pc.components.basics.TweenComponent
import be.catvert.pc.components.graphics.AtlasComponent
import be.catvert.pc.components.logics.*
import be.catvert.pc.components.logics.ai.SimpleMoverAIComponent
import be.catvert.pc.utility.*
import be.catvert.pc.utility.toFileWrapper
import be.catvert.pc.*

/**
 * Objet permettant la création de prefab préfait
 */

enum class PrefabType {
    All, Kenney, SMC
}


private fun setupSprite(region: Pair<FileWrapper, String>) = Prefab("sprite", GameObject(Tags.Sprite.tag, box = Rect(size = Size(50, 50))) {
    this += AtlasComponent(0, AtlasComponent.AtlasData("default", region))
})

private fun setupPhysicsSprite(region: Pair<FileWrapper, String>) = Prefab("physics sprite", GameObject(Tags.Sprite.tag, box = Rect(size = Size(50, 50))) {
    this += AtlasComponent(0, AtlasComponent.AtlasData("default", region))
    this += PhysicsComponent(true)
})

enum class PrefabFactory(val type: PrefabType, val prefab: Prefab) {
    Empty(PrefabType.All, Prefab("empty", GameObject(Tags.Empty.tag, box = Rect(size = Size(50, 50))))),

    BlockEnemy(PrefabType.All,
            Prefab("block enemy", GameObject(Tags.Special.tag, box = Rect(size = Size(20, 20))) {
                this += PhysicsComponent(true, ignoreTags = arrayListOf(Tags.Player.tag))
            })
    ),
    //region Kenney
    Sprite_Kenney(PrefabType.Kenney, setupSprite(Constants.atlasKenneyDirPath.child("grassSheet.atlas").toFileWrapper() to "slice01_01")),
    PhysicsSprite_Kenney(PrefabType.Kenney, setupPhysicsSprite(Constants.atlasKenneyDirPath.child("grassSheet.atlas").toFileWrapper() to "slice01_01")),
    Player_Kenney(PrefabType.Kenney,
            Prefab("player", GameObject("player", box = Rect(size = Size(48, 98))) {
                val jumpSoundIndex = 0

                val atlas = Constants.atlasKenneyDirPath.child("aliens.atlas").toFileWrapper()

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
            }.apply {
                onOutOfMapAction = LevelAction(LevelAction.LevelActions.FAIL_EXIT)
            })
    ),
    Spider_Kenney(PrefabType.Kenney,
            Prefab("spider", GameObject(Tags.Enemy.tag, box = Rect(size = Size(48, 48))) {
                this += AtlasComponent(0, AtlasComponent.AtlasData("walk", Constants.atlasKenneyDirPath.child("enemies.atlas").toFileWrapper(), "spider_walk", 0.33f))
                this += PhysicsComponent(false, collisionsActions = arrayListOf(
                        CollisionAction(BoxSide.Left).apply { tagAction.action = LifeAction(LifeAction.LifeActions.REMOVE_LP) },
                        CollisionAction(BoxSide.Right).apply { tagAction.action = LifeAction(LifeAction.LifeActions.REMOVE_LP) },
                        CollisionAction(BoxSide.Down).apply { tagAction.action = LifeAction(LifeAction.LifeActions.REMOVE_LP) },
                        CollisionAction(BoxSide.Up).apply { action = LifeAction(LifeAction.LifeActions.REMOVE_LP) }
                ))
                this += TweenComponent(TweenFactory.RemoveGOTween())
                this += LifeComponent(TweenAction(0))
                this += SimpleMoverAIComponent(SimpleMoverAIComponent.SimpleMoverOrientation.HORIZONTAL, false).apply { onReverseAction = RenderAction(RenderAction.RenderActions.FLIP_X); onUnReverseAction = RenderAction(RenderAction.RenderActions.UNFLIP_X) }
            })
    ),
    SnakeSlime_Kenney(PrefabType.Kenney,
            Prefab("snake slime", GameObject(Tags.Enemy.tag, box = Rect(size = Size(35, 120))) {
                this += AtlasComponent(0, AtlasComponent.AtlasData("base", Constants.atlasKenneyDirPath.child("enemies.atlas").toFileWrapper(), "snakeSlime", 0.33f))
                this += PhysicsComponent(true, collisionsActions = arrayListOf(
                        CollisionAction(BoxSide.Left).apply { tagAction.action = LifeAction(LifeAction.LifeActions.REMOVE_LP) },
                        CollisionAction(BoxSide.Right).apply { tagAction.action = LifeAction(LifeAction.LifeActions.REMOVE_LP) },
                        CollisionAction(BoxSide.Down).apply { tagAction.action = LifeAction(LifeAction.LifeActions.REMOVE_LP) },
                        CollisionAction(BoxSide.Up).apply { tagAction.action = LifeAction(LifeAction.LifeActions.REMOVE_LP) }
                ))
                this += LifeComponent(RemoveGOAction())
            })
    ),
    Bee_Kenney(PrefabType.Kenney,
            Prefab("bee", GameObject(Tags.Enemy.tag, box = Rect(size = Size(35, 35))) {
                this += AtlasComponent(0, AtlasComponent.AtlasData("default", Constants.atlasKenneyDirPath.child("enemies.atlas").toFileWrapper() to "bee"))
                this += PhysicsComponent(false, 5, gravity = false, collisionsActions = arrayListOf(
                        CollisionAction(BoxSide.Left).apply { tagAction.action = LifeAction(LifeAction.LifeActions.REMOVE_LP) },
                        CollisionAction(BoxSide.Right).apply { tagAction.action = LifeAction(LifeAction.LifeActions.REMOVE_LP) },
                        CollisionAction(BoxSide.Down).apply { tagAction.action = LifeAction(LifeAction.LifeActions.REMOVE_LP) },
                        CollisionAction(BoxSide.Up).apply { action = LifeAction(LifeAction.LifeActions.REMOVE_LP) }
                ))
                this += LifeComponent(RemoveGOAction())
                this += SimpleMoverAIComponent(SimpleMoverAIComponent.SimpleMoverOrientation.HORIZONTAL, false).apply { onReverseAction = RenderAction(RenderAction.RenderActions.FLIP_X); onUnReverseAction = RenderAction(RenderAction.RenderActions.UNFLIP_X) }
            })
    ),
    GoldCoin_Kenney(PrefabType.Kenney,
            Prefab("gold coin", GameObject(Tags.Special.tag, box = Rect(size = Size(35, 35))) {
                this += AtlasComponent(0, AtlasComponent.AtlasData("default", Constants.atlasKenneyDirPath.child("jumper.atlas").toFileWrapper() to "coin_gold"))
                this += SoundComponent(SoundComponent.SoundData(Constants.soundsDirPath.child("coin.wav").toFileWrapper()))
                this += PhysicsComponent(true, sensor = SensorData(true, sensorIn = MultiplexerAction(SoundAction(0), ScoreAction(1), RemoveGOAction())))
            })
    ),
    //endregion
    //region SMC
    Sprite_SMC(PrefabType.SMC, setupSprite(Constants.atlasSMCDirPath.child("smc_blocks.atlas").toFileWrapper() to "brick/Brick Blue")),
    PhysicsSprite_SMC(PrefabType.SMC, setupPhysicsSprite(Constants.atlasSMCDirPath.child("smc_blocks.atlas").toFileWrapper() to "brick/Brick Blue"));
    //endregion
}