package be.catvert.pc.eca.components.graphics

import be.catvert.pc.PCGame
import be.catvert.pc.eca.Entity
import be.catvert.pc.eca.components.Component
import be.catvert.pc.eca.containers.Level
import be.catvert.pc.managers.ResourceManager
import be.catvert.pc.scenes.EditorScene
import be.catvert.pc.utility.*
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.math.MathUtils
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnore
import glm_.vec2.Vec2
import glm_.vec4.Vec4
import imgui.ImGui
import imgui.ItemFlags
import imgui.WindowFlags
import imgui.functionalProgramming
import ktx.assets.toLocalFile
import ktx.collections.gdxArrayOf
import ktx.collections.isEmpty


typealias AtlasRegion = Pair<FileWrapper, String>

/**
 * Component permettant d'ajouter des textures et animations a l'entité
 * @param currentIndex L'atlas actuel à dessiner
 * @param data Les atlas disponibles pour l'entité
 */
@Description("Permet d'ajouter une texture ou une animation à une entité")
class AtlasComponent(var currentIndex: Int = 0, var data: ArrayList<AtlasData>) : Component(), Renderable, ResourceLoader, CustomEditorImpl, CustomEditorTextImpl {
    enum class Rotation(val degree: Float) {
        Zero(0f), Quarter(90f), Half(180f), ThreeQuarter(270f)
    }

    constructor(currentIndex: Int = 0, vararg data: AtlasData) : this(currentIndex, arrayListOf(*data))
    @JsonCreator private constructor() : this(0, arrayListOf())

    @ExposeEditor
    var rotation: Rotation = Rotation.Zero

    @ExposeEditor(customName = "miroir x")
    var flipX: Boolean = false
    @ExposeEditor(customName = "miroir y")
    var flipY: Boolean = false

    @JsonIgnore
    var alpha: Float = 1f

    /**
     * Représente un atlas, donc une texture ou une animation
     * @param name Le nom de l'atlas
     * @param regions Les régions disponibles dans l'atlas, une region correspond à une texture, en ajoutant plusieurs textures, on obtient une animation
     * @param frameDuration Représente la vitesse de transition entre 2 régions. Si la frameDuration correspond à 1, il s'écoulera 1 seconde entre chaque region.
     */
    class AtlasData(var name: String, vararg regions: AtlasRegion, animationPlayMode: Animation.PlayMode = Animation.PlayMode.LOOP, frameDuration: Float = 1f / regions.size) {
        @JsonCreator constructor() : this("default")
        constructor(name: String, packFile: FileWrapper, animation: String, frameDuration: Float, animationPlayMode: Animation.PlayMode = Animation.PlayMode.LOOP) : this(name, *findAnimationRegions(packFile, animation), animationPlayMode = animationPlayMode, frameDuration = frameDuration)
        constructor(name: String, textureFile: FileWrapper) : this(name, textureFile to textureIdentifier)

        var regions = arrayListOf(*regions)

        var animationPlayMode: Animation.PlayMode = animationPlayMode
            set(value) {
                field = value
                if (::animation.isInitialized)
                    animation.playMode = value
            }

        var frameDuration: Float = frameDuration
            set(value) {
                field = value
                if (::animation.isInitialized)
                    animation.frameDuration = value
            }
        var repeatRegion: Boolean = false
        var repeatRegionSize = Size(50, 50)

        private lateinit var animation: Animation<TextureAtlas.AtlasRegion>

        /**
         * Représente le temps écoulé depuis le début de l'affichage de l'atlas, si le stateTime = 1.3f et que le frameDuration = 1f, la deuxième région sera dessiné.
         */
        private var stateTime = 0f

        fun render(entity: Entity, flipX: Boolean, flipY: Boolean, rotation: Rotation, batch: Batch) {
            /**
             * Si le nombre de région est <= à 1, il n'y a pas besoin de mettre à jour le temps écoulé car de toute façon une seule région sera dessinée.
             */
            if (regions.size > 1)
                stateTime += Utility.getDeltaTime()

            val frame = currentKeyFrame()
            if (repeatRegion && regions.size == 1) {
                for (x in 0 until MathUtils.floor(entity.box.width / repeatRegionSize.width.toFloat())) {
                    for (y in 0 until MathUtils.floor(entity.box.height / repeatRegionSize.height.toFloat())) {
                        batch.draw(frame, entity.box.x + x * repeatRegionSize.width, entity.box.y + y * repeatRegionSize.height, repeatRegionSize.width.toFloat(), repeatRegionSize.height.toFloat())
                    }
                }
            } else
                batch.draw(frame, entity.box, flipX, flipY, rotation.degree)
        }

