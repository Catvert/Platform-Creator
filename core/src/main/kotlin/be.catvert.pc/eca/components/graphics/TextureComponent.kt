package be.catvert.pc.eca.components.graphics

import be.catvert.pc.PCGame
import be.catvert.pc.eca.Entity
import be.catvert.pc.eca.components.Component
import be.catvert.pc.eca.containers.Level
import be.catvert.pc.managers.ResourceManager
import be.catvert.pc.scenes.EditorScene
import be.catvert.pc.ui.*
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


typealias TextureRegion = Pair<FileWrapper, String>

/**
 * Component permettant d'ajouter des textures et animations a l'entité
 * @param currentIndex La texture actuelle à dessiner
 * @param data Les textures disponibles pour l'entité
 */
@Description("Permet d'ajouter une texture ou une animation à une entité")
class TextureComponent(var currentIndex: Int = 0, var data: ArrayList<TextureData>) : Component(), Renderable, ResourceLoader, UIImpl, UITextImpl {
    enum class Rotation(val degree: Float) {
        Zero(0f), Quarter(90f), Half(180f), ThreeQuarter(270f)
    }

    constructor(currentIndex: Int = 0, vararg data: TextureData) : this(currentIndex, arrayListOf(*data))
    @JsonCreator private constructor() : this(0, arrayListOf())

    @UI
    var rotation: Rotation = Rotation.Zero

    @UI(customName = "miroir x")
    var flipX: Boolean = false
    @UI(customName = "miroir y")
    var flipY: Boolean = false

    @JsonIgnore
    var alpha: Float = 1f

    /**
     * Représente une texture ou une animation
     * @param name Le nom de la texture
     * @param regions Les régions disponibles, une région représente une texture, en ajoutant plusieurs régions, on obtient une animation
     * @param frameDuration Représente la vitesse de transition entre 2 régions. Si la frameDuration correspond à 1, il s'écoulera 1 seconde entre chaque region.
     */
    class TextureData(var name: String, vararg regions: TextureRegion, animationPlayMode: Animation.PlayMode = Animation.PlayMode.LOOP, frameDuration: Float = 1f / regions.size) {
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
         * Représente le temps écoulé depuis le début de l'affichage de la texture/animation, si le stateTime = 1.3f et que le frameDuration = 1f, la deuxième région sera dessiné.
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
         * Permet de mettre à jour l'animation, si par exemple des régions sont modifiées.
         */
        fun updateRegions(frameDuration: Float = this.frameDuration) {
            this.animation = loadAnimation()
            this.frameDuration = frameDuration
        }

