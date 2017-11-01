package be.catvert.pc.components.graphics

import be.catvert.pc.GameObject
import be.catvert.pc.PCGame
import be.catvert.pc.components.RenderableComponent
import be.catvert.pc.scenes.EditorScene
import be.catvert.pc.utility.Constants
import be.catvert.pc.utility.CustomEditorImpl
import be.catvert.pc.utility.Rect
import be.catvert.pc.utility.draw
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnore
import com.kotcrab.vis.ui.widget.VisImageButton
import com.kotcrab.vis.ui.widget.VisTable
import ktx.actors.onClick
import ktx.assets.getValue
import ktx.assets.loadOnDemand
import ktx.assets.toLocalFile
import ktx.collections.toGdxArray

class AnimationComponent(atlasPath: FileHandle, animationRegionName: String, var frameDuration: Float) : RenderableComponent() {
    @JsonCreator private constructor(): this(Constants.noTextureAtlasFoundPath.toLocalFile(), "", 0f)

    var atlasPath: String = atlasPath.path()
        private set
    var animationRegionName: String = animationRegionName
        private set

    private var atlas = PCGame.assetManager.loadOnDemand<TextureAtlas>(this.atlasPath).asset

    private var animation: Animation<TextureAtlas.AtlasRegion>? = loadAnimation(atlas, animationRegionName, frameDuration)

    private var stateTime = 0f

    fun updateAnimation(atlasPath: FileHandle = this.atlasPath.toLocalFile(), animationRegionName: String = this.animationRegionName, frameDuration: Float = this.frameDuration) {
        this.atlasPath = atlasPath.path()
        this.animationRegionName = animationRegionName
        this.frameDuration = frameDuration

        atlas = PCGame.assetManager.loadOnDemand<TextureAtlas>(atlasPath.path()).asset
        animation = loadAnimation(atlas, animationRegionName, frameDuration)
    }

    override fun onGOAddToContainer(gameObject: GameObject) {
        super.onGOAddToContainer(gameObject)

        updateAnimation()
    }

    override fun render(batch: Batch) {
        if(animation != null) {
            stateTime += Gdx.graphics.deltaTime
            if(animation != null)
                batch.draw(animation!!.getKeyFrame(stateTime), gameObject.rectangle, flipX, flipY)
        }
    }

    companion object : CustomEditorImpl<AnimationComponent> {
        override fun createInstance(table: VisTable, editorScene: EditorScene, onCreate: (newInstance: AnimationComponent) -> Unit) {
            val texture = PCGame.assetManager.loadOnDemand<Texture>(Constants.noTextureFoundTexturePath).asset
            val imageButton = VisImageButton(TextureRegionDrawable(TextureAtlas.AtlasRegion(texture, 0, 0, texture.width, texture.height)))
            imageButton.onClick {
                editorScene.showSelectAnimationWindow(null) { atlasFile, animationRegion, frameDuration ->
                    val imgBtnDrawable = TextureRegionDrawable(PCGame.assetManager.loadOnDemand<TextureAtlas>(atlasFile.path()).asset.findRegion(animationRegion + "_0"))

                    imageButton.style.imageUp = imgBtnDrawable
                    imageButton.style.imageDown = imgBtnDrawable

                    onCreate(AnimationComponent(atlasFile, animationRegion, frameDuration))
                }
            }

            table.add(imageButton)

            table.row()
        }

        override fun insertChangeProperties(table: VisTable, editorScene: EditorScene, instance: AnimationComponent) {
            val imageButton = VisImageButton(TextureRegionDrawable(instance.atlas.findRegion(instance.animationRegionName + "_0")))
            imageButton.onClick {
                editorScene.showSelectAnimationWindow(instance.atlasPath.toLocalFile()) { atlasFile, animationRegion, frameDuration ->
                    instance.updateAnimation(atlasFile, animationRegion, frameDuration)

                    val imgBtnDrawable = TextureRegionDrawable(instance.atlas.findRegion(instance.animationRegionName + "_0"))

                    imageButton.style.imageUp = imgBtnDrawable
                    imageButton.style.imageDown = imgBtnDrawable
                }
            }

            table.add(imageButton)
        }

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

                    if(animationRegionName == name) {
                        var count = 1

                        while(atlas.findRegion(name + "_" + count) != null)
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
