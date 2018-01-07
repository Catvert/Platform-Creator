package be.catvert.pc.scenes

import be.catvert.pc.PCGame
import be.catvert.pc.containers.Level
import be.catvert.pc.i18n.MenusText
import be.catvert.pc.utility.ImguiHelper
import be.catvert.pc.utility.Size
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import glm_.vec2.Vec2
import imgui.ImGui
import imgui.WindowFlags
import ktx.actors.centerPosition
import ktx.actors.onClick
import ktx.actors.plus
import ktx.assets.toLocalFile
import ktx.scene2d.Scene2DSkin
import ktx.scene2d.table

/**
 * Scène de fin de niveau, appelé lorsque le joueur meurt ou fini le niveau
 * @param game L'objet du jeu
 * @param levelFile Le fichier utilisé pour charger le niveau
 * @param levelSuccess Permet de spécifier si oui ou non le joueur a réussi le niveau
 */
class EndLevelScene(private val level: Level) : Scene(level.background) {
    private val logo = PCGame.generateLogo(gameObjectContainer)

    init {
        stage + table {
            add(TextButton("Recommencer", Scene2DSkin.defaultSkin).apply {
                onClick {
                    val level = Level.loadFromFile(level.levelPath.toLocalFile().parent())
                    if (level != null)
                        PCGame.sceneManager.loadScene(GameScene(level))
                }
            }).minWidth(250f).space(10f)
            row()
            add(TextButton("Quitter", Scene2DSkin.defaultSkin).apply {
                onClick {
                    PCGame.sceneManager.loadScene(MainMenuScene(), false)
                }
            }).minWidth(250f).space(10f)
            row()
        }.apply { centerPosition(this@EndLevelScene.stage.width, this@EndLevelScene.stage.height) }
    }

    override fun resize(size: Size) {
        super.resize(size)
        logo.box.set(PCGame.getLogoRect())
    }
}