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


/**
 * Created by arno on 03/06/17.
 */

class GameScene(game: MtrGame, levelPath: String) : BaseScene(game, RenderingSystem(game), UpdateSystem()) {
    private val physicsSystem: PhysicsSystem

    private val cameraMoveSpeed = 10f

    private val level = LevelFactory.loadLevel(game, _engine, levelPath)

    private val followPlayerCamera = true

    private val transformPlayer: TransformComponent

    init {
        physicsSystem = PhysicsSystem(level, _camera)
        _engine.addSystem(physicsSystem)

        transformPlayer = level.player.getComponent(TransformComponent::class.java)
    }

    override fun render(delta: Float) {
        _game.batch.projectionMatrix = _camera.combined

        //_camera.position.set(transformPlayer.rectangle.x - Gdx.graphics.width / 2, transformPlayer.rectangle.y + Gdx.graphics.height / 2, 0f)

        super.render(delta)
    }



    override fun updateInputs() {
        super.updateInputs()

        if(Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            _game.setScene(PauseScene(_game))
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
    }
}