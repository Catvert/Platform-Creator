package be.catvert.pc.scenes

import be.catvert.pc.PCGame
import be.catvert.pc.utility.Constants
import be.catvert.pc.utility.Size
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Batch
import glm_.vec2.Vec2
import imgui.Cond
import imgui.ImGui
import imgui.WindowFlags
import imgui.functionalProgramming
import ktx.actors.onClick
import ktx.actors.plus
import ktx.assets.loadOnDemand
import ktx.vis.window

/**
 * Scène du menu pause
 */
class PauseScene(private val gameScene: GameScene) : Scene() {
    private val logo = PCGame.generateLogo(gameObjectContainer)

    init {
        stage + window("Menu principal") {
            verticalGroup {
                space(10f)

                textButton("Reprendre") {
                    onClick {
                        PCGame.setScene(gameScene)
                    }
                }
                textButton("Quitter le niveau") {
                    onClick {
                        PCGame.setScene(MainMenuScene())
                    }
                }
            }

            setPosition(Gdx.graphics.width / 2f - width / 2f, Gdx.graphics.height / 2f - height / 2f)
        }

        backgroundTexture = PCGame.assetManager.loadOnDemand<Texture>(Constants.gameBackgroundMenuPath).asset
    }

    override fun update() {
        super.update()

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE))
            PCGame.setScene(gameScene)
    }

    override fun resize(size: Size) {
        super.resize(size)
        logo.rectangle.set(PCGame.getLogoRect())
    }
}