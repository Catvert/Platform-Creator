package be.catvert.pc.scenes

import be.catvert.pc.GameKeys
import be.catvert.pc.Level
import be.catvert.pc.PCGame
import be.catvert.pc.components.graphics.TextureComponent
import be.catvert.pc.serialization.SerializationFactory
import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input

class EditorScene(private val level: Level): Scene() {
    private val cameraMoveSpeed = 10f
    init {
        addContainer(level)
    }

    override fun update() {
        super.update()

        if(Gdx.input.isKeyPressed(GameKeys.EDITOR_CAMERA_LEFT.key))
            camera.translate(cameraMoveSpeed, 0f)
        if(Gdx.input.isKeyPressed(GameKeys.EDITOR_CAMERA_RIGHT.key))
            camera.translate(-cameraMoveSpeed, 0f)
        if(Gdx.input.isKeyPressed(GameKeys.EDITOR_CAMERA_UP.key))
            camera.translate(0f, -cameraMoveSpeed)
        if(Gdx.input.isKeyPressed(GameKeys.EDITOR_CAMERA_DOWN.key))
            camera.translate(0f, cameraMoveSpeed)

        camera.update()

        if(Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            SerializationFactory.serializeToFile(level, level.levelPath)
            PCGame.setScene(MainMenuScene())
        }
    }
}