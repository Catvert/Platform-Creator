package be.catvert.plateformcreator.scenes

import be.catvert.plateformcreator.LevelFactory
import be.catvert.plateformcreator.MtrGame
import be.catvert.plateformcreator.ecs.systems.RenderingSystem
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import ktx.vis.window

/**
 * Created by Catvert on 26/06/17.
 */

/**
 * Scène de fin de niveau, appelé lorsque le joueur meurt ou fini le niveau
 */
class EndLevelScene(game: MtrGame, levelFile: FileHandle, levelSuccess: Boolean) : BaseScene(game, systems = RenderingSystem(game)) {
    init {
        background = game.getMainBackground()
        entities += game.getLogo()

        stage.addActor(window("Fin de partie") {
            setSize(200f, 200f)
            setPosition(Gdx.graphics.width / 2f - width / 2, Gdx.graphics.height / 2f - height / 2)

            verticalGroup {
                space(10f)

                textButton("Recommencer le niveau") {
                    addListener(object : ClickListener() {
                        override fun clicked(event: InputEvent?, x: Float, y: Float) {
                            super.clicked(event, x, y)

                            val (success, level, entityEvent) = LevelFactory(game).loadLevel(levelFile)
                            if (success)
                                game.setScene(GameScene(game, entityEvent, level))
                        }
                    })
                }
                textButton("Revenir au menu principal") {
                    addListener(object : ClickListener() {
                        override fun clicked(event: InputEvent?, x: Float, y: Float) {
                            super.clicked(event, x, y)

                            game.setScene(MainMenuScene(game))
                        }
                    })
                }
            }
        })
    }
}