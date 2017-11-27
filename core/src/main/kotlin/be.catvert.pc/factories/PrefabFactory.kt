package be.catvert.pc.factories

import be.catvert.pc.GameKeys
import be.catvert.pc.GameObject
import be.catvert.pc.Prefab
import be.catvert.pc.actions.*
import be.catvert.pc.components.SoundComponent
import be.catvert.pc.components.TweenComponent
import be.catvert.pc.components.graphics.AnimationComponent
import be.catvert.pc.components.graphics.AtlasComponent
import be.catvert.pc.components.logics.*
import be.catvert.pc.components.logics.ai.AIComponent
import be.catvert.pc.components.logics.ai.SimpleMoverComponent
import be.catvert.pc.utility.Constants
import be.catvert.pc.utility.Rect
import be.catvert.pc.utility.Size

/**
 * Objet permettant la création de prefab préfait
 */

enum class PrefabFactory(val prefab: Prefab) {
    Empty(Prefab("empty", "Catvert", GameObject(GameObject.Tag.Sprite, Rect(size = Size(50, 50))))),
    Sprite(
            Prefab("sprite", "Catvert", GameObject(GameObject.Tag.Sprite, Rect(size = Size(50, 50)), null) {
                this += AtlasComponent()
            })
    ),
    PhysicsSprite(
            Prefab("physicsSprite", "Catvert", GameObject(GameObject.Tag.PhysicsSprite, Rect(size = Size(50, 50)), null) {
                this += AtlasComponent()
                this += PhysicsComponent(true)
            })
    ),
    Player(
            Prefab("player", "Catvert", GameObject(GameObject.Tag.Player, Rect(size = Size(48, 98)), null) {
                val jumpSoundIndex = 0

                this += AtlasComponent(Constants.atlasDirPath.child("More Enemies Animations/aliens.atlas"), "alienGreen_stand")

                this += PhysicsComponent(false, 10, MovementType.SMOOTH, jumpHeight = 200, jumpAction = SoundAction(jumpSoundIndex))

                this += SoundComponent(arrayOf(SoundComponent.SoundData(Constants.soundsDirPath.child("player/jump.ogg"))))

                this += InputComponent(arrayOf(
                        InputComponent.InputData(GameKeys.GAME_PLAYER_LEFT.key, false, MultiplexerAction(arrayOf(PhysicsAction(PhysicsAction.PhysicsActions.GO_LEFT), RenderAction(RenderAction.RenderActions.FLIP_X)))),
                        InputComponent.InputData(GameKeys.GAME_PLAYER_RIGHT.key, false, MultiplexerAction(arrayOf(PhysicsAction(PhysicsAction.PhysicsActions.GO_RIGHT), RenderAction(RenderAction.RenderActions.UNFLIP_X)))),
                        InputComponent.InputData(GameKeys.GAME_PLAYER_GOD_UP.key, false, PhysicsAction(PhysicsAction.PhysicsActions.GO_UP)),
                        InputComponent.InputData(GameKeys.GAME_PLAYER_GOD_DOWN.key, false, PhysicsAction(PhysicsAction.PhysicsActions.GO_DOWN)),
                        InputComponent.InputData(GameKeys.GAME_PLAYER_JUMP.key, true, PhysicsAction(PhysicsAction.PhysicsActions.JUMP))
                ))

                this += TweenComponent(arrayOf(TweenFactory.RemoveGOTween().apply { endAction = LevelAction(LevelAction.LevelActions.FAIL_EXIT) }))

                this += LifeComponent(TweenAction(0))
            }.apply {
                onOutOfMapAction = LevelAction(LevelAction.LevelActions.FAIL_EXIT)
                keepActive = true
            })
    ),
    Spider(
            Prefab("spider", "Catvert", GameObject(GameObject.Tag.Enemy, Rect(size = Size(48, 48)), null) {
                this += AnimationComponent(0, arrayOf(AnimationComponent.AnimationData(Constants.atlasDirPath.child("More Enemies Animations/enemies.atlas"), "spider_walk", 0.33f)))
                this += PhysicsComponent(false, 5)
                this += LifeComponent(RemoveGOAction())
                this += AIComponent(LifeAction(LifeAction.LifeActions.REMOVE_LP), arrayOf(CollisionSide.OnDown, CollisionSide.OnRight, CollisionSide.OnLeft), LifeAction(LifeAction.LifeActions.REMOVE_LP), arrayOf(CollisionSide.OnUp))
                this += SimpleMoverComponent(SimpleMoverComponent.SimpleMoverOrientation.HORIZONTAL, false).apply { onReverseAction = RenderAction(RenderAction.RenderActions.FLIP_X); onUnReverseAction = RenderAction(RenderAction.RenderActions.UNFLIP_X) }
            })
    ),
    SnakeSlime(
            Prefab("snake slime", "Catvert", GameObject(GameObject.Tag.Enemy, Rect(size = Size(35, 120)), null) {
                this += AnimationComponent(0, arrayOf(AnimationComponent.AnimationData(Constants.atlasDirPath.child("More Enemies Animations/enemies.atlas"), "snakeSlime", 0.33f)))
                this += PhysicsComponent(true)
                this += LifeComponent(RemoveGOAction())
                this += AIComponent(LifeAction(LifeAction.LifeActions.REMOVE_LP), arrayOf(CollisionSide.OnDown, CollisionSide.OnRight, CollisionSide.OnLeft, CollisionSide.OnDown), LifeAction(LifeAction.LifeActions.REMOVE_LP), arrayOf())
            })
    ),
    Bee(
            Prefab("bee", "Catvert", GameObject(GameObject.Tag.Enemy, Rect(size = Size(35, 35)), null) {
                this += AtlasComponent(Constants.atlasDirPath.child("More Enemies Animations/enemies.atlas"), "bee")
                this += PhysicsComponent(false, 5, gravity = false)
                this += LifeComponent(RemoveGOAction())
                this += AIComponent(LifeAction(LifeAction.LifeActions.REMOVE_LP), arrayOf(CollisionSide.OnDown, CollisionSide.OnRight, CollisionSide.OnLeft), LifeAction(LifeAction.LifeActions.REMOVE_LP), arrayOf(CollisionSide.OnUp))
                this += SimpleMoverComponent(SimpleMoverComponent.SimpleMoverOrientation.HORIZONTAL, false).apply { onReverseAction = RenderAction(RenderAction.RenderActions.FLIP_X); onUnReverseAction = RenderAction(RenderAction.RenderActions.UNFLIP_X) }
            })
    ),
    GoldCoin(
            Prefab("gold coin", "Catvert", GameObject(GameObject.Tag.Special, Rect(size = Size(35, 35)), null) {
                this += AtlasComponent(Constants.atlasDirPath.child("Jumper Pack/spritesheet_jumper.atlas"), "coin_gold")
                this += PhysicsComponent(true)
                this += SoundComponent(arrayOf(SoundComponent.SoundData(Constants.soundsDirPath.child("coin.wav"))))
                this += AIComponent(EmptyAction(), arrayOf(), MultiplexerAction(arrayOf(ScoreAction(1), RemoveGOAction(), SoundAction(0))), arrayOf(CollisionSide.OnLeft, CollisionSide.OnRight, CollisionSide.OnUp, CollisionSide.OnDown))
            })
    ),
    BlockEnemy(
            Prefab("block enemy", "Catvert", GameObject(GameObject.Tag.Special, Rect(size = Size(20, 20)), null) {
                this += PhysicsComponent(true, maskCollision = MaskCollision.ONLY_ENEMY)
            })
    ),
}