package be.catvert.pc.scenes

import be.catvert.pc.GameKeys
import be.catvert.pc.Log
import be.catvert.pc.PCGame
import be.catvert.pc.containers.Level
import be.catvert.pc.i18n.MenusText
import be.catvert.pc.utility.*
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.kotcrab.vis.ui.widget.VisCheckBox
import com.kotcrab.vis.ui.widget.VisLabel
import com.kotcrab.vis.ui.widget.VisList
import com.kotcrab.vis.ui.widget.VisTextArea
import ktx.actors.onChange
import ktx.actors.onClick
import ktx.actors.onKeyUp
import ktx.actors.plus
import ktx.app.use
import ktx.collections.GdxArray
import ktx.collections.toGdxArray
import ktx.vis.window
import java.io.IOException


/**
 * Scène du menu principal
 */

class MainMenuScene : Scene() {
    private val glyphCreatedBy = GlyphLayout(PCGame.mainFont, "par Catvert - ${Constants.gameVersion}")

    private val logo = PCGame.generateLogo(gameObjectContainer)

    init {
        stage + window(MenusText.MM_WINDOW_TITLE()) {
            verticalGroup {
                space(10f)
                textButton(MenusText.MM_PLAY_BUTTON()) {
                    onClick {
                        showSelectLevelsWindow()
                    }
                }
                textButton(MenusText.MM_SETTINGS_BUTTON()) {
                    onClick {
                        showSettingsWindows()
                    }
                }
                textButton(MenusText.MM_EXIT_BUTTON()) {
                    onClick {
                        Gdx.app.exit()
                    }
                }
            }

            setPosition(Gdx.graphics.width / 2f - width / 2f, Gdx.graphics.height / 2f - height / 2f)
        }
        backgroundTexture = PCGame.assetManager.loadOnDemand<Texture>(Constants.gameBackgroundMenuPath).asset
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

    //region UI

    /**
     * Affiche une fenêtre pour sélectionner le nom du niveau
     * @param onNameSelected Méthode appelée quand l'utilisateur a correctement entré le nom du niveau
     */
    private fun showSetNameLevelWindow(onNameSelected: (name: String) -> Unit) {
        stage + window(MenusText.MM_SELECT_LEVEL_NAME_WINDOW_TITLE()) {
            isModal = true
            setSize(300f, 100f)
            setPosition(Gdx.graphics.width / 2f - width / 2, Gdx.graphics.height / 2f - height / 2)
            addCloseButton()

            table(defaultSpacing = true) {
                label(MenusText.MM_SELECT_LEVEL_NAME_NAME())

                val nameField = textField { }
                textButton(MenusText.MM_SELECT_LEVEL_NAME_CONFIRM()) {
                    onClick {
                        if (!nameField.text.isBlank()) {
                            onNameSelected(nameField.text)
                            this@window.remove()
                        }
                    }
                }
            }
        }
    }

    /**
     * Affiche la fenêtre de sélection de niveau
     */
    private fun showSelectLevelsWindow() {
        class LevelItem(val dir: FileHandle) {
            override fun toString(): String = dir.name()
        }

        fun getLevels(): GdxArray<LevelItem> = Constants.levelDirPath.list { dir -> dir.isDirectory && dir.list { _, s -> s == Constants.levelDataFile }.isNotEmpty() }.map { LevelItem(it) }.toGdxArray()

        val list = VisList<LevelItem>()
        list.setItems(getLevels())

        stage + window(MenusText.MM_SELECT_LEVEL_WINDOW_TITLE()) window@ {
            addCloseButton()
            isModal = true
            setSize(300f, 250f)
            setPosition(Gdx.graphics.width / 2f - width / 2, Gdx.graphics.height / 2f - height / 2)

            horizontalGroup {
                space(50f)
                addActor(list)
                verticalGroup {
                    space(10f)
                    textButton(MenusText.MM_SELECT_LEVEL_PLAY_BUTTON()) {
                        onClick {
                            if (list.selected != null) {
                                val level = Level.loadFromFile(list.selected.dir)
                                if (level != null)
                                    SceneManager.loadScene(GameScene(level))
                                else showWrongVersionLevelDialog()
                            }
                        }
                    }
                    textButton(MenusText.MM_SELECT_LEVEL_EDIT_BUTTON()) {
                        onClick {
                            if (list.selected != null) {
                                val level = Level.loadFromFile(list.selected.dir)
                                if (level != null)
                                    SceneManager.loadScene(EditorScene(level))
                                else showWrongVersionLevelDialog()
                            }
                        }
                    }
                    textButton(MenusText.MM_SELECT_LEVEL_NEW_BUTTON()) {
                        onClick {
                            showSetNameLevelWindow { name ->
                                val level = Level.newLevel(name)
                                SceneManager.loadScene(EditorScene(level))
                            }
                        }
                    }
                    textButton(MenusText.MM_SELECT_LEVEL_COPY_BUTTON()) {
                        onClick {
                            if (list.selected != null) {
                                showSetNameLevelWindow { name ->
                                    try {
                                        list.selected.dir.list().forEach {
                                            it.copyTo(list.selected.dir.parent().child(name))
                                        }
                                        list.setItems(getLevels())
                                    } catch (e: IOException) {
                                        UIUtility.showDialog(stage, "Opération échouée !", "Un problème s'est produit lors de la copie !")
                                        Log.error(e) { "Erreur survenue lors de la copie du niveau ! Erreur : $e" }
                                    }
                                }
                            }
                        }
                    }
                    textButton(MenusText.MM_SELECT_LEVEL_DELETE_BUTTON()) {
                        onClick {
                            if (list.selected != null) {
                                UIUtility.showDialog(stage, MenusText.MM_SELECT_LEVEL_DELETE_DIALOG_TITLE(), MenusText.MM_SELECT_LEVEL_DELETE_DIALOG_CONTENT(list.selected.dir.name()), listOf(MenusText.MM_SELECT_LEVEL_DELETE_DIALOG_YES(), MenusText.MM_SELECT_LEVEL_DELETE_DIALOG_NO())) {
                                    if (it == 0) {
                                        try {
                                            list.selected.dir.deleteDirectory()
                                            list.setItems(getLevels())
                                        } catch (e: IOException) {
                                            UIUtility.showDialog(stage, "Opération échouée !", "Un problème s'est produit lors de la suppression !")
                                            Log.error(e) { "Erreur survenue lors de la suppression du niveau !" }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Affiche la fenêtre de configuration du jeu et des touches
     */
    private fun showSettingsWindows() {
        stage + window(MenusText.MM_SETTINGS_WINDOW_TITLE()) {
            addCloseButton()
            setSize(700f, 300f)
            setPosition(Gdx.graphics.width / 2f - width / 2f, Gdx.graphics.height / 2f - height / 2f)

            val widthArea = VisTextArea(Gdx.graphics.width.toString())
            val heightArea = VisTextArea(Gdx.graphics.height.toString())

            val fullScreenCkbox = VisCheckBox("", Gdx.graphics.isFullscreen)

            val keyAreaList = mutableListOf<VisTextArea>()
            table(defaultSpacing = true) {
                table(defaultSpacing = true) {
                    label(MenusText.MM_SETTINGS_SCREEN_WIDTH())
                    add(widthArea).width(50f)

                    row()

                    label(MenusText.MM_SETTINGS_SCREEN_HEIGHT())
                    add(heightArea).width(50f)

                    row()

                    label(MenusText.MM_SETTINGS_FULLSCREEN())
                    add(fullScreenCkbox)

                    row()

                    label(MenusText.MM_SETTINGS_SOUND())

                    fun formatLabel() = "${(PCGame.soundVolume * 100).toInt()}%"

                    val labelPourcentageAudio = VisLabel(formatLabel())

                    slider(0.0f, 1.0f, 0.1f, false) {
                        width = 50f
                        value = PCGame.soundVolume
                        onChange {
                            PCGame.soundVolume = value
                            labelPourcentageAudio.setText(formatLabel())
                        }
                    }

                    add(labelPourcentageAudio)
                }

                scrollPane(table {
                    GameKeys.values().forEach { key ->
                        add(VisLabel(key.description))

                        val keyArea = VisTextArea(Input.Keys.toString(key.key))
                        keyArea.addListener(keyArea.onKeyUp {
                            keyArea.text = Input.Keys.toString(it)
                        })
                        keyArea.isReadOnly = true
                        keyArea.userObject = key
                        keyAreaList += keyArea

                        add(keyArea).width(50f)

                        row()
                    }
                })
            }
            row()
            textButton(MenusText.MM_SETTINGS_APPLY()) {
                onClick {
                    var successConfig = true
                    if (widthArea.text.toIntOrNull() == null) {
                        widthArea.color = Color.RED
                        successConfig = false
                    }
                    if (heightArea.text.toIntOrNull() == null) {
                        heightArea.color = Color.RED
                        successConfig = false
                    }
                    if (successConfig) {
                        widthArea.color = Color.WHITE
                        heightArea.color = Color.WHITE

                        if (fullScreenCkbox.isChecked)
                            Gdx.graphics.setFullscreenMode(Gdx.graphics.displayMode)
                        else
                            Gdx.graphics.setWindowedMode(widthArea.text.toInt(), heightArea.text.toInt())

                        if (!Utility.saveGameConfig(widthArea.text.toInt(), heightArea.text.toInt()))
                            UIUtility.showDialog(stage, "Erreur lors de la sauvegarde !", "Une erreur est survenue lors de la sauvegarde de la config du jeu !")
                    }

                    var successKeysConfig = true
                    keyAreaList.forEach {
                        val gameKey = it.userObject as GameKeys

                        val newKey = Input.Keys.valueOf(it.text)
                        if (newKey == -1 || newKey == Input.Keys.UNKNOWN) {
                            successKeysConfig = false
                            it.color = Color.RED
                        } else
                            gameKey.key = newKey
                    }

                    if (successKeysConfig) {
                        keyAreaList.forEach { it.color = Color.WHITE }
                        if (!GameKeys.saveKeysConfig())
                            UIUtility.showDialog(stage, "Erreur lors de la sauvegarde", "Une erreur est survenue lors de la sauvegarde du jeu !")
                    } else {
                        UIUtility.showDialog(stage, "Erreur lors de la sauvegarde !", "Une ou plusieurs touches sont invalides !")
                    }

                    if (successConfig && successKeysConfig)
                        this@window.remove()
                }
            }
        }
    }

    private fun showWrongVersionLevelDialog() = UIUtility.showDialog(stage, MenusText.MM_WRONG_LEVEL_VERSION_DIALOG_TITLE(), MenusText.MM_WRONG_LEVEL_VERSION_DIALOG_CONTENT())
    //endregion
}