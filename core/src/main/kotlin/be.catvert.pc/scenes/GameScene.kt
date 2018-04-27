package be.catvert.pc.scenes

import be.catvert.pc.GameKeys
import be.catvert.pc.PCGame
import be.catvert.pc.eca.containers.EntityContainer
import be.catvert.pc.eca.containers.Level
import be.catvert.pc.eca.containers.LevelStats
import be.catvert.pc.managers.MusicsManager
import be.catvert.pc.managers.ResourcesManager
import be.catvert.pc.serialization.SerializationFactory
import be.catvert.pc.ui.ImGuiHelper
import be.catvert.pc.utility.Constants
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import glm_.vec2.Vec2
import imgui.ImGui
import imgui.WindowFlag
import imgui.WindowFlags
import ktx.app.use


/**
 * Scène du jeu
 */
class GameScene(private val level: Level, private val levelNumberTries: Int = 1) : Scene(level.background, level.backgroundColor) {
    override var entityContainer: EntityContainer = level

    private var pause = false

    private val glyphTimer = GlyphLayout()

    private var isLevelExit = false

    init {
        level.entitiesInitialStartActions()

        level.updateCamera(camera, false)
        if (level.music != null)
            MusicsManager.startMusic(level.music!!, true)

        level.exit = {
            if(!isLevelExit) {
                isLevelExit = true

                ResourcesManager.getSound(if (it) Constants.gameDirPath.child("game-over-success.wav") else Constants.gameDirPath.child("game-over-fail.wav"))?.play(PCGame.soundVolume)
                if (it)
                    PCGame.scenesManager.loadScene(MainMenuScene(LevelStats(level.levelPath, level.getTimer(), levelNumberTries), true))
                else {
                    PCGame.scenesManager.loadScene(GameScene(SerializationFactory.deserializeFromFile(level.levelPath.get()), levelNumberTries + 1))
                }
            }
        }
    }

    override fun render(batch: Batch) {
        super.render(batch)

        with(ImGui) {
            if (pause) {
                ImGuiHelper.withCenteredWindow("pause", null, Vec2(200f, 105f), WindowFlag.NoResize.i or WindowFlag.NoCollapse.i or WindowFlag.NoTitleBar.i) {
                    if (button("Reprendre", Vec2(-1, 0))) {
                        pause = false
                        entityContainer.allowUpdating = true
                    }
                    if (button("Recommencer", Vec2(-1, 0))) {
                        val level = Level.loadFromFile(this@GameScene.level.levelPath.get().parent())
                        if (level != null)
                            PCGame.scenesManager.loadScene(GameScene(level))
                    }
                    if (button("Quitter le niveau", Vec2(-1, 0))) {
                        PCGame.scenesManager.loadScene(MainMenuScene(null, true))
                    }
                }
            }
        }
    }

    override fun postBatchRender() {
        super.postBatchRender()
        level.drawDebug()

        glyphTimer.setText(PCGame.mainFont, "Temps écoulé : ${level.getTimer()}")

        PCGame.hudBatch.projectionMatrix = PCGame.defaultProjection
        PCGame.hudBatch.use {
            PCGame.mainFont.color.a = alpha
            PCGame.mainFont.draw(it, "Score : ${level.scorePoints}", 10f, Gdx.graphics.height - PCGame.mainFont.lineHeight)

            PCGame.mainFont.draw(it, glyphTimer, Gdx.graphics.width - glyphTimer.width - 10f, Gdx.graphics.height - glyphTimer.height)
        }
    }

    override fun update() {
        super.update()

        updateCamera(true)

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            pause = true
            entityContainer.allowUpdating = false
        }
        if (Gdx.input.isKeyJustPressed(GameKeys.GAME_EDIT_LEVEL.key))
            PCGame.scenesManager.loadScene(EditorScene(Level.loadFromFile(level.levelPath.get().parent())!!, true))
    }

    private fun updateCamera(lerp: Boolean) {
        level.updateCamera(camera, lerp)

        camera.update()
    }
}