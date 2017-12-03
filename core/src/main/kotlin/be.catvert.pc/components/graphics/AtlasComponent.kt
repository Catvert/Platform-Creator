package be.catvert.pc.components.graphics

import be.catvert.pc.GameObject
import be.catvert.pc.PCGame
import be.catvert.pc.components.RenderableComponent
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
class AtlasComponent(var currentIndex: Int = 0, var data: ArrayList<AtlasData>) : RenderableComponent(), CustomEditorImpl {
    constructor(currentIndex: Int = 0, vararg data: AtlasData) : this(currentIndex, arrayListOf(*data))
    @JsonCreator private constructor() : this(0, arrayListOf())

    class AtlasData(var name: String, vararg regions: Pair<FileWrapper, String>, frameDuration: Float = 1f / regions.size) {
        @JsonCreator constructor() : this("default", Constants.defaultAtlasPath)
        constructor(name: String, atlasFile: FileWrapper, animation: String, frameDuration: Float) : this(name, *findAnimationRegions(atlasFile, animation), frameDuration = frameDuration)
        constructor(name: String, textureFile: FileWrapper) : this(name, textureFile to textureIdentifier)

        var regions = arrayListOf(*regions)

        var frameDuration: Float = frameDuration
            set(value) {
                field = value
                if (::animation.isInitialized)
                    animation.frameDuration = value
            }

        private lateinit var animation: Animation<TextureAtlas.AtlasRegion>

        private var stateTime = 0f

        fun render(gameObject: GameObject, flipX: Boolean, flipY: Boolean, batch: Batch) {
            if (regions.size > 1)
                stateTime += Gdx.graphics.deltaTime
            batch.draw(animation.getKeyFrame(stateTime), gameObject.box, flipX, flipY)
        }

        fun updateAtlas(frameDuration: Float = this.frameDuration) {
            this.animation = loadAnimation()
            this.frameDuration = frameDuration
        }

        fun previousFrameRegion(regionIndex: Int) {
            this.regions.elementAtOrNull(regionIndex)?.apply {
                val atlas = PCGame.assetManager.loadOnDemand<TextureAtlas>(this.first).asset
                val region = loadRegion(this)
                val index = atlas.regions.indexOf(region)

                if (index > 0) {
                    this@AtlasData.regions[regionIndex] = this.first to atlas.regions[index - 1].name
                    updateAtlas()
                }
            }
        }

        fun nextFrameRegion(regionIndex: Int) {
            this.regions.elementAtOrNull(regionIndex)?.apply {
                val atlas = PCGame.assetManager.loadOnDemand<TextureAtlas>(this.first).asset
                val region = loadRegion(this)
                val index = atlas.regions.indexOf(region)

                if (index < atlas.regions.size - 1) {
                    this@AtlasData.regions[regionIndex] = this.first to atlas.regions[index + 1].name
                    updateAtlas()
                }
            }
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
            fun loadRegion(region: Pair<FileWrapper, String>): TextureAtlas.AtlasRegion {
                return if (region.first.get().exists()) {
                    if (region.second != AtlasComponent.textureIdentifier)
                        PCGame.assetManager.loadOnDemand<TextureAtlas>(region.first).asset.findRegion(region.second)
                    else {
                        val texture = PCGame.assetManager.loadOnDemand<Texture>(region.first).asset
                        TextureAtlas.AtlasRegion(texture, 0, 0, texture.width, texture.height)
                    }
                } else {
                    PCGame.assetManager.loadOnDemand<TextureAtlas>(Constants.defaultAtlasPath.first).asset.findRegion(Constants.defaultAtlasPath.second)
                }
            }
        }
    }

    override fun onAddToContainer(gameObject: GameObject, container: GameObjectContainer) {
        super.onAddToContainer(gameObject, container)
        data.forEach {
            it.updateAtlas()
        }
    }

    override fun render(gameObject: GameObject, batch: Batch) {
        batch.setColor(1f, 1f, 1f, alpha)
        data.elementAtOrNull(currentIndex)?.render(gameObject, flipX, flipY, batch)
        batch.setColor(1f, 1f, 1f, 1f)
    }

    private var selectedAtlasIndex = -1
    private var showLevelAtlas = false

    private enum class EditAtlasType {
        Pack, Animations, Textures
    }

    private var editAtlasType: ImguiHelper.Item<Enum<*>> = ImguiHelper.Item(EditAtlasType.Pack)
    private var atlasIndex = 0
    private var regionIndex = 0
    private var addAtlasName = "test"
    private var ressourcesCollapsing = false

    override fun insertImgui(labelName: String, gameObject: GameObject, level: Level) {
        with(ImGui) {
            functionalProgramming.withItemWidth(100f) {
                combo("atlas initial", ::currentIndex, data.map { it.name })
            }

            if (button("Éditer", Vec2(-1, 20f))) {
                showEditAtlasWindow = true
            }

            if (showEditAtlasWindow)
                drawEditWindow(gameObject, level)
        }
    }

