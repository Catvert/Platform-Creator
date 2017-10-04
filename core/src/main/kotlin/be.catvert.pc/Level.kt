package be.catvert.pc

class Level(val levelName: String): GameObjectContainer {
    override val gameObjects: MutableSet<GameObject> = mutableSetOf()
    var backgroundPath: String? = null
}