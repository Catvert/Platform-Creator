package be.catvert.pc.scenes

import be.catvert.pc.Level
import be.catvert.pc.PCGame
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input

class GameScene(private val level: Level) : Scene() {
    init {
        addContainer(level)
    }

    override fun update() {
        super.update()

        if(Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE))
            PCGame.setScene(PauseScene(this), false)
    }
}