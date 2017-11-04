package be.catvert.pc.components.graphics

import be.catvert.pc.GameObject
import be.catvert.pc.GameObjectState
import be.catvert.pc.PCGame
import be.catvert.pc.components.RenderableComponent
import be.catvert.pc.scenes.EditorScene
import be.catvert.pc.utility.Constants
import be.catvert.pc.utility.CustomEditorImpl
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
import ktx.actors.onClick
import ktx.assets.loadOnDemand
import ktx.assets.toLocalFile
import ktx.collections.toGdxArray

class AnimationComponent(atlasPath: FileHandle, animationRegionName: String, var frameDuration: Float) : RenderableComponent(), CustomEditorImpl {
    @JsonCreator private constructor() : this(Constants.noTextureAtlasFoundPath.toLocalFile(), "", 0f)

    var atlasPath: String = atlasPath.path()
        private set
    var animationRegionName: String = animationRegionName
        private set

    @JsonIgnore private var atlas = PCGame.assetManager.loadOnDemand<TextureAtlas>(this.atlasPath).asset

    @JsonIgnore private var animation: Animation<TextureAtlas.AtlasRegion>? = loadAnimation(atlas, animationRegionName, frameDuration)

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
        if (animation != null) {
            stateTime += Gdx.graphics.deltaTime
            if (animation != null)
                batch.draw(animation!!.getKeyFrame(stateTime), gameObject.rectangle, flipX, flipY)
        }
    }

    override fun insertChangeProperties(table: VisTable, editorScene: EditorScene) {
        table.add(VisLabel("Animation : "))

        table.add(VisImageButton(TextureRegionDrawable(animation?.getKeyFrame(0f) ?: PCGame.assetManager.loadOnDemand<TextureAtlas>(Constants.noTextureAtlasFoundPath).asset.findRegion("notexture"))).apply {
            onClick {
                editorScene.showSelectAnimationWindow(atlasPath.toLocalFile()) { atlasFile, animationRegion, frameDuration ->
                    updateAnimation(atlasFile, animationRegion, frameDuration)

                    val imgBtnDrawable = TextureRegionDrawable(atlas.findRegion(animationRegionName + "_0"))

                    this.style.imageUp = imgBtnDrawable
                    this.style.imageDown = imgBtnDrawable
                }
            }
        })
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

        fun loadAnimation(atlas: TextureAtlas, animationRegionName: String, frameDuration: Float): Animation<TextureAtlas.AtlasRegion>? {
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
            return null
        }
    }
}
