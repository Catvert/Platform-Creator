package be.catvert.pc.scenes

import be.catvert.pc.PCGame
import be.catvert.pc.containers.Level
import be.catvert.pc.utility.ImGuiHelper
import be.catvert.pc.utility.Size
import com.badlogic.gdx.graphics.g2d.Batch
import glm_.vec2.Vec2
import imgui.Cond
import imgui.ImGui
import imgui.WindowFlags
import ktx.assets.toLocalFile

/**
 * Scène de fin de niveau, appelé lorsque le joueur meurt ou fini le niveau
 */
class EndLevelScene(private val level: Level) : Scene(level.background, level.backgroundColor) {
    private val logo = PCGame.generateLogo(gameObjectContainer)

    override fun render(batch: Batch) {
        super.render(batch)

        with(ImGui) {
            ImGuiHelper.withMenuButtonsStyle {
                ImGuiHelper.withCenteredWindow("end level menu", null, Vec2(300f, 125f), WindowFlags.NoTitleBar.i or WindowFlags.NoMove.i or WindowFlags.NoResize.i or WindowFlags.NoBringToFrontOnFocus.i, Cond.Always) {
                    if (button("Recommencer", Vec2(-1, 0))) {
                        val level = Level.loadFromFile(level.levelPath.toLocalFile().parent())
                        if (level != null)
                            PCGame.sceneManager.loadScene(GameScene(level))
                    }

                    if (button("Quitter", Vec2(-1, 0))) {
                        PCGame.sceneManager.loadScene(MainMenuScene())
                    }
                }
            }
        }
    }

    override fun resize(size: Size) {
        super.resize(size)
        logo.box.set(PCGame.getLogoRect())
    }
}