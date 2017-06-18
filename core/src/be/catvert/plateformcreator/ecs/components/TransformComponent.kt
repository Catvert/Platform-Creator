package be.catvert.plateformcreator.ecs.components

import be.catvert.plateformcreator.ecs.systems.physics.GridCell
import com.badlogic.gdx.math.Rectangle

/**
* Created by Catvert on 03/06/17.
*/

/**
 * Ce component permet d'ajouter un système de positionnement à l'entité
 * \!/ Beaucoup des autres components, tel que renderComponent nécessite ce component en dépendance !
 * rectangle : Le rectangle de l'entité (position + taille)
 * gridCell : Les cells où l'entité se trouve.
 * fixedSizeEditor : Permet de spécifier si la taille de l'entité est fixe et ne peut donc pas être changée dans l'éditeur.
 */
class TransformComponent(var rectangle: Rectangle = Rectangle(), val gridCell: MutableList<GridCell> = mutableListOf(), var fixedSizeEditor: Boolean = false) : BaseComponent<TransformComponent>() {
    override fun copy(): TransformComponent {
        return TransformComponent(Rectangle(rectangle), mutableListOf(), fixedSizeEditor)
    }
}