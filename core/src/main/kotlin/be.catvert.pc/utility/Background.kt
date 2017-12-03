package be.catvert.pc.utility

import be.catvert.pc.PCGame
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Batch
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS, include = JsonTypeInfo.As.WRAPPER_ARRAY)
sealed class Background : Renderable

class StandardBackground(var backgroundFile: FileWrapper) : Background() {
    private val background = if (backgroundFile.get().exists()) PCGame.assetManager.loadOnDemand<Texture>(backgroundFile) else null
    override fun render(batch: Batch) {
        background?.apply {
            batch.draw(this.asset, 0f, 0f, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
        }
    }
}

class ParallaxBackground(var parallaxDir: FileWrapper) : Background() {
    init {
        Utility.getFilesRecursivly(parallaxDir.get()).sortedBy { it.nameWithoutExtension() }.forEach {

        }
    }

    override fun render(batch: Batch) {

    }
}

