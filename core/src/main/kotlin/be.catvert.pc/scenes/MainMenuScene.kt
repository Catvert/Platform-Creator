package be.catvert.pc.scenes

import be.catvert.pc.*
import be.catvert.pc.serialization.SerializationFactory
import be.catvert.pc.utility.Constants
import be.catvert.pc.utility.Size
import be.catvert.pc.utility.Utility
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.kotcrab.vis.ui.widget.*
import ktx.actors.onChange
import ktx.actors.onClick
import ktx.actors.onKeyUp
import ktx.actors.plus
import ktx.assets.loadOnDemand
import ktx.collections.GdxArray
import ktx.collections.toGdxArray
import ktx.vis.window
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

class MainMenuScene : Scene() {
    private val glyphCreatedBy = GlyphLayout(PCGame.mainFont, "par Catvert")

    private var logo = PCGame.generateLogo(this)

    init {
        stage + window("Menu principal") {
            verticalGroup {
                space(10f)

                textButton("Jouer !") {
                    onClick {
                        showSelectLevelsWindow()
                    }
                }
                textButton("Options") {
                    onClick {
                        showSettingsWindows()
                    }
                }
                textButton("Quitter") {
                    onClick {
                        Gdx.app.exit()
                    }
                }
            }

            setPosition(Gdx.graphics.width / 2f - width / 2f, Gdx.graphics.height / 2f - height / 2f)
        }

        backgroundTexture = PCGame.assetManager.loadOnDemand<Texture>("assets/game/mainmenu.png").asset
    }

    override fun resize(size: Size) {
        super.resize(size)
        logo.rectangle.set(PCGame.getLogoRect())
    }

    /**
     * Affiche un dialogue complexe
     * @param title : Le titre
     * @param content : Le contenu du dialogue
     * @param buttons : Les boutons disponibles dans le dialogue
     * @param onClose : Fonction appelée quand l'utilisateur a appuié sur un bouton
     */
    private fun showDialog(title: String, content: String, buttons: List<VisTextButton> = listOf(), onClose: (button: Int) -> Unit = {}) {
        stage + window(title) {
            isModal = true
            setSize(400f, 150f)
            setPosition(Gdx.graphics.width / 2f - width / 2, Gdx.graphics.height / 2f - height / 2)
            addCloseButton()

            verticalGroup {
                space(10f)

                label(content)

                horizontalGroup {
                    space(10f)
                    buttons.forEachIndexed { i, button ->
                        addActor(button)
                        button.addListener(button.onClick {
                            this@window.remove()
                            onClose(i)
                        })
                    }
                }
            }
        }
    }

    /**
     * Affiche une fenêtre pour sélectionner le nom du niveau
     * @param onNameSelected Méthode appelée quand l'utilisateur a correctement entré le nom du niveau
     */
    private fun showSetNameLevelWindow(onNameSelected: (name: String) -> Unit) {
        stage + window("Choisissez un nom pour le niveau") {
            isModal = true
            setSize(300f, 150f)
            setPosition(Gdx.graphics.width / 2f - width / 2, Gdx.graphics.height / 2f - height / 2)
            addCloseButton()

            val textFieldName = VisTextField()

            verticalGroup {
                space(10f)

                horizontalGroup {
                    label("Nom : ")

                    addActor(textFieldName)
                }
                textButton("Confirmer") {
                    addListener(onClick { ->
                        if (!textFieldName.text.isBlank()) {
                            onNameSelected(textFieldName.text)
                            this@window.remove()
                        }
                    })
                }
            }
        }
    }

    /**
     * Affiche la fenêtre de sélection de niveau
     */
    private fun showSelectLevelsWindow() {
        class LevelItem(val file: FileHandle) {
            override fun toString(): String {
                return file.nameWithoutExtension()
            }
        }

        fun getLevels(): GdxArray<LevelItem> =
                Utility.getFilesRecursivly(Gdx.files.local(Constants.levelDirectoryPath), Constants.levelExtension).let {
                    val list = mutableListOf<LevelItem>()
                    it.forEach {
                        list += LevelItem(it)
                    }
                    list.toGdxArray()
                }

        val list = VisList<LevelItem>()
        list.setItems(getLevels())

        stage + window("Sélection d'un niveau") window@ {
            addCloseButton()
            isModal = true
            setSize(300f, 250f)
            setPosition(Gdx.graphics.width / 2f - width / 2, Gdx.graphics.height / 2f - height / 2)

            horizontalGroup {
                space(50f)
                addActor(list)
                verticalGroup {
                    space(10f)
                    textButton("Jouer") {
                        addListener(onClick {
                            if (list.selected != null) {
                                val level = SerializationFactory.deserializeFromFile<Level>(list.selected.file)
                                PCGame.setScene(GameScene(level))
                            }
                        })
                    }
                    textButton("Éditer") {
                        addListener(onClick {
                            if (list.selected != null) {
                                val level = SerializationFactory.deserializeFromFile<Level>(list.selected.file)
                                PCGame.setScene(EditorScene(level))
                            }
                        })
                    }
                    textButton("Nouveau") {
                        addListener(onClick {
                            showSetNameLevelWindow { name ->
                                val level = Level(Gdx.files.local(Constants.levelDirectoryPath + name + Constants.levelExtension), name)
                                PCGame.setScene(EditorScene(level))
                            }
                        })
                    }
                    textButton("Copier") {
                        addListener(onClick {
                            if (list.selected != null) {
                                showSetNameLevelWindow { name ->
                                    try {
                                        Files.copy(Paths.get(list.selected.file.path()), Paths.get(list.selected.file.parent().path() + "/$name.mtrlvl"), StandardCopyOption.REPLACE_EXISTING)
                                        list.setItems(getLevels())
                                        showDialog("Opération réussie !", "La copie s'est correctement effectuée !")
                                    } catch(e: IOException) {
                                        showDialog("Opération échouée !", "Un problème s'est produit lors de la copie !")
                                        Log.error(e) { "Erreur survenue lors de la copie du niveau ! Erreur : $e" }
                                    }
                                }
                            }
                        })
                    }
                    textButton("Supprimer") {
                        addListener(onClick {
                            if (list.selected != null) {
                                showDialog("Supprimer le niveau ?", "Etes-vous sur de vouloir supprimer ${list.selected} ?", listOf(VisTextButton("Oui"), VisTextButton("Non")), { button ->
                                    if (button == 0) {
                                        try {
                                            Files.delete(Paths.get(list.selected.file.path()))
                                            list.setItems(getLevels())
                                            showDialog("Opération réussie !", "La suppression s'est correctement effectuée !")
                                        } catch(e: IOException) {
                                            showDialog("Opération échouée !", "Un problème s'est produit lors de la suppression !")
                                            Log.error(e) { "Erreur survenue lors de la suppression du niveau !" }
                                        }
                                    }
                                })
                            }
                        })
                    }
                }
            }
        }
    }

