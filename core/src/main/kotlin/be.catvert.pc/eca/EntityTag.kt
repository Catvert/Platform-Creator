package be.catvert.pc.eca

/**
 * Permet de différencier un String d'un tag
 */
typealias EntityTag = String

/**
 * Catégories par défaut
 * @see TagAction
 */
enum class Tags(val tag: EntityTag) {
    Empty("empty"),
    Sprite("sprite"),
    Player("player"),
    Enemy("enemy"),
    Special("special")
}