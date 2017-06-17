package be.catvert.mtrktx.scenes

import be.catvert.mtrktx.MtrGame
import be.catvert.mtrktx.ecs.systems.RenderingSystem
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import ktx.app.clearScreen
import ktx.vis.window

/**
* Created by Catvert on 05/06/17.
*/

class PauseScene(game: MtrGame) : BaseScene(game, systems = RenderingSystem(game)) {
    override val entities: MutableList<Entity> = mutableListOf()

    init {
        _game.background = _game.getMainBackground()
        entities += game.getLogo()

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
}