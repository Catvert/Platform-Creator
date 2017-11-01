package be.catvert.pc.components.graphics

import be.catvert.pc.GameObject
import be.catvert.pc.PCGame
import be.catvert.pc.components.RenderableComponent
import be.catvert.pc.scenes.EditorScene
import be.catvert.pc.utility.*
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.fasterxml.jackson.annotation.JsonCreator
import com.kotcrab.vis.ui.widget.VisImageButton
import com.kotcrab.vis.ui.widget.VisTable
import ktx.actors.onClick
import ktx.assets.getValue
import ktx.assets.loadOnDemand
import ktx.assets.toLocalFile

/**
 * Component permettant d'afficher une texture chargée depuis un fichier
 * @param texturePath Le chemin vers la texture en question
 * @param rectangle Le rectangle dans lequel sera dessiné la texture
 * @param linkRectToGO Permet de spécifier si le rectangle à utiliser est celui du gameObject
 */
class TextureComponent(texturePath: FileHandle) : RenderableComponent() {
    @JsonCreator constructor(): this(Constants.noTextureFoundTexturePath.toLocalFile())

    var texturePath: String = texturePath.path()
        private set

    private var texture = PCGame.assetManager.loadOnDemand<Texture>(this.texturePath).asset

    fun updateTexture(texturePath: FileHandle = this.texturePath.toLocalFile()) {
        this.texturePath = texturePath.path()

        texture = PCGame.assetManager.loadOnDemand<Texture>(this.texturePath).asset
    }

    override fun onGOAddToContainer(gameObject: GameObject) {
        super.onGOAddToContainer(gameObject)

        updateTexture()
    }

    override fun render(batch: Batch) {
        batch.draw(texture, gameObject.rectangle, flipX, flipY)
    }

    companion object : CustomEditorImpl<TextureComponent> {
        override fun createInstance(table: VisTable, editorScene: EditorScene, onCreate: (newInstance: TextureComponent) -> Unit) {
            val texture = PCGame.assetManager.loadOnDemand<Texture>(Constants.noTextureFoundTexturePath).asset
            val imageButton = VisImageButton(TextureRegionDrawable(TextureAtlas.AtlasRegion(texture, 0, 0, texture.width, texture.height)))
            imageButton.onClick {
                editorScene.showSelectTextureWindow { textureFile ->
                    val newTexture = PCGame.assetManager.loadOnDemand<Texture>(textureFile.path()).asset
                    val imgBtnDrawable = TextureRegionDrawable(TextureAtlas.AtlasRegion(newTexture, 0, 0, newTexture.width, newTexture.height))

                    imageButton.style.imageUp = imgBtnDrawable
                    imageButton.style.imageDown = imgBtnDrawable

                    onCreate(TextureComponent(textureFile))
                }
            }

            table.add(imageButton)

            table.row()
        }

        override fun insertChangeProperties(table: VisTable, editorScene: EditorScene, instance: TextureComponent) {
            val texture = instance.texture
            val imageButton = VisImageButton(TextureRegionDrawable(TextureAtlas.AtlasRegion(texture, 0, 0, texture.width, texture.height)))
            imageButton.onClick {
                editorScene.showSelectTextureWindow { textureFile ->
                    instance.updateTexture(textureFile)

                    val texture = instance.texture

                    val imgBtnDrawable = TextureRegionDrawable(TextureAtlas.AtlasRegion(texture, 0, 0, texture.width, texture.height))

                    imageButton.style.imageUp = imgBtnDrawable
                    imageButton.style.imageDown = imgBtnDrawable
                }
            }

            table.add(imageButton)
        }
    }
}