package be.catvert.pc.scenes

import be.catvert.pc.GameKeys
import be.catvert.pc.Log
import be.catvert.pc.PCGame
import be.catvert.pc.PCGame.Companion.soundVolume
import be.catvert.pc.containers.Level
import be.catvert.pc.i18n.MenusText
import be.catvert.pc.utility.Constants
import be.catvert.pc.utility.ImguiHelper
import be.catvert.pc.utility.KeyDownSignalProcessor
import be.catvert.pc.utility.Size
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.utils.Align
import com.kotcrab.vis.ui.widget.VisList
import com.kotcrab.vis.ui.widget.VisTextButton
import glm_.vec2.Vec2
import imgui.ImGui
import imgui.WindowFlags
import imgui.functionalProgramming
import ktx.actors.*
import ktx.collections.toGdxArray
import ktx.vis.table
import ktx.vis.window
import java.io.IOException
import kotlin.collections.set


/**
 * Scène du menu principal
 */
class MainMenuScene : Scene(PCGame.mainBackground) {
    private val logo = PCGame.generateLogo(gameObjectContainer)

    private class LevelItem(val dir: FileHandle) {
        override fun toString(): String = dir.name()
    }

    private val levels = Constants.levelDirPath.list { dir -> dir.isDirectory && dir.list { _, s -> s == Constants.levelDataFile }.isNotEmpty() }.map { LevelItem(it) }.toMutableList()

