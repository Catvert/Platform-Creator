package be.catvert.pc.utility

import io.leangen.geantyref.TypeFactory

enum class CustomType {
    DEFAULT, TAG_STRING, NO_CHECK_COMPS_GO
}

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class ExposeEditor(val customName: String = "", val min: Float = 0f, val max: Float = 0f, val customType: CustomType = CustomType.DEFAULT, val description: String = "")

object ExposeEditorFactory {
    val empty: ExposeEditor = TypeFactory.annotation(ExposeEditor::class.java, mapOf())

    fun createExposeEditor(customName: String = "", min: Float = 0f, max: Float = 0f, customType: CustomType = CustomType.DEFAULT, description: String = ""): ExposeEditor =
            TypeFactory.annotation(ExposeEditor::class.java, mapOf(ExposeEditor::customName.name to customName, ExposeEditor::min.name to min, ExposeEditor::max.name to max, ExposeEditor::customType.name to customType, ExposeEditor::description.name to description))
}
