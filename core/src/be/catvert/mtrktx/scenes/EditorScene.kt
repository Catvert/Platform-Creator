package be.catvert.mtrktx.scenes

import be.catvert.mtrktx.Level
import be.catvert.mtrktx.LevelFactory
import be.catvert.mtrktx.MtrGame
import be.catvert.mtrktx.ecs.components.PhysicsComponent
import be.catvert.mtrktx.ecs.components.TransformComponent
import be.catvert.mtrktx.ecs.systems.RenderingSystem
import com.badlogic.ashley.core.ComponentMapper
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.scenes.scene2d.InputEvent
import ktx.actors.onClick
import ktx.app.clearScreen
import ktx.app.use
import ktx.vis.KVisTextButton
import ktx.vis.window

/**
 * Created by arno on 10/06/17.
 */

class EditorScene(game: MtrGame, private val level: Level) : BaseScene(game, RenderingSystem(game)) {
    override val entities: MutableList<Entity> = mutableListOf()

    private val transformMapper = ComponentMapper.getFor(TransformComponent::class.java)
    private val physicsMapper = ComponentMapper.getFor(PhysicsComponent::class.java)

    private val cameraMoveSpeed = 10f

    private var followMouseEntity: Pair<Entity,TransformComponent>? = null

    init {
        level.activeRect.setSize(Gdx.graphics.width.toFloat() * 4, Gdx.graphics.height.toFloat() * 4)
        level.followPlayerCamera = false
        level.drawDebugCells = true
        level.killEntityUnderY = false
    }

    override fun render(delta: Float) {
        clearScreen(186f/255f, 212f/255f, 1f)

        _game.batch.projectionMatrix = _game.defaultProjection
        _game.batch.use {
            _game.batch.draw(level.background.second.texture.second, 0f, 0f, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
        }
        _game.batch.projectionMatrix = level.camera.combined

        level.activeRect.setPosition(Math.max(0f, level.camera.position.x - level.activeRect.width / 2), Math.max(0f, level.camera.position.y - level.activeRect.height / 2))

        level.update(delta)

        entities.clear()
        entities.addAll(level.getAllEntitiesInCells(level.getActiveGridCells()))

        super.render(delta)
    }

    override fun updateInputs() {
        super.updateInputs()

        if(Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            showExitWindow()
        }

        if(Gdx.input.isKeyPressed(Input.Keys.P)) {
            level.camera.zoom -= 0.02f
        }
        if(Gdx.input.isKeyPressed(Input.Keys.M)) {
            level.camera.zoom += 0.02f
        }
        if(Gdx.input.isKeyPressed(Input.Keys.L)) {
            level.camera.zoom = 1f
        }

        if(Gdx.input.isKeyJustPressed(Input.Keys.C)) {
            level.followPlayerCamera = !level.followPlayerCamera
        }
        if(Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
            level.camera.position.x -= cameraMoveSpeed
        }
        if(Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
            level.camera.position.x += cameraMoveSpeed
        }
        if(Gdx.input.isKeyPressed(Input.Keys.DOWN)) {
            level.camera.position.y -= cameraMoveSpeed
        }
        if(Gdx.input.isKeyPressed(Input.Keys.UP)) {
            level.camera.position.y += cameraMoveSpeed
        }

        if( Gdx.input.isKeyJustPressed(Input.Keys.F12)) {
            level.drawDebugCells = !level.drawDebugCells
        }
        level.camera.update()

        val mousePosInWorld = level.camera.unproject(Vector3(Gdx.input.x.toFloat(), Gdx.input.y.toFloat(), 0f))

        if(followMouseEntity == null) {
            run run@{
                entities.forEach {
                    val transform = transformMapper[it]
                    if(transform.rectangle.contains(mousePosInWorld.x, mousePosInWorld.y)) {
                        followMouseEntity = Pair(it, transform)
                        return@run
                    }
                }
            }
        }
        else {
            if(Gdx.input.isButtonPressed(Input.Buttons.LEFT)) {
                val posX = Math.min(level.matrixRect.width - followMouseEntity!!.second.rectangle.width, // Les min et max permettent de rester dans le cadre du matrix
                        Math.max(0f, mousePosInWorld.x - followMouseEntity!!.second.rectangle.width / 2))
                val posY = Math.min(level.matrixRect.height - followMouseEntity!!.second.rectangle.height,
                        Math.max(0f, mousePosInWorld.y - followMouseEntity!!.second.rectangle.height / 2))

                followMouseEntity!!.second.rectangle.setPosition(posX, posY)

                level.setEntityGrid(followMouseEntity!!.first)
            }
            else {
                followMouseEntity = null
            }
        }


    }

    fun showExitWindow() {
        _stage.addActor(window("Quitter") {
            setPosition(Gdx.graphics.width / 2f - width / 2, Gdx.graphics.height / 2f - height / 2)
            verticalGroup {
                space(10f)

                textButton("Sauvegarder") {
                    addListener(onClick { event: InputEvent, actor: KVisTextButton ->
                        try {
                            LevelFactory.saveLevel(level)
                            this@window.remove()
                        } catch(e: Exception) {
                            println("Erreur lors de l'enregistrement du niveau ! Erreur : $e")
                        }
                    })
                }
                textButton("Quitter") {
                    addListener(onClick { event: InputEvent, actor: KVisTextButton -> _game.setScene(MainMenuScene(_game)) })
                }
            }
        })
    }
}