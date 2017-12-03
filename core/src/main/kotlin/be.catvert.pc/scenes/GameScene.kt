package be.catvert.pc.scenes

import be.catvert.pc.GameKeys
import be.catvert.pc.PCGame
import be.catvert.pc.containers.GameObjectContainer
import be.catvert.pc.containers.Level
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import ktx.app.use
import ktx.assets.toLocalFile


/**
 * Scène du jeu
 */
class GameScene(private val level: Level) : Scene(level.background) {
    override var gameObjectContainer: GameObjectContainer = level

    private val cameraMoveSpeed = 10f

    override fun postBatchRender() {
        super.postBatchRender()
        level.drawDebug()

        PCGame.hudBatch.use {
            PCGame.mainFont.draw(it, "Score : ${level.scorePoints}", 10f, Gdx.graphics.height - 10f)
            PCGame.mainFont.draw(it, "Temps écoulé : ${level.timer}", 10f, Gdx.graphics.height - 40f)
        }
    }

    override fun update() {
        super.update()

        updateCamera(true)

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE))
            SceneManager.loadScene(PauseScene(this), disposeCurrentScene = false)
        if (Gdx.input.isKeyJustPressed(GameKeys.GAME_SWITCH_GRAVITY.key))
            level.applyGravity = !level.applyGravity
        if (Gdx.input.isKeyJustPressed(GameKeys.GAME_EDIT_LEVEL.key))
            SceneManager.loadScene(EditorScene(Level.loadFromFile(level.levelPath.toLocalFile().parent())!!))
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