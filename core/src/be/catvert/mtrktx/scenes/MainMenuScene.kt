package be.catvert.mtrktx.scenes

import be.catvert.mtrktx.LevelFactory
import be.catvert.mtrktx.MtrGame
import be.catvert.mtrktx.Utility
import be.catvert.mtrktx.ecs.systems.RenderingSystem
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import com.kotcrab.vis.ui.widget.VisList
import com.kotcrab.vis.ui.widget.VisTextButton
import com.kotcrab.vis.ui.widget.VisTextField
import ktx.actors.onClick
import ktx.app.clearScreen
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

class MainMenuScene(game: MtrGame) : BaseScene(game, systems = RenderingSystem(game)) {
    override val entities: MutableList<Entity> = mutableListOf()

    init {
        entities += _game.getMainBackground()
        entities += _game.getLogo()

        _stage.addActor(window("Menu Principal") {
            setSize(200f, 200f)
            setPosition(Gdx.graphics.width / 2f - width / 2, Gdx.graphics.height / 2f - height / 2)

            verticalGroup {
                space(10f)

                textButton("Jouer") {
                    addListener(onClick { _, _ -> showSelectLevelsWindow() })
                }
                textButton("Options") {

                }
                textButton("Quitter") {
                    addListener(onClick { _, _ -> Gdx.app.exit() })
                }
            }
        })
    }

    fun showSetNameLevelWindow(onNameSelected: (name: String) -> Unit) {
        _stage.addActor(window("Choisissez un nom pour le niveau") {
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
                    addListener(onClick { _, _ ->
                        if(!textFieldName.text.isBlank()) {
                            onNameSelected(textFieldName.text)
                            this@window.remove()
                        }
                    })
                }
            }
        })
    }

    fun showDialog(title: String, content: String, onClose: (() -> Unit)? = null) {
        showDialog(title, content, listOf(VisTextButton("Fermer")), { _ -> onClose?.invoke() })
    }

    fun showDialog(title: String, content: String, buttons: List<VisTextButton>, onClose: ((button: Int) -> Unit)) {
        _stage.addActor(window(title) {
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
                        it.addListener(it.onClick { _, _ ->
                            this@window.remove()
                            onClose(i)
                        })
                    }
                }
            }
        })
    }

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

        _stage.addActor(window("Sélection d'un niveau") window@ {
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
                        addListener(onClick { _, _ ->
                            if(list.selected != null) {
                                val (success, level, entityEvent) = LevelFactory.loadLevel(_game, list.selected.file)
                                if(success)
                                    _game.setScene(GameScene(_game, entityEvent, level))
                            }
                        })
                    }
                    textButton("Éditer") {
                        addListener(onClick { _, _ ->
                            if(list.selected != null) {
                                val (success, level, entityEvent) = LevelFactory.loadLevel(_game, list.selected.file)
                                if(success)
                                    _game.setScene(EditorScene(_game, entityEvent, level))
                            }
                        })
                    }
                    textButton("Nouveau") {
                        addListener(onClick { _, _ ->
                            showSetNameLevelWindow { name ->
                                val (level, entityEvent) = LevelFactory.createEmptyLevel(_game, name, Gdx.files.internal("levels/$name.mtrlvl"))
                                _game.setScene(EditorScene(_game, entityEvent, level))
                            }
                        })
                    }
                    textButton("Copier") {
                        addListener(onClick { _, _ ->
                            if(list.selected != null) {
                                showSetNameLevelWindow { name ->
                                    try {
                                        Files.copy(Paths.get(list.selected.file.path()), Paths.get(list.selected.file.parent().path() + "/$name.mtrlvl"), StandardCopyOption.REPLACE_EXISTING)
                                        list.setItems(getLevels())
                                        showDialog("Opération réussie !", "La copie s'est correctement effectuée !")
                                    } catch(e: IOException) {
                                        showDialog("Opération échouée !", "Un problème s'est produit lors de la copie !")
                                        println("Erreur copie : $e")
                                    }
                                }
                            }
                        })
                    }
                    textButton("Supprimer") {
                        addListener(onClick { _, _ ->
                            if(list.selected != null) {
                                showDialog("Supprimer le niveau ?", "Etes-vous sur de vouloir supprimer ${list.selected} ?", listOf(VisTextButton("Oui"), VisTextButton("Non")), { button ->
                                    if(button == 0) {
                                        try {
                                            Files.delete(Paths.get(list.selected.file.path()))
                                            list.setItems(getLevels())
                                            showDialog("Opération réussie !", "La suppression s'est correctement effectuée !")
                                        } catch(e: IOException) {
                                            showDialog("Opération échouée !", "Un problème s'est produit lors de la suppression !")
                                            println("Erreur suppression : $e")
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

    override fun render(delta: Float) {
        clearScreen(186f/255f, 212f/255f, 1f)

        super.render(delta)

    }

}