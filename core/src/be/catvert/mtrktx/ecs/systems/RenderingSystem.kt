package be.catvert.mtrktx.ecs.systems

import be.catvert.mtrktx.MtrGame
import be.catvert.mtrktx.draw
import be.catvert.mtrktx.ecs.components.RenderComponent
import be.catvert.mtrktx.ecs.components.TransformComponent
import com.badlogic.ashley.core.ComponentMapper
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family
import com.badlogic.ashley.utils.ImmutableArray
import ktx.app.use

/**
* Created by Catvert on 03/06/17.
*/

class RenderingSystem(private val game: MtrGame) : BaseSystem() {
    private val renderMapper = ComponentMapper.getFor(RenderComponent::class.java)
    private val transformMapper = ComponentMapper.getFor(TransformComponent::class.java)

    private lateinit var entities: ImmutableArray<Entity>

    override fun update(deltaTime: Float) {
        super.update(deltaTime)

        entities =  engine.getEntitiesFor(Family.all(RenderComponent::class.java, TransformComponent::class.java).get())

        game.batch.use { batch ->
            entities.sortedWith(compareBy { renderMapper[it].renderLayer }).forEach {
                if(transformMapper[it].active) {
                    val rect = transformMapper[it].rectangle
                    val renderComp = renderMapper[it]

                    val atlas = renderComp.getActualAtlasRegion()

                    if(renderComp.autoResizeWithAtlas) {
                        rect.width = atlas.regionWidth.toFloat()
                        rect.height = atlas.regionHeight.toFloat()
                    }

                    batch.draw(atlas, rect, renderComp.flipX, renderComp.flipY)
                }
            }
        }
    }

}