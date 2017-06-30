package be.catvert.plateformcreator.ecs.components

import be.catvert.plateformcreator.Point
import be.catvert.plateformcreator.ecs.systems.physics.GridCell
import com.badlogic.gdx.math.Rectangle

/**
 * Created by Catvert on 03/06/17.
 */

/**
 * Ce component permet d'ajouter un système de positionnement à l'entité
 * \!/ Beaucoup des autres components, tel que renderComponent nécessite ce component en dépendance !
 * @property rectangle : Le rectangle de l'entité (position + taille)
 * @property fixedSizeEditor : Permet de spécifier si la taille de l'entité est fixe et ne peut donc pas être changée dans l'éditeur.
 */
class TransformComponent(var rectangle: Rectangle = Rectangle(), var fixedSizeEditor: Boolean = false) : BaseComponent<TransformComponent>() {
    override fun copy(): TransformComponent {
        return TransformComponent(Rectangle(rectangle), fixedSizeEditor)
    }

    /**
     * Représente les cells où l'entité ce trouve (mis à jour lors de chaque déplacement de l'entité)
     */
    val gridCell: MutableList<GridCell> = mutableListOf()

    /**
     * Retourne la position actuelle de l'entité
     */
    fun position() = Point(rectangle.x.toInt(), rectangle.y.toInt())
}