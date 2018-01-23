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

    private fun suffix() = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))

    fun info(info: () -> String) = write("Info.", info())

    fun error(error: () -> String) = write("Erreur", error())

    fun error(e: Exception, message: () -> String) = write("Erreur", message() + " -> ${e.message}")

    fun warn(warn: () -> String) = write("Attention", warn())

    private fun write(type: String, content: String) {
        val str = "${suffix()}  -> $type : $content\n"

        writer.write(str)
        print(str)
    }

    override fun dispose() {
        writer.flush()
        writer.close()
    }
}