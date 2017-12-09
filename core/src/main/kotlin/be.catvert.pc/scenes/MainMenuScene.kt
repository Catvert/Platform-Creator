package be.catvert.pc.scenes

import be.catvert.pc.GameKeys
import be.catvert.pc.Log
import be.catvert.pc.PCGame
import be.catvert.pc.PCGame.Companion.soundVolume
import be.catvert.pc.containers.Level
import be.catvert.pc.i18n.MenusText
import be.catvert.pc.utility.Constants
import be.catvert.pc.utility.ImguiHelper
import be.catvert.pc.utility.Size
import be.catvert.pc.utility.Utility
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import glm_.vec2.Vec2
import imgui.*
import ktx.app.use
import java.io.IOException


/**
 * Scène du menu principal
 */

class MainMenuScene : Scene(PCGame.mainBackground) {
    private val glyphCreatedBy = GlyphLayout(PCGame.mainFont, "par Catvert - ${Constants.gameVersion}")

    private val logo = PCGame.generateLogo(gameObjectContainer)

    override fun postBatchRender() {
        super.postBatchRender()
        PCGame.hudBatch.use {
            PCGame.mainFont.draw(it, glyphCreatedBy, Gdx.graphics.width - glyphCreatedBy.width, glyphCreatedBy.height)
        }
    }

    override fun resize(size: Size) {
        super.resize(size)
        logo.box.set(PCGame.getLogoRect())
    }

    override fun render(batch: Batch) {
        super.render(batch)
        drawUI()
    }

    //region UI

    private val mainWindowSize = Vec2(200f, 110f)
    private var showSelectLevelWindow = false
    private var showSettingsWindow = false
    private fun drawUI() {
        with(ImGui) {
            ImguiHelper.withCenteredWindow(MenusText.MM_WINDOW_TITLE(), null, mainWindowSize, WindowFlags.NoResize.i or WindowFlags.NoCollapse.i or WindowFlags.NoBringToFrontOnFocus) {
                if (button(MenusText.MM_PLAY_BUTTON(), Vec2(-1, 20f))) {
                    showSelectLevelWindow = true
                    openPopup(MenusText.MM_SELECT_LEVEL_WINDOW_TITLE())
                }
                if (button(MenusText.MM_SETTINGS_BUTTON(), Vec2(-1, 20f))) {
                    showSettingsWindow = true
                    openPopup(MenusText.MM_SETTINGS_WINDOW_TITLE())
                }
                if (button(MenusText.MM_EXIT_BUTTON(), Vec2(-1, 20f))) {
                    Gdx.app.exit()
                }

                if (showSelectLevelWindow)
                    drawSelectLevelWindow()
                if (showSettingsWindow)
                    drawSettingsWindow()
            }
        }

    }

    private class LevelItem(val dir: FileHandle) {
        override fun toString(): String = dir.name()
    }

