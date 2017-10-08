package be.catvert.pc

import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.TimeUtils
import ktx.async.ktxAsync
import java.io.FileWriter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

object Log: Disposable {
    private val _writer = FileWriter("last_log.txt")

    private fun suffix() = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))

    fun info(info: () -> String) {
        write("Info.", info())
    }

    fun error(error: () -> String) {
        write("Erreur", error())
    }

    fun error(e: Exception, message: () -> String) {
        write("Erreur", message() + " -> ${e.message}")
    }

    fun warn(warn: () -> String) {
        write("Attention", warn())
    }

    private fun write(type: String, content: String) {
        ktxAsync {
            val str = suffix() + " -> $type : " + content

            _writer.write(str)
            println(str)
        }
    }

    override fun dispose() {
        _writer.flush()
        _writer.close()
    }
}