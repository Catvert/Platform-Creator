package be.catvert.plateformcreator.scenes

import be.catvert.plateformcreator.GameKeys
import be.catvert.plateformcreator.LevelFactory
import be.catvert.plateformcreator.MtrGame
import be.catvert.plateformcreator.Utility
import be.catvert.plateformcreator.ecs.systems.RenderingSystem
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.kotcrab.vis.ui.widget.*
import ktx.actors.onClick
import ktx.actors.plus
import ktx.app.use
import ktx.collections.GdxArray
import ktx.collections.toGdxArray
import ktx.vis.window
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption


/**
 * Created by Catvert on 03/06/17.
 */

/**
 * Scène du menu principal
 */
class MainMenuScene(game: MtrGame) : BaseScene(game, systems = RenderingSystem(game)) {
    override val entities: MutableSet<Entity> = mutableSetOf()

    private val glyphCreatedBy = GlyphLayout(_game.mainFont, "par Catvert")

    private val levelFactory = LevelFactory(game)

    private var logo = _game.getLogo()
        set(value) {
            entities -= logo
            field = value
            entities += logo
        }

    init {
        background = game.getMainBackground()

        entities += logo

        stage.addActor(window("Menu Principal") {
            setSize(200f, 200f)
            setPosition(Gdx.graphics.width / 2f - width / 2, Gdx.graphics.height / 2f - height / 2)

            verticalGroup {
                space(10f)

                textButton("Jouer") {
                    addListener(onClick { showSelectLevelsWindow() })
                }
                textButton("Options") {
                    addListener(onClick {
                        showSettingsWindows()
                    })
                }
                textButton("Quitter") {
                    addListener(onClick { Gdx.app.exit() })
                }
            }
        })
    }

    override fun render(delta: Float) {
        super.render(delta)

        _game.batch.use {
            _game.mainFont.draw(it, glyphCreatedBy, Gdx.graphics.width - glyphCreatedBy.width, glyphCreatedBy.height)
        }
    }

    /**
     * Affiche une fenêtre pour sélectionner le nom du niveau
     */
    fun showSetNameLevelWindow(onNameSelected: (name: String) -> Unit) {
        stage.addActor(window("Choisissez un nom pour le niveau") {
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
        })
    }

    /**
     * Affiche un dialogue simple
     * title : Le titre
     * content : Le contenu du dialogue
     * onClose : Fonction appelée quand l'utilisateur a appuié sur le bouton fermer
     */
    fun showDialog(title: String, content: String, onClose: (() -> Unit)? = null) {
        showDialog(title, content, listOf(VisTextButton("Fermer")), { _ -> onClose?.invoke() })
    }

    /**
     * Affiche un dialogue complexe
     * title : Le titre
     * content : Le contenu du dialogue
     * buttons : Les boutons disponibles dans le dialogue
     * onClose : Fonction appelée quand l'utilisateur a appuié sur un bouton
     */
    fun showDialog(title: String, content: String, buttons: List<VisTextButton>, onClose: ((button: Int) -> Unit)) {
        stage.addActor(window(title) {
            isModal = true
            setSize(400f, 150f)
            setPosition(Gdx.graphics.width / 2f - width / 2, Gdx.graphics.height / 2f - height / 2)

            verticalGroup {
                space(10f)

                label(content)

                horizontalGroup {
                    space(10f)
                    buttons.forEachIndexed { i, it ->
                        addActor(it)
                        it.addListener(it.onClick {
                            this@window.remove()
                            onClose(i)
                        })
                    }
                }
            }
        })
    }

    /**
     * Affiche la fenêtre de sélection de niveau
     */
    fun showSelectLevelsWindow() {
        class LevelItem(val file: FileHandle) {
            override fun toString(): String {
                return file.nameWithoutExtension()
            }
        }

        fun getLevels(): GdxArray<LevelItem> =
                Utility.getFilesRecursivly(Gdx.files.internal("levels"), "mtrlvl").let {
                    val list = mutableListOf<LevelItem>()
                    it.forEach {
                        list += LevelItem(it)
                    }
                    list.toGdxArray()
                }

        val list = VisList<LevelItem>()
        list.setItems(getLevels())

        stage.addActor(window("Sélection d'un niveau") window@ {
            addCloseButton()
            isModal = true
            setSize(400f, 400f)
            setPosition(Gdx.graphics.width / 2f - width / 2, Gdx.graphics.height / 2f - height / 2)

            horizontalGroup {
                space(50f)
                addActor(list)
                verticalGroup {
                    space(10f)
                    textButton("Jouer") {
                        addListener(onClick {
                            if (list.selected != null) {
                                val (success, level, entityEvent) = levelFactory.loadLevel(list.selected.file)
                                if (success)
                                    _game.setScene(GameScene(_game, entityEvent, level))
                            }
                        })
                    }
                    textButton("Éditer") {
                        addListener(onClick {
                            if (list.selected != null) {
                                val (success, level, entityEvent) = levelFactory.loadLevel(list.selected.file)
                                if (success)
                                    _game.setScene(EditorScene(_game, entityEvent, level))
                            }
                        })
                    }
                    textButton("Nouveau") {
                        addListener(onClick {
                            showSetNameLevelWindow { name ->
                                val (level, entityEvent) = levelFactory.createEmptyLevel(name, Gdx.files.internal("levels/$name.mtrlvl"))
                                _game.setScene(EditorScene(_game, entityEvent, level))
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
                                        ktx.log.error(e, message = { "Erreur survenue lors de la copie du niveau ! Erreur : $e" })
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
                                            ktx.log.error(e, message = { "Erreur survenue lors de la suppression du niveau !" })
                                        }
                                    }
                                })
                            }
                        })
                    }
                }
            }
        })
    }

    /**
     * Affiche la fenêtre de configuration du jeu et des touches
     */
    fun showSettingsWindows() {
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
                    isChecked = _game.vsync
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

                            _game.vsync = vsync.isChecked

                            if (fullscreen.isChecked)
                                Gdx.graphics.setFullscreenMode(Gdx.graphics.displayMode)
                            else
                                Gdx.graphics.setWindowedMode(widthArea.text.toInt(), heightArea.text.toInt())

                            logo = _game.getLogo() // Met à jour le logo

                            if (_game.saveGameConfig())
                                showDialog("Sauvegarde effectuée !", "La sauvegarde de la config du jeu c'est correctement effectuée !")
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
                        if (it.text.isBlank()) {
                            success = false
                            it.color = Color.RED
                            return@forEach
                        }

                        val newKey = Input.Keys.valueOf(it.text)
                        if (newKey == -1) {
                            success = false
                            it.color = Color.RED
                        } else
                            gameKey.key = newKey
                    }

                    if (success) {
                        keyAreaList.forEach { it.color = Color.WHITE }
                        if (_game.saveKeysConfig())
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