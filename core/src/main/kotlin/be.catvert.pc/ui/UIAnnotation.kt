package be.catvert.pc.ui

import io.leangen.geantyref.TypeFactory

enum class CustomType {
    DEFAULT, TAG_STRING
}

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class UI(val customName: String = "", val min: Float = 0f, val max: Float = 0f, val customType: CustomType = CustomType.DEFAULT, val description: String = "")

object UIFactory {
    val empty: UI = TypeFactory.annotation(UI::class.java, mapOf())

    fun createUI(customName: String = "", min: Float = 0f, max: Float = 0f, customType: CustomType = CustomType.DEFAULT, description: String = ""): UI =
            TypeFactory.annotation(UI::class.java, mapOf(UI::customName.name to customName, UI::min.name to min, UI::max.name to max, UI::customType.name to customType, UI::description.name to description))
}
