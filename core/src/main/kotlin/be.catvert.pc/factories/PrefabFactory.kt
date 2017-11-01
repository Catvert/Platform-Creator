package be.catvert.pc.factories

import be.catvert.pc.GameKeys
import be.catvert.pc.GameObject
import be.catvert.pc.Prefab
import be.catvert.pc.actions.LevelAction
import be.catvert.pc.actions.NextPhysicsActions
import be.catvert.pc.actions.PhysicsAction
import be.catvert.pc.components.Component
import be.catvert.pc.components.graphics.*
import be.catvert.pc.components.logics.*
import be.catvert.pc.components.logics.ai.*
import be.catvert.pc.components.SoundComponent
import be.catvert.pc.utility.Constants
import be.catvert.pc.utility.Size
import be.catvert.pc.actions.*
import be.catvert.pc.components.logics.ai.SimpleAIComponent
import ktx.assets.toLocalFile

/**
 * Objet permettant la création de prefab préfait
 */

enum class PrefabFactory(val generate: () -> Prefab) {
    Sprite({
        Prefab("sprite", false, "Catvert", GameObject.Tag.Sprite, Size(50, 50), run {
            val comps = mutableSetOf<Component>()
            comps += AtlasComponent()
            comps
        })
    }),
    PhysicsSprite({
        Prefab("physicsSprite", false, "Catvert", GameObject.Tag.PhysicsSprite, Size(50, 50), run {
            val comps = mutableSetOf<Component>()
            comps += AtlasComponent()
            comps += PhysicsComponent(true)
            comps
        })
    }),
    Player(
            {
                Prefab("player", false, "Catvert", GameObject.Tag.Player, Size(48, 98), run {
                    val jumpSoundIndex = 0

                    val comps = mutableSetOf<Component>()

                    comps += AtlasComponent((Constants.atlasDirPath + "More Enemies Animations/aliens.atlas").toLocalFile(), "alienGreen_stand")

                    comps += PhysicsComponent(false, 10, MovementType.SMOOTH, jumpHeight = 200, jumpAction = SoundAction(jumpSoundIndex))

                    comps += SoundComponent((Constants.soundsDirPath + "player/jump.ogg").toLocalFile())

                    comps += InputComponent(GameKeys.GAME_PLAYER_LEFT.key, false, MultiplexerAction(arrayOf(PhysicsAction(NextPhysicsActions.GO_LEFT), RenderAction(RenderAction.RenderActions.FLIP_X))) )

                    comps += InputComponent(GameKeys.GAME_PLAYER_RIGHT.key, false, MultiplexerAction(arrayOf(PhysicsAction(NextPhysicsActions.GO_RIGHT), RenderAction(RenderAction.RenderActions.UNFLIP_X))))

                    comps += InputComponent(GameKeys.GAME_PLAYER_GOD_UP.key, false, PhysicsAction(NextPhysicsActions.GO_UP) )

                    comps += InputComponent(GameKeys.GAME_PLAYER_GOD_DOWN.key, false, PhysicsAction(NextPhysicsActions.GO_DOWN))
                    comps += InputComponent(GameKeys.GAME_PLAYER_JUMP.key, true,  PhysicsAction(NextPhysicsActions.JUMP) )

                    comps
                }, {
                    onRemoveAction = LevelAction(LevelAction.LevelActions.FAIL_EXIT)
                    keepActive = true
                    unique = true
                })
            }),
    Spider({
        Prefab("spider", false, "Catvert", GameObject.Tag.Enemy, Size(48, 48), run {
            val comps = mutableSetOf<Component>()
            comps += AnimationComponent((Constants.atlasDirPath + "More Enemies Animations/enemies.atlas").toLocalFile(), "spider_walk", 0.33f)
            comps += PhysicsComponent(false, 5)
            comps += SimpleAIComponent(true)
            comps
        })
    })
}