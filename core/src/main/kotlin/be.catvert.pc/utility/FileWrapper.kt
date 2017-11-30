package be.catvert.pc.utility

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import ktx.assets.toLocalFile
import java.io.File

class FileWrapper(file: FileHandle) {
    constructor(path: String): this(path.toLocalFile())
    @JsonCreator private constructor(): this("")

    @JsonProperty("path") private var file = file.path()

    @JsonIgnore
    fun setFile(file: FileHandle) {
        this.file = file.path()
    }

    fun get() = file.toLocalFile()
}