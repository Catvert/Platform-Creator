package be.catvert.plateformcreator.ecs.systems

import be.catvert.plateformcreator.Level
import be.catvert.plateformcreator.ecs.components.UpdateComponent
import com.badlogic.ashley.core.*
import com.badlogic.ashley.utils.ImmutableArray

/**
* Created by Catvert on 04/06/17.
*/

/**
 * Ce système permet d'appeler la méthode de mise a jour des entités ayant un updateComponent
 */
class UpdateSystem(private val level: Level) : EntitySystem() {
    private val updateMapper = ComponentMapper.getFor(UpdateComponent::class.java)

    private lateinit var entities: ImmutableArray<Entity>

    override fun update(deltaTime: Float) {
        super.update(deltaTime)

        entities = engine.getEntitiesFor(Family.all(UpdateComponent::class.java).get())

        entities.forEach {
            val updateComp = updateMapper[it]
            if(updateComp.active)
                updateComp.update(deltaTime, it, level)
        }
    }
}