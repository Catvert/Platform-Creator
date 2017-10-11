package be.catvert.pc.factories

import be.catvert.pc.GameKeys
import be.catvert.pc.Prefab
import be.catvert.pc.actions.Direction
import be.catvert.pc.actions.MoveAction
import be.catvert.pc.components.Component
import be.catvert.pc.components.graphics.AtlasComponent
import be.catvert.pc.components.logics.InputComponent
import be.catvert.pc.utility.Constants
import be.catvert.pc.utility.Size
import com.badlogic.gdx.Gdx

/**
 * Objet permettant la création de prefab préfait
 */
object EntityPrefabFactory {
    /**
     * Permet de généré le joueur
     */
    fun generatePlayer() = Prefab("player", false, "Catvert", Size(48, 88), kotlin.run {
        val comp = mutableSetOf<Component>()

        val moveSpeed = 10

        comp += AtlasComponent(Gdx.files.local(Constants.atlasDirPath + "More Enemies Animations/aliens.atlas"), "alienBeige")

        comp += InputComponent(GameKeys.GAME_PLAYER_LEFT.key, false, MoveAction(Direction.LEFT, moveSpeed))
        comp += InputComponent(GameKeys.GAME_PLAYER_RIGHT.key, false, MoveAction(Direction.RIGHT, moveSpeed))
        comp
    })
}