package be.catvert.mtrktx.scenes

import be.catvert.mtrktx.MtrGame
import be.catvert.mtrktx.ecs.systems.RenderingSystem
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import ktx.app.clearScreen
import ktx.scene2d.textButton
import ktx.scene2d.verticalGroup
import ktx.scene2d.window

/**
 * Created by arno on 03/06/17.
 */

class MainMenuScene(game: MtrGame) : BaseScene(game, RenderingSystem(game)) {
    override val entities: MutableList<Entity> = mutableListOf()

    init {
        entities += _game.getMainBackground()
        entities += _game.getLogo()

        _stage.addActor(window("Menu Principal") {
            setSize(200f, 200f)
            setPosition(Gdx.graphics.width / 2f - width / 2, Gdx.graphics.height / 2f - height / 2)

            verticalGroup {
                space(10f)

                textButton("Jouer") {
                    addListener(object : ClickListener() {
                        override fun clicked(event: InputEvent?, x: Float, y: Float) {
                            super.clicked(event, x, y)
                            _game.setScene(GameScene(_game, "levels/test.mtrlvl"))
                        }
                    })
                }
                textButton("Options") {

                }
                textButton("Quitter") {
                    addListener(object : ClickListener() {
                        override fun clicked(event: InputEvent?, x: Float, y: Float) {
                            super.clicked(event, x, y)
                            Gdx.app.exit()
                        }
                    })
                }
            }
        })
    }



    override fun render(delta: Float) {
        clearScreen(186f/255f, 212f/255f, 1f)

        super.render(delta)
    }

}