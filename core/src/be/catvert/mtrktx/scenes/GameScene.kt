package be.catvert.mtrktx.scenes

import be.catvert.mtrktx.Level
import be.catvert.mtrktx.LevelFactory
import be.catvert.mtrktx.MtrGame
import be.catvert.mtrktx.ecs.EntityFactory
import be.catvert.mtrktx.ecs.IUpdateable
import be.catvert.mtrktx.ecs.components.PhysicsComponent
import be.catvert.mtrktx.ecs.components.UpdateComponent
import be.catvert.mtrktx.ecs.systems.physics.PhysicsSystem
import be.catvert.mtrktx.ecs.systems.RenderingSystem
import be.catvert.mtrktx.ecs.systems.UpdateSystem
import be.catvert.mtrktx.plusAssign
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.math.Rectangle
import ktx.assets.loadOnDemand


/**
 * Created by arno on 03/06/17.
 */

class GameScene(game: MtrGame, levelPath: String) : BaseScene(game, RenderingSystem(game), UpdateSystem()) {
    private val physicsSystem: PhysicsSystem

    private val cameraMoveSpeed = 10f

    private val level = LevelFactory.loadLevel(game, _engine, levelPath)

    init {
        physicsSystem = PhysicsSystem(game, level, _camera)
        _engine.addSystem(physicsSystem)
    }

    override fun render(delta: Float) {
        _game.batch.projectionMatrix = _camera.combined

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