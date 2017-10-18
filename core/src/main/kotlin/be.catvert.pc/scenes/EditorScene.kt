package be.catvert.pc.scenes

import be.catvert.pc.*
import be.catvert.pc.components.graphics.AtlasComponent
import be.catvert.pc.serialization.SerializationFactory
import be.catvert.pc.utility.Constants
import be.catvert.pc.utility.Point
import be.catvert.pc.utility.Rect
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.math.Vector3
import ktx.app.use
import ktx.assets.loadOnDemand

/**
 * Scène de l'éditeur de niveau
 */
class EditorScene(private val level: Level) : Scene() {
    override val gameObjectContainer: GameObjectContainer = level

    private val cameraMoveSpeed = 10f

    private var selectGameObject: GameObject? = null

    private val editorFont = BitmapFont(Gdx.files.local(Constants.editorFont))

    init {
        gameObjectContainer.allowUpdating = false

        if (level.backgroundPath != null)
            backgroundTexture = PCGame.assetManager.loadOnDemand<Texture>(Gdx.files.local(level.backgroundPath).path()).asset
    }

    override fun postBatchRender() {
        super.postBatchRender()
        PCGame.mainBatch.use {
            it.projectionMatrix = PCGame.defaultProjection
            with(editorFont) {
                //draw(hudBatch, "Layer sélectionné : $selectedLayer", 10f, com.badlogic.gdx.Gdx.graphics.height - editorFont.lineHeight)
                draw(it, "Nombre d'entités : ${level.getGameObjectsData().size}", 10f, com.badlogic.gdx.Gdx.graphics.height - editorFont.lineHeight * 2)
                //draw(hudBatch, "Resize mode : ${resizeMode.name}", 10f, com.badlogic.gdx.Gdx.graphics.height - editorFont.lineHeight * 3)
            }
            it.projectionMatrix = camera.combined
        }
        level.drawDebug()
    }

    override fun update() {
        super.update()

        updateCamera()

        level.activeRect.position = Point(Math.max(0, camera.position.x.toInt() - level.activeRect.width / 2), Math.max(0, camera.position.y.toInt() - level.activeRect.height / 2))

        if (Gdx.input.isButtonPressed(Input.Buttons.LEFT)) {
            val mousePosVec3 = camera.unproject(Vector3(Gdx.input.x.toFloat(), Gdx.input.y.toFloat(), 0f))
            val mousePos = Point(mousePosVec3.x.toInt(), mousePosVec3.y.toInt())
            if (selectGameObject != null) {
                selectGameObject!!.rectangle.position = Point(mousePos.x - selectGameObject!!.size().width / 2, mousePos.y - selectGameObject!!.size().height / 2)
               // level.setGameObjectToGrid(selectGameObject!!)
            } else {
                gameObjectContainer.getGameObjectsData().forEach {
                    if (it.rectangle.contains(mousePos)) {
                        selectGameObject = it
                    }
                }
            }
        } else {
            selectGameObject = null
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.E)) {
            val posInWorld = camera.unproject(Vector3(Gdx.input.x.toFloat(), Gdx.input.y.toFloat(), 0f))
            level.createGameObject(Rect(posInWorld.x.toInt(), posInWorld.y.toInt(), 50, 50)) {
                this += AtlasComponent("assets/atlas/Abstract Plateform/spritesheet_complete.atlas", "blockBrown")
            }
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            SerializationFactory.serializeToFile(level, Gdx.files.local(level.levelPath))
            PCGame.setScene(MainMenuScene())
        }
    }

    private fun updateCamera() {
        if (Gdx.input.isKeyPressed(GameKeys.EDITOR_CAMERA_LEFT.key))
            camera.translate(-cameraMoveSpeed, 0f)
        if (Gdx.input.isKeyPressed(GameKeys.EDITOR_CAMERA_RIGHT.key))
            camera.translate(cameraMoveSpeed, 0f)
        if (Gdx.input.isKeyPressed(GameKeys.EDITOR_CAMERA_UP.key))
            camera.translate(0f, cameraMoveSpeed)
        if (Gdx.input.isKeyPressed(GameKeys.EDITOR_CAMERA_DOWN.key))
            camera.translate(0f, -cameraMoveSpeed)
        if (Gdx.input.isKeyPressed(GameKeys.CAMERA_ZOOM_UP.key))
            camera.zoom -= 0.02f
        if (Gdx.input.isKeyPressed(GameKeys.CAMERA_ZOOM_DOWN.key))
            camera.zoom += 0.02f

        camera.update()
    }
}