    private val levels = Constants.levelDirPath.list { dir -> dir.isDirectory && dir.list { _, s -> s == Constants.levelDataFile }.isNotEmpty() }.map { LevelItem(it) }.toMutableList()
    private var currentLevel = 0
    private val newLevelTitle = "Nouveau niveau"
    private val copyLevelTitle = "Copier un niveau"
    private val deleteLevelTitle = "Supprimer un niveau"
    private val errorLevelTitle = "Erreur lors du chargement du niveau"
    private var newLevelName = "test"
    private var copyLevelName = "test"
    private fun drawSelectLevelWindow() {
        with(ImGui) {
            ImguiHelper.popupModal(MenusText.MM_SELECT_LEVEL_WINDOW_TITLE(), ::showSelectLevelWindow, extraFlags = WindowFlags.NoResize.i) {
                pushItemFlag(ItemFlags.Disabled.i, levels.isEmpty())

                functionalProgramming.withItemWidth(100f) {
                    combo("niveau", ::currentLevel, levels.map { it.toString() })
                }

                if (button(MenusText.MM_SELECT_LEVEL_PLAY_BUTTON(), Vec2(-1, 20f))) {
                    if (currentLevel in levels.indices) {
                        val level = Level.loadFromFile(levels[currentLevel].dir)
                        if (level != null)
                            SceneManager.loadScene(GameScene(level))
                        else
                            openPopup(errorLevelTitle)
                    }
                }
                if (button(MenusText.MM_SELECT_LEVEL_EDIT_BUTTON(), Vec2(-1, 20f))) {
                    if (currentLevel in levels.indices) {
                        val level = Level.loadFromFile(levels[currentLevel].dir)
                        if (level != null)
                            SceneManager.loadScene(EditorScene(level))
                        else
                            openPopup(errorLevelTitle)
                    }
                }
                if (button(MenusText.MM_SELECT_LEVEL_COPY_BUTTON(), Vec2(-1, 20f))) {
                    if (currentLevel in levels.indices)
                        openPopup(copyLevelTitle)
                }
                if (button(MenusText.MM_SELECT_LEVEL_DELETE_BUTTON(), Vec2(-1, 20f))) {
                    if (currentLevel in levels.indices)
                        openPopup(deleteLevelTitle)
                }
                separator()

                popItemFlag()

                if (button(MenusText.MM_SELECT_LEVEL_NEW_BUTTON(), Vec2(-1, 20f))) {
                    openPopup(newLevelTitle)
                }

                functionalProgramming.popup(newLevelTitle) {
                    functionalProgramming.withItemWidth(100f) {
                        ImguiHelper.inputText("nom", ::newLevelName)
                    }
                    if (button("Créer", Vec2(-1, 20))) {
                        val level = Level.newLevel(newLevelName)
                        SceneManager.loadScene(EditorScene(level))
                        closeCurrentPopup()
                    }
                }

                functionalProgramming.popup(copyLevelTitle) {
                    functionalProgramming.withItemWidth(100f) {
                        ImguiHelper.inputText("nom", ::copyLevelName)
                    }

                    if (button("Copier", Vec2(-1, 20))) {
                        val levelDir = levels[currentLevel].dir
                        val copyLevelDir = levelDir.parent().child(copyLevelName)
                        levelDir.list().forEach {
                            it.copyTo(copyLevelDir)
                        }
                        levels.add(LevelItem(copyLevelDir))
                        closeCurrentPopup()
                    }
                }

                functionalProgramming.popup(deleteLevelTitle) {
                    if (button("Confirmer", Vec2(100f, 20))) {
                        try {
                            if (currentLevel in levels.indices) {
                                levels[currentLevel].dir.deleteDirectory()
                                levels.removeAt(currentLevel)
                            }
                        } catch (e: IOException) {
                            Log.error(e) { "Erreur survenue lors de la suppression du niveau !" }
                        }
                    }
                }

                functionalProgramming.popupModal(errorLevelTitle) {
                    text("Une erreur est survenue lors du chargement du niveau !")
                    if (button("Fermer", Vec2(-1, 20f)))
                        closeCurrentPopup()
                }
            }
        }
    }

    private var settingsWindowSize = intArrayOf(Gdx.graphics.width, Gdx.graphics.height)
    private val settingsFullscreen = booleanArrayOf(Gdx.graphics.isFullscreen)
    private fun drawSettingsWindow() {
        with(ImGui) {
            setNextWindowContentWidth(550f)
            ImguiHelper.popupModal(MenusText.MM_SETTINGS_WINDOW_TITLE(), ::showSettingsWindow, extraFlags = WindowFlags.NoResize.i) {
                functionalProgramming.withGroup {
                    if (!settingsFullscreen[0]) {
                        functionalProgramming.withItemWidth(100f) {
                            inputInt2("taille de la fenêtre", settingsWindowSize)
                        }
                    }
                    checkbox(MenusText.MM_SETTINGS_FULLSCREEN(), settingsFullscreen)
                    functionalProgramming.withItemWidth(100f) {
                        sliderFloat(MenusText.MM_SETTINGS_SOUND(), ::soundVolume, 0f, 1f, "%.1f")
                    }
                    if (button("Appliquer", Vec2(100f, 20))) {
                        if (settingsFullscreen[0])
                            Gdx.graphics.setFullscreenMode(Gdx.graphics.displayMode)
                        else
                            Gdx.graphics.setWindowedMode(settingsWindowSize[0], settingsWindowSize[1])

                        Utility.saveGameConfig(settingsWindowSize[0], settingsWindowSize[1])
                    }
                }
                sameLine()

                functionalProgramming.withChild("game keys") {
                    GameKeys.values().forEach {
                        functionalProgramming.withItemWidth(50f) {
                            inputText(it.description, Input.Keys.toString(it.key).toCharArray())
                        }
                    }
                }
            }
        }
    }
    //endregion
}