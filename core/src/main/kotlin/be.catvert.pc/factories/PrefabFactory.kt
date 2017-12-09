package be.catvert.pc.factories

import be.catvert.pc.GameKeys
import be.catvert.pc.GameObject
import be.catvert.pc.Prefab
import be.catvert.pc.actions.*
import be.catvert.pc.components.SoundComponent
import be.catvert.pc.components.TweenComponent
import be.catvert.pc.components.graphics.AtlasComponent
import be.catvert.pc.components.logics.*
import be.catvert.pc.components.logics.ai.AIComponent
import be.catvert.pc.components.logics.ai.SimpleMoverAIComponent
import be.catvert.pc.utility.*
import be.catvert.pc.utility.toFileWrapper
import be.catvert.pc.*
/**
 * Objet permettant la création de prefab préfait
 */

enum class PrefabFactory(val prefab: Prefab) {
    Empty(Prefab("empty", GameObject(Tags.Empty.tag, box = Rect(size = Size(50, 50))))),
    Sprite(
            Prefab("sprite", GameObject(Tags.Sprite.tag, box = Rect(size = Size(50, 50))) {
                this += AtlasComponent(0, AtlasComponent.AtlasData("default", Constants.atlasDirPath.child("Extended Tiles/grassSheet.atlas").toFileWrapper() to "slice01_01"))
            })
    ),
    PhysicsSprite(
            Prefab("physics sprite", GameObject(Tags.Sprite.tag, box = Rect(size = Size(50, 50))) {
                this += AtlasComponent(0, AtlasComponent.AtlasData("default", Constants.atlasDirPath.child("Extended Tiles/grassSheet.atlas").toFileWrapper() to "slice01_01"))
                this += PhysicsComponent(true)
            })
    ),
    Player(
            Prefab("player", GameObject("player", box = Rect(size = Size(48, 98))) {
                val jumpSoundIndex = 0

                val atlas = Constants.atlasDirPath.child("More Enemies Animations/aliens.atlas").toFileWrapper()

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

                this += SoundComponent(SoundComponent.SoundData(Constants.soundsDirPath.child("player/jump.ogg")))

                this += LifeComponent(LevelAction(LevelAction.LevelActions.FAIL_EXIT))
            }.apply {
                onOutOfMapAction = LevelAction(LevelAction.LevelActions.FAIL_EXIT)
            })
    ),
    Spider(
            Prefab("spider", GameObject(Tags.Enemy.tag, box = Rect(size = Size(48, 48))) {
                this += AtlasComponent(0, AtlasComponent.AtlasData("walk", Constants.atlasDirPath.child("More Enemies Animations/enemies.atlas").toFileWrapper(), "spider_walk", 0.33f))
                this += PhysicsComponent(false, 5)
                this += TweenComponent(TweenFactory.RemoveGOTween())
                this += LifeComponent(TweenAction(0))
                this += AIComponent(arrayListOf(Tags.Player.tag), LifeAction(LifeAction.LifeActions.REMOVE_LP), arrayListOf(BoxSide.Down, BoxSide.Right, BoxSide.Left), LifeAction(LifeAction.LifeActions.REMOVE_LP), arrayListOf(BoxSide.Up))
                this += SimpleMoverAIComponent(SimpleMoverAIComponent.SimpleMoverOrientation.HORIZONTAL, false).apply { onReverseAction = RenderAction(RenderAction.RenderActions.FLIP_X); onUnReverseAction = RenderAction(RenderAction.RenderActions.UNFLIP_X) }
            })
    ),
    SnakeSlime(
            Prefab("snake slime", GameObject(Tags.Enemy.tag, box = Rect(size = Size(35, 120))) {
                this += AtlasComponent(0, AtlasComponent.AtlasData("base", Constants.atlasDirPath.child("More Enemies Animations/enemies.atlas").toFileWrapper(), "snakeSlime", 0.33f))
                this += PhysicsComponent(true)
                this += LifeComponent(RemoveGOAction())
                this += AIComponent(arrayListOf(Tags.Player.tag), LifeAction(LifeAction.LifeActions.REMOVE_LP), arrayListOf(BoxSide.Down, BoxSide.Right, BoxSide.Left, BoxSide.Down), LifeAction(LifeAction.LifeActions.REMOVE_LP), arrayListOf())
            })
    ),
    Bee(
            Prefab("bee", GameObject(Tags.Enemy.tag, box = Rect(size = Size(35, 35))) {
                this += AtlasComponent(0, AtlasComponent.AtlasData("default", Constants.atlasDirPath.child("More Enemies Animations/enemies.atlas").toFileWrapper() to "bee"))
                this += PhysicsComponent(false, 5, gravity = false)
                this += LifeComponent(RemoveGOAction())
                this += AIComponent(arrayListOf(Tags.Player.tag), LifeAction(LifeAction.LifeActions.REMOVE_LP), arrayListOf(BoxSide.Down, BoxSide.Right, BoxSide.Left), LifeAction(LifeAction.LifeActions.REMOVE_LP), arrayListOf(BoxSide.Up))
                this += SimpleMoverAIComponent(SimpleMoverAIComponent.SimpleMoverOrientation.HORIZONTAL, false).apply { onReverseAction = RenderAction(RenderAction.RenderActions.FLIP_X); onUnReverseAction = RenderAction(RenderAction.RenderActions.UNFLIP_X) }
            })
    ),
    GoldCoin(
            Prefab("gold coin", GameObject(Tags.Special.tag, box = Rect(size = Size(35, 35))) {
                this += AtlasComponent(0, AtlasComponent.AtlasData("default", Constants.atlasDirPath.child("Jumper Pack/spritesheet_jumper.atlas").toFileWrapper() to "coin_gold"))
                this += PhysicsComponent(true)
                this += SoundComponent(SoundComponent.SoundData(Constants.soundsDirPath.child("coin.wav")))
                this += AIComponent(arrayListOf(Tags.Player.tag), EmptyAction(), arrayListOf(), MultiplexerAction(ScoreAction(1), RemoveGOAction(), SoundAction(0)), arrayListOf(BoxSide.Left, BoxSide.Right, BoxSide.Up, BoxSide.Down))
            })
    ),
    BlockEnemy(
            Prefab("block enemy", GameObject(Tags.Special.tag, box = Rect(size = Size(20, 20))) {
                this += PhysicsComponent(true, ignoreTags = arrayListOf(Tags.Player.tag))
            })
    );
}