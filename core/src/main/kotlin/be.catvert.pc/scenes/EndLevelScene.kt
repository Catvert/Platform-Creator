package be.catvert.pc.scenes

import be.catvert.pc.PCGame
import be.catvert.pc.containers.Level
import be.catvert.pc.utility.Size
import ktx.actors.centerPosition
import ktx.actors.onClick
import ktx.actors.plus
import ktx.assets.toLocalFile
import ktx.vis.table

/**
 * Scène de fin de niveau, appelé lorsque le joueur meurt ou fini le niveau
 */
class EndLevelScene(private val level: Level) : Scene(level.background) {
    private val logo = PCGame.generateLogo(gameObjectContainer)

    init {
        stage + table {
            textButton("Recommencer") { cell ->
                cell.minWidth(250f).space(10f)
                onClick {
                    val level = Level.loadFromFile(level.levelPath.toLocalFile().parent())
                    if (level != null)
                        PCGame.sceneManager.loadScene(GameScene(level))
                }
            }
            row()
            textButton("Quitter") { cell ->
                cell.minWidth(250f).space(10f)
                onClick {
                    PCGame.sceneManager.loadScene(MainMenuScene())
                }
            }
        }.apply { centerPosition(this@EndLevelScene.stage.width, this@EndLevelScene.stage.height) }
    }

    override fun resize(size: Size) {
        super.resize(size)
        logo.box.set(PCGame.getLogoRect())
    }
}