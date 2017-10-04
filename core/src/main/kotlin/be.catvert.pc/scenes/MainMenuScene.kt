package be.catvert.pc.scenes

import be.catvert.pc.Level
import be.catvert.pc.PCGame
import be.catvert.pc.components.graphics.TextureComponent
import be.catvert.pc.createGameObject
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.math.Rectangle
import ktx.actors.onClick
import ktx.actors.plus
import ktx.assets.loadOnDemand
import ktx.vis.window

class MainMenuScene : Scene() {
    init {
        stage + window("Menu principal") {
            verticalGroup {
                space(10f)

                textButton("Jouer !") {
                    onClick {
                        val level = Level("test")
                        level.createGameObject("test", Rectangle(10f, 10f, 50f, 50f)) {
                            this += TextureComponent("assets/game/notexture.png", this.rectangle, true)
                        }
                        PCGame.setScene(GameScene(level))
                    }
                }
                textButton("Options") {
                    onClick { }
                }
                textButton("Quitter") {
                    onClick {
                        Gdx.app.exit()
                    }
                }
            }

            setPosition(Gdx.graphics.width / 2f - width / 2f, Gdx.graphics.height / 2f - height / 2f)
        }

        backgroundTexture = PCGame.assetManager.loadOnDemand<Texture>("assets/game/mainmenu.png").asset
    }
}