package be.catvert.pc.components.graphics

import be.catvert.pc.GameObject
import be.catvert.pc.PCGame
import be.catvert.pc.components.RenderableComponent
import be.catvert.pc.components.graphics.AtlasComponent.AtlasData.Companion.findAnimationRegions
import be.catvert.pc.containers.GameObjectContainer
import be.catvert.pc.containers.Level
import be.catvert.pc.utility.*
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.fasterxml.jackson.annotation.JsonCreator
import glm_.vec2.Vec2
import glm_.vec4.Vec4
import imgui.Cond
import imgui.ImGui
import imgui.WindowFlags
import imgui.functionalProgramming
import ktx.collections.gdxArrayOf
import ktx.collections.isEmpty

/**
 * Component permettant d'ajouter une texture chargée au préalable depuis un atlas et une région
 * @param atlasPath Le chemin vers l'atlas en question
 * @param region La région à utiliser
 */
class AtlasComponent(var currentIndex: Int = 0, vararg data: AtlasData) : RenderableComponent(), CustomEditorImpl {
    var data = arrayOf(*data)

    class AtlasData(var name: String, vararg regions: Pair<FileWrapper, String>, frameDuration: Float = 1f / regions.size) {
        @JsonCreator constructor() : this("default", Constants.defaultAtlasPath)
        constructor(name: String, atlasFile: FileWrapper, animation: String, frameDuration: Float) : this(name, *findAnimationRegions(atlasFile, animation), frameDuration = frameDuration)
        constructor(name: String, textureFile: FileWrapper) : this(name, textureFile to textureIdentifier)

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

        fun updateAtlas(regions: Array<Pair<FileWrapper, String>> = this.regions, frameDuration: Float = this.frameDuration) {
            this.regions = regions
            this.frameDuration = frameDuration
            this.animation = loadAnimation()
        }

        private fun loadAnimation(): Animation<TextureAtlas.AtlasRegion> {
            val frames = gdxArrayOf<TextureAtlas.AtlasRegion>()

            regions.filter { it.first.get().exists() }.forEach {
                frames.add(loadRegion(it))
            }

            if (frames.isEmpty())
                frames.add(loadRegion(Constants.defaultAtlasPath))

            return Animation(frameDuration, frames, Animation.PlayMode.LOOP)
        }

        companion object {
            fun findAnimationRegions(atlasFile: FileWrapper, animation: String): Array<Pair<FileWrapper, String>> {
                val atlas = PCGame.assetManager.loadOnDemand<TextureAtlas>(atlasFile).asset
                val regions = mutableListOf<String>()

                var i = 0
                while (atlas.findRegion(animation + "_$i") != null) {
                    regions.add(animation + "_$i")
                    ++i
                }

                return regions.map { atlasFile to it }.toTypedArray()
            }

            fun findAnimationInAtlas(atlas: TextureAtlas): List<String> {
                val animationRegionNames = mutableListOf<String>()

                atlas.regions.forEach {
                    if (it.name.endsWith("_0")) {
                        animationRegionNames += it.name.removeSuffix("_0")
                    }
                }

                return animationRegionNames
            }

            fun loadRegion(region: Pair<FileWrapper, String>): TextureAtlas.AtlasRegion {
                return if (region.second != textureIdentifier)
                    PCGame.assetManager.loadOnDemand<TextureAtlas>(region.first).asset.findRegion(region.second)
                else {
                    val texture = PCGame.assetManager.loadOnDemand<Texture>(region.first).asset
                    TextureAtlas.AtlasRegion(texture, 0, 0, texture.width, texture.height)
                }
            }
        }

    }

    override fun onAddToContainer(gameObject: GameObject, container: GameObjectContainer) {
        super.onAddToContainer(gameObject, container)

        data.forEach {
            it.onAddToContainer()
        }
    }

    override fun render(gameObject: GameObject, batch: Batch) {
        batch.setColor(1f, 1f, 1f, alpha)
        data.elementAtOrNull(currentIndex)?.render(gameObject, flipX, flipY, batch)
        batch.setColor(1f, 1f, 1f, 1f)
    }

    private var showEditAtlasWindow = false
    private var selectedAtlasIndex = -1
    private var showLevelAtlas = false

    private enum class EditAtlasType {
        Pack, Animations, Textures
    }

