package be.catvert.pc

typealias GameObjectTag = String

enum class Tags(val tag: GameObjectTag) {
    Empty("empty"),
    Sprite("sprite"),
    Player("player"),
    Enemy("enemy"),
    Special("special")
}