package be.catvert.pc.utility

enum class CustomType {
    DEFAULT, KEY_INT
}

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class ExposeEditor(val minInt: Int = 0, val maxInt: Int = 0, val customType: CustomType = CustomType.DEFAULT)