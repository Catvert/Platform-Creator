package be.catvert.pc.utility

class AnnotationCache<out T: Annotation>(private val annotationClass: Class<T>) {
    private val _fieldsAnnotations = hashMapOf<Class<out Any>, Map<String, T>>()
    private val _classAnnotations = hashMapOf<Class<out Any>, List<T>>()

    fun getClassAnnotations(type: Class<out Any>): List<T> {
        if(!_classAnnotations.containsKey(type))
            _classAnnotations[type] = type.getAnnotationsByType(annotationClass).toList()
        return _classAnnotations[type]!!
    }

    fun getFieldsAnnotations(type: Class<out Any>): Map<String, T> {
        if(!_fieldsAnnotations.containsKey(type))
            _fieldsAnnotations[type] = mapOf(*ReflectionUtility.getAllFieldsOf(type).filter { it.isAnnotationPresent(annotationClass) }.map { it.name to it.getAnnotation(annotationClass) }.toTypedArray())

        return _fieldsAnnotations[type]!!
    }
}