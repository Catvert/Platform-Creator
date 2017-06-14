package be.catvert.mtrktx.scenes

import be.catvert.mtrktx.Level
import be.catvert.mtrktx.MtrGame
import be.catvert.mtrktx.ecs.EntityEvent
import be.catvert.mtrktx.ecs.components.TransformComponent
import be.catvert.mtrktx.ecs.systems.RenderingSystem
import be.catvert.mtrktx.ecs.systems.UpdateSystem
import be.catvert.mtrktx.ecs.systems.physics.PhysicsSystem
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import ktx.app.clearScreen
import ktx.app.use


/**
 * Created by arno on 03/06/17.
 */

class GameScene(game: MtrGame, entityEvent: EntityEvent, private val level: Level) : BaseScene(game, entityEvent, RenderingSystem(game), UpdateSystem(level), PhysicsSystem(level)) {
    override val entities: MutableList<Entity> = mutableListOf()

    private val cameraMoveSpeed = 10f

    private val transformPlayer = level.player.getComponent(TransformComponent::class.java)

    init {
        _entityEvent.onEntityAdded = { entity -> level.addEntity(entity) }
        _entityEvent.onEntityRemoved = { entity -> level.removeEntity(entity) }
    }

    override fun render(delta: Float) {
        clearScreen(186f/255f, 212f/255f, 1f)

        drawHUD(level.background.texture.texture, 0f, 0f, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())

        level.activeRect.setPosition(Math.max(0f, transformPlayer.rectangle.x - level.activeRect.width / 2 + transformPlayer.rectangle.width / 2), Math.max(0f, transformPlayer.rectangle.y - level.activeRect.height / 2 + transformPlayer.rectangle.height / 2))

        if(level.followPlayerCamera)
           _camera.position.set(Math.max(0f + _camera.viewportWidth / 2, transformPlayer.rectangle.x + transformPlayer.rectangle.width / 2), Math.max(0f + _camera.viewportHeight / 2, transformPlayer.rectangle.y + transformPlayer.rectangle.height / 2), 0f)

        level.update(delta)

        entities.clear()
        entities.addAll(level.getAllEntitiesInCells(level.getActiveGridCells()))

        super.render(delta)
    }

    override fun updateInputs() {
        super.updateInputs()

        if(Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            _game.setScene(PauseScene(_game))
        }

        if(Gdx.input.isKeyPressed(Input.Keys.P)) {
            _camera.zoom -= 0.02f
        }
        if(Gdx.input.isKeyPressed(Input.Keys.M)) {
            _camera.zoom += 0.02f
        }
        if(Gdx.input.isKeyPressed(Input.Keys.L)) {
            _camera.zoom = 1f
        }

        if(Gdx.input.isKeyJustPressed(Input.Keys.C)) {
            level.followPlayerCamera = !level.followPlayerCamera
        }
        if(Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
            _camera.position.x -= cameraMoveSpeed
        }
        if(Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
            _camera.position.x += cameraMoveSpeed
        }
        if(Gdx.input.isKeyPressed(Input.Keys.DOWN)) {
            _camera.position.y -= cameraMoveSpeed
        }
        if(Gdx.input.isKeyPressed(Input.Keys.UP)) {
            _camera.position.y += cameraMoveSpeed
        }

        if( Gdx.input.isKeyJustPressed(Input.Keys.F12)) {
            level.drawDebugCells = !level.drawDebugCells
        }
        if( Gdx.input.isKeyJustPressed(Input.Keys.G)) {
            level.applyGravity = !level.applyGravity
        }

       _camera.update()
    }
}