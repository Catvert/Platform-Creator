package be.catvert.pc.scenes

import be.catvert.pc.PCGame
import be.catvert.pc.containers.Level
import be.catvert.pc.utility.ImguiHelper
import be.catvert.pc.utility.Size
import com.badlogic.gdx.graphics.g2d.Batch
import glm_.vec2.Vec2
import imgui.ImGui
import imgui.WindowFlags
import ktx.assets.toLocalFile

/**
 * Scène de fin de niveau, appelé lorsque le joueur meurt ou fini le niveau
 * @param game L'objet du jeu
 * @param levelFile Le fichier utilisé pour charger le niveau
 * @param levelSuccess Permet de spécifier si oui ou non le joueur a réussi le niveau
 */
class EndLevelScene(private val levelPath: String) : Scene(PCGame.mainBackground) {
    private val logo = PCGame.generateLogo(gameObjectContainer)

    private val endLevelWindowSize = Vec2(200f, 85f)

    override fun render(batch: Batch) {
        super.render(batch)

        with(ImGui) {
            ImguiHelper.withCenteredWindow("Fin de partie", null, endLevelWindowSize, WindowFlags.NoResize.i or WindowFlags.NoCollapse.i) {
                if (button("Recommencer", Vec2(-1, 20))) {
                    val level = Level.loadFromFile(levelPath.toLocalFile().parent())
                    if (level != null)
                        PCGame.sceneManager.loadScene(GameScene(level))
                }
                if (button("Quitter", Vec2(-1, 20))) {
                    PCGame.sceneManager.loadScene(MainMenuScene(), false)
                }
            }
        }
    }

    override fun resize(size: Size) {
        super.resize(size)
        logo.box.set(PCGame.getLogoRect())
    }
}