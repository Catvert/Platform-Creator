package be.catvert.plateformcreator.ecs.systems

import be.catvert.plateformcreator.MtrGame
import be.catvert.plateformcreator.draw
import be.catvert.plateformcreator.ecs.components.RenderComponent
import be.catvert.plateformcreator.ecs.components.ResizeMode
import be.catvert.plateformcreator.ecs.components.TransformComponent
import com.badlogic.ashley.core.ComponentMapper
import com.badlogic.ashley.core.EntitySystem
import com.badlogic.ashley.core.Family
import ktx.app.use

/**
 * Created by Catvert on 03/06/17.
 */

/**
 * Ce système permet de dessiner les entités ayant un transformComponent et un renderComponent
 * @property game L'objet du jeu
 */
class RenderingSystem(private val game: MtrGame) : EntitySystem() {
    private val renderMapper = ComponentMapper.getFor(RenderComponent::class.java)
    private val transformMapper = ComponentMapper.getFor(TransformComponent::class.java)

    override fun update(deltaTime: Float) {
        super.update(deltaTime)

        val entities = engine.getEntitiesFor(Family.all(RenderComponent::class.java, TransformComponent::class.java).get())

        game.batch.use { batch ->
            entities.sortedWith(compareBy { renderMapper[it].renderLayer.layer }).forEach {
                if (transformMapper[it].active) {
                    val rect = transformMapper[it].rectangle
                    val renderComp = renderMapper[it]

                    val atlas = renderComp.getActualAtlasRegion()

                    when (renderComp.resizeMode) {
                        ResizeMode.NO_RESIZE -> {
                        }
                        ResizeMode.ACTUAL_REGION -> {
                            rect.width = atlas.regionWidth.toFloat()
                            rect.height = atlas.regionHeight.toFloat()
                        }
                        ResizeMode.FIXED_SIZE -> {
                            if (renderComp.fixedResize != null) {
                                rect.width = renderComp.fixedResize!!.x
                                rect.height = renderComp.fixedResize!!.y
                            } else {
                                ktx.log.error { "L'entité utilise le resizeMode : FIXED_SIZE mais n'a pas défini la taille" }
                            }
                        }
                    }

                    batch.draw(atlas, rect, renderComp.flipX, renderComp.flipY)
                }
            }
        }
    }

}