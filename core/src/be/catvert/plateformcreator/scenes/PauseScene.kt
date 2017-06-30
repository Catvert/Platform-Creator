package be.catvert.plateformcreator.scenes

import be.catvert.plateformcreator.LevelFactory
import be.catvert.plateformcreator.MtrGame
import be.catvert.plateformcreator.ecs.systems.RenderingSystem
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import ktx.actors.onClick
import ktx.actors.plus
import ktx.vis.window

/**
 * Created by Catvert on 05/06/17.
 */

/**
 * Scène pause appelé lorsque le joueur appuie sur échap en partie
 * @param game L'objet du jeu
 * @param levelFile Le fichier utilisé pour charger le niveau
 * @param gameScene La scène du jeu
 */
class PauseScene(game: MtrGame, levelFile: FileHandle, gameScene: GameScene) : BaseScene(game, systems = RenderingSystem(game)) {
    init {
        background = game.getMainBackground()

        entities += game.getLogo()

        stage + window("Pause") {
            setSize(200f, 200f)
            setPosition(Gdx.graphics.width / 2f - width / 2, Gdx.graphics.height / 2f - height / 2)

            verticalGroup {
                space(10f)

                textButton("Reprendre") {
                    addListener(onClick {
                        this@PauseScene.game.setScene(gameScene)
                    })
                }

                textButton("Recommencer le niveau") {
                    addListener(onClick {
                        val (success, level) = LevelFactory.loadLevel(game, levelFile)
                        if (success) {
                            gameScene.dispose()
                            game.setScene(GameScene(game, level))
                        }
                    })
                }

                textButton("Quitter le niveau") {
                    addListener(onClick {
                        gameScene.dispose()
                        this@PauseScene.game.setScene(MainMenuScene(this@PauseScene.game))
                    })
                }
            }
        }
    }
}