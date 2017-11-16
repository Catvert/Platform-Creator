package be.catvert.pc.scenes

import be.catvert.pc.GameKeys
import be.catvert.pc.containers.GameObjectContainer
import be.catvert.pc.containers.Level
import be.catvert.pc.PCGame
import be.catvert.pc.utility.Constants
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import ktx.assets.loadOnDemand
import ktx.assets.toLocalFile

/**
 * Scène du jeu
 */
class GameScene(private val level: Level) : Scene() {
    override var gameObjectContainer: GameObjectContainer = level

    //override val camera = OrthographicCamera().apply { setToOrtho(false, Gdx.graphics.width / Constants.PPM, Gdx.graphics.height / Constants.PPM) }

    private val cameraMoveSpeed = 10f

    init {
        if (level.backgroundPath != null)
            backgroundTexture = PCGame.assetManager.loadOnDemand<Texture>(level.backgroundPath!!.toLocalFile().path()).asset


    }

    override fun postBatchRender() {
        super.postBatchRender()
        level.drawDebug()
    }

    override fun update() {
        super.update()

        updateCamera(true)

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE))
            PCGame.setScene(PauseScene(this), false)
        if (Gdx.input.isKeyJustPressed(GameKeys.GAME_SWITCH_GRAVITY.key))
            level.applyGravity = !level.applyGravity
        if (Gdx.input.isKeyJustPressed(GameKeys.GAME_EDIT_LEVEL.key))
            PCGame.setScene(EditorScene(Level.loadFromFile(level.levelPath.toLocalFile())!!))
    }

    private fun updateCamera(lerp: Boolean) {
        if (Gdx.input.isKeyPressed(GameKeys.CAMERA_ZOOM_UP.key)) {
            camera.zoom -= 0.02f
        }
        if (Gdx.input.isKeyPressed(GameKeys.CAMERA_ZOOM_DOWN.key)) {
            camera.zoom += 0.02f
        }
        if (Gdx.input.isKeyPressed(GameKeys.CAMERA_ZOOM_RESET.key)) {
            camera.zoom = 1f
        }

        if (!level.moveCameraToFollowGameObject(camera, true)) {
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