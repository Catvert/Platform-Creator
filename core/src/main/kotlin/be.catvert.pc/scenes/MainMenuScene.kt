package be.catvert.pc.scenes

import be.catvert.pc.GameKeys
import be.catvert.pc.Log
import be.catvert.pc.PCGame
import be.catvert.pc.PCGame.Companion.soundVolume
import be.catvert.pc.eca.containers.Level
import be.catvert.pc.i18n.MenusText
import be.catvert.pc.managers.MusicManager
import be.catvert.pc.managers.ResourceManager
import be.catvert.pc.ui.ImGuiHelper
import be.catvert.pc.utility.Constants
import be.catvert.pc.utility.PCInputProcessor
import be.catvert.pc.utility.Size
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.g2d.Batch
import glm_.vec2.Vec2
import imgui.Cond
import imgui.ImGui
import imgui.WindowFlags
import imgui.functionalProgramming
import imgui.internal.LayoutType
import java.io.IOException
import kotlin.collections.set


/**
 * Scène du menu principal
 */
class MainMenuScene(applyMusicTransition: Boolean) : Scene(PCGame.mainBackground) {
    private val logo = PCGame.generateLogo(entityContainer)

    private class LevelItem(val dir: FileHandle) {
        override fun toString(): String = dir.name()
    }

    private val levels = Constants.levelDirPath.list { dir -> dir.isDirectory && dir.list { _, s -> s == Constants.levelDataFile }.isNotEmpty() }.map { LevelItem(it) }.toMutableList()

    init {
        if (applyMusicTransition)
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

    private val newLevelTitle = "Nouveau niveau"
    private var newLevelNameBuf = ""
    private var newLevelOpen = booleanArrayOf(true)

    private val copyLevelTitle = "Copier un niveau"
    private var copyLevelNameBuf = ""
    private val copyLevelOpen = booleanArrayOf(true)

    private var errorInLevelTitle = "Impossible de charger le niveau !"

    private var levelPopupTitle = "level popup"
    private lateinit var levelPopupItem: LevelItem
    private val levelPopupOpen = booleanArrayOf(true)

    private val settingsLevelTitle = "settings level"

    private fun drawSelectLevelWindow() {
        with(ImGui) {
            ImGuiHelper.withCenteredWindow(MenusText.MM_SELECT_LEVEL_WINDOW_TITLE(), ::showSelectLevelWindow, Vec2(500f, 200f), WindowFlags.NoResize.i or WindowFlags.NoCollapse.i) {
                functionalProgramming.withChild("levels", Vec2(500, 120)) {
                    levels.forEachIndexed { index, it ->
                        val preview = it.dir.child(Constants.levelPreviewFile)
                        if (imageButton(ResourceManager.getTexture(preview).textureObjectHandle, Vec2(160, 90), uv1 = Vec2(1))) {
                            levelPopupItem = it
                            levelPopupOpen[0] = true
                            levelPopupTitle = "Niveau $it"
                            openPopup(levelPopupTitle)
                        }
                        if (isItemHovered()) {
                            functionalProgramming.withTooltip {
                                text(it.dir.name())
                            }
                        }

                        if (index + 1 < levels.count())
                            sameLine()
                    }

                    functionalProgramming.popupModal(levelPopupTitle, levelPopupOpen) {
                        val preview = levelPopupItem.dir.child(Constants.levelPreviewFile)
                        image(ResourceManager.getTexture(preview).textureObjectHandle, Vec2(160, 90), uv1 = Vec2(1))
                        separator()
                        if (button(MenusText.MM_SELECT_LEVEL_PLAY_BUTTON(), Vec2(-1, 0))) {
                            val level = Level.loadFromFile(levelPopupItem.dir)
                            if (level != null)
                                PCGame.sceneManager.loadScene(GameScene(level))
                            else
                                openPopup(errorInLevelTitle)
                        }

                        if (button(MenusText.MM_SELECT_LEVEL_EDIT_BUTTON(), Vec2(-1, 0))) {
                            val level = Level.loadFromFile(levelPopupItem.dir)
                            if (level != null)
                                PCGame.sceneManager.loadScene(EditorScene(level, false))
                            else
                                openPopup(errorInLevelTitle)
                        }

                        if (button("...", Vec2(-1, 0))) {
                            openPopup(settingsLevelTitle)
                        }

                        var openCopyPopup = false
                        functionalProgramming.popup(settingsLevelTitle) {
                            if (button(MenusText.MM_SELECT_LEVEL_COPY_BUTTON(), Vec2(Constants.defaultWidgetsWidth, 0))) {
                                openCopyPopup = true
                            }

                            if (button(MenusText.MM_SELECT_LEVEL_DELETE_BUTTON(), Vec2(Constants.defaultWidgetsWidth, 0))) {
                                try {
                                    levelPopupItem.dir.deleteDirectory()
                                    levels.remove(levelPopupItem)
                                    closeCurrentPopup()
                                } catch (e: IOException) {
                                    Log.error(e) { "Une erreur est survenue lors de la suppression de $levelPopupItem !" }
                                }
                            }
                        }

                        if (openCopyPopup) {
                            openPopup(copyLevelTitle)
                            copyLevelOpen[0] = true
                        }

                        functionalProgramming.popupModal(copyLevelTitle, copyLevelOpen) {
                            functionalProgramming.withItemWidth(Constants.defaultWidgetsWidth) {
                                ImGuiHelper.inputText(MenusText.MM_NAME(), ::copyLevelNameBuf)
                            }
                            if (button(MenusText.MM_SELECT_LEVEL_COPY_BUTTON(), Vec2(-1, 0))) {
                                if (copyLevelNameBuf.isNotBlank()) {
                                    val copyLevelDir = levelPopupItem.dir.parent().child(copyLevelNameBuf)
                                    levelPopupItem.dir.list().forEach {
                                        try {
                                            it.copyTo(copyLevelDir)
                                        } catch (e: IOException) {
                                            Log.error(e) { "Une erreur est survenue lors de la copie de $levelPopupItem !" }
                                        }
                                    }
                                    levels.add(LevelItem(copyLevelDir))
                                    closeCurrentPopup()
                                }
                            }
                        }

                        functionalProgramming.popupModal(errorInLevelTitle) {
                            text(MenusText.MM_ERROR_LEVEL_POPUP(levelPopupItem.toString()))
                            if (button(MenusText.MM_ERROR_LEVEL_CLOSE(), Vec2(-1, 0)))
                                closeCurrentPopup()
                        }
                    }

                    scrollbar(LayoutType.Horizontal)
                }

                if (button("Nouveau", Vec2(-1, 0))) {
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
            }
        }
    }

    private val settingsFullscreen = booleanArrayOf(Gdx.graphics.isFullscreen)
    private val settingsKeys = GameKeys.values().associate { it to false }.toMutableMap()
    private val settingsCurrentLocaleIndex = intArrayOf(PCGame.availableLocales.indexOf(PCGame.locale))

    private fun drawSettingsWindow() {
        with(ImGui) {
            ImGuiHelper.withCenteredWindow(MenusText.MM_SETTINGS_WINDOW_TITLE(), ::showSettingsWindow, Vec2(435f, 440f), WindowFlags.NoResize.i) {
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
                functionalProgramming.withChild("game keys", size = Vec2(420f, 265f)) {
                    settingsKeys.forEach { keyValue ->
                        text(keyValue.key.description)
                        sameLine()
                        cursorPosX = 320f
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