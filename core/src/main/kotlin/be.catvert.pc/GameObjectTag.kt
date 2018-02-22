package be.catvert.pc

        /**
         * Permet de différencier un String d'un tag
         */
typealias GameObjectTag = String

/**
 * Catégories par défaut
 * @see TagAction
 */
enum class Tags(val tag: GameObjectTag) {
    Empty("empty"),
    Sprite("sprite"),
    Player("player"),
    Enemy("enemy"),
    Special("special")
}