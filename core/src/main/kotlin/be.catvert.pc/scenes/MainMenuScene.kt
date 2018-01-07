package be.catvert.pc.scenes

import be.catvert.pc.GameKeys
import be.catvert.pc.Log
import be.catvert.pc.PCGame
import be.catvert.pc.PCGame.Companion.darkUI
import be.catvert.pc.PCGame.Companion.soundVolume
import be.catvert.pc.containers.Level
import be.catvert.pc.i18n.MenusText
import be.catvert.pc.utility.Constants
import be.catvert.pc.utility.ImguiHelper
import be.catvert.pc.utility.Signal
import be.catvert.pc.utility.Size
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import glm_.vec2.Vec2
import imgui.ImGui
import imgui.ItemFlags
import imgui.WindowFlags
import imgui.functionalProgramming
import ktx.actors.centerPosition
import ktx.actors.onClick
import ktx.actors.plus
import ktx.app.use
import ktx.scene2d.*
import java.io.IOException
import kotlin.collections.associate
import kotlin.collections.forEach
import kotlin.collections.indices
import kotlin.collections.isNotEmpty
import kotlin.collections.map
import kotlin.collections.set
import kotlin.collections.toMutableList
import kotlin.collections.toMutableMap


/**
 * Scène du menu principal
 */
class MainMenuScene : Scene(PCGame.mainBackground) {
    private val glyphCreatedBy = GlyphLayout(PCGame.mainFont, "par Catvert - ${Constants.gameVersion}")

    private val logo = PCGame.generateLogo(gameObjectContainer)