    init {
        showMainMenu()
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
            textButton(MenusText.MM_PLAY_BUTTON()) { cell ->
                cell.minWidth(250f).space(10f)
                onClick {
                    showSelectLevelWindow()
                }
            }
            row()
            textButton(MenusText.MM_SETTINGS_BUTTON()) { cell ->
                cell.minWidth(250f).space(10f)
                onClick {
                    settingsKeys.forEach {
                        settingsKeys[it.key] = false
                        KeyDownSignalProcessor.keyDownSignal.clear()
                    }
                    showSettingsWindow()
                }
            }
            row()
            textButton(MenusText.MM_EXIT_BUTTON()) { cell ->
                cell.minWidth(250f).space(10f)
                onClick {
                    Gdx.app.exit()
                }
            }
        }.apply { centerPosition(this@MainMenuScene.stage.width, this@MainMenuScene.stage.height) }
    }

    private fun showSelectLevelWindow() {
        fun showErrorLevelDialog(level: LevelItem) {
            stage + window("Erreur !") {
                isModal = true
                setSize(600f, 100f)

                label("Le niveau $level est invalide ! :(")
            }.apply { centerPosition(this@MainMenuScene.stage.width, this@MainMenuScene.stage.height) }
        }

        val newLevelButton = VisTextButton(MenusText.MM_SELECT_LEVEL_NEW_BUTTON()).apply {
            onClick {
                stage + window("Nouveau niveau") {
                    setSize(300f, 150f)
                    isModal = true
                    addCloseButton()
                    table {
                        val newNameField = textField { cell ->
                            cell.minWidth(250f).space(10f)
                            messageText = "Nom du niveau"
                        }
                        row()

                        textButton("Créer") { cell ->
                            cell.width(250f).space(10f)
                            onClick {
                                if (!newNameField.text.isBlank()) {
                                    val level = Level.newLevel(newNameField.text)
                                    PCGame.sceneManager.loadScene(EditorScene(level))
                                }
                            }
                        }
                    }
                }.apply { centerPosition(this@MainMenuScene.stage.width, this@MainMenuScene.stage.height) }
            }
        }

        if (levels.isNotEmpty()) {
            stage + window(MenusText.MM_SELECT_LEVEL_WINDOW_TITLE()) {
                setSize(450f, 400f)
                isModal = true
                addCloseButton()

                table {
                    val list = VisList<LevelItem>().apply { setItems(levels.toGdxArray()) }

                    scrollPane(list) { cell -> cell.minWidth(200f).height(300f).space(10f) }

                    table { cell ->
                        cell.size(200f)

                        textButton(MenusText.MM_SELECT_LEVEL_PLAY_BUTTON()) { cell ->
                            cell.minWidth(200f).space(10f)

                            onClick {
                                if (list.selectedIndex in levels.indices) {
                                    val level = Level.loadFromFile(levels[list.selectedIndex].dir)
                                    if (level != null)
                                        PCGame.sceneManager.loadScene(GameScene(level))
                                    else
                                        showErrorLevelDialog(levels[list.selectedIndex])
                                }
                            }
                        }

                        row()
                        textButton(MenusText.MM_SELECT_LEVEL_EDIT_BUTTON()) { cell ->
                            cell.minWidth(200f).space(10f)
                            onClick {
                                if (list.selectedIndex in levels.indices) {
                                    val level = Level.loadFromFile(levels[list.selectedIndex].dir)
                                    if (level != null)
                                        PCGame.sceneManager.loadScene(EditorScene(level))
                                    else
                                        showErrorLevelDialog(levels[list.selectedIndex])
                                }
                            }
                        }

                        row()
                        textButton(MenusText.MM_SELECT_LEVEL_COPY_BUTTON()) { cell ->
                            cell.minWidth(200f).space(10f)

                            onClick {
                                if (list.selectedIndex in levels.indices) {
                                    stage + window("Copier un niveau") {
                                        setSize(350f, 150f)
                                        isModal = true
                                        addCloseButton()

                                        table {
                                            val newNameField = textField { cell -> cell.width(250f).space(10f); messageText = "Nouveau nom" }

                                            row()

                                            textButton("Copier") { cell ->
                                                cell.width(250f).space(10f)
                                                onClick {
                                                    if (!newNameField.text.isBlank()) {
                                                        val levelDir = levels[list.selectedIndex].dir
                                                        val copyLevelDir = levelDir.parent().child(newNameField.text)
                                                        levelDir.list().forEach {
                                                            try {
                                                                it.copyTo(copyLevelDir)
                                                            } catch (e: IOException) {
                                                                Log.error(e) { "Une erreur est survenue lors de la copie de ${levels[list.selectedIndex]} !" }
                                                            }
                                                        }
                                                        levels.add(LevelItem(copyLevelDir))
                                                        list.setItems(levels.toGdxArray())
                                                        this@window.remove()
                                                    }
                                                }
                                            }
                                        }
                                    }.apply { centerPosition(this@MainMenuScene.stage.width, this@MainMenuScene.stage.height) }
                                }
                            }
                        }

                        row()
                        textButton(MenusText.MM_SELECT_LEVEL_DELETE_BUTTON()) { cell ->
                            cell.minWidth(200f).space(10f)

                            onClick {
                                if (list.selectedIndex in levels.indices) {
                                    stage + window("Supprimer ${levels[list.selectedIndex]} ?") deleteWindow@ {
                                        setSize(300f, 100f)
                                        isModal = true
                                        addCloseButton()
                                        table {
                                            textButton("Supprimer") { cell ->
                                                cell.width(250f)
                                                onClick {
                                                    try {
                                                        levels[list.selectedIndex].dir.deleteDirectory()
                                                        levels.removeAt(list.selectedIndex)
                                                        list.setItems(levels.toGdxArray())
                                                        this@deleteWindow.remove()

                                                        if (levels.isEmpty()) {
                                                            this@window.remove()
                                                            showSelectLevelWindow()
                                                        }
                                                    } catch (e: IOException) {
                                                        Log.error(e) { "Une erreur est survenue lors de la suppression de ${levels[list.selectedIndex]} !" }
                                                    }
                                                }
                                            }
                                        }
                                    }.apply { centerPosition(this@MainMenuScene.stage.width, this@MainMenuScene.stage.height) }
                                }
                            }
                        }
                        row()
                        add(newLevelButton).minWidth(200f).space(10f)
                    }
                }
            }.apply { centerPosition(this@MainMenuScene.stage.width, this@MainMenuScene.stage.height) }
        } else {
            stage + window(MenusText.MM_SELECT_LEVEL_WINDOW_TITLE()) {
                setSize(400f, 100f)
                isModal = true
                addCloseButton()
                table {
                    add(newLevelButton).minWidth(400f).space(10f)
                }
            }.apply { centerPosition(this@MainMenuScene.stage.width, this@MainMenuScene.stage.height) }
        }
    }

    private fun showSettingsWindow() {
        stage + window("Paramètres du jeu") {
            setSize(700f, 300f)
            isModal = true
            addCloseButton()

            table {
                table { cell ->
                    cell.size(300f, 300f)
                    checkBox(MenusText.MM_SETTINGS_FULLSCREEN()) { cell ->
                        align(Align.left)
                        cell.width(300f).padLeft(50f)
                    }

                    row()
                    checkBox(MenusText.MM_SETTINGS_DARK_INTERFACE()) { cell ->
                        align(Align.left)
                        cell.width(300f).padLeft(50f)
                        onChange {
                            PCGame.darkUI = this.isChecked
                        }
                    }

                    row()
                    horizontalGroup {
                        space(10f)
                        label(MenusText.MM_SETTINGS_SOUND())
                        slider(0f, 1f, 0.1f) {
                            value = PCGame.soundVolume
                            onChange {
                                PCGame.soundVolume = value
                            }
                        }
                    }

                    row()
                    selectBox<String> { cell ->
                        cell.width(200f)
                        items = PCGame.availableLocales.map { it.displayName }.toGdxArray()
                        onChange {
                            PCGame.locale = PCGame.availableLocales[this.selectedIndex]
                        }
                    }
                }
                scrollPane(table {
                    settingsKeys.forEach {
                        label(it.key.description).setFontScale(0.5f)

                        textField(Input.Keys.toString(it.key.key)) { cell ->
                            cell.width(100f).space(10f)
                            onKeyUp {
                                text = Input.Keys.toString(it)
                            }

                            isReadOnly = true
                            setAlignment(Align.center)
                        }
                        row()
                    }
                }) { cell ->
                    cell.spaceTop(50f).size(380f, 250f)
                }
            }
        }.apply { centerPosition(this@MainMenuScene.stage.width, this@MainMenuScene.stage.height) }
    }

    private var showSettingsWindow = false
    private fun drawUI() {
        with(ImGui) {
            if (showSettingsWindow)
                drawSettingsWindow()
            stage.actors.forEach { it.touchable = if (showSettingsWindow) Touchable.disabled else Touchable.enabled }
        }
    }

    private var settingsWindowCurrentDisplayMode = intArrayOf(Gdx.graphics.displayModes.indexOfFirst { it.width == Gdx.graphics.displayMode.width && it.height == Gdx.graphics.displayMode.height && it.refreshRate == Gdx.graphics.displayMode.refreshRate })
    private val settingsFullscreen = booleanArrayOf(Gdx.graphics.isFullscreen)
    private val settingsKeys = GameKeys.values().associate { it to false }.toMutableMap()
    private val settingsCurrentLocaleIndex = intArrayOf(PCGame.availableLocales.indexOf(PCGame.locale))
    private fun drawSettingsWindow() {
        with(ImGui) {
            ImguiHelper.withCenteredWindow(MenusText.MM_SETTINGS_WINDOW_TITLE(), ::showSettingsWindow, Vec2(370f, 375f), WindowFlags.NoResize.i) {
                functionalProgramming.withGroup {
                    checkbox(MenusText.MM_SETTINGS_FULLSCREEN(), settingsFullscreen)
                    functionalProgramming.withItemWidth(100f) {
                        combo("résolution", settingsWindowCurrentDisplayMode, Lwjgl3ApplicationConfiguration.getDisplayModes().map { "${it.width}x${it.height}x${it.refreshRate}" })
                    }
                    if (button(MenusText.MM_SETTINGS_APPLY(), Vec2(100f, 0))) {
                        val mode = Gdx.graphics.displayModes.elementAtOrNull(settingsWindowCurrentDisplayMode[0])
                        if (mode != null) {
                            if (settingsFullscreen[0])
                                Gdx.graphics.setFullscreenMode(mode)
                            else
                                Gdx.graphics.setWindowedMode(mode.width, mode.height)
                        }
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
                                        KeyDownSignalProcessor.keyDownSignal.clear()
                                    }

                                    settingsKeys[keyValue.key] = true

                                    KeyDownSignalProcessor.keyDownSignal.register(true) {
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