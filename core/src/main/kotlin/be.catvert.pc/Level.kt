package be.catvert.pc

import be.catvert.pc.factories.PrefabFactory
import be.catvert.pc.serialization.SerializationFactory
import be.catvert.pc.utility.Constants
import be.catvert.pc.utility.Point
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle

class Level(val levelPath: String, val levelName: String,  val gameVersion: GameVersion, var backgroundPath: String? = null) : GameObjectContainer() {
    companion object {
        fun newLevel(levelName: String): Level {
            val level = Level(Constants.levelDirPath + levelName + Constants.levelExtension, levelName, Constants.gameVersion)

            PrefabFactory.Player.generate().generate(Point(100, 100), level)

            return level
        }
    }
}