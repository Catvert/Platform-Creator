package be.catvert.pc.scenes

import be.catvert.pc.PCGame
import be.catvert.pc.containers.Level
import be.catvert.pc.utility.Constants
import be.catvert.pc.utility.Size
import be.catvert.pc.utility.loadOnDemand
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.graphics.Texture
import ktx.actors.onClick
import ktx.actors.plus
import ktx.assets.toLocalFile
import ktx.vis.window

/**
 * Scène de fin de niveau, appelé lorsque le joueur meurt ou fini le niveau
 * @param game L'objet du jeu
 * @param levelFile Le fichier utilisé pour charger le niveau
 * @param levelSuccess Permet de spécifier si oui ou non le joueur a réussi le niveau
 */
class EndLevelScene(private val levelPath: String, levelSuccess: Boolean) : Scene() {
    private val logo = PCGame.generateLogo(gameObjectContainer)

    init {
        stage + window("Fin de partie") {
            verticalGroup {
                space(10f)

                textButton("Recommencer") {
                    addListener(onClick {
                        val level = Level.loadFromFile(levelPath.toLocalFile().parent())
                        if (level != null)
                            SceneManager.loadScene(GameScene(level))
                    })
                }
                textButton("Quitter") {
                    addListener(onClick {
                        SceneManager.loadScene(MainMenuScene(), false)
                    })
                }
            }
            setPosition(Gdx.graphics.width / 2f - width / 2f, Gdx.graphics.height / 2f - height / 2f)
        }

        PCGame.assetManager.loadOnDemand<Sound>(if (levelSuccess) "sounds/game-over-success.wav".toLocalFile() else "sounds/game-over-fail.wav".toLocalFile()).asset.play(PCGame.soundVolume)
        backgroundTexture = PCGame.assetManager.loadOnDemand<Texture>(Constants.gameBackgroundMenuPath).asset
    }


    override fun resize(size: Size) {
        super.resize(size)
        logo.box.set(PCGame.getLogoRect())
    }
}