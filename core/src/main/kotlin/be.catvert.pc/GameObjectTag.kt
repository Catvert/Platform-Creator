package be.catvert.pc

        /**
         * Permet de différencier un Tag d'un string dans le code pour le rendre plus clair.
         */
typealias GameObjectTag = String

/**
 * Permet de différencier les différents gameObjects en leur attribuant un tag.
 * @see TagAction
 */
enum class Tags(val tag: GameObjectTag) {
    Empty("empty"),
    Sprite("sprite"),
    Player("player"),
    Enemy("enemy"),
    Special("special")
}