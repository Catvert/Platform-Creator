package be.catvert.pc.components.graphics

import be.catvert.pc.GameObject
import be.catvert.pc.PCGame
import be.catvert.pc.serialization.PostDeserialization
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.math.Rectangle
import ktx.assets.loadOnDemand

/**
 * Component permettant d'afficher une texture chargée au préalable dans un spritesheet
 * @param atlasPath Le chemin vers l'atlas en question
 * @param region La région à utiliser
 * @param rectangle Le rectangle dans lequel sera dessiné la texture
 * @param linkRectToGO Permet de spécifier si le rectangle à utiliser est celui du gameObject
 */
class AtlasComponent(val atlasPath: FileHandle, region: String, rectangle: Rectangle = Rectangle(), val linkRectToGO: Boolean = true) : RenderableComponent(), PostDeserialization {
    var region: String = region
        set(value) {
            field = value
            atlasRegion = atlas.findRegion(value)
        }

    @Transient
    private var atlas: TextureAtlas = loadAtlas()

    @Transient
    private var atlasRegion: TextureAtlas.AtlasRegion = loadRegion()

    var rectangle: Rectangle = rectangle
        private set

    override fun onGameObjectSet(gameObject: GameObject) {
        super.onGameObjectSet(gameObject)

        if (linkRectToGO)
            rectangle = gameObject.rectangle
    }

    override fun postDeserialization() {
        atlas = loadAtlas()
        atlasRegion = loadRegion()
    }

    override fun render(batch: Batch) {
        batch.draw(atlasRegion, rectangle.x, rectangle.y, rectangle.width, rectangle.height)
    }

    private fun loadAtlas(): TextureAtlas = PCGame.assetManager.loadOnDemand<TextureAtlas>(atlasPath.path()).asset
    private fun loadRegion(): TextureAtlas.AtlasRegion = atlas.findRegion(region)
}