    /**
     * Affiche la fenêtre de configuration du jeu et des touches
     */
    private fun showSettingsWindows() {
        stage + window("Options du jeu") {
            addCloseButton()
            setSize(300f, 275f)
            setPosition(Gdx.graphics.width / 2f - width - 1, Gdx.graphics.height / 2f - height / 2)

            val widthArea = VisTextArea(Gdx.graphics.width.toString())
            val heightArea = VisTextArea(Gdx.graphics.height.toString())
            verticalGroup {
                space(10f)
                horizontalGroup {
                    label("Largeur de l'écran : ")
                    addActor(widthArea)
                }
                horizontalGroup {
                    label("Hauteur de l'écran : ")
                    addActor(heightArea)
                }

                val fullscreen = checkBox("Plein écran") {
                    isChecked = Gdx.graphics.isFullscreen
                }
                val vsync = checkBox("VSync") {
                    isChecked = PCGame.vsync
                }

                horizontalGroup {
                    space(10f)
                    setPosition(0f, 10f)

                    label("Audio")

                    val labelPourcentageAudio = VisLabel("${(PCGame.soundVolume * 100).toInt()}%")

                    val slider = VisSlider(0.0f, 1.0f, 0.1f, false)
                    slider.value = PCGame.soundVolume
                    slider.addListener(slider.onChange {
                        PCGame.soundVolume = slider.value
                        labelPourcentageAudio.setText("${(PCGame.soundVolume * 100).toInt()}%")
                    })

                    this + slider

                    this + labelPourcentageAudio
                }

                textButton("Appliquer") {
                    addListener(onClick {
                        var success = true
                        if (widthArea.text.toIntOrNull() == null) {
                            widthArea.color = Color.RED
                            success = false
                        }
                        if (heightArea.text.toIntOrNull() == null) {
                            heightArea.color = Color.RED
                            success = false
                        }
                        if (success) {
                            widthArea.color = Color.WHITE
                            heightArea.color = Color.WHITE

                            PCGame.vsync = vsync.isChecked

                            if (fullscreen.isChecked)
                                Gdx.graphics.setFullscreenMode(Gdx.graphics.displayMode)
                            else
                                Gdx.graphics.setWindowedMode(widthArea.text.toInt(), heightArea.text.toInt())

                            if (PCGame.saveGameConfig())
                                showDialog("Sauvegarde effectuée !", "Changement effectué !")
                            else
                                showDialog("Erreur lors de la sauvegarde !", "Une erreur est survenue lors de la sauvegarde de la config du jeu !")
                        }
                    })
                }
            }
        }

        stage + window("Configuration des touches") {
            addCloseButton()
            setSize(325f, 275f)
            setPosition(Gdx.graphics.width / 2f + 1, Gdx.graphics.height / 2f - height / 2)

            val keyAreaList = mutableListOf<VisTextArea>()

            val pane = ScrollPane(table {
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
            pane.setScrollingDisabled(true, false)

            table {
                add(pane)
                row()

                val saveButton = VisTextButton("Appliquer")
                saveButton.addListener(saveButton.onClick {
                    var success = true

                    keyAreaList.forEach {
                        val gameKey = it.userObject as GameKeys

                        val newKey = Input.Keys.valueOf(it.text)
                        if (newKey == -1 || newKey == Input.Keys.UNKNOWN) {
                            success = false
                            it.color = Color.RED
                        } else
                            gameKey.key = newKey
                    }

                    if (success) {
                        keyAreaList.forEach { it.color = Color.WHITE }
                        if (PCGame.saveKeysConfig())
                            showDialog("Sauvegarde effectuée !", "La sauvegarde des touches c'est correctement effectuée !")
                        else
                            showDialog("Erreur lors de la sauvegarde", "Une erreur est survenue lors de la sauvegarde du jeu !")
                    } else {
                        showDialog("Erreur lors de la sauvegarde !", "Une ou plusieurs touches sont invalides !")
                    }
                })

                add(saveButton)
            }
        }
    }
}