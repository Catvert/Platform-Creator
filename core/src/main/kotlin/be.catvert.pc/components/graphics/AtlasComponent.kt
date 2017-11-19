package be.catvert.pc.components.graphics

import be.catvert.pc.GameObject
import be.catvert.pc.GameObjectState
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
import glm_.vec2.Vec2
import imgui.ImGui
import imgui.WindowFlags
import ktx.actors.onClick
import ktx.assets.loadOnDemand
import ktx.assets.toLocalFile
import ktx.vis.horizontalGroup

/**
 * Component permettant d'ajouter une texture chargée au préalable depuis un atlas et une région
 * @param atlasPath Le chemin vers l'atlas en question
 * @param region La région à utiliser
 */
class AtlasComponent(atlasPath: FileHandle, region: String) : RenderableComponent(), CustomEditorImpl {
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

    override fun onGOAddToContainer(state: GameObjectState, gameObject: GameObject) {
        super.onGOAddToContainer(state, gameObject)

        updateAtlas()
    }

    override fun render(gameObject: GameObject, batch: Batch) {
        batch.draw(atlasRegion, gameObject.rectangle, flipX, flipY)
    }

    @JsonIgnore private val selectAtlasTitle = "Sélection de l'atlas"
    @JsonIgnore private var selectedAtlasIndex = -1
    @JsonIgnore private var useAtlasSize = booleanArrayOf(false)

    override fun insertImgui(gameObject: GameObject, editorScene: EditorScene) {

        with(ImGui) {
            if(button("<-")) {
                val index = atlas.regions.indexOfFirst { it.name == region }
                if(index > 0) {
                    updateAtlas(region = atlas.regions[index - 1].name)
                }
            }

            sameLine()
            if(imageButton(atlasRegion.texture.textureObjectHandle, Vec2(gameObject.rectangle.width, gameObject.rectangle.height), Vec2(atlasRegion.u, atlasRegion.v), Vec2(atlasRegion.u2, atlasRegion.v2))) {
                openPopup(selectAtlasTitle)
            }

            sameLine()
            if(button("->")) {
                val index = atlas.regions.indexOfFirst { it.name == region }
                if(index in 0 until atlas.regions.size - 1) {
                    updateAtlas(region = atlas.regions[index + 1].name)
                }
            }
        }
    }

    override fun insertImguiPopup(gameObject: GameObject, editorScene: EditorScene) {
        super.insertImguiPopup(gameObject, editorScene)

        with(ImGui) {
            if(beginPopupModal(selectAtlasTitle, extraFlags = WindowFlags.AlwaysHorizontalScrollbar.i or WindowFlags.AlwaysVerticalScrollbar.i)) {
                if(selectedAtlasIndex == -1) {
                    selectedAtlasIndex = PCGame.loadedAtlas.indexOfFirst { it == atlasPath.toLocalFile() }
                    if(selectedAtlasIndex == -1)
                        selectedAtlasIndex = 0
                }
                combo("atlas", this@AtlasComponent::selectedAtlasIndex, PCGame.loadedAtlas.map { it.nameWithoutExtension() })
                checkbox("Mettre à jour la taille du gameObject", useAtlasSize)

                separator()

                var count = 0
                PCGame.assetManager.loadOnDemand<TextureAtlas>(PCGame.loadedAtlas[selectedAtlasIndex].path()).asset.regions.forEach{ it ->
                    if(imageButton(it.texture.textureObjectHandle, Vec2(it.regionWidth, it.regionHeight), Vec2(it.u, it.v), Vec2(it.u2, it.v2))) {
                        updateAtlas(PCGame.loadedAtlas[selectedAtlasIndex], it.name)
                        if(useAtlasSize[0])
                            gameObject.rectangle.size = Size(it.regionWidth, it.regionHeight)
                        closeCurrentPopup()
                    }
                    if(++count <= 8)
                        sameLine()
                    else
                        count = 0
                }

                endPopup()
            }
        }
    }
}