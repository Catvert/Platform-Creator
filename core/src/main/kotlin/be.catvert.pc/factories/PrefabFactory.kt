package be.catvert.pc.factories

import be.catvert.pc.GameKeys
import be.catvert.pc.GameObject
import be.catvert.pc.Prefab
import be.catvert.pc.actions.*
import be.catvert.pc.components.SoundComponent
import be.catvert.pc.components.logics.*
import be.catvert.pc.components.logics.ai.*
import be.catvert.pc.utility.Constants
import be.catvert.pc.utility.Point
import be.catvert.pc.utility.Rect
import be.catvert.pc.utility.Size
import ktx.assets.toLocalFile
import java.util.*
import be.catvert.pc.components.graphics.*
/**
 * Objet permettant la création de prefab préfait
 */

enum class PrefabFactory(val prefab: Prefab) {
    Empty(Prefab("empty", "Catvert", GameObject(GameObject.Tag.Sprite))),
    Sprite(
        Prefab("sprite", "Catvert", GameObject(GameObject.Tag.Sprite, UUID.randomUUID(), Rect(Point(), Size(50, 50)), null) {
            this += AtlasComponent()
        })
    ),
    PhysicsSprite(
        Prefab("physicsSprite", "Catvert", GameObject(GameObject.Tag.PhysicsSprite, UUID.randomUUID(), Rect(Point(), Size(50, 50)), null) {
            this += AtlasComponent()
            this += PhysicsComponent(true)
        })
    ),
    Player(
        Prefab("player", "Catvert", GameObject(GameObject.Tag.Player, UUID.randomUUID(), Rect(Point(), Size(48, 98)), null) {
            val jumpSoundIndex = 0

            this += AtlasComponent((Constants.atlasDirPath + "More Enemies Animations/aliens.atlas").toLocalFile(), "alienGreen_stand")

            this += PhysicsComponent(false, 10, MovementType.SMOOTH, jumpHeight = 200, jumpAction = SoundAction(jumpSoundIndex))

            this += SoundComponent((Constants.soundsDirPath + "player/jump.ogg").toLocalFile())

            this += InputComponent(GameKeys.GAME_PLAYER_LEFT.key, false, MultiplexerAction(arrayOf(PhysicsAction(NextPhysicsActions.GO_LEFT), RenderAction(RenderAction.RenderActions.FLIP_X))))

            this += InputComponent(GameKeys.GAME_PLAYER_RIGHT.key, false, MultiplexerAction(arrayOf(PhysicsAction(NextPhysicsActions.GO_RIGHT), RenderAction(RenderAction.RenderActions.UNFLIP_X))))

            this += InputComponent(GameKeys.GAME_PLAYER_GOD_UP.key, false, PhysicsAction(NextPhysicsActions.GO_UP))

            this += InputComponent(GameKeys.GAME_PLAYER_GOD_DOWN.key, false, PhysicsAction(NextPhysicsActions.GO_DOWN))
            this += InputComponent(GameKeys.GAME_PLAYER_JUMP.key, true, PhysicsAction(NextPhysicsActions.JUMP))

            this += LifeComponent(LevelAction(LevelAction.LevelActions.FAIL_EXIT), setOf())
        }.apply {
            keepActive = true
            unique = true
        })
    ),
    Spider(
        Prefab("spider", "Catvert", GameObject(GameObject.Tag.Enemy, UUID.randomUUID(), Rect(Point(), Size(48, 48)), null) {
            this += AnimationComponent((Constants.atlasDirPath + "More Enemies Animations/enemies.atlas").toLocalFile(), "spider_walk", 0.33f)
            this += PhysicsComponent(false, 5)
            this += LifeComponent(RemoveGOAction(), setOf())
            this += AIComponent(LifeAction(LifeActions.REMOVE_LP) to listOf(CollisionSide.OnDown, CollisionSide.OnRight, CollisionSide.OnLeft), LifeAction(LifeActions.REMOVE_LP) to listOf(CollisionSide.OnUp))
        })
    )
}