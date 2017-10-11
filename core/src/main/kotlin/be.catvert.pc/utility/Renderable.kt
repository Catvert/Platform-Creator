package be.catvert.pc.utility

import com.badlogic.gdx.graphics.g2d.Batch

/**
 * Interface permettant à un objet d'être dessinable à l'écran
 */
interface Renderable {
    fun render(batch: Batch)
}