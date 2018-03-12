package be.catvert.pc.scenes

import be.catvert.pc.GameKeys
import be.catvert.pc.Log
import be.catvert.pc.PCGame
import be.catvert.pc.PCGame.Companion.soundVolume
import be.catvert.pc.eca.containers.Level
import be.catvert.pc.i18n.MenusText
import be.catvert.pc.managers.MusicsManager
import be.catvert.pc.managers.ResourcesManager
import be.catvert.pc.ui.ImGuiHelper
import be.catvert.pc.utility.*
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.utils.JsonReader
import com.badlogic.gdx.utils.JsonWriter
import glm_.vec2.Vec2
import imgui.*
import java.io.FileReader
import java.io.FileWriter
import java.io.IOException
import java.util.zip.ZipOutputStream
import kotlin.collections.set


/**
 * Scène du menu principal
 */
class MainMenuScene(applyMusicTransition: Boolean) : Scene(StandardBackground(Constants.gameBackgroundMenuPath.toFileWrapper())) {
    private val logo = PCGame.generateLogo(entityContainer)

    private class LevelItem(val dir: FileHandle) {
        override fun toString(): String = dir.name()
    }

    private lateinit var levels: List<LevelItem>

    init {
        if (applyMusicTransition)
            MusicsManager.startMusic(Constants.menuMusicPath, true)

        refreshLevels()
    }

    override fun resize(size: Size) {
        super.resize(size)
        logo.box.set(PCGame.getLogoRect())
    }

    override fun render(batch: Batch) {
        super.render(batch)
        drawUI()
    }

