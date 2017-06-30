package be.catvert.plateformcreator.ecs.components

import be.catvert.plateformcreator.Level
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.signals.Listener
import com.badlogic.ashley.signals.Signal

/**
 * Created by Catvert on 04/06/17.
 */

/**
 * Listener appelé à chaque frame
 * @property delta Le delta au moment que où ce listener est appelé
 * @property entity L'entité ayant le updateComponent
 * @property level Le niveau
 */
data class UpdateListener(val delta: Float, val entity: Entity, val level: Level)

/**
 * Ce component permet d'ajouter une méthode à l'entité qui sera appelée à chaque frame
 */
class UpdateComponent : BaseComponent<UpdateComponent>() {
    override fun copy(): UpdateComponent {
        val updateComp = UpdateComponent()
        updateComp.update = update
        return updateComp
    }

    var update = Signal<UpdateListener>()
        private set
}

fun updateComponent(update: (UpdateComponent.() -> Listener<UpdateListener>)? = null): UpdateComponent {
    val updateComp = UpdateComponent()

    if (update != null)
        updateComp.update.add(updateComp.update())

    return updateComp
}