package be.catvert.pc.components.graphics

import be.catvert.pc.GameObject
import be.catvert.pc.PCGame
import be.catvert.pc.components.RenderableComponent
import be.catvert.pc.scenes.EditorScene
import be.catvert.pc.utility.*
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnore
import com.kotcrab.vis.ui.widget.VisImageButton
import com.kotcrab.vis.ui.widget.VisLabel
import com.kotcrab.vis.ui.widget.VisTable
import com.kotcrab.vis.ui.widget.VisTextButton
import ktx.actors.onClick
import ktx.assets.loadOnDemand
import ktx.assets.toLocalFile
import ktx.vis.horizontalGroup

/**
 * Component permettant d'afficher une texture chargée au préalable dans un spritesheet
 * @param atlasPath Le chemin vers l'atlasPath en question
 * @param region La région à utiliser
 */

class AtlasComponent(atlasPath: FileHandle, region: String) : RenderableComponent(), CustomEditorImpl<AtlasComponent> {
    @JsonCreator constructor(): this(Constants.noTextureAtlasFoundPath.toLocalFile(), "notexture")

    var atlasPath: String = atlasPath.path()
        private set

    var region: String = region
        private set

    @JsonIgnore private var atlas = PCGame.assetManager.loadOnDemand<TextureAtlas>(this.atlasPath).asset

    @JsonIgnore private var atlasRegion: TextureAtlas.AtlasRegion = atlas.findRegion(region)

    fun updateAtlas(atlasPath: FileHandle = this.atlasPath.toLocalFile(), region: String = this.region) {
        this.atlasPath = atlasPath.path()
        this.region = region

        atlas = PCGame.assetManager.loadOnDemand<TextureAtlas>(atlasPath.path()).asset
        atlasRegion = atlas.findRegion(region)
    }

    override fun onGOAddToContainer(gameObject: GameObject) {
        super.onGOAddToContainer(gameObject)

        updateAtlas()
    }

    override fun render(batch: Batch) {
        batch.draw(atlasRegion, gameObject.rectangle, flipX, flipY)
    }

    override fun insertChangeProperties(instance: AtlasComponent, table: VisTable, editorScene: EditorScene) {
        table.add(VisLabel("Atlas : "))

        fun updateImageBtn(imgBtn: VisImageButton) {
            val imgBtnDrawable = TextureRegionDrawable(instance.atlasRegion)

            imgBtn.style.imageUp = imgBtnDrawable
            imgBtn.style.imageDown = imgBtnDrawable
        }

        val imageButton = VisImageButton(TextureRegionDrawable(instance.atlasRegion)).apply {
            onClick {
                editorScene.showSelectAtlasRegionWindow(instance.atlasPath.toLocalFile()) { atlasFile, region ->
                    instance.updateAtlas(atlasFile, region)

                   updateImageBtn(this)
                }
            }
        }

        table.add(horizontalGroup {
            space(10f)

            textButton("<-") {
                onClick {
                    val index = atlas.regions.indexOfFirst { it.name == region }
                    if(index > 0) {
                        updateAtlas(region = atlas.regions[index - 1].name)
                        updateImageBtn(imageButton)
                    }
                }
            }

            addActor(imageButton)

            textButton("->") {
                onClick {
                    val index = atlas.regions.indexOfFirst { it.name == region }
                    if(index in 0 until atlas.regions.size - 1) {
                        updateAtlas(region = atlas.regions[index + 1].name)
                        updateImageBtn(imageButton)
                    }
                }
            }
        })
    }
}