        fun currentKeyFrame(): TextureAtlas.AtlasRegion = animation.getKeyFrame(stateTime)

        /**
         * Permet de mettre à jour l'atlas, si par exemple des régions sont modifiées.
         */
        fun updateAtlas(frameDuration: Float = this.frameDuration) {
            this.animation = loadAnimation()
            this.frameDuration = frameDuration
        }

        /**
         * Permet de changer la texture d'une région si celle-ci est un pack, dans ce cas, la région prendra la TextureRegion précédente du pack.
         */
        fun previousFrameRegion(regionIndex: Int) {
            this.regions.elementAtOrNull(regionIndex)?.apply {
                if (this.second != textureIdentifier) {
                    val atlas = ResourceManager.getPack(this.first.get())
                    val region = loadRegion(this)

                    val atlasRegions = atlas.regions.sortedBy { it.name }

                    val index = atlasRegions.indexOf(region)

                    if (index > 0) {
                        this@AtlasData.regions[regionIndex] = this.first to atlasRegions[index - 1].name
                        updateAtlas()
                    }
                }
            }
        }

        /**
         * Permet de changer la texture d'une région si celle-ci est un pack, dans ce cas, la région prendra la TextureRegion suivante du pack.
         */
        fun nextFrameRegion(regionIndex: Int) {
            this.regions.elementAtOrNull(regionIndex)?.apply {
                if (this.second != textureIdentifier) {
                    val atlas = ResourceManager.getPack(this.first.get())
                    val region = loadRegion(this)

                    val atlasRegions = atlas.regions.sortedBy { it.name }

                    val index = atlasRegions.indexOf(region)

                    if (index < atlas.regions.size - 1) {
                        this@AtlasData.regions[regionIndex] = this.first to atlasRegions[index + 1].name
                        updateAtlas()
                    }
                }
            }
        }

        /**
         * Permet de charger l'animation responsable d'animer les différentes régions si il y en a plusieurs.
         */
        private fun loadAnimation(): Animation<TextureAtlas.AtlasRegion> {
            val frames = gdxArrayOf<TextureAtlas.AtlasRegion>()

            regions.filter { it.first.get().exists() }.forEach {
                frames.add(loadRegion(it))
            }

            if (frames.isEmpty())
                frames.add(ResourceManager.defaultPackRegion)

            return Animation(frameDuration, frames, animationPlayMode)
        }

        companion object {
            const val emptyRegionIdentifier = "\$EMPTY"
            /**
             * Permet de charger une région depuis un pack ou une texture.
             */
            fun loadRegion(region: AtlasRegion): TextureAtlas.AtlasRegion {
                return when {
                    region.second == emptyRegionIdentifier -> ResourceManager.defaultPackRegion
                    region.second == AtlasComponent.textureIdentifier -> ResourceManager.getTexture(region.first.get()).toAtlasRegion()
                    else -> ResourceManager.getPackRegion(region.first.get(), region.second)
                }
            }
        }
    }

    override fun loadResources() {
        data.forEach {
            it.updateAtlas()
        }
    }

    override fun render(batch: Batch) {
        batch.setColor(1f, 1f, 1f, alpha)
        data.elementAtOrNull(currentIndex)?.render(entity, flipX, flipY, rotation, batch)
        batch.setColor(1f, 1f, 1f, 1f)
    }

    private var selectedAtlasIndex = -1
    private var showLevelAtlas = false

    private enum class EditAtlasType {
        Pack, Animations, Textures
    }

    private var editAtlasType: ImGuiHelper.Item<Enum<*>> = ImGuiHelper.Item(EditAtlasType.Pack)
    private var packFolderIndex = 0
    private var atlasIndex = 0
    private var selectRegionIndex = 0
    private var addAtlasName = "Nouveau atlas"
    private var ressourcesCollapsing = false

