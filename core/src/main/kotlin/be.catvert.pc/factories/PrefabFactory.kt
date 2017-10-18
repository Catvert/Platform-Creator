package be.catvert.pc.factories

import be.catvert.pc.GameObject
import be.catvert.pc.Prefab
import be.catvert.pc.components.logics.*
import be.catvert.pc.components.graphics.*
import be.catvert.pc.components.Component
import be.catvert.pc.utility.Constants
import be.catvert.pc.GameKeys
import be.catvert.pc.utility.Size
import be.catvert.pc.actions.*
/**
 * Objet permettant la création de prefab préfait
 */

enum class PrefabFactory(val generate: () -> Prefab) {
    Player(
            {
                Prefab("player", false, "Catvert", GameObject.Tag.Player, Size(48, 88), kotlin.run {
                    val comp = mutableSetOf<Component>()

                    comp += AtlasComponent(Constants.atlasDirPath + "More Enemies Animations/aliens.atlas", "alienBeige")

                    val physics = PhysicsComponent(false, 10, MovementType.SMOOTH)

                    physics.jumpData = JumpData(200)

                    comp += physics

                    comp += InputComponent(GameKeys.GAME_PLAYER_LEFT.key, false, PhysicsAction(NextPhysicsActions.GO_LEFT))
                    comp += InputComponent(GameKeys.GAME_PLAYER_RIGHT.key, false, PhysicsAction(NextPhysicsActions.GO_RIGHT))
                    comp += InputComponent(GameKeys.GAME_PLAYER_GOD_UP.key, false, PhysicsAction(NextPhysicsActions.GO_UP))
                    comp += InputComponent(GameKeys.GAME_PLAYER_GOD_DOWN.key, false, PhysicsAction(NextPhysicsActions.GO_DOWN))
                    comp += InputComponent(GameKeys.GAME_PLAYER_JUMP.key, true, PhysicsAction(NextPhysicsActions.JUMP))
                    comp
                })
            })
}