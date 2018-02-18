package be.catvert.pc.scenes

import be.catvert.pc.GameKeys
import be.catvert.pc.Log
import be.catvert.pc.PCGame
import be.catvert.pc.PCGame.Companion.soundVolume
import be.catvert.pc.containers.Level
import be.catvert.pc.i18n.MenusText
import be.catvert.pc.utility.*
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.g2d.Batch
import glm_.vec2.Vec2
import imgui.*
import java.io.IOException
import kotlin.collections.set


/**
 * Scène du menu principal
 */
class MainMenuScene(applyMusicTransition: Boolean) : Scene(PCGame.mainBackground) {
    private val logo = PCGame.generateLogo(gameObjectContainer)

    private class LevelItem(val dir: FileHandle) {
        override fun toString(): String = dir.name()
    }

    private val levels = Constants.levelDirPath.list { dir -> dir.isDirectory && dir.list { _, s -> s == Constants.levelDataFile }.isNotEmpty() }.map { LevelItem(it) }.toMutableList()

    init {
        if(applyMusicTransition)
            MusicManager.startMusic(Constants.menuMusicPath, true)
    }

    override fun resize(size: Size) {
        super.resize(size)
        logo.box.set(PCGame.getLogoRect())
    }

    override fun render(batch: Batch) {
        super.render(batch)
        drawUI()
    }

    private var showSelectLevelWindow = false
    private var showSettingsWindow = false
    private fun drawUI() {
        drawMainMenu()

        if (showSelectLevelWindow)
            drawSelectLevelWindow()

        if (showSettingsWindow)
            drawSettingsWindow()
    }

    private fun drawMainMenu() {
        with(ImGui) {
            ImGuiHelper.withMenuButtonsStyle {
                ImGuiHelper.withCenteredWindow("main menu", null, Vec2(300f, 180f), WindowFlags.NoTitleBar.i or WindowFlags.NoCollapse.i or WindowFlags.NoMove.i or WindowFlags.NoResize.i or WindowFlags.NoBringToFrontOnFocus.i, Cond.Always) {
                    if (ImGuiHelper.tickSoundButton(MenusText.MM_PLAY_BUTTON(), Vec2(-1, 0))) {
                        showSelectLevelWindow = true
                    }

                    if (ImGuiHelper.tickSoundButton(MenusText.MM_SETTINGS_BUTTON(), Vec2(-1, 0))) {
                        settingsKeys.forEach {
                            settingsKeys[it.key] = false
                            PCInputProcessor.keyDownSignal.clear()
                        }
                        showSettingsWindow = true
                    }

                    if (ImGuiHelper.tickSoundButton(MenusText.MM_EXIT_BUTTON(), Vec2(-1, 0))) {
                        Gdx.app.exit()
                    }
                }
            }
        }
    }

    private var currentLevelIndex = 0

    private val newLevelTitle = "Nouveau niveau"
    private var newLevelNameBuf = ""
    private var newLevelOpen = booleanArrayOf(true)

    private val copyLevelTitle = "Copier un niveau"
    private var copyLevelNameBuf = ""

    private var errorInLevelTitle = "Impossible de charger le niveau !"

