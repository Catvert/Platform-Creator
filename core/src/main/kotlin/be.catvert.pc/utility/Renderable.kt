package be.catvert.pc.utility

import com.badlogic.gdx.graphics.g2d.Batch

/**
 * Interface permettant de spécifier qu'un objet peut-être dessinable
 */
interface Renderable {
    fun render(batch: Batch)
}