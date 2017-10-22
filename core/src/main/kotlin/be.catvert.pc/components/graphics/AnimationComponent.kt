package be.catvert.pc.components.graphics

import be.catvert.pc.GameObject
import be.catvert.pc.PCGame
import be.catvert.pc.components.RenderableComponent
import be.catvert.pc.utility.Rect
import be.catvert.pc.utility.draw
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.fasterxml.jackson.annotation.JsonIgnore
import ktx.assets.getValue
import ktx.assets.loadOnDemand
import ktx.assets.toLocalFile
import ktx.collections.toGdxArray

class AnimationComponent(val atlasPath: String, val animationRegionName: String, val frameDuration: Float, rectangle: Rect = Rect(), val linkRectToGO: Boolean = true) : RenderableComponent() {
    private val atlas by PCGame.assetManager.loadOnDemand<TextureAtlas>(atlasPath.toLocalFile().path())

    private val animation = loadAnimation()

    private var stateTime = 0f

    var rectangle: Rect = rectangle
        private set

    override fun onGOAddToContainer(gameObject: GameObject) {
        super.onGOAddToContainer(gameObject)
        if (linkRectToGO)
            this.rectangle = gameObject.rectangle
    }

    override fun render(batch: Batch) {
        if(animation != null) {
            stateTime += Gdx.graphics.deltaTime
            batch.draw(animation.getKeyFrame(stateTime), rectangle, flipX, flipY)
        }
    }

    private fun loadAnimation(): Animation<TextureAtlas.AtlasRegion>? {
        atlas.regions.forEach {
            /* les animations de Kenney finissent par une lettre puis par exemple 1 donc -> alienGreen_walk1 puis alienGreen_walk2
            mais des autres textures normale tel que foliagePack_001 existe donc on doit vérifier si le nombre avant 1 fini bien par une lettre
            */
            if (it.name.endsWith("_0")) {
                val name = it.name.removeSuffix("_0")

                var count = 1

                while(atlas.findRegion(name + "_" + count) != null)
                    ++count

                val frameList = mutableListOf<TextureAtlas.AtlasRegion>()

                val initialRegion = atlas.findRegion(name + "_0")

                for (i in 0 until count) {
                    val nameNextFrame = name + "_" + i
                    val region = atlas.findRegion(nameNextFrame)
                    region.regionWidth = initialRegion.regionWidth
                    region.regionHeight = initialRegion.regionHeight
                    frameList.add(region)
                }

                return Animation(frameDuration, frameList.toGdxArray(), Animation.PlayMode.LOOP)
            }
        }
        return null
    }
}