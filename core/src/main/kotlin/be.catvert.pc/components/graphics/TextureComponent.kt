package be.catvert.pc.components.graphics

import be.catvert.pc.GameObject
import be.catvert.pc.PCGame
import be.catvert.pc.serialization.PostDeserialization
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.math.Rectangle
import ktx.assets.loadOnDemand

/**
 * Component permettant d'afficher une texture chargée depuis un fichier
 * @param texturePath Le chemin vers la texture en question
 * @param rectangle Le rectangle dans lequel sera dessiné la texture
 * @param linkRectToGO Permet de spécifier si le rectangle à utiliser est celui du gameObject
 */
class TextureComponent(val texturePath: FileHandle, rectangle: Rectangle = Rectangle(), val linkRectToGO: Boolean = true) : RenderableComponent(), PostDeserialization {
    @Transient
    private var texture: Texture = loadTexture()

    var rectangle: Rectangle = rectangle
        private set

    override fun onGameObjectSet(gameObject: GameObject) {
        super.onGameObjectSet(gameObject)

        if (linkRectToGO)
            rectangle = gameObject.rectangle
    }

    override fun postDeserialization() {
        texture = loadTexture()
    }

    override fun render(batch: Batch) {
        batch.draw(texture, rectangle.x, rectangle.y, rectangle.width, rectangle.height)
    }

    private fun loadTexture(): Texture = PCGame.assetManager.loadOnDemand<Texture>(texturePath.path()).asset
}