    override fun insertImgui(label: String, entity: Entity, level: Level, editorSceneUI: EditorScene.EditorSceneUI) {
        with(ImGui) {
            if (button("Éditer", Vec2(Constants.defaultWidgetsWidth, 0))) {
                showEditAtlasWindow = true
            }

            functionalProgramming.withItemWidth(Constants.defaultWidgetsWidth) {
                combo("atlas initial", ::currentIndex, data.map { it.name })
            }

            if (showEditAtlasWindow)
                drawEditWindow(level)
        }
    }

    /**
     * Permet de dessiner la fenêtre permettant d'éditer les différentes régions de l'atlas actuel.
     */
    private val imgBtnSize = Vec2(50, 50)
    private val editWindowWidth = 460f
    private fun drawEditWindow(level: Level) {
        with(ImGui) {
            setNextWindowSizeConstraints(Vec2(editWindowWidth, 200f), Vec2(editWindowWidth, 500f))
            functionalProgramming.withWindow("Éditer l'atlas", ::showEditAtlasWindow, flags = WindowFlags.AlwaysAutoResize.i) {
                val addAtlasTitle = "Ajouter un atlas"

                if (data.isEmpty()) {
                    if (button("Ajouter un atlas", Vec2(-1))) {
                        openPopup(addAtlasTitle)
                    }
                } else if (atlasIndex in data.indices) {
                    var openAddAtlasPopup = false

                    ImGuiHelper.comboWithSettingsButton("atlas", ::atlasIndex, data.map { it.name }, {
                        pushItemFlag(ItemFlags.Disabled.i, data.isEmpty())
                        if (button("Supprimer ${data.elementAtOrNull(atlasIndex)?.name
                                        ?: ""}", Vec2(Constants.defaultWidgetsWidth, 0f))) {
                            data.removeAt(atlasIndex)
                            atlasIndex = let {
                                if (atlasIndex > 0)
                                    atlasIndex - 1
                                else
                                    atlasIndex
                            }
                        }
                        popItemFlag()

                        if (button("Nouveau atlas", Vec2(Constants.defaultWidgetsWidth, 0f))) {
                            openAddAtlasPopup = true
                        }
                    })

                    if (openAddAtlasPopup)
                        openPopup(addAtlasTitle)

                    separator()

                    data.elementAtOrNull(atlasIndex)?.apply data@{
                        val regionBtnSize = Vec2(50f, 50f)

                        val itRegions = this.regions.listIterator()
                        var regionIndex = 0

                        fun addPlusBtn() {
                            if (button("+", Vec2(20f, regionBtnSize.y + style.framePadding.y * 2f)))
                                itRegions.add(AtlasData.emptyRegionIdentifier.toLocalFile().toFileWrapper() to AtlasData.emptyRegionIdentifier)
                        }

                        while (itRegions.hasNext()) {
                            val it = itRegions.next()

                            val region = AtlasComponent.AtlasData.loadRegion(it)
                            val tintCol = if (selectRegionIndex != regionIndex) Vec4(1f, 1f, 1f, 0.3f) else Vec4(1f)

                            functionalProgramming.withGroup {
                                functionalProgramming.withGroup {
                                    val btnSize = Vec2((regionBtnSize.x + style.itemInnerSpacing.x) / 2f)
                                    if (button("<-", btnSize)) {
                                        this.previousFrameRegion(regionIndex)
                                    }
                                    sameLine(0f, style.itemInnerSpacing.x)
                                    if (button("->", btnSize)) {
                                        this.nextFrameRegion(regionIndex)
                                    }
                                }

                                if (imageButton(region.texture.textureObjectHandle, regionBtnSize, Vec2(region.u, region.v), Vec2(region.u2, region.v2), tintCol = tintCol)) {
                                    selectRegionIndex = regionIndex
                                }

                                if (regionIndex == this.regions.size - 1) {
                                    sameLine(0f, style.itemInnerSpacing.x)
                                    addPlusBtn()
                                }

                                functionalProgramming.withId("suppr region $regionIndex") {
                                    if (button("Suppr.", Vec2(regionBtnSize.x + style.framePadding.x * 2f, 0))) {
                                        itRegions.remove()
                                        updateAtlas()
                                        regionIndex -= 1
                                    }
                                }
                            }

                            if (regionIndex < this.regions.size - 1)
                                sameLine()

                            ++regionIndex
                        }

                        if (this.regions.isEmpty()) {
                            addPlusBtn()
                        }

                        if (this.regions.size > 1) {
                            functionalProgramming.withItemWidth(Constants.defaultWidgetsWidth) {
                                sliderFloat("vitesse", ::frameDuration, 0f, 1f)
                            }
                            val playModeItem = ImGuiHelper.Item(animationPlayMode)
                            ImGuiHelper.enum("mode", playModeItem.cast())
                            animationPlayMode = playModeItem.obj
                        } else if (this.regions.size == 1) {
                            checkbox("répéter la région", ::repeatRegion)
                            if (repeatRegion) {
                                ImGuiHelper.size(::repeatRegionSize, Size(1), Size(Constants.maxEntitySize))
                            }
                        }

                        separator()
                    }

                    if (selectedAtlasIndex == -1) {
                        val searchRegion = data.elementAtOrNull(atlasIndex)?.regions?.elementAtOrNull(0)?.first?.get()
                        selectedAtlasIndex = PCGame.gameAtlas.entries.elementAtOrNull(packFolderIndex)?.value?.indexOfFirst { it == searchRegion } ?: -1
                        if (selectedAtlasIndex == -1) {
                            selectedAtlasIndex = level.resourcesAtlas().indexOfFirst { it == searchRegion }
                            if (selectedAtlasIndex == -1)
                                selectedAtlasIndex = 0
                            else
                                showLevelAtlas = true
                        }
                    }

                    drawRessources(level)
                }

                functionalProgramming.popupModal(addAtlasTitle, extraFlags = WindowFlags.AlwaysAutoResize.i) {
                    ImGuiHelper.inputText("nom", ::addAtlasName)
                    if (button("Ajouter", Vec2(-1, 0))) {
                        data.add(AtlasData(addAtlasName).apply { updateAtlas() })
                        closeCurrentPopup()
                    }
                    if (button("Fermer", Vec2(-1, 0)))
                        closeCurrentPopup()
                }
            }
        }
    }

