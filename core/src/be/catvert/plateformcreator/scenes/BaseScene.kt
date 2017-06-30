package be.catvert.plateformcreator.scenes

import be.catvert.plateformcreator.MtrGame
import be.catvert.plateformcreator.ecs.IUpdateable
import be.catvert.plateformcreator.ecs.components.RenderComponent
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
 * @property game : L'objet du jeu
 * @param systems : Permet de spécifier les systèmes à ajouter à la scène
 */
abstract class BaseScene(protected val game: MtrGame, vararg systems: EntitySystem) : KtxScreen, IUpdateable {
    /**
     * Représente la partie de l'écran qu'utilisera le stage pour se dessiner
     */
    val viewport: Viewport

    /**
     * Stage permettant d'ajouter des éléments de l'UI
     */
    val stage: Stage

    /**
     * La caméra de la scène, non utilisée par le stage
     */
    val camera = OrthographicCamera()

    /**
     * Les entités chargés dans la scène
     */
    val entities: MutableSet<Entity> = mutableSetOf()

    /**
     * Les systèmes ajoutées à la scène
     */
    val addedSystems = systems

    /**
     * Le fond d'écran de la scène
     */
    var background = RenderComponent()

    init {
        camera.setToOrtho(false, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())

        viewport = ScreenViewport()
        stage = Stage(viewport, game.stageBatch)

        Gdx.input.inputProcessor = stage
    }

    override fun render(delta: Float) {
        super.render(delta)

        stage.act(delta)
        stage.draw()
    }

    override fun dispose() {
        super.dispose()
        entities.clear()
        stage.dispose()
    }

    override fun resize(width: Int, height: Int) {
        super.resize(width, height)

        stage.viewport.update(width, height)
        viewport.update(width, height)
        camera.setToOrtho(false, width.toFloat(), height.toFloat())
    }

    override fun update(deltaTime: Float) {}
}