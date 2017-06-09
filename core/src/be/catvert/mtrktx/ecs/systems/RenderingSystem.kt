package be.catvert.mtrktx.ecs.systems

import be.catvert.mtrktx.MtrGame
import be.catvert.mtrktx.ecs.components.PhysicsComponent
import be.catvert.mtrktx.ecs.components.RenderComponent
import be.catvert.mtrktx.ecs.components.TransformComponent
import com.badlogic.ashley.core.*
import com.badlogic.ashley.utils.ImmutableArray
import ktx.app.use

/**
 * Created by arno on 03/06/17.
 */

class RenderingSystem(private val game: MtrGame) : EntitySystem() {
    private val renderMapper = ComponentMapper.getFor(RenderComponent::class.java)
    private val transformMapper = ComponentMapper.getFor(TransformComponent::class.java)

    private lateinit var entities: ImmutableArray<Entity>;

    override fun addedToEngine(engine: Engine?) {
        super.addedToEngine(engine)

        entities = getEngine().getEntitiesFor(Family.all(RenderComponent::class.java, TransformComponent::class.java).get())
    }

    override fun update(deltaTime: Float) {
        super.update(deltaTime)

        game.batch.use {
            entities.forEach {
                if(transformMapper[it].active) {
                    val rect = transformMapper[it].rectangle
                    val renderComp = renderMapper[it]
                    game.batch.draw(renderComp.texture, rect.x, rect.y, rect.width, rect.height, 0, 0, renderComp.texture.width, renderComp.texture.height, renderComp.flipX, renderComp.flipY)
                }
            }
        }
    }

}