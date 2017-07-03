package be.catvert.plateformcreator.ecs.components

/**
 * Created by catvert on 2/07/17.
 */

data class EntityParameter<T>(var param: T? = null, val description: String, val drawInEditor: Boolean = false) {

}

class ParametersComponent(vararg params: EntityParameter<*>) : BaseComponent<ParametersComponent>() {
    private var parameters = mutableListOf<EntityParameter<*>>()

    init {
        params.forEach {
            addParameter(it)
        }
    }

    fun getParameters() = parameters.toList()

    fun addParameter(param: EntityParameter<*>) {
        parameters.add(param)
    }

    override fun copy(): ParametersComponent {
        val parametersComponent = ParametersComponent()
        parametersComponent.parameters = parameters
        return ParametersComponent()
    }
}