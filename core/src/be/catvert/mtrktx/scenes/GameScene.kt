package be.catvert.mtrktx.scenes

import be.catvert.mtrktx.Level
import be.catvert.mtrktx.LevelFactory
import be.catvert.mtrktx.MtrGame
import be.catvert.mtrktx.ecs.components.TransformComponent
import be.catvert.mtrktx.ecs.systems.physics.PhysicsSystem
import be.catvert.mtrktx.ecs.systems.RenderingSystem
import be.catvert.mtrktx.ecs.systems.UpdateSystem
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.math.MathUtils


/**
 * Created by arno on 03/06/17.
 */

class GameScene(game: MtrGame, levelPath: String) : BaseScene(game, RenderingSystem(game), UpdateSystem()) {
    private val physicsSystem: PhysicsSystem

    private val cameraMoveSpeed = 10f

    private val level = LevelFactory.loadLevel(game, _engine, levelPath)

    private var followPlayerCamera = true

    private val transformPlayer: TransformComponent

    init {
        physicsSystem = PhysicsSystem(_game, level, _camera)
        _engine.addSystem(physicsSystem)

        transformPlayer = level.player.getComponent(TransformComponent::class.java)
    }

    override fun render(delta: Float) {
        _game.batch.projectionMatrix = _camera.combined

        if(followPlayerCamera)
            _camera.position.set(Math.max(0f + _camera.viewportWidth / 2, transformPlayer.rectangle.x + transformPlayer.rectangle.width / 2), Math.max(0f + _camera.viewportHeight / 2, transformPlayer.rectangle.y + transformPlayer.rectangle.height / 2), 0f)

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
            followPlayerCamera = !followPlayerCamera
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
            physicsSystem.drawDebugCells = !physicsSystem.drawDebugCells
        }
        //_camera.zoom = MathUtils.clamp(_camera.zoom, 0.1f, 100/_camera.viewportWidth)
        _camera.update()
    }
}