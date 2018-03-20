package be.catvert.pc.utility

import com.badlogic.gdx.files.FileHandle
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import ktx.assets.toLocalFile

/**
 * Wrapper permettant de sérialiser un FileHandle
 */
class FileWrapper(file: FileHandle) {
    constructor(path: String) : this(path.toLocalFile())
    @JsonCreator private constructor() : this(INVALID.file)

    @JsonProperty("path")
    private var file: String = file.path()

    @JsonIgnore
    fun setFile(file: FileHandle) {
        this.file = file.path()
    }

    @JsonIgnore
    fun get() = file.toLocalFile()

    override fun equals(other: Any?) = other.cast<FileWrapper>()?.file == this.file

    override fun toString() = get().nameWithoutExtension()

    companion object {
        val INVALID = FileWrapper("#INVALID")
    }
}