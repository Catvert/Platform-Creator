package be.catvert.mtrktx.scenes

import be.catvert.mtrktx.LevelFactory
import be.catvert.mtrktx.MtrGame
import be.catvert.mtrktx.ecs.systems.RenderingSystem
import be.catvert.mtrktx.ecs.systems.UpdateSystem
import be.catvert.mtrktx.ecs.systems.physics.PhysicsSystem
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input


/**
 * Created by arno on 03/06/17.
 */

class GameScene(game: MtrGame, levelPath: String) : BaseScene(game, RenderingSystem(game), UpdateSystem()) {
    override val entities: MutableList<Entity> = mutableListOf()

    private val physicsSystem: PhysicsSystem

    private val cameraMoveSpeed = 10f

    private val level = LevelFactory.loadLevel(game, entities, levelPath)

    init {
        physicsSystem = PhysicsSystem(_game, level, level.camera)
        addSystem(physicsSystem)
    }

    override fun render(delta: Float) {
        _game.batch.projectionMatrix = level.camera.combined

        entities.clear()
        entities.addAll(level.getAllEntitiesInCells(level.getActiveGridCells()))

        level.update(delta)

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
        //_camera.zoom = MathUtils.clamp(_camera.zoom, 0.1f, 100/_camera.viewportWidth)
        level.camera.update()
    }
}