package be.catvert.pc.components.graphics

import be.catvert.pc.GameObject
import be.catvert.pc.PCGame
import be.catvert.pc.components.RenderableComponent
import be.catvert.pc.containers.GameObjectContainer
import be.catvert.pc.scenes.EditorScene
import be.catvert.pc.utility.Constants
import be.catvert.pc.utility.CustomEditorImpl
import be.catvert.pc.utility.Size
import be.catvert.pc.utility.draw
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnore
import glm_.vec2.Vec2
import imgui.ImGui
import ktx.assets.loadOnDemand
import ktx.assets.toLocalFile

/**
 * Component permettant d'ajouter une texture chargée au préalable depuis un atlas et une région
 * @param atlasPath Le chemin vers l'atlas en question
 * @param region La région à utiliser
 */
class AtlasComponent(atlasPath: FileHandle, region: String) : RenderableComponent(), CustomEditorImpl {
    @JsonCreator constructor() : this(Constants.defaultAtlasPath.toLocalFile(), "notexture")

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

    override fun onAddToContainer(gameObject: GameObject, container: GameObjectContainer) {
        super.onAddToContainer(gameObject, container)

        updateAtlas()
    }

    override fun render(gameObject: GameObject, batch: Batch) {
        batch.draw(atlasRegion, gameObject.rectangle, flipX, flipY)
    }

    @JsonIgnore private val selectAtlasTitle = "Sélection de l'atlas"
    @JsonIgnore private var selectedAtlasIndex = -1
    @JsonIgnore private var useAtlasSize = false
    @JsonIgnore private var showLevelAtlas = false

    override fun insertImgui(gameObject: GameObject, labelName: String, editorScene: EditorScene) {

        with(ImGui) {
            if (button("<-")) {
                val index = atlas.regions.indexOfFirst { it.name == region }
                if (index > 0) {
                    updateAtlas(region = atlas.regions[index - 1].name)
                }
            }

            sameLine()
            if (imageButton(atlasRegion.texture.textureObjectHandle, Vec2(gameObject.rectangle.width, gameObject.rectangle.height), Vec2(atlasRegion.u, atlasRegion.v), Vec2(atlasRegion.u2, atlasRegion.v2))) {
                openPopup(selectAtlasTitle)
            }

            sameLine()
            if (button("->")) {
                val index = atlas.regions.indexOfFirst { it.name == region }
                if (index in 0 until atlas.regions.size - 1) {
                    updateAtlas(region = atlas.regions[index + 1].name)
                }
            }
        }
    }

    override fun insertImguiPopup(gameObject: GameObject, editorScene: EditorScene) {
        super.insertImguiPopup(gameObject, editorScene)

        with(ImGui) {
            val popupWidth = Gdx.graphics.width / 3 * 2
            val popupHeight = Gdx.graphics.height / 3 * 2
            setNextWindowSize(Vec2(popupWidth, popupHeight))
            setNextWindowPos(Vec2(Gdx.graphics.width / 2f - popupWidth / 2f, Gdx.graphics.height / 2f - popupHeight / 2f))
            if (beginPopup(selectAtlasTitle)) {
                if (selectedAtlasIndex == -1) {
                    selectedAtlasIndex = PCGame.gameAtlas.indexOfFirst { it == atlasPath.toLocalFile() }
                    if (selectedAtlasIndex == -1) {
                        selectedAtlasIndex = editorScene.level.resourcesAtlas().indexOfFirst { it == atlasPath.toLocalFile() }
                        if (selectedAtlasIndex == -1)
                            selectedAtlasIndex = 0
                        else
                            showLevelAtlas = true
                    }
                }

                if (checkbox("Afficher les atlas importés", this@AtlasComponent::showLevelAtlas))
                    selectedAtlasIndex = 0
                sameLine()
                combo("atlas", this@AtlasComponent::selectedAtlasIndex, if (showLevelAtlas) editorScene.level.resourcesAtlas().map { it.nameWithoutExtension() } else PCGame.gameAtlas.map { it.nameWithoutExtension() })
                checkbox("Mettre à jour la taille du gameObject", this@AtlasComponent::useAtlasSize)

                var sumImgsWidth = 0f

                val atlas = if (showLevelAtlas) editorScene.level.resourcesAtlas().getOrNull(selectedAtlasIndex)?.path() else PCGame.gameAtlas.getOrNull(selectedAtlasIndex)?.path()

                if(atlas != null) {
                    PCGame.assetManager.loadOnDemand<TextureAtlas>(atlas).asset.regions.forEach { it ->
                        val imgBtnSize = Vec2(Math.min(it.regionWidth, 200), Math.min(it.regionHeight, 200))

                        if (imageButton(it.texture.textureObjectHandle, imgBtnSize, Vec2(it.u, it.v), Vec2(it.u2, it.v2))) {
                            updateAtlas(atlas.toLocalFile(), it.name)

                            if (useAtlasSize)
                                gameObject.rectangle.size = Size(it.regionWidth, it.regionHeight)
                            closeCurrentPopup()
                        }

                        sumImgsWidth += imgBtnSize.x

                        if (sumImgsWidth + 400f < popupWidth)
                            sameLine()
                        else
                            sumImgsWidth = 0f
                    }
                }

                endPopup()
            }
        }
    }
}