package be.catvert.pc.utility

import com.badlogic.gdx.Graphics
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Rectangle

fun Batch.draw(texture: Texture, rect: Rect, flipX: Boolean = false, flipY: Boolean = false) {
    draw(texture, rect.position.x.toFloat(), rect.position.y.toFloat(), rect.size.width.toFloat(), rect.size.height.toFloat(), 0, 0, texture.width, texture.height, flipX, flipY)
}

fun Batch.draw(textureRegion: TextureRegion, rect: Rect, flipX: Boolean = false, flipY: Boolean = false) {
    if (flipX && !textureRegion.isFlipX || !flipX && textureRegion.isFlipX) {
        textureRegion.flip(true, false)
    }
    if (flipY && !textureRegion.isFlipY || !flipY && textureRegion.isFlipY) {
        textureRegion.flip(false, true)
    }

    draw(textureRegion, rect.position.x.toFloat(), rect.position.y.toFloat(), rect.size.width.toFloat(), rect.size.height.toFloat())
}



fun Graphics.toSize() = Size(width, height)

object Utility {
    fun getFilesRecursivly(dir: FileHandle, fileExt: String = ""): List<FileHandle> {
        val files = mutableListOf<FileHandle>()

        dir.list().forEach {
            if (it.isDirectory)
                files += getFilesRecursivly(it, fileExt)
            else {
                if (fileExt.isBlank() || it.extension() == if (fileExt[0] == '.') fileExt.removePrefix(".") else fileExt)
                    files += it
            }
        }
        return files
    }
}