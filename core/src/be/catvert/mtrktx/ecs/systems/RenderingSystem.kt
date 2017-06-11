package be.catvert.mtrktx.ecs.systems

import be.catvert.mtrktx.MtrGame
import be.catvert.mtrktx.ecs.components.RenderComponent
import be.catvert.mtrktx.ecs.components.TransformComponent
import com.badlogic.ashley.core.ComponentMapper
import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family
import com.badlogic.ashley.utils.ImmutableArray
import ktx.app.use

/**
 * Created by arno on 03/06/17.
 */

class RenderingSystem(private val game: MtrGame) : BaseSystem() {
    private val renderMapper = ComponentMapper.getFor(RenderComponent::class.java)
    private val transformMapper = ComponentMapper.getFor(TransformComponent::class.java)

    private lateinit var entities: ImmutableArray<Entity>

    override fun update(deltaTime: Float) {
        super.update(deltaTime)
        entities = engine.getEntitiesFor(Family.all(RenderComponent::class.java, TransformComponent::class.java).get())
        game.batch.use {
            entities.sortedWith(compareBy { renderMapper[it].renderLayer }).forEach {
                if(transformMapper[it].active) {
                    val rect = transformMapper[it].rectangle
                    val renderComp = renderMapper[it]
                    game.batch.draw(renderComp.texture.second, rect.x, rect.y, rect.width, rect.height, 0, 0, renderComp.texture.second.width, renderComp.texture.second.height, renderComp.flipX, renderComp.flipY)
                }
            }
        }
    }

}