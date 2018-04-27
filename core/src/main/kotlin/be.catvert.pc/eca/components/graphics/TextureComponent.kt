package be.catvert.pc.eca.components.graphics

import be.catvert.pc.PCGame
import be.catvert.pc.eca.Entity
import be.catvert.pc.eca.components.Component
import be.catvert.pc.eca.components.graphics.PackRegionData.Companion.findAnimationRegions
import be.catvert.pc.eca.containers.Level
import be.catvert.pc.managers.ResourcesManager
import be.catvert.pc.managers.ScenesManager
import be.catvert.pc.scenes.EditorScene
import be.catvert.pc.ui.*
import be.catvert.pc.utility.*
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.MathUtils
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonTypeInfo
import glm_.vec2.Vec2
import glm_.vec4.Vec4
import imgui.*

/**
 * Component permettant d'ajouter des textures et animations a l'entité
 * @param currentIndex La texture actuelle à dessiner
 * @param groups Les groupes disponibles pour l'entité
 */
@Description("Permet d'ajouter une texture ou une animation à une entité")
class TextureComponent(var currentIndex: Int = 0, vararg groups: TextureGroup) : Component(), Renderable, UIImpl, UITextImpl {
    @JsonCreator private constructor() : this(0)

    enum class Rotation(val degree: Float) {
        Zero(0f), Quarter(90f), Half(180f), ThreeQuarter(270f)
    }

    var groups = arrayListOf(*groups)

    @UI
    var rotation: Rotation = Rotation.Zero

    @UI(customName = "miroir x")
    var flipX: Boolean = false
    @UI(customName = "miroir y")
    var flipY: Boolean = false

    @JsonIgnore
    var alpha: Float = 1f

    override fun render(batch: Batch) {
        batch.setColor(1f, 1f, 1f, alpha)
        groups.elementAtOrNull(currentIndex)?.render(entity, flipX, flipY, rotation, batch)
        batch.setColor(1f, 1f, 1f, 1f)
    }

    private var selectedPackIndex = -1
    private var showLevelTexture = false

    private enum class EditTextureType {
        Pack, Animation, Texture
    }

    private var editTextureType: ImGuiHelper.Item<Enum<*>> = ImGuiHelper.Item(EditTextureType.Pack)
    private var packFolderIndex = 0
    private var groupIndex = 0
    private var selectRegionIndex = 0
    private var addGroupBuf = "Nouveau groupe"
    private var ressourcesCollapsing = false

    private val addGroupTitle = "Ajouter un groupe"

