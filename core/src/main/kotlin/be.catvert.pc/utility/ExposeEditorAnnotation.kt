package be.catvert.pc.utility

import be.catvert.pc.GameObject

enum class CustomType {
    DEFAULT, KEY_INT
}

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class ExposeEditor(val customName: String = "", val minInt: Int = 0, val maxInt: Int = 0, val customType: CustomType = CustomType.DEFAULT)