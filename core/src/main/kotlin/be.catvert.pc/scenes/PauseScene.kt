package be.catvert.pc.scenes

import be.catvert.pc.PCGame
import be.catvert.pc.utility.Size
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import ktx.actors.onClick
import ktx.actors.plus
import ktx.vis.window

/**
 * Scène du menu pause
 */
class PauseScene(private val gameScene: GameScene) : Scene(PCGame.mainBackground) {
    private val logo = PCGame.generateLogo(gameObjectContainer)

    init {
        stage + window("Pause") {
            verticalGroup {
                space(10f)

                textButton("Reprendre") {
                    onClick {
                        SceneManager.loadScene(gameScene)
                    }
                }
                textButton("Quitter le niveau") {
                    onClick {
                        gameScene.dispose()
                        SceneManager.loadScene(MainMenuScene(), false)
                    }
                }
            }

            setPosition(Gdx.graphics.width / 2f - width / 2f, Gdx.graphics.height / 2f - height / 2f)
        }
    }

    override fun update() {
        super.update()

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE))
            SceneManager.loadScene(gameScene)
    }

    override fun resize(size: Size) {
        super.resize(size)
        logo.box.set(PCGame.getLogoRect())
    }
}