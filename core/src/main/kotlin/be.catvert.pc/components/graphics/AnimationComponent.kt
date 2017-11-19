package be.catvert.pc.components.graphics

import be.catvert.pc.GameObject
import be.catvert.pc.GameObjectState
import be.catvert.pc.PCGame
import be.catvert.pc.components.RenderableComponent
import be.catvert.pc.scenes.EditorScene
import be.catvert.pc.utility.Constants
import be.catvert.pc.utility.CustomEditorImpl
import be.catvert.pc.utility.Size
import be.catvert.pc.utility.draw
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.g2d.Animation
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
import ktx.collections.toGdxArray

/**
 * Component permettant d'ajouter une animation à un gameObject
 */
class AnimationComponent(atlasPath: FileHandle, animationRegionName: String, frameDuration: Float) : RenderableComponent(), CustomEditorImpl {
    @JsonCreator private constructor() : this(Constants.noTextureAtlasFoundPath.toLocalFile(), "", 0f)

    var atlasPath: String = atlasPath.path()
        private set
    var animationRegionName: String = animationRegionName
        private set

    var frameDuration: Float = frameDuration
        set(value) {
            field = value
            animation.frameDuration = value
        }

    @JsonIgnore private var atlas = PCGame.assetManager.loadOnDemand<TextureAtlas>(this.atlasPath).asset

    @JsonIgnore private var animation: Animation<TextureAtlas.AtlasRegion> = loadAnimation(atlas, animationRegionName, frameDuration)

    @JsonIgnore private var stateTime = 0f

    fun updateAnimation(atlasPath: FileHandle = this.atlasPath.toLocalFile(), animationRegionName: String = this.animationRegionName, frameDuration: Float = this.frameDuration) {
        this.atlasPath = atlasPath.path()
        this.animationRegionName = animationRegionName
        this.frameDuration = frameDuration

        atlas = PCGame.assetManager.loadOnDemand<TextureAtlas>(atlasPath.path()).asset
        animation = loadAnimation(atlas, animationRegionName, frameDuration)
    }

    override fun onGOAddToContainer(state: GameObjectState, gameObject: GameObject) {
        super.onGOAddToContainer(state, gameObject)

        updateAnimation()
    }

    override fun render(gameObject: GameObject, batch: Batch) {
        stateTime += Gdx.graphics.deltaTime
        batch.draw(animation.getKeyFrame(stateTime), gameObject.rectangle, flipX, flipY)
    }

    companion object {
        fun findAnimationRegionsNameInAtlas(atlas: TextureAtlas): List<String> {
            val animationRegionNames = mutableListOf<String>()

            atlas.regions.forEach {
                if (it.name.endsWith("_0")) {
                    animationRegionNames += it.name.removeSuffix("_0")
                }
            }

            return animationRegionNames
        }

        fun loadAnimation(atlas: TextureAtlas, animationRegionName: String, frameDuration: Float): Animation<TextureAtlas.AtlasRegion> {
            atlas.regions.forEach {
                /* les animations de Kenney finissent par une lettre puis par exemple 1 donc -> alienGreen_walk1 puis alienGreen_walk2
                mais des autres textures normale tel que foliagePack_001 existe donc on doit vérifier si le nombre avant 1 fini bien par une lettre
                */
                if (it.name.endsWith("_0")) {
                    val name = it.name.removeSuffix("_0")

                    if (animationRegionName == name) {
                        var count = 1

                        while (atlas.findRegion(name + "_" + count) != null)
                            ++count

                        val frameList = mutableListOf<TextureAtlas.AtlasRegion>()

                        val initialRegion = atlas.findRegion(name + "_0")

                        for (i in 0 until count) {
                            val nameNextFrame = name + "_" + i
                            val region = atlas.findRegion(nameNextFrame)
                            region.regionWidth = initialRegion.regionWidth
                            region.regionHeight = initialRegion.regionHeight
                            frameList.add(region)
                        }

                        return Animation(frameDuration, frameList.toGdxArray(), Animation.PlayMode.LOOP)
                    }
                }
            }

            return Animation(0f, PCGame.assetManager.loadOnDemand<TextureAtlas>(Constants.noTextureAtlasFoundPath).asset.findRegion("notexture"))
        }
    }

    @JsonIgnore private val selectAnimationTitle = "Sélection de l'animation"
    @JsonIgnore private var selectedAtlasIndex = -1
    @JsonIgnore private var useAtlasSize = booleanArrayOf(false)

    override fun insertImgui(gameObject: GameObject, editorScene: EditorScene) {

        with(ImGui) {
            val region = animation.getKeyFrame(0f)
            if (imageButton(region.texture.textureObjectHandle, Vec2(gameObject.rectangle.width, gameObject.rectangle.height), Vec2(region.u, region.v), Vec2(region.u2, region.v2))) {
                openPopup(selectAnimationTitle)
            }

            sliderFloat("Vitesse de l'animation", this@AnimationComponent::frameDuration, 0f, 1f)
        }
    }

    override fun insertImguiPopup(gameObject: GameObject, editorScene: EditorScene) {
        super.insertImguiPopup(gameObject, editorScene)

        with(ImGui) {
            if (beginPopupModal(selectAnimationTitle, extraFlags = WindowFlags.AlwaysHorizontalScrollbar.i or WindowFlags.AlwaysVerticalScrollbar.i)) {
                if (selectedAtlasIndex == -1) {
                    selectedAtlasIndex = PCGame.loadedAtlas.indexOfFirst { it == atlasPath.toLocalFile() }
                    if (selectedAtlasIndex == -1)
                        selectedAtlasIndex = 0
                }
                combo("atlas", this@AnimationComponent::selectedAtlasIndex, PCGame.loadedAtlas.map { it.nameWithoutExtension() })
                checkbox("Mettre à jour la taille du gameObject", useAtlasSize)

                separator()

                var count = 0

                val atlas = PCGame.assetManager.loadOnDemand<TextureAtlas>(PCGame.loadedAtlas[selectedAtlasIndex].path()).asset

                findAnimationRegionsNameInAtlas(atlas).forEach { it ->
                    val region = atlas.findRegion(it + "_0")
                    if (imageButton(region.texture.textureObjectHandle, Vec2(region.regionWidth, region.regionHeight), Vec2(region.u, region.v), Vec2(region.u2, region.v2))) {
                        updateAnimation(PCGame.loadedAtlas[selectedAtlasIndex], it)
                        if (useAtlasSize[0])
                            gameObject.rectangle.size = Size(region.regionWidth, region.regionHeight)
                        closeCurrentPopup()
                    }
                    if (++count <= 8)
                        sameLine()
                    else
                        count = 0
                }

                endPopup()
            }
        }
    }
}
