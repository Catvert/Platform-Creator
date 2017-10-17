package be.catvert.pc.components.graphics

import be.catvert.pc.GameObject
import be.catvert.pc.PCGame
import be.catvert.pc.utility.Rect
import be.catvert.pc.utility.draw
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import ktx.assets.loadOnDemand

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

    private var atlas: TextureAtlas = loadAtlas()

    private var atlasRegion: TextureAtlas.AtlasRegion = loadRegion()

    var rectangle: Rect = rectangle
        private set

    override fun onGameObjectSet(gameObject: GameObject) {
        super.onGameObjectSet(gameObject)

        if (linkRectToGO)
            rectangle = gameObject.rectangle
    }

    override fun render(batch: Batch) {
        batch.draw(atlasRegion, rectangle)
    }

    private fun loadAtlas(): TextureAtlas = PCGame.assetManager.loadOnDemand<TextureAtlas>(Gdx.files.local(atlasPath).path()).asset
    private fun loadRegion(): TextureAtlas.AtlasRegion = atlas.findRegion(region)
}