    private fun refreshLevels() {
        levels = Constants.levelDirPath.list { dir -> dir.isDirectory && dir.list { _, s -> s == Constants.levelDataFile }.isNotEmpty() }.map { LevelItem(it) }
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

    private var levelItem: LevelItem? = null

    private val settingsLevelTitle = "settings level"
    private lateinit var settingsLevelItem: LevelItem

    private val selectLevelTitle = "Sélection d'un niveau##list"
    private val selectLevelOpen = booleanArrayOf(true)
    private var selectLevelSearchBuf = ""

    private fun drawSelectLevelWindow() {
        with(ImGui) {
            ImGuiHelper.withCenteredWindow(MenusText.MM_SELECT_LEVEL_WINDOW_TITLE(), ::showSelectLevelWindow, Vec2(225f, 285f), WindowFlags.NoResize.i or WindowFlags.NoCollapse.i) {
                functionalProgramming.withChild("level panel", Vec2(210f, 205f)) {
                    val titleText = if (levelItem == null) "Aucun niveau sélectionné" else levelItem.toString()
                    ImGuiHelper.centeredTextPropertyColored(200f, Color.ORANGE, if (levelItem == null) "" else "Niveau : ", titleText)

                    if (imageButton(if (levelItem == null) ResourcesManager.defaultTexture.textureObjectHandle else ResourcesManager.getTexture(levelItem!!.dir.child(Constants.levelPreviewFile)).textureObjectHandle, Vec2(200f, 112.5f), uv1 = Vec2(1))) {
                        openPopup(selectLevelTitle)
                        selectLevelOpen[0] = true
                    }

                    if (isItemHovered()) {
                        functionalProgramming.withTooltip {
                            text("Cliquer pour sélectionner un niveau")
                        }
                    }

                    pushItemFlag(ItemFlags.Disabled.i, levelItem == null)

                    if (button(MenusText.MM_SELECT_LEVEL_PLAY_BUTTON(), Vec2(-1, 0))) {
                        val level = Level.loadFromFile(levelItem!!.dir)
                        if (level != null)
                            PCGame.scenesManager.loadScene(GameScene(level))
                        else
                            openPopup(errorInLevelTitle)
                    }

                    if (button(MenusText.MM_SELECT_LEVEL_EDIT_BUTTON(), Vec2(-1, 0))) {
                        val level = Level.loadFromFile(levelItem!!.dir)
                        if (level != null)
                            PCGame.scenesManager.loadScene(EditorScene(level, false))
                        else
                            openPopup(errorInLevelTitle)
                    }

                    popItemFlag()

                    setNextWindowSize(Vec2(205f, if (levels.isEmpty()) 70f else 350f), Cond.Always)
                    functionalProgramming.popupModal(selectLevelTitle, selectLevelOpen, WindowFlags.NoResize.i) {
                        functionalProgramming.withIndent(10f) {
                            if (levels.isNotEmpty()) {
                                ImGuiHelper.inputText("", ::selectLevelSearchBuf, 168f)

                                levels.filter { it.toString().startsWith(selectLevelSearchBuf) }.forEach {
                                    val preview = it.dir.child(Constants.levelPreviewFile)
                                    if (imageButton(ResourcesManager.getTexture(preview).textureObjectHandle, Vec2(160, 90), uv1 = Vec2(1))) {
                                        levelItem = it
                                        closeCurrentPopup()
                                    }
                                    if (isItemHovered()) {
                                        functionalProgramming.withTooltip {
                                            text(it.dir.name())
                                        }

                                        if (Gdx.input.isButtonPressed(Input.Buttons.RIGHT)) {
                                            openPopup(settingsLevelTitle)
                                            settingsLevelItem = it
                                        }
                                    }
                                }
                            } else {
                                ImGuiHelper.centeredTextColored(170f, Color.FIREBRICK, "Aucun niveau disponible")
                            }
                        }

                        var openCopyPopup = false
                        functionalProgramming.popup(settingsLevelTitle) {
                            if (button("Exporter", Vec2(Constants.defaultWidgetsWidth, 0))) {
                                try {
                                    val file = Utility.saveFileDialog("Exporter le niveau $settingsLevelItem", "Niveau", Constants.exportLevelExtension)
                                    if (file != null) {
                                        val outStream = file.write(false)
                                        val zipOutputStream = ZipOutputStream(outStream)
                                        Utility.zipFile(settingsLevelItem.dir, settingsLevelItem.dir.name(), zipOutputStream)
                                        zipOutputStream.close()
                                        outStream.close()

                                        closeCurrentPopup()
                                    }
                                } catch (e: Exception) {
                                    Log.error(e) { "Une erreur est survenue lors de l'exportation du niveau $settingsLevelItem !" }
                                }
                            }

                            separator()

                            if (button(MenusText.MM_SELECT_LEVEL_COPY_BUTTON(), Vec2(Constants.defaultWidgetsWidth, 0))) {
                                openCopyPopup = true
                            }

                            if (button(MenusText.MM_SELECT_LEVEL_DELETE_BUTTON(), Vec2(Constants.defaultWidgetsWidth, 0))) {
                                try {
                                    settingsLevelItem.dir.deleteDirectory()
                                    if (levelItem === settingsLevelItem)
                                        levelItem = null
                                    refreshLevels()
                                    closeCurrentPopup()
                                } catch (e: IOException) {
                                    Log.error(e) { "Une erreur est survenue lors de la suppression de $settingsLevelItem !" }
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
                                    val copyLevelDir = settingsLevelItem.dir.parent().child(copyLevelNameBuf)
                                    if (!copyLevelDir.exists()) {
                                        settingsLevelItem.dir.list().forEach {
                                            try {
                                                it.copyTo(copyLevelDir)
                                            } catch (e: IOException) {
                                                Log.error(e) { "Une erreur est survenue lors de la copie de $settingsLevelItem !" }
                                            }
                                        }

                                        // On a besoin de modifier le levelPath pour être sûr qu'il pointe vers le nouveau niveau
                                        val copyLevelFile = copyLevelDir.child(Constants.levelDataFile)

                                        val fr = FileReader(copyLevelFile.path())
                                        val levelRoot = JsonReader().parse(fr)
                                        fr.close()

                                        levelRoot[Level::levelPath::name.get()].set(copyLevelFile.path())

                                        val fw = FileWriter(copyLevelFile.path())
                                        fw.write(levelRoot.toJson(JsonWriter.OutputType.json))
                                        fw.close()

                                        refreshLevels()

                                        closeCurrentPopup()
                                    } else {
                                        Log.warn { "Le niveau $settingsLevelItem existe déjà !" }
                                    }
                                }
                            }
                        }
                    }


                    functionalProgramming.popupModal(errorInLevelTitle) {
                        text(MenusText.MM_ERROR_LEVEL_POPUP(levelItem.toString()))
                        if (button(MenusText.MM_ERROR_LEVEL_CLOSE(), Vec2(-1, 0)))
                            closeCurrentPopup()
                    }
                }

                separator()

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
                            PCGame.scenesManager.loadScene(EditorScene(level, false))
                        }
                    }

                    separator()

                    if (button("Importer ..", Vec2(-1, 0))) {
                        try {
                            Utility.openFileDialog("Importer un niveau..", "Niveau", Constants.exportLevelExtension, false).firstOrNull()?.apply {
                                Utility.unzipFile(this, Constants.levelDirPath)

                                refreshLevels()

                                closeCurrentPopup()
                            }
                        } catch (e: Exception) {
                            Log.error(e) { "Une erreur est survenue lors de l'importation du niveau !" }
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