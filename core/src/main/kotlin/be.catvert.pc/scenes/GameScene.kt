package be.catvert.pc.scenes

import be.catvert.pc.GameKeys
import be.catvert.pc.PCGame
import be.catvert.pc.containers.GameObjectContainer
import be.catvert.pc.containers.Level
import be.catvert.pc.utility.Constants
import be.catvert.pc.utility.ImguiHelper
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.Batch
import glm_.vec2.Vec2
import imgui.ImGui
import imgui.WindowFlags
import ktx.app.use
import ktx.assets.toLocalFile


/**
 * Scène du jeu
 */
class GameScene(private val level: Level) : Scene(level.background) {
    override var gameObjectContainer: GameObjectContainer = level
    override val camera: OrthographicCamera = OrthographicCamera(Constants.levelCameraRatio, Constants.levelCameraRatio * (Gdx.graphics.height.toFloat() / Gdx.graphics.width))

    private val cameraMoveSpeed = 10f

    private var pause = false

    init {
        level.moveCameraToFollowGameObject(camera, false)
    }

    override fun render(batch: Batch) {
        super.render(batch)

        with(ImGui) {
            if (pause) {
                ImguiHelper.withCenteredWindow("Pause", null, Vec2(200, 110), WindowFlags.NoResize.i or WindowFlags.NoCollapse.i) {
                    if (button("Reprendre", Vec2(-1, 20))) {
                        pause = false
                        gameObjectContainer.allowUpdatingGO = true
                    }
                    if (button("Recommencer", Vec2(-1, 20))) {
                        val level = Level.loadFromFile(this@GameScene.level.levelPath.toLocalFile().parent())
                        if (level != null)
                            PCGame.sceneManager.loadScene(GameScene(level))
                    }
                    if (button("Quitter le niveau", Vec2(-1, 20))) {
                        PCGame.sceneManager.loadScene(MainMenuScene())
                    }
                }
            }
        }
    }

    override fun postBatchRender() {
        super.postBatchRender()
        level.drawDebug()

        PCGame.hudBatch.use {
            PCGame.mainFont.draw(it, "Score : ${level.scorePoints}", 10f, Gdx.graphics.height - 10f)
            PCGame.mainFont.draw(it, "Temps écoulé : ${level.getTimer()}", 10f, Gdx.graphics.height - 40f)
        }
    }

    override fun update() {
        super.update()

        updateCamera(true)

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            pause = true
            gameObjectContainer.allowUpdatingGO = false
        }
        if (Gdx.input.isKeyJustPressed(GameKeys.GAME_SWITCH_GRAVITY.key))
            level.applyGravity = !level.applyGravity
        if (Gdx.input.isKeyJustPressed(GameKeys.GAME_EDIT_LEVEL.key))
            PCGame.sceneManager.loadScene(EditorScene(Level.loadFromFile(level.levelPath.toLocalFile().parent())!!))
    }

    private fun updateCamera(lerp: Boolean) {
        if (Gdx.input.isKeyPressed(GameKeys.CAMERA_ZOOM_UP.key)) {
            if (camera.zoom > 1f)
                camera.zoom -= 0.02f
        }
        if (Gdx.input.isKeyPressed(GameKeys.CAMERA_ZOOM_DOWN.key)) {
            if (level.matrixRect.width > camera.zoom * (camera.viewportWidth))
                camera.zoom += 0.02f
        }
        if (Gdx.input.isKeyPressed(GameKeys.CAMERA_ZOOM_RESET.key))
            camera.zoom = 1f

        if (!level.moveCameraToFollowGameObject(camera, lerp)) {
            if (Gdx.input.isKeyPressed(GameKeys.GAME_CAMERA_LEFT.key)) {
                camera.position.x -= cameraMoveSpeed
            }
            if (Gdx.input.isKeyPressed(GameKeys.GAME_CAMERA_RIGHT.key)) {
                camera.position.x += cameraMoveSpeed
            }
            if (Gdx.input.isKeyPressed(GameKeys.GAME_CAMERA_DOWN.key)) {
                camera.position.y -= cameraMoveSpeed
            }
            if (Gdx.input.isKeyPressed(GameKeys.GAME_CAMERA_UP.key)) {
                camera.position.y += cameraMoveSpeed
            }
        }

        camera.update()
    }
}