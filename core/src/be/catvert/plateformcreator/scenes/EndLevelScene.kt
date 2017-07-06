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
 * Created by Catvert on 26/06/17.
 */

/**
 * Scène de fin de niveau, appelé lorsque le joueur meurt ou fini le niveau
 * @param game L'objet du jeu
 * @param levelFile Le fichier utilisé pour charger le niveau
 * @param levelSuccess Permet de spécifier si oui ou non le joueur a réussi le niveau
 */
class EndLevelScene(game: MtrGame, levelFile: FileHandle, levelSuccess: Boolean) : BaseScene(game, systems = RenderingSystem(game)) {
    init {
        background = game.getMainBackground()
        entities += game.getLogo()

        game.getGameSound(Gdx.files.internal(if (levelSuccess) "sounds/game-over-success.wav" else "sounds/game-over-fail.wav")).play(game.soundVolume)

        stage + window("Fin de partie") {
            setSize(200f, 200f)
            setPosition(Gdx.graphics.width / 2f - width / 2, Gdx.graphics.height / 2f - height / 2)

            verticalGroup {
                space(10f)

                textButton("Recommencer le niveau") {
                    addListener(onClick {
                        val level = LevelFactory.loadLevel(game, levelFile)
                        if (level != null)
                            game.setScene(GameScene(game, level))
                    })
                }
                textButton("Revenir au menu principal") {
                    addListener(onClick {
                        game.setScene(MainMenuScene(game))
                    })
                }
            }
        }
    }
}