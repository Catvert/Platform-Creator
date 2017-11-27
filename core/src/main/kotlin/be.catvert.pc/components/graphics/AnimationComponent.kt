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
import com.fasterxml.jackson.annotation.JsonIgnore
import glm_.vec2.Vec2
import imgui.ImGui
import imgui.functionalProgramming
import ktx.assets.loadOnDemand
import ktx.assets.toLocalFile
import ktx.collections.toGdxArray

/**
 * Component permettant d'ajouter une animation à un gameObject
 */
class AnimationComponent(var currentAnimation: Int, var animations: Array<AnimationData>) : RenderableComponent(), CustomEditorImpl {
    @JsonCreator private constructor() : this(0, arrayOf())

    class AnimationData(atlasFile: FileHandle, var animationRegionName: String, frameDuration: Float): CustomEditorImpl {
        @JsonCreator private constructor() : this(Constants.defaultAtlasPath, "", 0.33f)

        var atlasPath = atlasFile.path()

        var frameDuration: Float = frameDuration
            set(value) {
                field = value
                animation.frameDuration = value
            }

        private var atlas = PCGame.assetManager.loadOnDemand<TextureAtlas>(this.atlasPath).asset

        private var animation: Animation<TextureAtlas.AtlasRegion> = loadAnimation(atlas, animationRegionName, frameDuration)

        private var stateTime = 0f

        @JsonIgnore
        fun getAnimationFrame(): TextureAtlas.AtlasRegion {
            stateTime += Gdx.graphics.deltaTime
            return animation.getKeyFrame(stateTime, true)
        }

        fun updateAnimation(atlasPath: FileHandle = this.atlasPath.toLocalFile(), animationRegionName: String = this.animationRegionName, frameDuration: Float = this.frameDuration) {
            this.atlasPath = atlasPath.path()
            this.animationRegionName = animationRegionName
            this.frameDuration = frameDuration

            atlas = PCGame.assetManager.loadOnDemand<TextureAtlas>(atlasPath).asset
            animation = loadAnimation(atlas, animationRegionName, frameDuration)
        }

        private val selectAnimationTitle = "Sélection de l'animation"
        private var selectedAtlasIndex = -1
        private var useAtlasSize = false
        private var showLevelAnimations = false
        override fun insertImgui(labelName: String, gameObject: GameObject, level: Level) {

            with(ImGui) {
                val region = animation.getKeyFrame(0f)
                if (imageButton(region.texture.textureObjectHandle, Vec2(gameObject.box.width, gameObject.box.height), Vec2(region.u, region.v), Vec2(region.u2, region.v2))) {
                    openPopup(selectAnimationTitle)
                }

                functionalProgramming.withItemWidth(100f) {
                    sliderFloat("Vitesse", this@AnimationData::frameDuration, 0f, 1f)
                }
            }
        }

        override fun insertImguiPopup(gameObject: GameObject, level: Level) {
            super.insertImguiPopup(gameObject, level)

            with(ImGui) {
                val popupWidth = Gdx.graphics.width / 3 * 2
                val popupHeight = Gdx.graphics.height / 3 * 2
                setNextWindowSize(Vec2(popupWidth, popupHeight))
                setNextWindowPos(Vec2(Gdx.graphics.width / 2f - popupWidth / 2f, Gdx.graphics.height / 2f - popupHeight / 2f))
                if (beginPopup(selectAnimationTitle)) {
                    if (selectedAtlasIndex == -1) {
                        selectedAtlasIndex = PCGame.gameAtlas.indexOfFirst { it == atlasPath.toLocalFile() }
                        if (selectedAtlasIndex == -1) {
                            selectedAtlasIndex = level.resourcesAtlas().indexOfFirst { it == atlasPath.toLocalFile() }
                            if (selectedAtlasIndex == -1)
                                selectedAtlasIndex = 0
                            else
                                showLevelAnimations = true
                        }
                    }
                    checkbox("Utiliser les animations importées (atlas)", this@AnimationData::showLevelAnimations)
                    sameLine()
                    combo("atlas", this@AnimationData::selectedAtlasIndex, if (showLevelAnimations) level.resourcesAtlas().map { it.nameWithoutExtension() } else PCGame.gameAtlas.map { it.nameWithoutExtension() })
                    checkbox("Mettre à jour la taille du gameObject", this@AnimationData::useAtlasSize)

                    var sumImgsWidth = 0f

                    val atlasPath = if (showLevelAnimations) level.resourcesAtlas().getOrNull(selectedAtlasIndex)?.path() else PCGame.gameAtlas.getOrNull(selectedAtlasIndex)?.path()
                    if (atlasPath != null) {
                        val atlas = PCGame.assetManager.loadOnDemand<TextureAtlas>(atlasPath).asset

                        findAnimationRegionsNameInAtlas(atlas).forEach { it ->
                            val region = atlas.findRegion(it + "_0")
                            val imgBtnSize = Vec2(Math.min(region.regionWidth, 200), Math.min(region.regionHeight, 200))
                            if (imageButton(region.texture.textureObjectHandle, imgBtnSize, Vec2(region.u, region.v), Vec2(region.u2, region.v2))) {
                                updateAnimation(atlasPath.toLocalFile(), it)
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

                    endPopup()
                }
            }
        }
    }


    override fun onAddToContainer(gameObject: GameObject, container: GameObjectContainer) {
        super.onAddToContainer(gameObject, container)

        animations.forEach {
            it.updateAnimation()
        }
    }

    override fun render(gameObject: GameObject, batch: Batch) {
        if(currentAnimation in animations.indices) {
            val animation = animations[currentAnimation]
            batch.setColor(1f, 1f, 1f, alpha)
            batch.draw(animation.getAnimationFrame(), gameObject.box, flipX, flipY)
            batch.setColor(1f, 1f, 1f, 1f)
        }
    }

    override fun insertImgui(labelName: String, gameObject: GameObject, level: Level) {
        with(ImGui) {
            functionalProgramming.withItemWidth(100f) {
                sliderInt("animation", this@AnimationComponent::currentAnimation, 0, animations.size - 1)
            }
            ImguiHelper.addImguiWidgetsArray("animations", this@AnimationComponent::animations, { AnimationData(Constants.defaultAtlasPath, "", 0.33f) }, {
                if(treeNode(it.obj.animationRegionName)) {
                    it.obj.insertImgui(it.obj.animationRegionName, gameObject, level)
                    it.obj.insertImguiPopup(gameObject, level)
                    treePop()
                }
            }, {})
        }
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

                        for (i in 0 until count) {
                            val nameNextFrame = name + "_" + i
                            val region = atlas.findRegion(nameNextFrame)
                            frameList.add(region)
                        }

                        return Animation(frameDuration, frameList.toGdxArray(), Animation.PlayMode.LOOP)
                    }
                }
            }

            return Animation(0f, PCGame.assetManager.loadOnDemand<TextureAtlas>(Constants.defaultAtlasPath).asset.findRegion("notexture"))
        }
    }

}
