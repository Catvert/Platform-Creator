package be.catvert.pc.components.graphics

import be.catvert.pc.GameObject
import be.catvert.pc.PCGame
import be.catvert.pc.components.RenderableComponent
import be.catvert.pc.utility.Rect
import be.catvert.pc.utility.draw
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import ktx.assets.getValue
import ktx.assets.load
import ktx.assets.loadOnDemand
import ktx.assets.toLocalFile

/**
 * Component permettant d'afficher une texture chargée au préalable dans un spritesheet
 * @param atlasPath Le chemin vers l'atlas en question
 * @param region La région à utiliser
 * @param rectangle Le rectangle dans lequel sera dessiné la texture
 * @param linkRectToGO Permet de spécifier si le rectangle à utiliser est celui du gameObject
 */

class AtlasComponent(val atlasPath: String, region: String, rectangle: Rect = Rect(), val linkRectToGO: Boolean = true) : RenderableComponent() {
    var region: String = region
        set(value) {
            field = value
            atlasRegion = atlas.findRegion(value)
        }

    private val atlas by PCGame.assetManager.loadOnDemand<TextureAtlas>(atlasPath.toLocalFile().path())

    private var atlasRegion: TextureAtlas.AtlasRegion = atlas.findRegion(region)

    var rectangle: Rect = rectangle
        private set

    override fun onGOAddToContainer(gameObject: GameObject) {
        super.onGOAddToContainer(gameObject)
        if (linkRectToGO)
            this.rectangle = gameObject.rectangle
    }

    override fun render(batch: Batch) {
        batch.draw(atlasRegion, rectangle, flipX, flipY)
    }
}