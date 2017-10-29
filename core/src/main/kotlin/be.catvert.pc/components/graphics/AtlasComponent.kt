package be.catvert.pc.components.graphics

import be.catvert.pc.GameObject
import be.catvert.pc.PCGame
import be.catvert.pc.components.RenderableComponent
import be.catvert.pc.scenes.EditorScene
import be.catvert.pc.utility.Constants
import be.catvert.pc.utility.CustomEditorImpl
import be.catvert.pc.utility.Rect
import be.catvert.pc.utility.draw
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.fasterxml.jackson.annotation.JsonIgnore
import com.kotcrab.vis.ui.widget.VisImageButton
import com.kotcrab.vis.ui.widget.VisTable
import ktx.actors.onClick
import ktx.assets.getValue
import ktx.assets.load
import ktx.assets.loadOnDemand
import ktx.assets.toLocalFile
import ktx.vis.tab

/**
 * Component permettant d'afficher une texture chargée au préalable dans un spritesheet
 * @param atlasPath Le chemin vers l'atlasPath en question
 * @param region La région à utiliser
 * @param rectangle Le rectangle dans lequel sera dessiné la texture
 * @param linkRectToGO Permet de spécifier si le rectangle à utiliser est celui du gameObject
 */

class AtlasComponent(atlasPath: String, region: String, rectangle: Rect = Rect(), val linkRectToGO: Boolean = true) : RenderableComponent() {
    var rectangle: Rect = rectangle
        private set

    var region: String = region
        private set

    var atlasPath: String = atlasPath
        private set

    @JsonIgnore private var textureAtlas = PCGame.assetManager.loadOnDemand<TextureAtlas>(atlasPath.toLocalFile().path()).asset

    @JsonIgnore private var atlasRegion: TextureAtlas.AtlasRegion = textureAtlas.findRegion(region)

    fun getAtlasRegion() = atlasRegion

    fun updateAtlas(atlasPath: String, region: String) {
        this.atlasPath = atlasPath
        this.region = region

        textureAtlas = PCGame.assetManager.loadOnDemand<TextureAtlas>(atlasPath.toLocalFile().path()).asset
        atlasRegion = textureAtlas.findRegion(region)
    }

    override fun onGOAddToContainer(gameObject: GameObject) {
        super.onGOAddToContainer(gameObject)
        if (linkRectToGO)
            this.rectangle = gameObject.rectangle
    }

    override fun render(batch: Batch) {
        batch.draw(atlasRegion, rectangle, flipX, flipY)
    }

    companion object : CustomEditorImpl<AtlasComponent> {
        override fun createInstance(table: VisTable, editorScene: EditorScene, onCreate: (newInstance: AtlasComponent) -> Unit) {
            val texture = PCGame.assetManager.loadOnDemand<Texture>(Constants.noTextureFoundTexturePath).asset
            val imageButton = VisImageButton(TextureRegionDrawable(TextureAtlas.AtlasRegion(texture, 0, 0, texture.width, texture.height)))
            imageButton.onClick {
                editorScene.showSelectAtlasRegionWindow(null) { atlasFile, region ->
                    val imgBtnDrawable = TextureRegionDrawable(PCGame.assetManager.loadOnDemand<TextureAtlas>(atlasFile.path()).asset.findRegion(region))

                    imageButton.style.imageUp = imgBtnDrawable
                    imageButton.style.imageDown = imgBtnDrawable

                    onCreate(AtlasComponent(atlasFile.path(), region))
                }
            }

            table.add(imageButton)

            table.row()
        }

        override fun insertChangeProperties(table: VisTable, editorScene: EditorScene, instance: AtlasComponent) {
            val imageButton = VisImageButton(TextureRegionDrawable(instance.getAtlasRegion()))
            imageButton.onClick {
                editorScene.showSelectAtlasRegionWindow(instance.atlasPath.toLocalFile()) { atlasFile, region ->
                    instance.updateAtlas(atlasFile.path(), region)

                    val imgBtnDrawable = TextureRegionDrawable(instance.getAtlasRegion())

                    imageButton.style.imageUp = imgBtnDrawable
                    imageButton.style.imageDown = imgBtnDrawable
                }
            }

            table.add(imageButton)
        }
    }
}