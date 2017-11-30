package be.catvert.pc.components.graphics

import be.catvert.pc.GameObject
import be.catvert.pc.PCGame
import be.catvert.pc.components.RenderableComponent
import be.catvert.pc.containers.GameObjectContainer
import be.catvert.pc.containers.Level
import be.catvert.pc.utility.*
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Batch
import com.fasterxml.jackson.annotation.JsonCreator
import glm_.vec2.Vec2
import imgui.ImGui
import ktx.assets.loadOnDemand
import ktx.assets.toLocalFile

/**
 * Component permettant d'afficher une texture chargée depuis un fichier
 * @param texturePath Le chemin vers la texture en question
 */
class TextureComponent(texturePath: FileHandle) : RenderableComponent(), CustomEditorImpl {
    @JsonCreator constructor() : this(Constants.defaultTexturePath)

    var texturePath: String = texturePath.path()
        private set

    private var texture = PCGame.assetManager.loadOnDemand<Texture>(this.texturePath).asset

    fun updateTexture(texturePath: FileHandle = this.texturePath.toLocalFile()) {
        this.texturePath = texturePath.path()

        texture = PCGame.assetManager.loadOnDemand<Texture>(this.texturePath).asset
    }

    override fun onAddToContainer(gameObject: GameObject, container: GameObjectContainer) {
        super.onAddToContainer(gameObject, container)

        updateTexture()
    }

    override fun render(gameObject: GameObject, batch: Batch) {
        batch.setColor(1f, 1f, 1f, alpha)
        batch.draw(texture, gameObject.box, flipX, flipY)
        batch.setColor(1f, 1f, 1f, 1f)
    }

    private val selectTextureTitle = "Sélection de la texture"
    private var useTextureSize = false
    private var showLevelTextures = false

    override fun insertImgui(labelName: String, gameObject: GameObject, level: Level) {
        with(ImGui) {
            if (imageButton(texture.textureObjectHandle, Vec2(gameObject.box.width, gameObject.box.height), uv1 = Vec2(1))) {
                openPopup(selectTextureTitle)
            }

            with(ImGui) {
                val popupWidth = Gdx.graphics.width / 3 * 2
                val popupHeight = Gdx.graphics.height / 3 * 2
                setNextWindowSize(Vec2(popupWidth, popupHeight))
                setNextWindowPos(Vec2(Gdx.graphics.width / 2f - popupWidth / 2f, Gdx.graphics.height / 2f - popupHeight / 2f))
                if (beginPopup(selectTextureTitle)) {
                    checkbox("Afficher les textures importées", ::showLevelTextures)
                    sameLine()
                    checkbox("Mettre à jour la taille du gameObject", ::useTextureSize)

                    var sumImgsWidth = 0f

                    val textures = if (showLevelTextures) level.resourcesTextures() else PCGame.gameTextures

                    textures.forEach {
                        val texture = PCGame.assetManager.loadOnDemand<Texture>(it.toFileWrapper()).asset
                        val imgBtnSize = Vec2(Math.min(texture.width, 200), Math.min(texture.height, 200))
                        if (imageButton(texture.textureObjectHandle, imgBtnSize, uv1 = Vec2(1))) {
                            updateTexture(it)
                            if (useTextureSize)
                                gameObject.box.size = Size(texture.width, texture.height)
                            closeCurrentPopup()
                        }

                        sumImgsWidth += imgBtnSize.x

                        if (sumImgsWidth + 400f < popupWidth)
                            sameLine()
                        else
                            sumImgsWidth = 0f
                    }

                    endPopup()
                }
            }
        }
    }
}