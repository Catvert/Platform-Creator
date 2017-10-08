package be.catvert.pc

import com.badlogic.gdx.files.FileHandle

class Level(val levelPath: FileHandle, val levelName: String, var backgroundPath: String? = null): GameObjectContainer {
    override val gameObjects: MutableSet<GameObject> = mutableSetOf()
}