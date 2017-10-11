package be.catvert.pc

import be.catvert.pc.factories.EntityPrefabFactory
import be.catvert.pc.utility.Constants
import be.catvert.pc.utility.Point
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle

class Level(val levelPath: FileHandle, val levelName: String, val gameVersion: GameVersion, var backgroundPath: String? = null) : GameObjectContainer {
    override val gameObjects: MutableSet<GameObject> = mutableSetOf()

    companion object {
        fun newLevel(levelName: String): Level {
            val level = Level(Gdx.files.local(Constants.levelDirPath + levelName + Constants.levelExtension), levelName, Constants.gameVersion)

            EntityPrefabFactory.generatePlayer().generate(Point(100, 100), level)

            return level
        }
    }
}