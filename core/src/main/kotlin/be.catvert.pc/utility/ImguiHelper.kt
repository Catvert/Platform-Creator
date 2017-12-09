package be.catvert.pc.utility

import be.catvert.pc.GameObject
import be.catvert.pc.GameObjectTag
import be.catvert.pc.PCGame
import be.catvert.pc.Prefab
import be.catvert.pc.actions.Action
import be.catvert.pc.containers.Level
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import glm_.vec2.Vec2
import imgui.Cond
import imgui.ImGui
import imgui.functionalProgramming
import io.leangen.geantyref.GenericTypeReflector
import java.lang.reflect.GenericArrayType
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty0

object ImguiHelper {
    class Item<T>(var obj: T) {
        inline fun <reified T : Any> cast() = this as Item<T>
    }

    inline fun <reified T : Any> addImguiWidgetsArray(label: String, array: ArrayList<T>, crossinline createItem: () -> T, itemBlock: (item: Item<T>) -> Boolean, endBlock: () -> Unit = {}): Boolean {
        var valueChanged = false

        with(ImGui) {
            if (collapsingHeader(label)) {
                functionalProgramming.withIndent {
                    for (index in 0 until array.size) {
                        pushId("suppr $index")
                        if (button("Suppr.")) {
                            array.removeAt(index)
                            valueChanged = true
                            break
                        }
                        popId()

                        sameLine()
                        functionalProgramming.withId(index) {
                            val item = Item(array[index])
                            if (itemBlock(item))
                                valueChanged = true
                            array[index] = item.obj
                        }
                    }
                }
                if (button("Ajouter", Vec2(-1, 20f))) {
                    array.add(createItem())
                    valueChanged = true
                }

                endBlock()
            }
        }

        return valueChanged
    }

    inline fun <reified T : Any> addImguiWidget(label: String, item: KMutableProperty0<T>, gameObject: GameObject, level: Level, exposeEditor: ExposeEditor) {
        val value = Item(item.get())

        if (addImguiWidget(label, value, gameObject, level, exposeEditor)) {
            item.set(value.obj)
        }
    }

    inline fun <reified T : Any> addImguiWidget(label: String, item: Item<T>, gameObject: GameObject, level: Level, exposeEditor: ExposeEditor): Boolean {
        var valueChanged = false

        val value = item.obj

        with(ReflectionUtility) {
            with(ImGui) {
                when (value) {
                    is Action -> {
                        valueChanged = action(label, item.cast(), gameObject, level)
                    }
                    is CustomEditorImpl -> {
                        custom(label, item.cast<CustomEditorImpl>().obj, gameObject, level)
                    }
                    is Boolean -> {
                        valueChanged = checkbox(label, item.cast<Boolean>()::obj)
                    }
                    is Int -> {
                        when (exposeEditor.customType) {
                            CustomType.DEFAULT -> {
                                functionalProgramming.withItemWidth(100f) {
                                    valueChanged = sliderInt(label, item.cast<Int>()::obj, exposeEditor.min, exposeEditor.max)
                                }
                            }
                            CustomType.KEY_INT -> {
                                valueChanged = gdxKey(item.cast())
                            }
                            else -> {}
                        }
                    }
                    is Prefab -> {
                        valueChanged = prefab(item.cast(), level, label)
                    }
                    is Size -> {
                        valueChanged = size(item.cast(), Size(exposeEditor.min, exposeEditor.min), Size(exposeEditor.min, exposeEditor.min))
                    }
                    is Point -> {
                        valueChanged = point(item.cast(), Point(exposeEditor.min, exposeEditor.min), Point(exposeEditor.min, exposeEditor.min))
                    }
                    is String -> {
                        when(exposeEditor.customType) {
                            CustomType.DEFAULT -> {
                                functionalProgramming.withItemWidth(100f) {
                                    if (inputText(label, value.toCharArray())) {
                                        item.obj = value.toString() as T
                                        valueChanged = true
                                    }
                                }
                            }
                            CustomType.TAG_STRING -> {
                                valueChanged = gameObjectTag(item.cast(), level)
                            }
                            else -> {}
                        }
                    }
                    is Enum<*> -> {
                        valueChanged = enum(label, item.cast())
                    }
                    else -> {
                        text(ReflectionUtility.simpleNameOf(value))
                    }
                }
            }
        }

        return valueChanged
    }

    fun inputText(label: String, value: KMutableProperty0<String>) {
        val buf = value.get().toCharArray()

        if(ImGui.inputText(label, buf))
            value.set(String(buf))
    }

    fun custom(label: String, value: CustomEditorImpl, gameObject: GameObject, level: Level) {
        value.insertImgui(label, gameObject, level)
    }

    fun action(label: String, action: KMutableProperty0<Action>, gameObject: GameObject, level: Level): Boolean {
        val item = Item(action())
        return action(label, item, gameObject, level).apply { action.set(item.obj) }
    }

    fun action(label: String, action: Item<Action>, gameObject: GameObject, level: Level): Boolean {
        var valueChanged = false
        with(ImGui) {
            if (treeNode(label)) {
                val index = intArrayOf(PCGame.actionsClasses.indexOfFirst { it.isInstance(action.obj) })

                functionalProgramming.withItemWidth(150f) {
                    if (combo("action", index, PCGame.actionsClasses.map { it.simpleName ?: "Nom inconnu" })) {
                        action.obj = ReflectionUtility.findNoArgConstructor(PCGame.actionsClasses[index[0]])!!.newInstance()
                        valueChanged = true
                    }
                }

                if (treeNode("Propriétés")) {
                    insertImguiExposeEditorField(action.obj, gameObject, level)
                    treePop()
                }

                treePop()
            }
        }
        return valueChanged
    }

