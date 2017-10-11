package be.catvert.pc.utility

import com.badlogic.gdx.Graphics
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.math.Rectangle

fun Rectangle.position() = Point(this.x.toInt(), this.y.toInt())
fun Rectangle.size() = Size(this.width.toInt(), this.height.toInt())
fun Rectangle.setSize(newSize: Size) = this.setSize(newSize.width.toFloat(), newSize.height.toFloat())
fun Rectangle.setPosition(newPosition: Point) = this.setPosition(newPosition.x.toFloat(), newPosition.y.toFloat())
fun Rectangle.contains(point: Point) = this.contains(point.x.toFloat(), point.y.toFloat())

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