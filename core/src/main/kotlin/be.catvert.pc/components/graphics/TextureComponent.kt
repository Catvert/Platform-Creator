package be.catvert.pc.components.graphics

import be.catvert.pc.GameObject
import be.catvert.pc.PCGame
import be.catvert.pc.utility.Rect
import be.catvert.pc.utility.draw
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Batch
import ktx.assets.loadOnDemand

/**
 * Component permettant d'afficher une texture chargée depuis un fichier
 * @param texturePath Le chemin vers la texture en question
 * @param rectangle Le rectangle dans lequel sera dessiné la texture
 * @param linkRectToGO Permet de spécifier si le rectangle à utiliser est celui du gameObject
 */
class TextureComponent(val texturePath: String, rectangle: Rect = Rect(), val linkRectToGO: Boolean = true) : RenderableComponent() {
    private var texture: Texture = loadTexture()

    var rectangle: Rect = rectangle
        private set

    override fun onGameObjectSet(gameObject: GameObject) {
        super.onGameObjectSet(gameObject)

        if (linkRectToGO)
            rectangle = gameObject.rectangle
    }

    override fun render(batch: Batch) {
        batch.draw(texture, rectangle)
    }

    private fun loadTexture(): Texture = PCGame.assetManager.loadOnDemand<Texture>(Gdx.files.local(texturePath).path()).asset
}