package be.catvert.pc.utility

/**
 * Représente un côté d'un rectangle
 */
enum class BoxSide {
    Left, Right, Up, Down, All;

    /**
     * Permet d'inverser le côté
     */
    operator fun unaryMinus(): BoxSide = when (this) {
        BoxSide.Left -> Right
        BoxSide.Right -> Left
        BoxSide.Up -> Down
        BoxSide.Down -> Up
        BoxSide.All -> All
    }

    override fun toString() = when (this) {
        BoxSide.Left -> "Gauche"
        BoxSide.Right -> "Droite"
        BoxSide.Up -> "Haut"
        BoxSide.Down -> "Bas"
        BoxSide.All -> "Tous"
    }
}