package be.catvert.pc.components.graphics

import be.catvert.pc.GameObject
import be.catvert.pc.PCGame
import be.catvert.pc.components.RenderableComponent
import be.catvert.pc.containers.GameObjectContainer
import be.catvert.pc.containers.Level
import be.catvert.pc.utility.*
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.fasterxml.jackson.annotation.JsonCreator
import glm_.vec2.Vec2
import imgui.ImGui
import imgui.functionalProgramming
import ktx.collections.gdxArrayOf
import java.util.logging.FileHandler

/**
 * Component permettant d'ajouter une texture chargée au préalable depuis un atlas et une région
 * @param atlasPath Le chemin vers l'atlas en question
 * @param region La région à utiliser
 */
class AtlasComponent(val data: ComponentData<AtlasData>) : RenderableComponent(), CustomEditorImpl {
    @JsonCreator constructor() : this(componentDataOf())

    class AtlasData(var name: String, frameDuration: Float = 0f, vararg regions: Pair<FileWrapper, String>) : CustomEditorImpl {
        @JsonCreator constructor(): this("default")

        var regions = arrayOf(*regions)

        var frameDuration: Float = frameDuration
            set(value) {
                field = value
                animation.frameDuration = value
            }

        private var animation: Animation<TextureAtlas.AtlasRegion> = loadAnimation()

        private var stateTime = 0f

        fun render(gameObject: GameObject, flipX: Boolean, flipY: Boolean, batch: Batch) {
            if (regions.size > 1)
                stateTime += Gdx.graphics.deltaTime
            batch.draw(animation.getKeyFrame(stateTime), gameObject.box, flipX, flipY)
        }

        fun onAddToContainer() {
            updateAtlas()
        }

        private fun updateAtlas(regions: Array<Pair<FileWrapper, String>> = this.regions, frameDuration: Float = this.frameDuration) {
            this.regions = regions
            this.frameDuration = frameDuration
            this.animation = loadAnimation()
        }

        private fun loadAnimation(): Animation<TextureAtlas.AtlasRegion> {
            val frames = gdxArrayOf<TextureAtlas.AtlasRegion>()

            regions.forEach {
                val region = PCGame.assetManager.loadOnDemand<TextureAtlas>(it.first).asset.findRegion(it.second)

                if (region != null)
                    frames.add(region)
            }

            return Animation(frameDuration, frames, Animation.PlayMode.LOOP)
        }

        private val selectAtlasRegionTitle = "Sélection de la région"
        private var selectedAtlasIndex = -1
        private var useAtlasSize = false
        private var showLevelAtlas = false
        private var showAnimations = false

        override fun insertImgui(labelName: String, gameObject: GameObject, level: Level) {
            with(ImGui) {
                if(ImguiHelper.addImguiWidgetsArray(labelName, ::regions, { Constants.defaultAtlasPath.first.toFileWrapper() to Constants.defaultAtlasPath.second }, { regionItem ->
                    val region = PCGame.assetManager.loadOnDemand<TextureAtlas>(regionItem.obj.first).asset.findRegion(regionItem.obj.second) // todo optimisation
                    if (imageButton(region.texture.textureObjectHandle, Vec2(gameObject.box.width, gameObject.box.height), Vec2(region.u, region.v), Vec2(region.u2, region.v2))) {
                        openPopup(selectAtlasRegionTitle)
                    }
                    return@addImguiWidgetsArray showSelectAtlasPopup(gameObject, level, regionItem)
                })) {
                    updateAtlas()
                }

                if(regions.size > 1) {
                    functionalProgramming.withItemWidth(100f) {
                        sliderFloat("Vitesse", ::frameDuration, 0f, 1f)
                    }
                }
            }
        }

