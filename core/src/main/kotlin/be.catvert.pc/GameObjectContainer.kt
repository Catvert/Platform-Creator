package be.catvert.pc

interface GameObjectContainer {
    val gameObjects: MutableSet<GameObject>

    fun removeGameObject(gameObject: GameObject) {
        gameObjects.remove(gameObject)
        gameObject.container = null
    }

    fun addGameObject(gameObject: GameObject) {
        gameObjects.add(gameObject)
        gameObject.container = this
    }

    fun addContainer(gameObjectContainer: GameObjectContainer) {
        gameObjects.addAll(gameObjectContainer.gameObjects)
    }
}