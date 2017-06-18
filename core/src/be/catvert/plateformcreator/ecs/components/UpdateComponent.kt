package be.catvert.plateformcreator.ecs.components

import be.catvert.plateformcreator.Level
import com.badlogic.ashley.core.Entity

/**
* Created by Catvert on 04/06/17.
*/

/**
 * Ce component permet d'ajouter une méthode à l'entité qui sera appelée à chaque frame
 * update : La méthode appelée à chaque frame
 */
class UpdateComponent(val update: (delta: Float, entity: Entity, level: Level) -> Unit) : BaseComponent<UpdateComponent>() {
    override fun copy(): UpdateComponent {
        return UpdateComponent(update)
    }
}