    override fun insertUI(label: String, entity: Entity, level: Level, editorUI: EditorScene.EditorUI) {
        with(ImGui) {
            if (button("Éditer", Vec2(Constants.defaultWidgetsWidth, 0))) {
                showEditTextureWindow = true
            }

            functionalProgramming.withItemWidth(Constants.defaultWidgetsWidth) {
                combo("groupe initial", ::currentIndex, groups.map { it.name })
            }

            if (showEditTextureWindow)
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
            setNextWindowSizeConstraints(Vec2(editWindowWidth, 170f), Vec2(editWindowWidth, 500f))
            functionalProgramming.withWindow("Éditer la texture", ::showEditTextureWindow, flags = WindowFlag.AlwaysAutoResize.i) {
                if (groups.isEmpty()) {
                    if (button("Ajouter un groupe", Vec2(-1))) {
                        openPopup(addGroupTitle)
                    }
                } else if (groupIndex in groups.indices) {
                    var openAddGroupPopup = false

                    ImGuiHelper.comboWithSettingsButton("groupe", ::groupIndex, groups.map { it.name }, {
                        pushItemFlag(ItemFlag.Disabled.i, groups.isEmpty())
                        if (button("Supprimer ${groups.elementAtOrNull(groupIndex)?.name
                                        ?: ""}", Vec2(Constants.defaultWidgetsWidth, 0f))) {
                            groups.removeAt(groupIndex)
                            groupIndex = let {
                                if (groupIndex > 0)
                                    groupIndex - 1
                                else
                                    groupIndex
                            }
                        }
                        popItemFlag()

                        if (button("Nouveau groupe", Vec2(Constants.defaultWidgetsWidth, 0f))) {
                            openAddGroupPopup = true
                        }
                    })

                    if (openAddGroupPopup)
                        openPopup(addGroupTitle)

                    separator()

                    groups.elementAtOrNull(groupIndex)?.apply data@{
                        val regionBtnSize = Vec2(50f, 50f)

                        val itRegions = this.regions.listIterator()
                        var regionIndex = 0

                        fun addPlusBtn() {
                            if (button("+", Vec2(20f, regionBtnSize.y + style.framePadding.y * 2f)))
                                itRegions.add(EmptyData())
                        }

                        while (itRegions.hasNext()) {
                            val it = itRegions.next()

                            val region = it.getTextureRegion()

                            val tintCol = if (selectRegionIndex != regionIndex) Vec4(1f, 1f, 1f, 0.3f) else Vec4(1f)

                            functionalProgramming.withGroup {
                                functionalProgramming.withGroup {
                                    val btnSize = Vec2((regionBtnSize.x + style.itemInnerSpacing.x) / 2f)

                                    pushItemFlag(ItemFlag.Disabled.i, it !is PackRegionData)
                                    functionalProgramming.withId("previous frame $regionIndex") {
                                        if (button("<-", btnSize)) {
                                            it.cast<PackRegionData>()?.previousFrameRegion()
                                        }
                                    }
                                    sameLine(0f, style.itemInnerSpacing.x)
                                    functionalProgramming.withId("next frame $regionIndex") {
                                        if (button("->", btnSize)) {
                                            it.cast<PackRegionData>()?.nextFrameRegion()
                                        }
                                    }
                                    popItemFlag()
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
                                sliderFloat("vitesse", ::frameDuration, 0f, 2f)
                            }
                            val playModeItem = ImGuiHelper.Item(playMode)
                            ImGuiHelper.enum("mode", playModeItem.cast())
                            playMode = playModeItem.obj
                        } else if (this.regions.size == 1) {
                            checkbox("répéter la région", ::repeatRegion)
                            if (repeatRegion) {
                                ImGuiHelper.size(::repeatRegionSize, Size(1), Size(Constants.maxEntitySize))
                            }
                        }

                        separator()
                    }

                    if (selectedPackIndex == -1) {
                        groups.elementAtOrNull(groupIndex)?.regions?.elementAtOrNull(0)?.apply {
                            when (this) {
                                is PackRegionData -> {
                                    selectedPackIndex = PCGame.gamePacks.entries.elementAtOrNull(packFolderIndex)?.value?.indexOfFirst { it.path == pack.path } ?: -1
                                    if (selectedPackIndex == -1) {
                                        selectedPackIndex = level.resourcesPacks().indexOfFirst { it.path == pack.path }
                                        if (selectedPackIndex == -1)
                                            selectedPackIndex = 0
                                        else
                                            showLevelTexture = true
                                    }
                                }
                                is TextureData -> {
                                    // TODO
                                }
                            }
                        }
                    }

                    drawResources(level)
                }

                functionalProgramming.popupModal(addGroupTitle, extraFlags = WindowFlag.AlwaysAutoResize.i) {
                    ImGuiHelper.inputText("nom", ::addGroupBuf)
                    if (button("Ajouter", Vec2(-1, 0))) {
                        groups.add(TextureGroup(addGroupBuf))
                        closeCurrentPopup()
                    }
                    if (button("Fermer", Vec2(-1, 0)))
                        closeCurrentPopup()
                }
            }
        }
    }

    private fun drawResources(level: Level) {
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
                        combo("pack", ::selectedPackIndex, if (showLevelTexture) level.resourcesPacks().map { it.toString() } else PCGame.gamePacks.entries.elementAtOrNull(packFolderIndex)?.value?.map { it.toString() }
                                ?: arrayListOf())
                    }
                    (if (showLevelTexture) level.resourcesPacks().getOrNull(selectedPackIndex) else PCGame.gamePacks.entries.elementAtOrNull(packFolderIndex)?.value?.getOrNull(selectedPackIndex))?.also { pack ->
                        when (editTextureType) {
                            TextureComponent.EditTextureType.Pack -> {
                                pack()?.regions?.sortedBy { it.name }?.forEach { region ->
                                    if (imageButton(region.texture.textureObjectHandle, imgBtnSize, Vec2(region.u, region.v), Vec2(region.u2, region.v2))) {
                                        groups.elementAtOrNull(groupIndex)?.apply {
                                            if (selectRegionIndex in this.regions.indices) {
                                                this.regions[selectRegionIndex] = PackRegionData(pack, region.name)
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
                            TextureComponent.EditTextureType.Animation -> {
                                pack()?.apply {
                                    findAnimationInPack(this).forEach {
                                        val region = this.findRegion(it + "_0")

                                        if (imageButton(region.texture.textureObjectHandle, imgBtnSize, Vec2(region.u, region.v), Vec2(region.u2, region.v2))) {
                                            groups.elementAtOrNull(groupIndex)?.apply {
                                                this.regions.clear()
                                                this.regions.addAll(PackRegionData.findAnimationRegions(pack.path, it))
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
                            else -> {
                            }
                        }
                    }
                } else {
                    (PCGame.gameTextures + level.resourcesTextures()).forEach { wrapper ->
                        wrapper()?.apply {
                            if (imageButton(this.textureObjectHandle, imgBtnSize, uv1 = Vec2(1))) {
                                groups.elementAtOrNull(groupIndex)?.apply {
                                    if (selectRegionIndex in this.regions.indices) {
                                        this.regions[selectRegionIndex] = TextureData(wrapper)
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
    }

    override fun insertText() {
        ImGuiHelper.textPropertyColored(Color.ORANGE, "Groupe actuel :", groups.elementAtOrNull(currentIndex)?.name
                ?: "/")
        ImGuiHelper.textPropertyColored(Color.OLIVE, "alpha :", alpha)
        groups.forEach {
            ImGuiHelper.textColored(Color.RED, "<-->")
            ImGuiHelper.textPropertyColored(Color.ORANGE, "nom :", it.name)
            if (it.regions.size > 1)
                ImGuiHelper.textPropertyColored(Color.ORANGE, "anim mode :", it.playMode.toString())
            it.regions.forEach {
                ImGuiHelper.textPropertyColored(Color.ORANGE, " - ", it)
            }
            ImGuiHelper.textColored(Color.RED, "<-->")
        }
    }

    companion object {
        private var showEditTextureWindow = false

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
}

class TextureGroup(var name: String, vararg regions: DrawableData, var playMode: Animation.PlayMode = Animation.PlayMode.LOOP, var frameDuration: Float = 1f / regions.size) {
    constructor(name: String, packFile: FileWrapper, animation: String, frameDuration: Float, animationPlayMode: Animation.PlayMode = Animation.PlayMode.LOOP) : this(name, *findAnimationRegions(packFile, animation).toTypedArray(), playMode = animationPlayMode, frameDuration = frameDuration)
    @JsonCreator private constructor() : this("default")

    var regions = arrayListOf(*regions)

    var repeatRegion: Boolean = false
    var repeatRegionSize = Size(50, 50)

    /**
     * Représente le temps écoulé depuis le début de l'affichage de la texture/animation, si le stateTime = 1.3f et que le frameDuration = 1f, la deuxième région sera dessiné.
     */
    private var stateTime = 0f
    private var lastStateTime = 0f
    private var lastFrameNumber = 0

    fun render(entity: Entity, flipX: Boolean, flipY: Boolean, rotation: TextureComponent.Rotation, batch: Batch) {
        /**
         * Si le nombre de région est <= à 1, il n'y a pas besoin de mettre à jour le temps écoulé car de toute façon une seule région sera dessinée.
         */
        if (regions.size > 1)
            stateTime += Utility.getDeltaTime()

        val frame = currentFrame()

        if (repeatRegion && regions.size == 1) {
            for (x in 0 until MathUtils.floor(entity.box.width / repeatRegionSize.width.toFloat())) {
                for (y in 0 until MathUtils.floor(entity.box.height / repeatRegionSize.height.toFloat())) {
                    frame.render(Rect(entity.box.x + x * repeatRegionSize.width, entity.box.y + y * repeatRegionSize.height, repeatRegionSize.width, repeatRegionSize.height), flipX, flipY, rotation, batch)
                }
            }
        } else
            frame.render(entity.box, flipX, flipY, rotation, batch)
    }

    /**
     * Inspiré du code source de libGDX
     * @see Animation
     */
    fun currentFrame(): DrawableData {
        if (regions.isEmpty())
            return EmptyData()
        else if (regions.size == 1)
            return regions[0]

        var frameNumber = (stateTime / frameDuration).toInt()
        when (playMode) {
            Animation.PlayMode.NORMAL -> frameNumber = Math.min(regions.size - 1, frameNumber)
            Animation.PlayMode.LOOP -> frameNumber %= regions.size
            Animation.PlayMode.LOOP_PINGPONG -> {
                frameNumber %= (regions.size * 2 - 2)
                if (frameNumber >= regions.size) frameNumber = regions.size - 2 - (frameNumber - regions.size)
            }
            Animation.PlayMode.LOOP_RANDOM -> {
                val lastFrameNumber = (lastStateTime / frameDuration).toInt()
                if (lastFrameNumber != frameNumber) {
                    frameNumber = MathUtils.random(regions.size - 1)
                } else {
                    frameNumber = this.lastFrameNumber
                }
            }
            Animation.PlayMode.REVERSED -> frameNumber = Math.max(regions.size - frameNumber - 1, 0)
            Animation.PlayMode.LOOP_REVERSED -> {
                frameNumber %= regions.size
                frameNumber = regions.size - frameNumber - 1
            }
        }

        lastFrameNumber = frameNumber
        lastStateTime = stateTime

        return regions[frameNumber]
    }
}

@JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS, include = JsonTypeInfo.As.WRAPPER_ARRAY)
abstract class DrawableData {
    fun render(rect: Rect, flipX: Boolean, flipY: Boolean, rotation: TextureComponent.Rotation, batch: Batch) {
        batch.draw(getTextureRegion(), rect, flipX, flipY, rotation.degree)
    }

    @JsonIgnore
    abstract fun getTextureRegion(): TextureRegion
}

class EmptyData : DrawableData() {
    override fun getTextureRegion() = ResourcesManager.defaultPackRegion

    override fun toString() = "vide"
}

class TextureData(val texture: ResourceWrapper<Texture>) : DrawableData() {
    override fun getTextureRegion(): TextureRegion {
        val texture = texture()
        if (texture != null)
            return TextureRegion(texture)
        else
            return ResourcesManager.defaultPackRegion
    }

    override fun toString() = texture.toString()
}

/**
 * Représente une région
 */
class PackRegionData(val pack: ResourceWrapper<TextureAtlas>, var region: String) : DrawableData() {
    override fun toString() = "${pack.path} -> $region"

    override fun getTextureRegion(): TextureRegion = pack()?.findRegion(region) ?: ResourcesManager.defaultPackRegion

    fun previousFrameRegion() {
        pack()?.apply {
            val regions = regions.sortedBy { it.name }

            val index = regions.indexOfFirst { it.name == region }

            if (index > 0) {
                region = regions[index - 1].name
            }
        }
    }

    fun nextFrameRegion() {
        pack()?.apply {
            val regions = regions.sortedBy { it.name }

            val index = regions.indexOfFirst { it.name == region }

            if (index < regions.size - 1) {
                region = regions[index + 1].name
            }
        }
    }

    companion object {
        /**
         * Permet de charger les différentes régions requise pour une animation prédéfinies dans un pack
         */
        fun findAnimationRegions(packFile: FileWrapper, animation: String): List<PackRegionData> {
            val pack = ResourcesManager.getPack(packFile.get())
            val regions = mutableListOf<String>()

            var i = 0
            while (pack.findRegion(animation + "_$i") != null) {
                regions.add(animation + "_$i")
                ++i
            }

            return regions.map { PackRegionData(resourceWrapperOf(packFile), it) }
        }
    }
}