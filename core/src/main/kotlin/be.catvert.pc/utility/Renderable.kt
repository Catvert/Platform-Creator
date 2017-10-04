package be.catvert.pc.utility

import com.badlogic.gdx.graphics.g2d.Batch

interface Renderable {
    fun render(batch: Batch)
}