        /**
         * Permet de changer la texture d'une région si celle-ci est un pack, dans ce cas, la région prendra la TextureRegion précédente du pack.
         */
        fun previousFrameRegion(regionIndex: Int) {
            this.regions.elementAtOrNull(regionIndex)?.apply {
                if (this.second != textureIdentifier) {
                    val pack = ResourceManager.getPack(this.first.get())
                    val region = loadRegion(this)

                    val packRegions = pack.regions.sortedBy { it.name }

                    val index = packRegions.indexOf(region)

                    if (index > 0) {
                        this@TextureData.regions[regionIndex] = this.first to packRegions[index - 1].name
                        updateRegions()
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
                    val pack = ResourceManager.getPack(this.first.get())
                    val region = loadRegion(this)

                    val packRegions = pack.regions.sortedBy { it.name }

                    val index = packRegions.indexOf(region)

                    if (index < pack.regions.size - 1) {
                        this@TextureData.regions[regionIndex] = this.first to packRegions[index + 1].name
                        updateRegions()
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
            fun loadRegion(region: TextureRegion): TextureAtlas.AtlasRegion {
                return when {
                    region.second == emptyRegionIdentifier -> ResourceManager.defaultPackRegion
                    region.second == TextureComponent.textureIdentifier -> ResourceManager.getTexture(region.first.get()).toAtlasRegion()
                    else -> ResourceManager.getPackRegion(region.first.get(), region.second)
                }
            }
        }
    }

    override fun loadResources() {
        data.forEach {
            it.updateRegions()
        }
    }

    override fun render(batch: Batch) {
        batch.setColor(1f, 1f, 1f, alpha)
        data.elementAtOrNull(currentIndex)?.render(entity, flipX, flipY, rotation, batch)
        batch.setColor(1f, 1f, 1f, 1f)
    }

    private var selectedTextureIndex = -1
    private var showLevelTexture = false

    private enum class EditTextureType {
        Pack, Animation, Texture
    }

    private var editTextureType: ImGuiHelper.Item<Enum<*>> = ImGuiHelper.Item(EditTextureType.Pack)
    private var packFolderIndex = 0
    private var textureIndex = 0
    private var selectRegionIndex = 0
    private var addTextureName = "Nouvelle texture"
    private var ressourcesCollapsing = false

    override fun insertUI(label: String, entity: Entity, level: Level, editorSceneUI: EditorScene.EditorSceneUI) {
        with(ImGui) {
            if (button("Éditer", Vec2(Constants.defaultWidgetsWidth, 0))) {
                showEdiTextureWindow = true
            }

            functionalProgramming.withItemWidth(Constants.defaultWidgetsWidth) {
                combo("texture initiale", ::currentIndex, data.map { it.name })
            }

            if (showEdiTextureWindow)
                drawEditWindow(level)
        }
    }

    /**
     * Permet de dessiner la fenêtre permettant d'éditer les différentes régions de la texture actuelle.
     */
    private val imgBtnSize = Vec2(50, 50)
    private val editWindowWidth = 460f
    private fun drawEditWindow(level: Level) {
        with(ImGui) {
            setNextWindowSizeConstraints(Vec2(editWindowWidth, 200f), Vec2(editWindowWidth, 500f))
            functionalProgramming.withWindow("Éditer la texture", ::showEdiTextureWindow, flags = WindowFlags.AlwaysAutoResize.i) {
                val addTextureTitle = "Ajouter une texture"

                if (data.isEmpty()) {
                    if (button("Ajouter une texture", Vec2(-1))) {
                        openPopup(addTextureTitle)
                    }
                } else if (textureIndex in data.indices) {
                    var openAddTexturePopup = false

                    ImGuiHelper.comboWithSettingsButton("texture", ::textureIndex, data.map { it.name }, {
                        pushItemFlag(ItemFlags.Disabled.i, data.isEmpty())
                        if (button("Supprimer ${data.elementAtOrNull(textureIndex)?.name
                                        ?: ""}", Vec2(Constants.defaultWidgetsWidth, 0f))) {
                            data.removeAt(textureIndex)
                            textureIndex = let {
                                if (textureIndex > 0)
                                    textureIndex - 1
                                else
                                    textureIndex
                            }
                        }
                        popItemFlag()

                        if (button("Nouvelle texture", Vec2(Constants.defaultWidgetsWidth, 0f))) {
                            openAddTexturePopup = true
                        }
                    })

                    if (openAddTexturePopup)
                        openPopup(addTextureTitle)

                    separator()

                    data.elementAtOrNull(textureIndex)?.apply data@{
                        val regionBtnSize = Vec2(50f, 50f)

                        val itRegions = this.regions.listIterator()
                        var regionIndex = 0

                        fun addPlusBtn() {
                            if (button("+", Vec2(20f, regionBtnSize.y + style.framePadding.y * 2f)))
                                itRegions.add(TextureData.emptyRegionIdentifier.toLocalFile().toFileWrapper() to TextureData.emptyRegionIdentifier)
                        }

                        while (itRegions.hasNext()) {
                            val it = itRegions.next()

                            val region = TextureComponent.TextureData.loadRegion(it)
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
                                        updateRegions()
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

                    if (selectedTextureIndex == -1) {
                        val searchRegion = data.elementAtOrNull(textureIndex)?.regions?.elementAtOrNull(0)?.first?.get()
                        selectedTextureIndex = PCGame.gamePacks.entries.elementAtOrNull(packFolderIndex)?.value?.indexOfFirst { it == searchRegion } ?: -1
                        if (selectedTextureIndex == -1) {
                            selectedTextureIndex = level.resourcesPacks().indexOfFirst { it == searchRegion }
                            if (selectedTextureIndex == -1)
                                selectedTextureIndex = 0
                            else
                                showLevelTexture = true
                        }
                    }

                    drawRessources(level)
                }

                functionalProgramming.popupModal(addTextureTitle, extraFlags = WindowFlags.AlwaysAutoResize.i) {
                    ImGuiHelper.inputText("nom", ::addTextureName)
                    if (button("Ajouter", Vec2(-1, 0))) {
                        data.add(TextureData(addTextureName).apply { updateRegions() })
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
                ImGuiHelper.enumWithSettingsButton("type", editTextureType, {
                    checkbox("ressources importées", ::showLevelTexture)
                })

                var sumImgsWidth = 0f

                val editTextureType = editTextureType.obj as EditTextureType
                if (editTextureType != EditTextureType.Texture) {
                    functionalProgramming.withItemWidth(Constants.defaultWidgetsWidth) {
                        if (!showLevelTexture) {
                            combo("dossier", ::packFolderIndex, PCGame.gamePacks.map { it.key.name() })
                        }
                        combo("pack", ::selectedTextureIndex, if (showLevelTexture) level.resourcesPacks().map { it.nameWithoutExtension() } else PCGame.gamePacks.entries.elementAtOrNull(packFolderIndex)?.value?.map { it.nameWithoutExtension() }
                                ?: arrayListOf())
                    }
                    (if (showLevelTexture) level.resourcesPacks().getOrNull(selectedTextureIndex) else PCGame.gamePacks.entries.elementAtOrNull(packFolderIndex)?.value?.getOrNull(selectedTextureIndex))?.also { packPath ->
                        if (packPath.exists()) {
                            val pack = ResourceManager.getPack(packPath)
                            when (editTextureType) {
                                TextureComponent.EditTextureType.Pack -> {
                                    if (packPath.exists()) {
                                        pack.regions.sortedBy { it.name }.forEach { region ->
                                            if (imageButton(region.texture.textureObjectHandle, imgBtnSize, Vec2(region.u, region.v), Vec2(region.u2, region.v2))) {
                                                data.elementAtOrNull(textureIndex)?.apply {
                                                    if (selectRegionIndex in this.regions.indices) {
                                                        this.regions[selectRegionIndex] = packPath.toFileWrapper() to region.name
                                                        updateRegions()
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
                                TextureComponent.EditTextureType.Animation -> {
                                    findAnimationInPack(pack).forEach {
                                        val region = pack.findRegion(it + "_0")

                                        if (imageButton(region.texture.textureObjectHandle, imgBtnSize, Vec2(region.u, region.v), Vec2(region.u2, region.v2))) {
                                            data.elementAtOrNull(textureIndex)?.apply {
                                                this.regions.clear()
                                                this.regions.addAll(findAnimationRegions(packPath.toFileWrapper(), it))
                                                this.updateRegions()
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
                            data.elementAtOrNull(textureIndex)?.apply {
                                if (selectRegionIndex in this.regions.indices) {
                                    this.regions[selectRegionIndex] = it.toFileWrapper() to textureIdentifier
                                    updateRegions()
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
        private var showEdiTextureWindow = false
        private val textureIdentifier = "\$texture_atlas"

        /**
         * Permet de charger les différentes régions requise pour une animation prédéfinies dans un pack
         */
        private fun findAnimationRegions(packFile: FileWrapper, animation: String): Array<Pair<FileWrapper, String>> {
            val pack = ResourceManager.getPack(packFile.get())
            val regions = mutableListOf<String>()

            var i = 0
            while (pack.findRegion(animation + "_$i") != null) {
                regions.add(animation + "_$i")
                ++i
            }

            return regions.map { packFile to it }.toTypedArray()
        }

        /**
         * Permet de trouver les différentes animations prédéfinies dans un pack
         */
        private fun findAnimationInPack(pack: TextureAtlas): List<String> {
            val animationRegionNames = mutableListOf<String>()

            pack.regions.forEach {
                if (it.name.endsWith("_0")) {
                    animationRegionNames += it.name.removeSuffix("_0")
                }
            }

            return animationRegionNames
        }
    }

    override fun insertText() {
        ImGuiHelper.textPropertyColored(Color.ORANGE, "texture actuelle :", data.elementAtOrNull(currentIndex)?.name
                ?: "/")
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