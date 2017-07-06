package be.catvert.plateformcreator.ecs.components

import be.catvert.plateformcreator.Copyable
import be.catvert.plateformcreator.Point
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.utils.JsonValue
import com.badlogic.gdx.utils.JsonWriter
import ktx.ashley.mapperFor

/**
 * Created by Catvert on 2/07/17.
 */

/**
 * Interface permettant d'implémenter un nouveau type de paramètre
 * @param T Le paramètre
 * @param V Le type de valeur que le paramètre implémente
 */
interface Parameter<T : Parameter<T, *>, V : Any> : Copyable<T> {
    val type: ParameterType
    var value: V

    companion object {
        /**
         * Permet de charger un paramètre depuis le fichier de configuration d'un niveau
         * @param type Le type de valeur du paramètre
         * @param it La valeur JSON
         */
        fun loadParameterFromJson(type: ParameterType, it: JsonValue) = when (type) {
            ParameterType.Point -> loadParameter(Point(it["x"].asInt(), it["y"].asInt()))
            ParameterType.Boolean -> loadParameter(it["value"].asBoolean())
            ParameterType.Integer -> loadParameter(it["value"].asInt())
        }

        /**
         * Permet de charger un paramètre selon le type de valeur qu'il implémente
         * @param value La valeur que le paramètre doit prendre
         */
        inline fun <reified V : Any> loadParameter(value: V): Parameter<*, V> = when (value) {
            is Point -> PointParameter(value) as Parameter<*, V>
            is Boolean -> BooleanParameter(value) as Parameter<*, V>
            is Int -> IntParameter(value) as Parameter<*, V>
            else -> {
                throw Exception("Impossible de créer le paramètre depuis la valeur : $value")
            }
        }
    }

    /**
     * Permet de sauvegarder le paramètre
     * @param writer Le writer utilisé pour l'enregistrement
     */
    fun saveParam(writer: JsonWriter)

    /**
     * Méthode appelée quand le paramètre est dessiner dans l'éditeur (optionel)
     */
    fun onDrawInEditor(entity: Entity, shapeRenderer: ShapeRenderer) {}
}

/**
 * Classe de donnée représentant un paramètre de type Point
 */
class PointParameter(override var value: Point) : Parameter<PointParameter, Point> {
    override val type = ParameterType.Point

    private val transformMapper = mapperFor<TransformComponent>()

    override fun saveParam(writer: JsonWriter) {
        writer.name("x").value(value.x)
        writer.name("y").value(value.y)
    }

    override fun copy(): PointParameter {
        return PointParameter(Point(value.x, value.y))
    }

    override fun onDrawInEditor(entity: Entity, shapeRenderer: ShapeRenderer) {
        shapeRenderer.color = Color.BLACK
        val pos = transformMapper[entity].position()
        shapeRenderer.line(pos.x.toFloat(), pos.y.toFloat(), value.x.toFloat(), value.y.toFloat())
        shapeRenderer.circle(value.x.toFloat(), value.y.toFloat(), 5f)
    }
}

/**
 * Classe de donnée représentant un paramètre de type booléenne
 */
class BooleanParameter(override var value: Boolean) : Parameter<BooleanParameter, Boolean> {
    override val type = ParameterType.Boolean

    override fun saveParam(writer: JsonWriter) {
        writer.name("value").value(value)
    }

    override fun copy(): BooleanParameter {
        return BooleanParameter(value)
    }
}

class IntParameter(value: Int, var min: Int? = null, var max: Int? = null) : Parameter<IntParameter, Int> {
    override var value: Int = value
        set(value) {
            var correctValue = true

            if ((min != null && value < min!!) || (max != null && value > max!!))
                correctValue = false

            if (correctValue)
                field = value
        }

    override val type = ParameterType.Integer

    override fun saveParam(writer: JsonWriter) {
        writer.name("value").value(value)
    }

    override fun copy(): IntParameter {
        return IntParameter(value)
    }
}

/**
 * Enum permettant de décrire le type que le paramètre utilise
 */
enum class ParameterType(val type: String) {
    Point("point"),
    Boolean("bool"),
    Integer("int");

    companion object {
        /**
         * Permet de retourner le type du paramètre depuis un string
         */
        fun getType(type: String) = ParameterType.values().first { it.type == type }

        /**
         * Permet de sauvegarder le type du paramètre
         */
        fun saveType(writer: JsonWriter, paramType: ParameterType) {
            writer.name("type").value(paramType.type)
        }
    }
}

/**
 * Classe permettant d'implémenter un paramètre à une entité
 */
class EntityParameter<V : Any>(val id: Int, val description: String, var param: Parameter<*, V>, val drawInEditor: Boolean = false) : Copyable<EntityParameter<V>> {
    companion object {
        inline operator fun <reified V : Any> invoke(id: Int, description: String, value: V, drawInEditor: Boolean = false): EntityParameter<V> = EntityParameter(id, description, Parameter.loadParameter(value), drawInEditor)
    }

    /**
     * Permet de retourner la valeur du paramètre
     */
    fun getValue(): V = param.value

    /**
     * Permet d'assigner une valeur au paramètre
     */
    fun setValue(newValue: V) {
        param.value = newValue
    }

    /**
     * Permet de caster l'EntityParameter vers son vrai type
     */
    inline fun <reified NV : Any> cast(): EntityParameter<NV> {
        if (NV::class.java.isAssignableFrom(param.value::class.java))
            return this as EntityParameter<NV>
        else
            throw Exception("Impossible de cast le paramètre vers ${NV::class.java.name}")
    }

    inline fun <reified NV : Parameter<NV, *>> castParam(): NV {
        return param as NV
    }

    /**
     * Permet de copier cette classe pour réimplémenter le paramètre dans une autre entité (deep copy)
     */
    override fun copy(): EntityParameter<V> {
        return EntityParameter(id, description, param.copy(), drawInEditor) as EntityParameter<V>
    }
}

/**
 * Ce component permet d'implémenter plusieurs paramètres à l'entité
 */
class ParametersComponent(vararg params: EntityParameter<*>) : BaseComponent<ParametersComponent>() {
    private var parameters = mutableListOf<EntityParameter<*>>()

    init {
        params.forEach {
            addParameter(it)
        }
    }

    companion object {
        /**
         * Permet à l'entité de se construire avec ses paramètres par défaut
         */
        val defaultParameters = listOf<EntityParameter<*>>()
    }

    /**
     * Permet de retourner l'ensemble des paramètres ajoutées à l'entité
     */
    fun getParameters() = parameters.toList()

    /**
     * Permet d'ajouter un paramètre à l'entité
     */
    fun addParameter(param: EntityParameter<*>) = parameters.add(param)

    fun removeParameter(param: EntityParameter<*>) = parameters.remove(param)

    operator fun plus(param: EntityParameter<*>) = addParameter(param)
    operator fun minus(param: EntityParameter<*>) = removeParameter(param)

    override fun copy(): ParametersComponent {
        val parametersComponent = ParametersComponent()

        parameters.forEach {
            parametersComponent + it.copy()
        }

        return parametersComponent
    }
}

fun parametersComponent(init: ParametersComponent.() -> Unit): ParametersComponent {
    val parametersComp = ParametersComponent()

    parametersComp.init()

    return parametersComp
}