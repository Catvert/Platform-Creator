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
 * Created by arno on 05/06/17.
 */

class PauseScene(game: MtrGame) : BaseScene(game, RenderingSystem(game)) {
    override val entities: MutableList<Entity> = mutableListOf()

    init {
        entities += _game.getMainBackground()
        entities += _game.getLogo()

        _stage.addActor(window("Pause") {
            setSize(200f, 200f)
            setPosition(Gdx.graphics.width / 2f - width / 2, Gdx.graphics.height / 2f - height / 2)

            verticalGroup {
                space(10f)

                textButton("Reprendre") {
                    addListener(object : ClickListener() {
                        override fun clicked(event: InputEvent?, x: Float, y: Float) {
                            super.clicked(event, x, y)
                            _game.setScreen(GameScene::class.java)
                        }
                    })
                }
                textButton("Quitter le niveau") {
                    addListener(object : ClickListener() {
                        override fun clicked(event: InputEvent?, x: Float, y: Float) {
                            super.clicked(event, x, y)
                            _game.removeSceneSafely<GameScene>()
                            _game.setScene(MainMenuScene(_game))
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