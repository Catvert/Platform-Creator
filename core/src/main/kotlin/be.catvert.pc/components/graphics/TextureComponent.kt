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
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.Texture
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
    @JsonIgnore private var useTextureSize = booleanArrayOf(false)

    override fun insertImgui(gameObject: GameObject, editorScene: EditorScene) {
        with(ImGui) {
            if(imageButton(texture.textureObjectHandle, Vec2(gameObject.rectangle.width, gameObject.rectangle.height), uv1 = Vec2(1))) {
                openPopup(selectTextureTitle)
            }
        }
    }

    override fun insertImguiPopup(gameObject: GameObject, editorScene: EditorScene) {
        super.insertImguiPopup(gameObject, editorScene)

        with(ImGui) {
            if(beginPopupModal(selectTextureTitle, extraFlags = WindowFlags.AlwaysHorizontalScrollbar.i or WindowFlags.AlwaysVerticalScrollbar.i)) {
                checkbox("Mettre à jour la taille du gameObject", useTextureSize)

                separator()

                var count = 0

                PCGame.loadedTextures.forEach {
                    val texture = PCGame.assetManager.loadOnDemand<Texture>(it.path()).asset
                    if(imageButton(texture.textureObjectHandle, Vec2(texture.width, texture.height), uv1 = Vec2(1))) {
                        updateTexture(it)
                        if(useTextureSize[0])
                            gameObject.rectangle.size = Size(texture.width, texture.height)
                        closeCurrentPopup()
                    }
                    if(++count <= 8)
                        sameLine()
                    else
                        count = 0
                }

                endPopup()
            }
        }
    }
}