    fun gameObjectTag(tag: Item<GameObjectTag>, level: Level, label: String = "tag"): Boolean {
        var valueChanged = false
        val selectedIndex = intArrayOf(level.tags.indexOfFirst { it == tag.obj })
        functionalProgramming.withItemWidth(100f) {
            if(ImGui.combo(label, selectedIndex, level.tags)) {
                tag.obj = level.tags[selectedIndex[0]]
                valueChanged = true
            }
        }
        return valueChanged
    }

    fun gameObjectTag(tag: KMutableProperty0<GameObjectTag>, level: Level, label: String = "tag"): Boolean {
        val item = Item(tag.get())
        return gameObjectTag(item, level, label).apply { tag.set(item.obj) }
    }

    fun prefab(prefab: Item<Prefab>, level: Level, label: String = "prefab"): Boolean {
        var valueChanged = false
        val selectedIndex = intArrayOf(level.resourcesPrefabs().indexOfFirst { it.name == prefab.obj.name })
        functionalProgramming.withItemWidth(100f) {
            if(ImGui.combo(label, selectedIndex, level.resourcesPrefabs().map { it.name })) {
                prefab.obj = level.resourcesPrefabs()[selectedIndex[0]]
                valueChanged = true
            }
        }
        return valueChanged
    }

    fun point(point: Item<Point>, minPoint: Point, maxPoint: Point): Boolean {
        var valueChanged = false
        val pos = intArrayOf(point.obj.x, point.obj.y)
        functionalProgramming.withItemWidth(150f) {
            if (ImGui.inputInt2("position", pos, 0)) {
                val x = pos[0]
                val y = pos[1]

                if (x >= minPoint.x && x <= maxPoint.x && y >= minPoint.y && y <= maxPoint.y)
                    point.obj = Point(x, y)
                valueChanged = true
            }
        }
        return valueChanged
    }

    fun size(size: Item<Size>, minSize: Size, maxSize: Size): Boolean {
        var valueChanged = false
        val sizeArr = intArrayOf(size.obj.width, size.obj.height)
        functionalProgramming.withItemWidth(150f) {
            if (ImGui.inputInt2("taille", sizeArr, 0)) {
                val width = sizeArr[0]
                val height = sizeArr[1]

                if (width >= minSize.width && width <= maxSize.width && height >= minSize.height && height <= maxSize.height)
                    size.obj = Size(width, height)
                valueChanged = true
            }
        }
        return valueChanged
    }

    fun gdxKey(key: Item<Int>): Boolean {
        var valueChanged = false

        val value = Input.Keys.toString(key.obj).toCharArray()
        functionalProgramming.withItemWidth(100f) {
            if (ImGui.inputText("touche", value)) {
                key.obj = Input.Keys.valueOf(String(value))
                valueChanged = true
            }
        }

        return valueChanged
    }

    fun enum(label: String, enum: Item<Enum<*>>): Boolean {
        var valueChanged = false

        val enumConstants = enum.obj.javaClass.enumConstants
        val selectedIndex = intArrayOf(enumConstants.indexOfFirst { it == enum.obj })

        functionalProgramming.withItemWidth(100f) {
            if (ImGui.combo(label, selectedIndex, enumConstants.map { (it as Enum<*>).name })) {
                enum.obj = enumConstants[selectedIndex[0]] as Enum<*>
                valueChanged = true
            }
        }

        return valueChanged
    }

    fun withCenteredWindow(name: String, open: KMutableProperty0<Boolean>? = null, size: Vec2, flags: Int = 0, centerCond: Cond = Cond.Once, block: () -> Unit) {
        ImGui.setNextWindowSize(size, centerCond)
        ImGui.setNextWindowPos(Vec2(Gdx.graphics.width / 2f - size.x / 2f, Gdx.graphics.height / 2f - size.y / 2f), centerCond)
        functionalProgramming.withWindow(name, open, flags) {
            block()
        }
    }

    fun popupModal(name: String, open: KMutableProperty0<Boolean>? = null, extraFlags: Int = 0, block: () -> Unit) {
        val bool = if (open != null) booleanArrayOf(open()) else null
        functionalProgramming.popupModal(name, bool, extraFlags, block)
        open?.set(bool!![0])
    }

    fun insertImguiExposeEditorField(instance: Any, gameObject: GameObject, level: Level) {
        ReflectionUtility.getAllFieldsOf(instance.javaClass).filter { it.isAnnotationPresent(ExposeEditor::class.java) }.forEach { field ->
            field.isAccessible = true

            val exposeField = field.getAnnotation(ExposeEditor::class.java)
            val item = Item(field.get(instance))
            if (addImguiWidget(if (exposeField.customName.isBlank()) field.name else exposeField.customName, item, gameObject, level, exposeField)) {
                field.set(instance, item.obj)
            }
        }

        if (instance is CustomEditorImpl) {
            instance.insertImgui(ReflectionUtility.simpleNameOf(instance), gameObject, level)
        }
    }
}