    private fun drawSelectLevelWindow() {
        with(ImGui) {
            ImGuiHelper.withCenteredWindow(MenusText.MM_SELECT_LEVEL_WINDOW_TITLE(), ::showSelectLevelWindow, Vec2(215f, 165f), WindowFlags.NoResize.i or WindowFlags.NoCollapse.i) {
                var openCopyPopup = false

                pushItemFlag(ItemFlags.Disabled.i, levels.isEmpty())
                ImGuiHelper.comboWithSettingsButton(MenusText.MM_SELECT_LEVEL_LEVEL_COMBO(), ::currentLevelIndex, levels.map { it.toString() }, {
                    if (button(MenusText.MM_SELECT_LEVEL_COPY_BUTTON(), Vec2(Constants.defaultWidgetsWidth, 0))) {
                        openCopyPopup = true
                    }

                    if (button(MenusText.MM_SELECT_LEVEL_DELETE_BUTTON(), Vec2(Constants.defaultWidgetsWidth, 0))) {
                        if (currentLevelIndex in levels.indices) {
                            try {
                                levels[currentLevelIndex].dir.deleteDirectory()
                                levels.removeAt(currentLevelIndex)
                                closeCurrentPopup()
                            } catch (e: IOException) {
                                Log.error(e) { "Une erreur est survenue lors de la suppression de ${levels[currentLevelIndex]} !" }
                            }
                        }
                    }
                }, levels.isEmpty(), searchBar = true)
                popItemFlag()

                if (openCopyPopup)
                    openPopup(copyLevelTitle)

                pushItemFlag(ItemFlags.Disabled.i, levels.isEmpty())
                if (button(MenusText.MM_SELECT_LEVEL_PLAY_BUTTON(), Vec2(-1, 0))) {
                    if (currentLevelIndex in levels.indices) {
                        val level = Level.loadFromFile(levels[currentLevelIndex].dir)
                        if (level != null)
                            PCGame.sceneManager.loadScene(GameScene(level))
                        else
                            openPopup(errorInLevelTitle)
                    }
                }

                if (button(MenusText.MM_SELECT_LEVEL_EDIT_BUTTON(), Vec2(-1, 0))) {
                    if (currentLevelIndex in levels.indices) {
                        val level = Level.loadFromFile(levels[currentLevelIndex].dir)
                        if (level != null)
                            PCGame.sceneManager.loadScene(EditorScene(level, false))
                        else
                            openPopup(errorInLevelTitle)
                    }
                }
                popItemFlag()

                separator()
                if (button(MenusText.MM_SELECT_LEVEL_NEW_BUTTON(), Vec2(-1, 0))) {
                    openPopup(newLevelTitle)
                    newLevelOpen[0] = true
                }

                functionalProgramming.popupModal(newLevelTitle, newLevelOpen, WindowFlags.NoResize.i or WindowFlags.NoCollapse.i) {
                    functionalProgramming.withItemWidth(Constants.defaultWidgetsWidth) {
                        ImGuiHelper.inputText(MenusText.MM_NAME(), ::newLevelNameBuf)
                    }
                    if (button(MenusText.MM_SELECT_LEVEL_NEW_LEVEL_CREATE(), Vec2(-1, 0))) {
                        if (newLevelNameBuf.isNotBlank()) {
                            val level = Level.newLevel(newLevelNameBuf)
                            PCGame.sceneManager.loadScene(EditorScene(level, false))
                        }
                    }
                }

                functionalProgramming.popup(copyLevelTitle) {
                    functionalProgramming.withItemWidth(Constants.defaultWidgetsWidth) {
                        ImGuiHelper.inputText(MenusText.MM_NAME(), ::copyLevelNameBuf)
                    }
                    if (button(MenusText.MM_SELECT_LEVEL_COPY_BUTTON(), Vec2(-1, 0))) {
                        if (copyLevelNameBuf.isNotBlank()) {
                            val levelDir = levels[currentLevelIndex].dir
                            val copyLevelDir = levelDir.parent().child(copyLevelNameBuf)
                            levelDir.list().forEach {
                                try {
                                    it.copyTo(copyLevelDir)
                                } catch (e: IOException) {
                                    Log.error(e) { "Une erreur est survenue lors de la copie de ${levels[currentLevelIndex]} !" }
                                }
                            }
                            levels.add(LevelItem(copyLevelDir))
                            closeCurrentPopup()
                        }
                    }
                }

                functionalProgramming.popupModal(errorInLevelTitle) {
                    text(MenusText.MM_ERROR_LEVEL_POPUP(levels[currentLevelIndex].toString()))
                    if (button(MenusText.MM_ERROR_LEVEL_CLOSE(), Vec2(-1, 0)))
                        closeCurrentPopup()
                }
            }
        }
    }

    private val settingsFullscreen = booleanArrayOf(Gdx.graphics.isFullscreen)
    private val settingsKeys = GameKeys.values().associate { it to false }.toMutableMap()
    private val settingsCurrentLocaleIndex = intArrayOf(PCGame.availableLocales.indexOf(PCGame.locale))

    private fun drawSettingsWindow() {
        with(ImGui) {
            ImGuiHelper.withCenteredWindow(MenusText.MM_SETTINGS_WINDOW_TITLE(), ::showSettingsWindow, Vec2(385f, 435f), WindowFlags.NoResize.i) {
                functionalProgramming.withGroup {
                    functionalProgramming.withItemWidth(Constants.defaultWidgetsWidth) {
                        sliderFloat(MenusText.MM_SETTINGS_SOUND(), ::soundVolume, 0f, 1f, "%.1f")

                        if (combo(MenusText.MM_SETTINGS_LOCALE(), settingsCurrentLocaleIndex, PCGame.availableLocales.map { it.displayLanguage }))
                            PCGame.locale = PCGame.availableLocales[settingsCurrentLocaleIndex[0]]

                        checkbox(MenusText.MM_SETTINGS_DARK_INTERFACE(), PCGame.Companion::darkUI)

                        checkbox(MenusText.MM_SETTINGS_FULLSCREEN(), settingsFullscreen)
                    }
                }

                separator()
                functionalProgramming.withChild("game keys", size = Vec2(360f, 265f)) {
                    settingsKeys.forEach { keyValue ->
                        text(keyValue.key.description)
                        sameLine()
                        cursorPosX = 260f
                        functionalProgramming.withId(keyValue.key.name) {
                            if (button(if (keyValue.value) MenusText.MM_SETTINGS_PRESSKEY() else Input.Keys.toString(keyValue.key.key), Vec2(75f, 0))) {
                                if (!keyValue.value) {
                                    settingsKeys.forEach {
                                        settingsKeys[it.key] = false
                                        PCInputProcessor.keyDownSignal.clear()
                                    }

                                    settingsKeys[keyValue.key] = true

                                    PCInputProcessor.keyDownSignal.register(true) {
                                        keyValue.key.key = it
                                        settingsKeys[keyValue.key] = false
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (Gdx.graphics.isFullscreen != settingsFullscreen[0]) {
                // Workaround lors du switch pour éviter à imgui de planter
                ImGui.render()

                if (settingsFullscreen[0])
                    Gdx.graphics.setFullscreenMode(Gdx.graphics.displayMode)
                else
                    Gdx.graphics.setWindowedMode(1280, 720)

                ImGui.newFrame()
            }
        }
    }
}