    private var editAtlasType: ImguiHelper.Item<Enum<*>> = ImguiHelper.Item(EditAtlasType.Pack)
    private var atlasIndex = 0
    private var regionIndex = 0
    private var addAtlasName = "test"

    override fun insertImgui(labelName: String, gameObject: GameObject, level: Level) {
        with(ImGui) {
            functionalProgramming.withItemWidth(100f) {
                combo("atlas initial", ::currentIndex, data.map { it.name })
            }

            if (button("Éditer", Vec2(-1, 20f))) {
                showEditAtlasWindow = true
            }

            if(showEditAtlasWindow)
                drawEditWindow(gameObject, level)
        }
    }

    private fun drawEditWindow(gameObject: GameObject, level: Level) {
        with(ImGui) {
            val editWindowSize = if (data.isEmpty()) Vec2(200f, 100f) else Vec2(500f, 500f)
            setNextWindowSize(editWindowSize, Cond.Always)
            setNextWindowPos(Vec2(Gdx.graphics.width / 2f - editWindowSize.x / 2f, Gdx.graphics.height / 2f - editWindowSize.y / 2f), Cond.Once)
            functionalProgramming.withWindow("Éditer l'atlas", ::showEditAtlasWindow, flags = WindowFlags.NoResize.i or WindowFlags.AlwaysHorizontalScrollbar.i) {
                val addAtlasTitle = "Ajouter un atlas"

                functionalProgramming.popupModal(addAtlasTitle, extraFlags = WindowFlags.AlwaysAutoResize.i) {
                    val atlasName = addAtlasName.toCharArray()
                    functionalProgramming.withItemWidth(150f) {
                        if (inputText("Nom", atlasName))
                            addAtlasName = String(atlasName)
                    }
                    if (button("Ajouter", Vec2(-1, 20f))) {
                        data += AtlasData(addAtlasName)
                        closeCurrentPopup()
                    }
                    if (button("Fermer", Vec2(-1, 20f)))
                        closeCurrentPopup()
                }

                if (data.isEmpty()) {
                    if (button("Ajouter un atlas", Vec2(-1))) {
                        openPopup(addAtlasTitle)
                    }
                } else if (atlasIndex in data.indices) {
                    if (data.isNotEmpty()) {
                        if (button("Supprimer")) {
                            data = data.toMutableList().apply { removeAt(atlasIndex) }.toTypedArray()
                            atlasIndex = let {
                                if (atlasIndex > 0)
                                    atlasIndex - 1
                                else
                                    atlasIndex
                            }
                        }
                        sameLine()
                    }

                    functionalProgramming.withItemWidth(-1) {
                        if (combo("atlas", ::atlasIndex, data.map { it.name } + "Ajouter un atlas")) {
                            selectedAtlasIndex = -1

                            if (atlasIndex == data.size) {
                                openPopup(addAtlasTitle)
                            }
                        }
                    }

                    separator()

                    data.elementAtOrNull(atlasIndex)?.apply {
                        this.regions.forEachIndexed { index, it ->
                            val region = AtlasComponent.AtlasData.loadRegion(it)
                            val tintCol = if (regionIndex != index) Vec4(1f, 1f, 1f, 0.3f) else Vec4(1f)

                            functionalProgramming.withGroup {
                                if (imageButton(region.texture.textureObjectHandle, Vec2(gameObject.box.width, gameObject.box.height), Vec2(region.u, region.v), Vec2(region.u2, region.v2), tintCol = tintCol)) {
                                    regionIndex = index
                                }
                                functionalProgramming.withId("suppr region $index") {
                                    if (button("Suppr.", Vec2(gameObject.box.width + 10f, 20f))) {
                                        data.elementAtOrNull(atlasIndex)?.apply {
                                            this.regions = this.regions.toMutableList().apply { removeAt(index) }.toTypedArray()
                                        }
                                        return@withWindow
                                    }
                                }
                            }

                            if (index < this.regions.size)
                                sameLine()
                        }

                        if (button("+", Vec2(20f, gameObject.box.height + 8f))) {
                            this.regions += Constants.defaultAtlasPath
                        }

                        if (this.regions.size > 1) {
                            functionalProgramming.withItemWidth(-1) {
                                sliderFloat("Vitesse", ::frameDuration, 0f, 1f)
                            }
                        }

                        separator()
                    }

                    if (selectedAtlasIndex == -1) {
                        val searchRegion = data.elementAtOrNull(atlasIndex)?.regions?.elementAtOrNull(0)?.first?.get()
                        selectedAtlasIndex = PCGame.gameAtlas.indexOfFirst { it == searchRegion }
                        if (selectedAtlasIndex == -1) {
                            selectedAtlasIndex = level.resourcesAtlas().indexOfFirst { it == searchRegion }
                            if (selectedAtlasIndex == -1)
                                selectedAtlasIndex = 0
                            else
                                showLevelAtlas = true
                        }
                    }

                    ImguiHelper.enum("type", editAtlasType)

                    var sumImgsWidth = 0f

                    val editAtlasType = editAtlasType.obj as EditAtlasType
                    if (editAtlasType != EditAtlasType.Textures) {
                        if (checkbox("pack importés", ::showLevelAtlas))
                            selectedAtlasIndex = 0
                        functionalProgramming.withItemWidth(150f) {
                            combo("pack", ::selectedAtlasIndex, if (showLevelAtlas) level.resourcesAtlas().map { it.nameWithoutExtension() } else PCGame.gameAtlas.map { it.nameWithoutExtension() })
                        }
                        (if (showLevelAtlas) level.resourcesAtlas().getOrNull(selectedAtlasIndex) else PCGame.gameAtlas.getOrNull(selectedAtlasIndex))?.also { atlasPath ->
                            if (atlasPath.exists()) {
                                val atlas = PCGame.assetManager.loadOnDemand<TextureAtlas>(atlasPath.toFileWrapper()).asset
                                when (editAtlasType) {
                                    AtlasComponent.EditAtlasType.Pack -> {
                                        if (atlasPath.exists()) {
                                            atlas.regions.forEach { region ->
                                                val imgBtnSize = Vec2(Math.min(region.regionWidth, 200), Math.min(region.regionHeight, 200))

                                                if (imageButton(region.texture.textureObjectHandle, imgBtnSize, Vec2(region.u, region.v), Vec2(region.u2, region.v2))) {
                                                    data.elementAtOrNull(atlasIndex)?.apply {
                                                        this.regions[regionIndex] = atlasPath.toFileWrapper() to region.name
                                                        updateAtlas()
                                                    }
                                                }

                                                sumImgsWidth += imgBtnSize.x + 15f

                                                if (sumImgsWidth + imgBtnSize.x + 15f < editWindowSize.x)
                                                    sameLine()
                                                else
                                                    sumImgsWidth = 0f
                                            }
                                        }
                                    }
                                    AtlasComponent.EditAtlasType.Animations -> {
                                        AtlasData.findAnimationInAtlas(atlas).forEach {
                                            val region = atlas.findRegion(it + "_0")
                                            val imgBtnSize = Vec2(Math.min(region.regionWidth, 200), Math.min(region.regionHeight, 200))

                                            if (imageButton(region.texture.textureObjectHandle, imgBtnSize, Vec2(region.u, region.v), Vec2(region.u2, region.v2))) {
                                                data.elementAtOrNull(atlasIndex)?.apply {
                                                    this.regions = findAnimationRegions(atlasPath.toFileWrapper(), it)
                                                    this.updateAtlas()
                                                }
                                            }

                                            sumImgsWidth += imgBtnSize.x + 15f

                                            if (sumImgsWidth < editWindowSize.x)
                                                sameLine()
                                            else
                                                sumImgsWidth = 0f
                                        }
                                    }
                                    else -> {
                                    }
                                }
                            }
                        }
                    } else {
                        (PCGame.gameTextures + level.resourcesTextures()).filter { it.exists() }.forEach {
                            val texture = PCGame.assetManager.loadOnDemand<Texture>(it).asset
                            val imgBtnSize = Vec2(Math.min(texture.width, 200), Math.min(texture.height, 200))

                            if (imageButton(texture.textureObjectHandle, imgBtnSize, uv1 = Vec2(1))) {
                                data.elementAtOrNull(atlasIndex)?.regions?.apply {
                                    set(regionIndex, it.toFileWrapper() to textureIdentifier)
                                }
                            }

                            sumImgsWidth += imgBtnSize.x + 15f

                            if (sumImgsWidth + imgBtnSize.x + 15f < editWindowSize.x)
                                sameLine()
                            else
                                sumImgsWidth = 0f
                        }
                    }
                }
            }
        }
    }

    companion object {
        private val textureIdentifier = "texture_atlas"
    }
}