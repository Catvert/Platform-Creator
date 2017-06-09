package be.catvert.mtrktx.scenes

import be.catvert.mtrktx.MtrGame
import be.catvert.mtrktx.ecs.systems.EntityEventSystem
import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.EntityListener
import com.badlogic.ashley.core.EntitySystem
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.badlogic.gdx.utils.viewport.Viewport
import ktx.app.KtxScreen
import ktx.app.LetterboxingViewport
import ktx.assets.disposeSafely

/**
 * Created by arno on 03/06/17.
 */

abstract class BaseScene(protected val _game: MtrGame, vararg systems: EntitySystem) : KtxScreen {
    protected val _viewPort: Viewport
    protected val _stage: Stage
    protected val _camera: OrthographicCamera = OrthographicCamera()

    protected val _engine = Engine()

    init {
        _viewPort = ScreenViewport(_camera)
        _stage = Stage(_viewPort, _game.batch)

        Gdx.input.inputProcessor = _stage

        _engine.addEntityListener(object : EntityListener {
            override fun entityAdded(entity: Entity?) {
                _engine.systems.forEach {
                    if(it is EntityEventSystem && entity != null)
                        it.onEntityAdded(entity)
                }
            }

            override fun entityRemoved(entity: Entity?) {
                _engine.systems.forEach {
                    if(it is EntityEventSystem && entity != null)
                        it.onEntityRemoved(entity)
                }
            }
        })

        for(system in systems)
            _engine.addSystem(system)
    }

    override fun render(delta: Float) {
        super.render(delta)

        _engine.update(delta)

        updateInputs()

        _stage.act(delta)
        _stage.draw()
    }

    open fun updateInputs() {}

    override fun dispose() {
        super.dispose()

        _stage.dispose()
    }
}