package be.catvert.pc.utility

enum class BoxSide {
    Left, Right, Up, Down, All;

    /**
     * Permet d'inverser le côté de la collision
     */
    operator fun unaryMinus(): BoxSide = when (this) {
        BoxSide.Left -> Right
        BoxSide.Right -> Left
        BoxSide.Up -> Down
        BoxSide.Down -> Up
        BoxSide.All -> All
    }
}