    private fun drawEditWindow(gameObject: GameObject, level: Level) {
        with(ImGui) {
            setNextWindowSizeConstraints(Vec2(500f, 200f), Vec2(500f, 500f))
            functionalProgramming.withWindow("Éditer l'atlas", ::showEditAtlasWindow, flags = WindowFlags.AlwaysAutoResize.i) {
                val addAtlasTitle = "Ajouter un atlas"

                functionalProgramming.popupModal(addAtlasTitle, extraFlags = WindowFlags.AlwaysAutoResize.i) {
                    val atlasName = addAtlasName.toCharArray()
                    functionalProgramming.withItemWidth(150f) {
                        if (inputText("Nom", atlasName))
                            addAtlasName = String(atlasName)
                    }
                    if (button("Ajouter", Vec2(-1, 20f))) {
                        data.add(AtlasData(addAtlasName))
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
                            data.removeAt(atlasIndex)
                            atlasIndex = let {
                                if (atlasIndex > 0)
                                    atlasIndex - 1
                                else
                                    atlasIndex
                            }
                            return@withWindow
                        }
                        sameLine()
                    }

                    functionalProgramming.withItemWidth(300f) {
                        if (combo("atlas", ::atlasIndex, data.map { it.name } + "Ajouter un atlas")) {
                            selectedAtlasIndex = -1

                            if (atlasIndex == data.size) {
                                openPopup(addAtlasTitle)
                            }
                        }
                    }

                    separator()

                    data.elementAtOrNull(atlasIndex)?.apply data@ {

                        fun addPlusBtn(): Boolean {
                            if (button("+", Vec2(20f, gameObject.box.height + 8f))) {
                                this.regions.add(Constants.defaultAtlasPath)
                                updateAtlas()
                                return true
                            }
                            return false
                        }

                        this.regions.forEachIndexed { index, it ->
                            val region = AtlasComponent.AtlasData.loadRegion(it)
                            val tintCol = if (regionIndex != index) Vec4(1f, 1f, 1f, 0.3f) else Vec4(1f)

                            functionalProgramming.withGroup {
                                functionalProgramming.withGroup {
                                    if (button("<-", Vec2((gameObject.box.width + 5f) / 2f, 20f))) {
                                        this.previousFrameRegion(index)
                                    }
                                    sameLine()
                                    if (button("->", Vec2((gameObject.box.width + 5f) / 2f, 20f))) {
                                        this.nextFrameRegion(index)
                                    }
                                }
                                if (imageButton(region.texture.textureObjectHandle, Vec2(gameObject.box.width, gameObject.box.height), Vec2(region.u, region.v), Vec2(region.u2, region.v2), tintCol = tintCol)) {
                                    regionIndex = index
                                }

                                if (index == this.regions.size - 1) {
                                    sameLine()
                                    if (addPlusBtn())
                                        return@data
                                }

                                functionalProgramming.withId("suppr region $index") {
                                    if (button("Suppr.", Vec2(gameObject.box.width + 10f, 20f))) {
                                        data.elementAtOrNull(atlasIndex)?.apply {
                                            this.regions.removeAt(index)
                                            updateAtlas()
                                        }
                                        return@data
                                    }
                                }
                            }

                            if (index < this.regions.size - 1)
                                sameLine()
                        }

                        if (this.regions.isEmpty()) {
                            addPlusBtn()
                        }

                        if (this.regions.size > 1) {
                            functionalProgramming.withItemWidth(calcItemWidth()) {
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

                    ressourcesCollapsing = false
                    functionalProgramming.collapsingHeader("ressources") {
                        ressourcesCollapsing = true
                        ImguiHelper.enum("type", editAtlasType)

                        var sumImgsWidth = 0f

                        val editAtlasType = editAtlasType.obj as EditAtlasType
                        if (editAtlasType != EditAtlasType.Textures) {
                            checkbox("pack importés", ::showLevelAtlas)
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
                                                            if (regionIndex in this.regions.indices) {
                                                                this.regions[regionIndex] = atlasPath.toFileWrapper() to region.name
                                                                updateAtlas()
                                                            }
                                                        }
                                                    }

                                                    sumImgsWidth += imgBtnSize.x + 15f

                                                    if (sumImgsWidth + imgBtnSize.x + 15f < 450f)
                                                        sameLine()
                                                    else
                                                        sumImgsWidth = 0f
                                                }
                                            }
                                        }
                                        AtlasComponent.EditAtlasType.Animations -> {
                                            findAnimationInAtlas(atlas).forEach {
                                                val region = atlas.findRegion(it + "_0")
                                                val imgBtnSize = Vec2(Math.min(region.regionWidth, 200), Math.min(region.regionHeight, 200))

                                                if (imageButton(region.texture.textureObjectHandle, imgBtnSize, Vec2(region.u, region.v), Vec2(region.u2, region.v2))) {
                                                    data.elementAtOrNull(atlasIndex)?.apply {
                                                        this.regions.clear()
                                                        this.regions.addAll(findAnimationRegions(atlasPath.toFileWrapper(), it))
                                                        this.updateAtlas()
                                                    }
                                                }

                                                sumImgsWidth += imgBtnSize.x + 15f

                                                if (sumImgsWidth + imgBtnSize.x + 15f < 450f)
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
                                    data.elementAtOrNull(atlasIndex)?.apply {
                                        if (regionIndex in this.regions.indices) {
                                            this.regions[regionIndex] = it.toFileWrapper() to textureIdentifier
                                            updateAtlas()
                                        }
                                    }
                                }

                                sumImgsWidth += imgBtnSize.x + 15f

                                if (sumImgsWidth + imgBtnSize.x + 15f < 500f)
                                    sameLine()
                                else
                                    sumImgsWidth = 0f
                            }
                        }
                    }
                }
            }
        }
    }

    companion object {
        private var showEditAtlasWindow = false
        private val textureIdentifier = "\$texture_atlas"

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