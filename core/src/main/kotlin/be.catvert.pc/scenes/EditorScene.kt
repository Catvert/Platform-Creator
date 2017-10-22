package be.catvert.pc.scenes

import be.catvert.pc.*
import be.catvert.pc.factories.PrefabFactory
import be.catvert.pc.serialization.SerializationFactory
import be.catvert.pc.utility.Constants
import be.catvert.pc.utility.Point
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.math.Vector3
import ktx.actors.onClick
import ktx.actors.plus
import ktx.app.use
import ktx.assets.loadOnDemand
import ktx.assets.toLocalFile
import ktx.vis.window

/**
 * Scène de l'éditeur de niveau
 */
class EditorScene(private val level: Level) : Scene() {
    override val gameObjectContainer: GameObjectContainer = level

    private val cameraMoveSpeed = 10f

    private var selectGameObject: GameObject? = null

    private val editorFont = BitmapFont(Constants.editorFont.toLocalFile())

    init {
        gameObjectContainer.allowUpdating = false

        if (level.backgroundPath != null)
            backgroundTexture = PCGame.assetManager.loadOnDemand<Texture>(level.backgroundPath.toLocalFile().path()).asset
    }

    override fun postBatchRender() {
        super.postBatchRender()
        PCGame.mainBatch.use {
            it.projectionMatrix = PCGame.defaultProjection
            with(editorFont) {
                //draw(hudBatch, "Layer sélectionné : $selectedLayer", 10f, com.badlogic.gdx.Gdx.graphics.height - editorFont.lineHeight)
                draw(it, "Nombre d'entités : ${level.getGameObjectsData().size}", 10f, Gdx.graphics.height - editorFont.lineHeight * 2)
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
            PrefabFactory.PhysicsSprite.generate().generate(Point(posInWorld.x.toInt(), posInWorld.y.toInt()), level)
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            val posInWorld = camera.unproject(Vector3(Gdx.input.x.toFloat(), Gdx.input.y.toFloat(), 0f))
            PrefabFactory.Spider.generate().generate(Point(posInWorld.x.toInt(), posInWorld.y.toInt()), level)
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            showExitWindow()
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

    //region UI
    /**
     * Permet d'afficher la fenêtre permettant de sauvegarder et quitter l'éditeur
     */
    private fun showExitWindow() {
        stage + window("Quitter") {
            setPosition(Gdx.graphics.width / 2f - width / 2, Gdx.graphics.height / 2f - height / 2)
            addCloseButton()

            verticalGroup {
                space(10f)

                textButton("Sauvegarder") {
                    onClick {
                        try {
                            SerializationFactory.serializeToFile(level, level.levelPath.toLocalFile())
                            this@window.remove()
                        } catch (e: Exception) {
                            Log.error(e, message = { "Erreur lors de l'enregistrement du niveau !" })
                        }
                    }
                }
                textButton("Options du niveau") {
                    onClick {
                        showSettingsLevelWindow()
                    }
                }
                textButton("Quitter") {
                    onClick { PCGame.setScene(MainMenuScene()) }
                }
            }
        }
    }

    /**
     * Permet d'afficher la fenêtre permettant de modifier les paramètres du niveau
     */
    private fun showSettingsLevelWindow() {
        fun switchBackground(i: Int) {
            var index = PCGame.getBackgrounds().indexOfFirst { it == level.backgroundPath?.toLocalFile() }
            if (index == -1)
                index = 0

            val newIndex = index + i
            if (newIndex >= 0 && newIndex < PCGame.getBackgrounds().size) {
                val newBackground = PCGame.getBackgrounds()[newIndex]
                level.backgroundPath = newBackground.path()
                backgroundTexture = PCGame.assetManager.loadOnDemand<Texture>(newBackground.path()).asset
            }

        }

        stage + window("Paramètres du niveau") {
            setPosition(Gdx.graphics.width / 2f - width / 2, Gdx.graphics.height / 2f - height / 2)
            addCloseButton()

            verticalGroup {
                space(10f)

                horizontalGroup {
                    space(10f)

                    textButton("<-") {
                        onClick {
                            switchBackground(-1)
                        }
                    }

                    label("Fond d'écran")

                    textButton("->") {
                        onClick {
                            switchBackground(1)
                        }
                    }
                }
            }
        }
    }

    //endregion
}