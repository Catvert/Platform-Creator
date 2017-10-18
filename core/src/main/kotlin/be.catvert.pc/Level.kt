package be.catvert.pc

import be.catvert.pc.factories.PrefabFactory
import be.catvert.pc.utility.Constants
import be.catvert.pc.utility.Point
import java.util.*

class Level(val levelPath: String, val levelName: String, val gameVersion: GameVersion, val playerUUID: UUID, var backgroundPath: String? = null) : GameObjectMatrixContainer() {
    companion object {
        fun newLevel(levelName: String): Level {
            val player = PrefabFactory.Player.generate().generateWithoutContainer(Point(100, 100))

            val level = Level(Constants.levelDirPath + levelName + Constants.levelExtension, levelName, Constants.gameVersion, player.id)

            level.addGameObject(player)

            return level
        }
    }

    override fun onPostDeserialization() {
        super.onPostDeserialization()
        followGameObject = findGameObjectByID(playerUUID)
    }
/*
    if (killEntityNegativeY && it is TransformComponent && it.rectangle.y < 0) {
        if (lifeMapper.has(entity))
            LifeComponent.killInstant(entity)
        removeEntity(entity)
    }*/
}
