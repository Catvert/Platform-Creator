package be.catvert.mtrktx.scenes

import be.catvert.mtrktx.LevelFactory
import be.catvert.mtrktx.MtrGame
import be.catvert.mtrktx.Utility
import be.catvert.mtrktx.ecs.systems.RenderingSystem
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.kotcrab.vis.ui.widget.VisList
import ktx.actors.onClick
import ktx.app.clearScreen
import ktx.collections.toGdxArray
import ktx.vis.KVisTextButton
import ktx.vis.window

/**
 * Created by arno on 03/06/17.
 */

class MainMenuScene(game: MtrGame) : BaseScene(game, RenderingSystem(game)) {
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
                    addListener(onClick { event: InputEvent, actor: KVisTextButton -> showSelectLevelsWindow() })
                }
                textButton("Options") {

                }
                textButton("Quitter") {
                    addListener(onClick { event: InputEvent, actor: KVisTextButton -> Gdx.app.exit() })
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
        val levelsPath = Utility.getFilesRecursivly(Gdx.files.internal("levels"), "mtrlvl").let {
            val list = mutableListOf<LevelItem>()
            it.forEach {
                list += LevelItem(it)
            }
            list.toGdxArray()
        }

        val list = VisList<LevelItem>()
        list.setItems(levelsPath)

        _stage.addActor(window("Sélection d'un niveau") window@ {
            isModal = true
            setSize(400f, 400f)
            setPosition(Gdx.graphics.width / 2f - width / 2, Gdx.graphics.height / 2f - height / 2)

            horizontalGroup {
                space(50f)
                addActor(list)
                verticalGroup {
                    space(10f)
                    textButton("Jouer") {
                        addListener(onClick { event: InputEvent, actor: KVisTextButton ->
                            if(list.selected != null) {
                                val (success, level) = LevelFactory.loadLevel(_game, list.selected.file)
                                if(success)
                                    _game.setScene(GameScene(_game, level))
                            }
                        })
                    }
                    textButton("Éditer") {
                        addListener(onClick { event: InputEvent, actor: KVisTextButton ->
                            if(list.selected != null) {
                                val (success, level) = LevelFactory.loadLevel(_game, list.selected.file)
                                if(success)
                                    _game.setScene(EditorScene(_game, level))
                            }
                        })
                    }
                    textButton("Fermer") {
                        addListener(onClick { event: InputEvent, actor: KVisTextButton ->
                            this@window.remove()
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