    private fun drawRessources(level: Level) {
        with(ImGui) {
            ressourcesCollapsing = false
            functionalProgramming.collapsingHeader("ressources") {
                ressourcesCollapsing = true
                ImGuiHelper.enumWithSettingsButton("type", editAtlasType, {
                    checkbox("ressources importées", ::showLevelAtlas)
                })

                var sumImgsWidth = 0f

                val editAtlasType = editAtlasType.obj as EditAtlasType
                if (editAtlasType != EditAtlasType.Textures) {
                    functionalProgramming.withItemWidth(Constants.defaultWidgetsWidth) {
                        if (!showLevelAtlas) {
                            combo("dossier", ::packFolderIndex, PCGame.gameAtlas.map { it.key.name() })
                        }
                        combo("pack", ::selectedAtlasIndex, if (showLevelAtlas) level.resourcesAtlas().map { it.nameWithoutExtension() } else PCGame.gameAtlas.entries.elementAtOrNull(packFolderIndex)?.value?.map { it.nameWithoutExtension() }
                                ?: arrayListOf())
                    }
                    (if (showLevelAtlas) level.resourcesAtlas().getOrNull(selectedAtlasIndex) else PCGame.gameAtlas.entries.elementAtOrNull(packFolderIndex)?.value?.getOrNull(selectedAtlasIndex))?.also { atlasPath ->
                        if (atlasPath.exists()) {
                            val atlas = ResourceManager.getPack(atlasPath)
                            when (editAtlasType) {
                                AtlasComponent.EditAtlasType.Pack -> {
                                    if (atlasPath.exists()) {
                                        atlas.regions.sortedBy { it.name }.forEach { region ->
                                            if (imageButton(region.texture.textureObjectHandle, imgBtnSize, Vec2(region.u, region.v), Vec2(region.u2, region.v2))) {
                                                data.elementAtOrNull(atlasIndex)?.apply {
                                                    if (selectRegionIndex in this.regions.indices) {
                                                        this.regions[selectRegionIndex] = atlasPath.toFileWrapper() to region.name
                                                        updateAtlas()
                                                    }
                                                }
                                            }

                                            sumImgsWidth += imgBtnSize.x + style.itemInnerSpacing.x * 2f

                                            if (sumImgsWidth + imgBtnSize.x + style.itemInnerSpacing.x * 2f < editWindowWidth)
                                                sameLine(0f, style.itemInnerSpacing.x)
                                            else
                                                sumImgsWidth = 0f
                                        }
                                    }
                                }
                                AtlasComponent.EditAtlasType.Animations -> {
                                    findAnimationInPack(atlas).forEach {
                                        val region = atlas.findRegion(it + "_0")

                                        if (imageButton(region.texture.textureObjectHandle, imgBtnSize, Vec2(region.u, region.v), Vec2(region.u2, region.v2))) {
                                            data.elementAtOrNull(atlasIndex)?.apply {
                                                this.regions.clear()
                                                this.regions.addAll(findAnimationRegions(atlasPath.toFileWrapper(), it))
                                                this.updateAtlas()
                                            }
                                        }

                                        sumImgsWidth += imgBtnSize.x + style.itemInnerSpacing.x * 2f

                                        if (sumImgsWidth + imgBtnSize.x + style.itemInnerSpacing.x * 2f < editWindowWidth)
                                            sameLine(0f, style.itemInnerSpacing.x)
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
                        val texture = ResourceManager.getTexture(it)

                        if (imageButton(texture.textureObjectHandle, imgBtnSize, uv1 = Vec2(1))) {
                            data.elementAtOrNull(atlasIndex)?.apply {
                                if (selectRegionIndex in this.regions.indices) {
                                    this.regions[selectRegionIndex] = it.toFileWrapper() to textureIdentifier
                                    updateAtlas()
                                }
                            }
                        }

                        sumImgsWidth += imgBtnSize.x + style.itemInnerSpacing.x * 2f

                        if (sumImgsWidth + imgBtnSize.x + style.itemInnerSpacing.x * 2f < editWindowWidth)
                            sameLine(0f, style.itemInnerSpacing.x)
                        else
                            sumImgsWidth = 0f
                    }
                }
            }
        }
    }

    companion object {
        private var showEditAtlasWindow = false
        private val textureIdentifier = "\$texture_atlas"

        /**
         * Permet de charger les différentes régions requise pour une animation prédéfinies dans un pack
         */
        private fun findAnimationRegions(atlasFile: FileWrapper, animation: String): Array<Pair<FileWrapper, String>> {
            val atlas = ResourceManager.getPack(atlasFile.get())
            val regions = mutableListOf<String>()

            var i = 0
            while (atlas.findRegion(animation + "_$i") != null) {
                regions.add(animation + "_$i")
                ++i
            }

            return regions.map { atlasFile to it }.toTypedArray()
        }

        /**
         * Permet de trouver les différentes animations prédéfinies dans un pack
         */
        private fun findAnimationInPack(textureAtlas: TextureAtlas): List<String> {
            val animationRegionNames = mutableListOf<String>()

            textureAtlas.regions.forEach {
                if (it.name.endsWith("_0")) {
                    animationRegionNames += it.name.removeSuffix("_0")
                }
            }

            return animationRegionNames
        }
    }

    override fun insertText() {
        ImGuiHelper.textPropertyColored(Color.ORANGE, "atlas actuel :", data.elementAtOrNull(currentIndex)?.name ?: "/")
        ImGuiHelper.textPropertyColored(Color.OLIVE, "alpha :", alpha)
        data.forEach {
            ImGuiHelper.textColored(Color.RED, "<-->")
            ImGuiHelper.textPropertyColored(Color.ORANGE, "nom :", it.name)
            if (it.regions.size > 1)
                ImGuiHelper.textPropertyColored(Color.ORANGE, "anim mode :", it.animationPlayMode.toString())
            it.regions.forEach {
                ImGuiHelper.textPropertyColored(Color.ORANGE, " - ${it.first.get().nameWithoutExtension()} ->", it.second)
            }
            ImGuiHelper.textColored(Color.RED, "<-->")
        }
    }
}