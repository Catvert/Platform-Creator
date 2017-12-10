package be.catvert.pc.utility

import com.badlogic.gdx.math.Vector2


/**
 * Classe de donnée représentant un point dans l'espace
 */
data class Point(val x: Int = 0, val y: Int = 0) {
    override fun toString(): String = "{ $x ; $y }"
}