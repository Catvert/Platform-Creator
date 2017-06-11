package be.catvert.mtrktx.scenes

import be.catvert.mtrktx.Level
import be.catvert.mtrktx.MtrGame
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

class GameScene(game: MtrGame, private val level: Level) : BaseScene(game, RenderingSystem(game), UpdateSystem(), PhysicsSystem(level)) {
    override val entities: MutableList<Entity> = mutableListOf()

    private val cameraMoveSpeed = 10f

    private val transformPlayer = level.player.getComponent(TransformComponent::class.java)

    override fun render(delta: Float) {
        clearScreen(186f/255f, 212f/255f, 1f)

        _game.batch.projectionMatrix = _game.defaultProjection
        _game.batch.use {
            _game.batch.draw(level.background.second.texture.second, 0f, 0f, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
        }
        _game.batch.projectionMatrix = level.camera.combined

        level.activeRect.setPosition(Math.max(0f, transformPlayer.rectangle.x - level.activeRect.width / 2 + transformPlayer.rectangle.width / 2), Math.max(0f, transformPlayer.rectangle.y - level.activeRect.height / 2 + transformPlayer.rectangle.height / 2))

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
        if( Gdx.input.isKeyJustPressed(Input.Keys.G)) {
            level.applyGravity = !level.applyGravity
        }
        //_camera.zoom = MathUtils.clamp(_camera.zoom, 0.1f, 100/_camera.viewportWidth)
        level.camera.update()
    }
}