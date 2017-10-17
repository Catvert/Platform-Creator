package be.catvert.pc.factories

import be.catvert.pc.GameKeys
import be.catvert.pc.Prefab
import be.catvert.pc.actions.Direction
import be.catvert.pc.actions.MoveAction
import be.catvert.pc.components.Component
import be.catvert.pc.components.graphics.*
import be.catvert.pc.components.logics.*
import be.catvert.pc.utility.Constants
import com.badlogic.gdx.Gdx
import be.catvert.pc.utility.Size

/**
 * Objet permettant la création de prefab préfait
 */

enum class PrefabFactory(val generate: () -> Prefab) {
    Player(
            {
                Prefab("player", false, "Catvert", Size(48, 88), kotlin.run {
                    val comp = mutableSetOf<Component>()

                    val moveSpeed = 10

                    comp += AtlasComponent(Constants.atlasDirPath + "More Enemies Animations/aliens.atlas", "alienBeige")

                    comp += InputComponent(GameKeys.GAME_PLAYER_LEFT.key, false, MoveAction(Direction.LEFT, moveSpeed))
                    comp += InputComponent(GameKeys.GAME_PLAYER_RIGHT.key, false, MoveAction(Direction.RIGHT, moveSpeed))
                    comp
                })
            })
}