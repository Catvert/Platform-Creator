package be.catvert.mtrktx.scenes

import be.catvert.mtrktx.MtrGame
import be.catvert.mtrktx.ecs.EntityEvent
import be.catvert.mtrktx.ecs.systems.BaseSystem
import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.EntitySystem
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.badlogic.gdx.utils.viewport.Viewport
import ktx.app.KtxScreen

import ktx.app.use

/**
* Created by Catvert on 03/06/17.
*/

abstract class BaseScene(protected val _game: MtrGame, protected val _entityEvent: EntityEvent = EntityEvent(), vararg systems: EntitySystem) : KtxScreen {
    protected val _engine: Engine = Engine()

    protected val _viewPort: Viewport
    protected val _stage: Stage

    protected val _camera = OrthographicCamera()

    protected abstract val entities: MutableList<Entity>

    init {
        _camera.setToOrtho(false, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())

        _viewPort = ScreenViewport()
        _stage = Stage(_viewPort, _game.stageBatch)

        Gdx.input.inputProcessor = _stage

        for (system in systems)
            _engine.addSystem(system)
    }

    override fun render(delta: Float) {
        super.render(delta)

        _game.batch.projectionMatrix = _camera.combined

        (0.._engine.systems.size() - 1).asSequence().forEach {
            (_engine.systems[it] as BaseSystem).processEntities(entities)
        }

        _engine.update(delta)

        updateInputs()

        _stage.act(delta)
        _stage.draw()
    }

    fun drawHUD(texture: Texture, posX: Float, posY: Float, width: Float, height: Float) {
        _game.batch.projectionMatrix = _game.defaultProjection
        _game.batch.use {
            _game.batch.draw(texture, posX, posY, width, height)
        }
        _game.batch.projectionMatrix = _camera.combined
    }

    override fun dispose() {
        super.dispose()
        _engine.removeAllEntities()
        entities.clear()
        _stage.dispose()
    }

    open fun updateInputs() {}
}