        private fun showSelectAtlasPopup(gameObject: GameObject, level: Level, item: ImguiHelper.Item<Pair<FileWrapper, String>>): Boolean {
            var valueChanged = false
            with(ImGui) {
                val popupWidth = Gdx.graphics.width / 3 * 2
                val popupHeight = Gdx.graphics.height / 3 * 2
                setNextWindowSize(Vec2(popupWidth, popupHeight))
                setNextWindowPos(Vec2(Gdx.graphics.width / 2f - popupWidth / 2f, Gdx.graphics.height / 2f - popupHeight / 2f))
                if (beginPopup(selectAtlasRegionTitle)) {
                    if (selectedAtlasIndex == -1) {
                        selectedAtlasIndex = PCGame.gameAtlas.indexOfFirst { it == item.obj.first.get() }
                        if (selectedAtlasIndex == -1) {
                            selectedAtlasIndex = level.resourcesAtlas().indexOfFirst { it == item.obj.first.get() }
                            if (selectedAtlasIndex == -1)
                                selectedAtlasIndex = 0
                            else
                                showLevelAtlas = true
                        }
                    }

                    if (checkbox("Afficher les atlas importés", ::showLevelAtlas))
                        selectedAtlasIndex = 0
                    sameLine()
                    combo("atlas", ::selectedAtlasIndex, if (showLevelAtlas) level.resourcesAtlas().map { it.nameWithoutExtension() } else PCGame.gameAtlas.map { it.nameWithoutExtension() })
                    checkbox("Mettre à jour la taille du gameObject", ::useAtlasSize)
                    checkbox("Afficher les animations", ::showAnimations)
                    var sumImgsWidth = 0f

                    val atlasPath = if (showLevelAtlas) level.resourcesAtlas().getOrNull(selectedAtlasIndex) else PCGame.gameAtlas.getOrNull(selectedAtlasIndex)

                    if (atlasPath != null) {
                        if(showAnimations) {
                            val atlas = PCGame.assetManager.loadOnDemand<TextureAtlas>(atlasPath).asset
                            findAnimationInAtlas(atlas).forEach {
                                val region = atlas.findRegion(it + "_0")
                                val imgBtnSize = Vec2(Math.min(region.regionWidth, 200), Math.min(region.regionHeight, 200))

                                if (imageButton(region.texture.textureObjectHandle, imgBtnSize, Vec2(region.u, region.v), Vec2(region.u2, region.v2))) {
                                    this@AtlasData.regions = findAnimationRegions(atlasPath.toFileWrapper(), it)
                                    updateAtlas()

                                    if (useAtlasSize)
                                        gameObject.box.size = Size(region.regionWidth, region.regionHeight)
                                    closeCurrentPopup()
                                }

                                sumImgsWidth += imgBtnSize.x

                                if (sumImgsWidth + 400f < popupWidth)
                                    sameLine()
                                else
                                    sumImgsWidth = 0f
                            }
                        }
                        else {
                            PCGame.assetManager.loadOnDemand<TextureAtlas>(atlasPath).asset.regions.forEach { region ->
                                val imgBtnSize = Vec2(Math.min(region.regionWidth, 200), Math.min(region.regionHeight, 200))

                                if (imageButton(region.texture.textureObjectHandle, imgBtnSize, Vec2(region.u, region.v), Vec2(region.u2, region.v2))) {
                                    item.obj = atlasPath.toFileWrapper() to region.name
                                    valueChanged = true

                                    if (useAtlasSize)
                                        gameObject.box.size = Size(region.regionWidth, region.regionHeight)
                                    closeCurrentPopup()
                                }

                                sumImgsWidth += imgBtnSize.x

                                if (sumImgsWidth + 400f < popupWidth)
                                    sameLine()
                                else
                                    sumImgsWidth = 0f
                            }
                        }
                    }

                    endPopup()
                }
            }
            return valueChanged
        }

        companion object {
            fun fromAnimationRegion(name: String, atlasFile: FileWrapper, animation: String, frameDuration: Float): AtlasData = AtlasData(name, frameDuration, *findAnimationRegions(atlasFile, animation))

            private fun findAnimationRegions(atlasFile: FileWrapper, animation: String): Array<Pair<FileWrapper, String>> {
                val atlas = PCGame.assetManager.loadOnDemand<TextureAtlas>(atlasFile).asset
                val regions = mutableListOf<String>()

                var i = 0
                while (atlas.findRegion(animation + "_$i") != null) {
                    regions.add(animation + "_$i")
                    ++i
                }

                return regions.map { atlasFile to it }.toTypedArray()
            }

            private fun findAnimationInAtlas(atlas: TextureAtlas): List<String> {
                val animationRegionNames = mutableListOf<String>()

                atlas.regions.forEach {
                    if (it.name.endsWith("_0")) {
                        animationRegionNames += it.name.removeSuffix("_0")
                    }
                }

                return animationRegionNames
            }
        }

    }

    override fun onAddToContainer(gameObject: GameObject, container: GameObjectContainer) {
        super.onAddToContainer(gameObject, container)

        data.items.forEach {
            it.onAddToContainer()
        }
    }

    override fun render(gameObject: GameObject, batch: Batch) {
        batch.setColor(1f, 1f, 1f, alpha)
        data.get()?.render(gameObject, flipX, flipY, batch)
        batch.setColor(1f, 1f, 1f, 1f)
    }


    override fun insertImgui(labelName: String, gameObject: GameObject, level: Level) {
        with(ImGui) {
            ImguiHelper.addImguiWidgetsArray("atlas", data::items, { AtlasData() }, {
                ImguiHelper.addImguiWidget(it.obj.name, it, gameObject, level, ExposeEditorFactory.empty)
            })
        }
    }
}