package be.catvert.mtrktx.ecs.components.render

import be.catvert.mtrktx.TextureInfo
import be.catvert.mtrktx.ecs.components.BaseComponent
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.TextureAtlas

/**
* Created by Catvert on 05/06/17.
*/

class RenderComponent(val textureInfoList: List<TextureInfo>, val animationList: List<Animation<TextureAtlas.AtlasRegion>> = listOf(), var animationSpeed: Float = 0.33f, var flipX: Boolean = false, var flipY: Boolean = false, var renderLayer: Int = 0, var autoResizeWithAtlas: Boolean = false) : BaseComponent() {
    override fun copy(target: Entity): BaseComponent {
       return RenderComponent(textureInfoList, animationList, animationSpeed, initialFlipX, initialFlipY, renderLayer)
    }

    private var stateTime = 0f

    var actualTextureInfoIndex: Int = 0
        set(value) {
            field = value

            useAnimation = false
        }

    var actualAnimationIndex: Int = 0

    var useAnimation = false

    private val initialFlipX = flipX
    private val initialFlipY = flipY

    fun getActualAtlasRegion(): TextureAtlas.AtlasRegion {
        if(useAnimation) {
            val animation  = animationList[actualAnimationIndex]
            animation.frameDuration = animationSpeed
            stateTime += Gdx.graphics.deltaTime
            return animationList[actualAnimationIndex].getKeyFrame(stateTime, true)
        }
        else {
            return textureInfoList[actualTextureInfoIndex].texture
        }
    }
}