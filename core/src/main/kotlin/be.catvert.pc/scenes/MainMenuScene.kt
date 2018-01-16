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
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.kotcrab.vis.ui.widget.VisList
import com.kotcrab.vis.ui.widget.VisTextButton
import glm_.vec2.Vec2
import imgui.ImGui
import imgui.WindowFlags
import imgui.functionalProgramming
import ktx.actors.centerPosition
import ktx.actors.onClick
import ktx.actors.plus
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
                cell.width(250f).space(10f)
                onClick {
                    showSelectLevelWindow()
                }
            }
            row()
            textButton(MenusText.MM_SETTINGS_BUTTON()) { cell ->
                cell.width(250f).space(10f)
                onClick {
                    settingsKeys.forEach {
                        settingsKeys[it.key] = false
                        KeyDownSignalProcessor.keyDownSignal.clear()
                    }
                    showSettingsWindow = true
                }
            }
            row()
            textButton(MenusText.MM_EXIT_BUTTON()) { cell ->
                cell.width(250f).space(10f)
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
                            cell.width(250f).space(10f)
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

                    scrollPane(list) { cell -> cell.width(200f).height(300f).space(10f) }

                    table { cell ->
                        cell.size(200f)

                        textButton(MenusText.MM_SELECT_LEVEL_PLAY_BUTTON()) { cell ->
                            cell.width(200f).space(10f)

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
                            cell.width(200f).space(10f)
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
                            cell.width(200f).space(10f)

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
                            cell.width(200f).space(10f)

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
                        add(newLevelButton).width(200f).space(10f)
                    }
                }
            }.apply { centerPosition(this@MainMenuScene.stage.width, this@MainMenuScene.stage.height) }
        } else {
            stage + window(MenusText.MM_SELECT_LEVEL_WINDOW_TITLE()) {
                setSize(400f, 100f)
                isModal = true
                addCloseButton()
                table {
                    add(newLevelButton).width(400f).space(10f)
                }
            }.apply { centerPosition(this@MainMenuScene.stage.width, this@MainMenuScene.stage.height) }
        }
    }

    private var showSettingsWindow = false
    private fun drawUI() {
        with(ImGui) {
            if (showSettingsWindow)
                drawSettingsWindow()
            stage.actors.forEach { it.touchable = if (showSettingsWindow) Touchable.disabled else Touchable.enabled }
        }
    }

    private val settingsWindowSize = intArrayOf(Gdx.graphics.width, Gdx.graphics.height)
    private val settingsFullscreen = booleanArrayOf(Gdx.graphics.isFullscreen)
    private val settingsKeys = GameKeys.values().associate { it to false }.toMutableMap()
    private val settingsCurrentLocaleIndex = intArrayOf(PCGame.availableLocales.indexOf(PCGame.locale))
    private fun drawSettingsWindow() {
        with(ImGui) {
            ImguiHelper.withCenteredWindow(MenusText.MM_SETTINGS_WINDOW_TITLE(), ::showSettingsWindow, Vec2(375f, 375f), WindowFlags.NoResize.i) {
                functionalProgramming.withGroup {
                    functionalProgramming.withItemWidth(100f) {
                        sliderFloat(MenusText.MM_SETTINGS_SOUND(), ::soundVolume, 0f, 1f, "%.1f")

                        if (combo(MenusText.MM_SETTINGS_LOCALE(), settingsCurrentLocaleIndex, PCGame.availableLocales.map { it.displayLanguage }))
                            PCGame.locale = PCGame.availableLocales[settingsCurrentLocaleIndex[0]]
                        if (isMouseHoveringRect(itemRectMin, itemRectMax)) {
                            functionalProgramming.withTooltip {
                                text("Un redémarrage du jeu est requis\npour appliquer les changements !")
                            }
                        }

                        checkbox(MenusText.MM_SETTINGS_DARK_INTERFACE(), PCGame.Companion::darkUI)
                    }
                }
                sameLine()
                functionalProgramming.withGroup {
                    functionalProgramming.withItemWidth(-1) {
                        checkbox(MenusText.MM_SETTINGS_FULLSCREEN(), settingsFullscreen)
                    }
                    if (!settingsFullscreen[0]) {
                        functionalProgramming.withItemWidth(75f) {
                            inputInt2(MenusText.MM_SETTINGS_SCREEN_SIZE(), settingsWindowSize)
                        }
                    }
                    if (button(MenusText.MM_SETTINGS_APPLY(), Vec2(-1, 0))) {
                        if (settingsFullscreen[0])
                            Gdx.graphics.setFullscreenMode(Gdx.graphics.displayMode)
                        else
                            Gdx.graphics.setWindowedMode(settingsWindowSize[0], settingsWindowSize[1])
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