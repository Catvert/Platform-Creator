package be.catvert.pc.utility

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Graphics
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.math.Rectangle
import org.w3c.dom.css.Rect

fun Rectangle.position() = Point(this.x.toInt(), this.y.toInt())
fun Rectangle.size() = Size(this.width.toInt(), this.height.toInt())

fun Graphics.toSize() = Size(width, height)

object Utility {
    fun getFilesRecursivly(dir: FileHandle, fileExt: String = ""): List<FileHandle> {
        val files = mutableListOf<FileHandle>()

        dir.list().forEach {
            if (it.isDirectory)
                files += getFilesRecursivly(it, fileExt)
            else {
                if (fileExt.isBlank() || it.extension() == if(fileExt[0] == '.') fileExt.removePrefix(".") else fileExt)
                    files += it
            }
        }
        return files
    }
}