package be.catvert.mtrktx.scenes

import be.catvert.mtrktx.MtrGame
import be.catvert.mtrktx.ecs.EntityEvent
import be.catvert.mtrktx.ecs.systems.BaseSystem
import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.EntitySystem
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.badlogic.gdx.utils.viewport.Viewport
import ktx.app.KtxScreen

/**
 * Created by arno on 03/06/17.
 */

abstract class BaseScene(protected val _game: MtrGame, protected val _entityEvent: EntityEvent = EntityEvent(), vararg systems: EntitySystem) : KtxScreen {
    protected val _engine: Engine = Engine()

    protected val _viewPort: Viewport
    protected val _stage: Stage

    protected abstract val entities: MutableList<Entity>

    init {
        _viewPort = ScreenViewport()
        _stage = Stage(_viewPort, _game.batch)

        Gdx.input.inputProcessor = _stage

        for(system in systems)
            _engine.addSystem(system)
    }

    override fun render(delta: Float) {
        super.render(delta)

        for(i in 0.._engine.systems.size() - 1) {
            if(_engine.systems[i] is BaseSystem) {
                (_engine.systems[i] as BaseSystem).processEntities(entities)
            }
        }
        _engine.update(delta)

        updateInputs()

        _stage.act(delta)
        _stage.draw()
    }

    override fun dispose() {
        super.dispose()
        _engine.removeAllEntities()
        entities.clear()
        _stage.dispose()
    }

    open fun updateInputs() {}
}