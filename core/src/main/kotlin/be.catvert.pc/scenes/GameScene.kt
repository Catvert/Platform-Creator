package be.catvert.pc.scenes

import be.catvert.pc.Level
import be.catvert.pc.PCGame
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Texture
import ktx.assets.loadOnDemand

/**
 * Scène du jeu
 */
class GameScene(private val level: Level) : Scene() {
    init {
        addContainer(level)

        if (level.backgroundPath != null)
            backgroundTexture = PCGame.assetManager.loadOnDemand<Texture>(Gdx.files.local(level.backgroundPath).path()).asset
    }

    override fun update() {
        super.update()

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE))
            PCGame.setScene(PauseScene(this), false)
    }
}