    private val settingsKeyProcessor = object : InputProcessor {
        val keyDownSignal = Signal<Int>()

        override fun mouseMoved(screenX: Int, screenY: Int) = false
        override fun keyTyped(character: Char) = false
        override fun scrolled(amount: Int) = false
        override fun keyUp(keycode: Int) = false
        override fun touchDragged(screenX: Int, screenY: Int, pointer: Int) = false
        override fun keyDown(keycode: Int): Boolean {
            keyDownSignal(keycode)
            return false
        }

        override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int) = false
        override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int) = false
    }

    init {
        Gdx.input.inputProcessor = InputMultiplexer(settingsKeyProcessor, stage)
        showMainMenu()
    }

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

    private fun showMainMenu() {
        stage + table {
            add(TextButton(MenusText.MM_PLAY_BUTTON(), Scene2DSkin.defaultSkin).apply {
                onClick {
                    showSelectLevelWindow = true
                }
            }).minWidth(250f).space(10f)
            row()
            add(TextButton(MenusText.MM_SETTINGS_BUTTON(), Scene2DSkin.defaultSkin).apply {
                onClick {
                    settingsKeys.forEach {
                        settingsKeys[it.key] = false
                        settingsKeyProcessor.keyDownSignal.clear()
                    }
                    showSettingsWindow = true
                }
            }).minWidth(250f).space(10f)
            row()
            add(TextButton(MenusText.MM_EXIT_BUTTON(), Scene2DSkin.defaultSkin).apply {
                onClick {
                    Gdx.app.exit()
                }
            }).minWidth(250f).space(10f)
        }.apply { centerPosition(this@MainMenuScene.stage.width, this@MainMenuScene.stage.height) }
    }

    private var showSelectLevelWindow = false
    private var showSettingsWindow = false
    private fun drawUI() {
        with(ImGui) {
            if (showSelectLevelWindow)
                drawSelectLevelWindow()
            if (showSettingsWindow)
                drawSettingsWindow()
            stage.actors.forEach { it.touchable = if(showSelectLevelWindow || showSettingsWindow) Touchable.disabled else Touchable.enabled }
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
            ImguiHelper.withCenteredWindow(MenusText.MM_SELECT_LEVEL_WINDOW_TITLE(), ::showSelectLevelWindow, Vec2(160f, 180f), WindowFlags.NoResize.i) {
                pushItemFlag(ItemFlags.Disabled.i, levels.isEmpty())

                functionalProgramming.withItemWidth(100f) {
                    combo(MenusText.MM_SELECT_LEVEL_LEVEL_COMBO(), ::currentLevel, levels.map { it.toString() })
                }

                if (button(MenusText.MM_SELECT_LEVEL_PLAY_BUTTON(), Vec2(-1, 0f))) {
                    if (currentLevel in levels.indices) {
                        val level = Level.loadFromFile(levels[currentLevel].dir)
                        if (level != null)
                            PCGame.sceneManager.loadScene(GameScene(level))
                        else
                            openPopup(errorLevelTitle)
                    }
                }
                if (button(MenusText.MM_SELECT_LEVEL_EDIT_BUTTON(), Vec2(-1, 0f))) {
                    if (currentLevel in levels.indices) {
                        val level = Level.loadFromFile(levels[currentLevel].dir)
                        if (level != null)
                            PCGame.sceneManager.loadScene(EditorScene(level))
                        else
                            openPopup(errorLevelTitle)
                    }
                }
                if (button(MenusText.MM_SELECT_LEVEL_COPY_BUTTON(), Vec2(-1, 0f))) {
                    if (currentLevel in levels.indices)
                        openPopup(copyLevelTitle)
                }
                if (button(MenusText.MM_SELECT_LEVEL_DELETE_BUTTON(), Vec2(-1, 0f))) {
                    if (currentLevel in levels.indices)
                        openPopup(deleteLevelTitle)
                }
                separator()

                popItemFlag()

                if (button(MenusText.MM_SELECT_LEVEL_NEW_BUTTON(), Vec2(-1, 0))) {
                    openPopup(newLevelTitle)
                }

                functionalProgramming.popup(newLevelTitle) {
                    functionalProgramming.withItemWidth(100f) {
                        ImguiHelper.inputText(MenusText.MM_NAME(), ::newLevelName)
                    }
                    if (button(MenusText.MM_SELECT_LEVEL_NEW_LEVEL_CREATE(), Vec2(-1, 0))) {
                        val level = Level.newLevel(newLevelName)
                        PCGame.sceneManager.loadScene(EditorScene(level))
                        closeCurrentPopup()
                    }
                }

                functionalProgramming.popup(copyLevelTitle) {
                    functionalProgramming.withItemWidth(100f) {
                        ImguiHelper.inputText(MenusText.MM_NAME(), ::copyLevelName)
                    }

                    if (button(MenusText.MM_SELECT_LEVEL_COPY_BUTTON(), Vec2(-1, 0))) {
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
                    if (button(MenusText.MM_CONFIRM(), Vec2(100f, 0))) {
                        try {
                            if (currentLevel in levels.indices) {
                                levels[currentLevel].dir.deleteDirectory()
                                levels.removeAt(currentLevel)
                                closeCurrentPopup()
                            }
                        } catch (e: IOException) {
                            Log.error(e) { "Une erreur est survenue lors de la suppression du niveau !" }
                        }
                    }
                }

                functionalProgramming.popupModal(errorLevelTitle, extraFlags = WindowFlags.NoTitleBar.i or WindowFlags.NoResize.i) {
                    text(MenusText.MM_ERROR_LEVEL_POPUP())
                    if (button(MenusText.MM_ERROR_LEVEL_CLOSE(), Vec2(-1, 0)))
                        closeCurrentPopup()
                }
            }
        }
    }

    private var settingsWindowSize = intArrayOf(Gdx.graphics.width, Gdx.graphics.height)
    private val settingsFullscreen = booleanArrayOf(Gdx.graphics.isFullscreen)
    private val settingsKeys = GameKeys.values().associate { it to false }.toMutableMap()
    private val settingsCurrentLocaleIndex = intArrayOf(PCGame.availableLocales.indexOf(PCGame.locale))
    private fun drawSettingsWindow() {
        with(ImGui) {
            ImguiHelper.withCenteredWindow(MenusText.MM_SETTINGS_WINDOW_TITLE(), ::showSettingsWindow, Vec2(375f, 400f), WindowFlags.NoResize.i) {
                functionalProgramming.withGroup {
                    checkbox(MenusText.MM_SETTINGS_FULLSCREEN(), settingsFullscreen)
                    if (!settingsFullscreen[0]) {
                        functionalProgramming.withItemWidth(100f) {
                            inputInt2(MenusText.MM_SETTINGS_SCREEN_SIZE(), settingsWindowSize)
                        }
                    }
                    if (button(MenusText.MM_SETTINGS_APPLY(), Vec2(100f, 0))) {
                        if (settingsFullscreen[0])
                            Gdx.graphics.setFullscreenMode(Gdx.graphics.displayMode)
                        else
                            Gdx.graphics.setWindowedMode(settingsWindowSize[0], settingsWindowSize[1])
                    }
                    functionalProgramming.withItemWidth(100f) {
                        sliderFloat(MenusText.MM_SETTINGS_SOUND(), ::soundVolume, 0f, 1f, "%.1f")
                    }
                    checkbox(MenusText.MM_SETTINGS_DARK_INTERFACE(), PCGame.Companion::darkUI)
                    functionalProgramming.withItemWidth(100f) {
                        if (combo(MenusText.MM_SETTINGS_LOCALE(), settingsCurrentLocaleIndex, PCGame.availableLocales.map { it.displayLanguage }))
                            PCGame.locale = PCGame.availableLocales[settingsCurrentLocaleIndex[0]]
                    }
                }
                separator()
                functionalProgramming.withChild("game keys", size = Vec2(350f, 200f)) {
                    settingsKeys.forEach { keyValue ->
                        text(keyValue.key.description)
                        sameLine()
                        cursorPosX = 250f
                        functionalProgramming.withId(keyValue.key.name) {
                            if (button(if (keyValue.value) MenusText.MM_SETTINGS_PRESSKEY() else Input.Keys.toString(keyValue.key.key), Vec2(75f, 0))) {
                                if (!keyValue.value) {
                                    settingsKeys.forEach {
                                        settingsKeys[it.key] = false
                                        settingsKeyProcessor.keyDownSignal.clear()
                                    }

                                    settingsKeys[keyValue.key] = true

                                    settingsKeyProcessor.keyDownSignal.register(true) {
                                        keyValue.key.key = it
                                        settingsKeys[keyValue.key] = false
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    //endregion
}