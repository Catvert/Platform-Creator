package be.catvert.pc.utility

import io.leangen.geantyref.TypeFactory
import kotlin.reflect.full.findAnnotation

enum class CustomType {
    DEFAULT, KEY_INT
}

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class ExposeEditor(val customName: String = "", val minInt: Int = 0, val maxInt: Int = 0, val customType: CustomType = CustomType.DEFAULT)

object ExposeEditorFactory {
    fun createExposeEditor(customName: String = "", minInt: Int = 0, maxInt: Int = 0, customType: CustomType = CustomType.DEFAULT): ExposeEditor {
        return TypeFactory.annotation(ExposeEditor::class.java, mapOf(ExposeEditor::customName.name to customName, ExposeEditor::minInt.name to minInt, ExposeEditor::maxInt.name to maxInt, ExposeEditor::customType.name to customType))
    }
}
