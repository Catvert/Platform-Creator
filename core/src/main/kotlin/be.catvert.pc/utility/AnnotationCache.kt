package be.catvert.pc.utility

/**
 * Permet de mettre en cache les annotations d'une classe.
 */
class AnnotationCache<out T : Annotation>(private val annotationClass: Class<T>) {
    private val fieldsAnnotations = hashMapOf<Class<out Any>, Map<String, T>>()
    private val classAnnotations = hashMapOf<Class<out Any>, List<T>>()

    fun getClassAnnotations(type: Class<out Any>): List<T> {
        if (!classAnnotations.containsKey(type))
            classAnnotations[type] = type.getAnnotationsByType(annotationClass).toList()
        return classAnnotations[type]!!
    }

    fun getFieldsAnnotations(type: Class<out Any>): Map<String, T> {
        if (!fieldsAnnotations.containsKey(type))
            fieldsAnnotations[type] = mapOf(*ReflectionUtility.getAllFieldsOf(type).filter { it.isAnnotationPresent(annotationClass) }.map { it.name to it.getAnnotation(annotationClass) }.toTypedArray())

        return fieldsAnnotations[type]!!
    }
}