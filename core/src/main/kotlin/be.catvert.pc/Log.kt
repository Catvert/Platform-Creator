package be.catvert.pc

import com.badlogic.gdx.utils.Disposable
import java.io.FileWriter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Permet d'écrire des informations de débuggage pendant l'exécution du jeu.
 */
object Log : Disposable {
    private val writer = FileWriter("last_log.txt")

    private fun prefix() = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))

    inline fun info(info: () -> String) = write("Info.", info())

    inline fun error(error: () -> String) = write("Erreur", error())

    inline fun error(e: Exception, message: () -> String) = write("Erreur", message() + " -> ${e.message}")

    inline fun warn(warn: () -> String) = write("Attention", warn())

    fun write(type: String, content: String) {
        val str = "${prefix()}  -> $type : $content\n"

        writer.write(str)
        print(str)
    }

    override fun dispose() {
        writer.flush()
        writer.close()
    }
}