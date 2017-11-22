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
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Batch
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnore
import glm_.vec2.Vec2
import imgui.ImGui
import ktx.assets.loadOnDemand
import ktx.assets.toLocalFile

/**
 * Component permettant d'afficher une texture chargée depuis un fichier
 * @param texturePath Le chemin vers la texture en question
 */
class TextureComponent(texturePath: FileHandle) : RenderableComponent(), CustomEditorImpl {
    @JsonCreator constructor() : this(Constants.noTextureFoundTexturePath.toLocalFile())

    var texturePath: String = texturePath.path()
        private set

    @JsonIgnore private var texture = PCGame.assetManager.loadOnDemand<Texture>(this.texturePath).asset

    fun updateTexture(texturePath: FileHandle = this.texturePath.toLocalFile()) {
        this.texturePath = texturePath.path()

        texture = PCGame.assetManager.loadOnDemand<Texture>(this.texturePath).asset
    }

    override fun onGOAddToContainer(state: GameObjectState, gameObject: GameObject) {
        super.onGOAddToContainer(state, gameObject)

        updateTexture()
    }

    override fun render(gameObject: GameObject, batch: Batch) {
        batch.draw(texture, gameObject.rectangle, flipX, flipY)
    }

    @JsonIgnore private val selectTextureTitle = "Sélection de la texture"
    @JsonIgnore private var useTextureSize = false
    @JsonIgnore private var showLevelTextures = false

    override fun insertImgui(gameObject: GameObject, labelName: String, editorScene: EditorScene) {
        with(ImGui) {
            if(imageButton(texture.textureObjectHandle, Vec2(gameObject.rectangle.width, gameObject.rectangle.height), uv1 = Vec2(1))) {
                openPopup(selectTextureTitle)
            }
        }
    }

    override fun insertImguiPopup(gameObject: GameObject, editorScene: EditorScene) {
        super.insertImguiPopup(gameObject, editorScene)

        with(ImGui) {
            val popupWidth = Gdx.graphics.width / 3 * 2
            val popupHeight = Gdx.graphics.height / 3 * 2
            setNextWindowSize(Vec2(popupWidth, popupHeight))
            setNextWindowPos(Vec2(Gdx.graphics.width / 2f - popupWidth / 2f, Gdx.graphics.height / 2f - popupHeight / 2f))
            if(beginPopup(selectTextureTitle)) {
                checkbox("Afficher les textures importées", this@TextureComponent::showLevelTextures)
                sameLine()
                checkbox("Mettre à jour la taille du gameObject", this@TextureComponent::useTextureSize)

                var sumImgsWidth = 0f

                val textures = if(showLevelTextures) editorScene.level.resourcesTextures() else PCGame.loadedTextures

                textures.forEach {
                    val texture = PCGame.assetManager.loadOnDemand<Texture>(it.path()).asset
                    val imgBtnSize = Vec2(texture.width, texture.height)
                    if(imageButton(texture.textureObjectHandle, imgBtnSize, uv1 = Vec2(1))) {
                        updateTexture(it)
                        if(useTextureSize)
                            gameObject.rectangle.size = Size(texture.width, texture.height)
                        closeCurrentPopup()
                    }

                    sumImgsWidth += imgBtnSize.x

                    if(sumImgsWidth + 400f < popupWidth)
                        sameLine()
                    else
                        sumImgsWidth = 0f
                }

                endPopup()
            }
        }
    }
}