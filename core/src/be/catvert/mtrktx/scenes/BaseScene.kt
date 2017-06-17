package be.catvert.mtrktx.scenes

import be.catvert.mtrktx.MtrGame
import be.catvert.mtrktx.ecs.EntityEvent
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.EntitySystem
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.badlogic.gdx.utils.viewport.Viewport
import ktx.app.KtxScreen

/**
* Created by Catvert on 03/06/17.
*/

/**
 * Classe abstraite permettant de créer une scène
 * game : L'objet du jeu
 * entityEvent : Permet d'implémenter la méthode d'ajout et de suppression d'entité utilisé par les entités
 * systems : Permet de spécifier les systèmes à ajouter à la scène
 */
abstract class BaseScene(protected val _game: MtrGame, protected val _entityEvent: EntityEvent = EntityEvent(), vararg systems: EntitySystem) : KtxScreen {
    protected val _viewPort: Viewport
    protected val _stage: Stage

    val camera = OrthographicCamera()

    abstract val entities: MutableList<Entity>

    val addedSystems = systems

    init {
        camera.setToOrtho(false, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())

        _viewPort = ScreenViewport()
        _stage = Stage(_viewPort, _game.stageBatch)

        Gdx.input.inputProcessor = _stage
    }

    override fun render(delta: Float) {
        super.render(delta)

        _game.batch.projectionMatrix = camera.combined

        updateInputs()

        _stage.act(delta)
        _stage.draw()
    }

    override fun dispose() {
        super.dispose()
        entities.clear()
        _stage.dispose()
    }

    open fun updateInputs() {}
}