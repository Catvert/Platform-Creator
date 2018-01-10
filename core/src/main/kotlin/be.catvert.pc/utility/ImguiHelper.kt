package be.catvert.pc.utility

import be.catvert.pc.GameObject
import be.catvert.pc.GameObjectTag
import be.catvert.pc.PCGame
import be.catvert.pc.Prefab
import be.catvert.pc.actions.Action
import be.catvert.pc.components.RequiredComponent
import be.catvert.pc.containers.Level
import be.catvert.pc.factories.PrefabFactory
import be.catvert.pc.i18n.MenusText
import be.catvert.pc.scenes.EditorScene
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import glm_.vec2.Vec2
import imgui.Cond
import imgui.ImGui
import imgui.ItemFlags
import imgui.functionalProgramming
import kotlin.math.roundToInt
import kotlin.reflect.KMutableProperty0
import kotlin.reflect.full.findAnnotation

object ImguiHelper {
    class Item<T>(var obj: T) {
        inline fun <reified T : Any> cast() = this as Item<T>
    }

    fun <T : Any> addImguiWidgetsArray(label: String, array: ArrayList<T>, itemLabel: (item: T) -> String, createItem: () -> T, gameObject: GameObject, level: Level, editorSceneUI: EditorScene.EditorSceneUI, itemExposeEditor: ExposeEditor = ExposeEditorFactory.empty, endBlock: () -> Unit = {}) {
        addImguiWidgetsArray(label, array, itemLabel, createItem, {
            addImguiWidget(itemLabel(it.obj), it, gameObject, level, itemExposeEditor, editorSceneUI)
        }, editorSceneUI, endBlock)
    }

    fun <T : Any> addImguiWidgetsArray(label: String, array: ArrayList<T>, itemLabel: (item: T) -> String, createItem: () -> T, itemBlock: (item: Item<T>) -> Unit, editorSceneUI: EditorScene.EditorSceneUI, endBlock: () -> Unit = {}) {
        with(ImGui) {
            if (collapsingHeader(label)) {
                functionalProgramming.withIndent {
                    for (index in 0 until array.size) {
                        pushId("remove $index")
                        if (button("Suppr.")) {
                            array.removeAt(index)
                            break
                        }

                        popId()

                        sameLine()

                        val item = Item(array[index])
                        functionalProgramming.withId("collapse $index") {
                            functionalProgramming.collapsingHeader(itemLabel(item.obj)) {
                                functionalProgramming.withId("array item $index") {
                                    itemBlock(item)
                                }
                            }
                            array[index] = item.obj
                        }
                    }
                }
                if (button("Ajouter", Vec2(-1, 0))) {
                    array.add(createItem())
                }

                endBlock()
            }
        }
    }

    fun <T : Any> addImguiWidget(label: String, item: KMutableProperty0<T>, gameObject: GameObject, level: Level, exposeEditor: ExposeEditor, editorSceneUI: EditorScene.EditorSceneUI) {
        val value = Item(item.get())

        addImguiWidget(label, value, gameObject, level, exposeEditor, editorSceneUI)
        item.set(value.obj)
    }

    fun <T : Any> addImguiWidget(label: String, item: Item<T>, gameObject: GameObject, level: Level, exposeEditor: ExposeEditor, editorSceneUI: EditorScene.EditorSceneUI) {
        val value = item.obj

        with(ReflectionUtility) {
            with(ImGui) {
                when (value) {
                    is Action -> {
                        action(label, item.cast(), gameObject, level, editorSceneUI)
                    }
                    is CustomEditorImpl -> {
                        insertImguiExposeEditorField(value, gameObject, level, editorSceneUI)
                    }
                    is Boolean -> {
                        checkbox(label, item.cast<Boolean>()::obj)
                    }
                    is Int -> {
                        functionalProgramming.withItemWidth(100f) {
                            sliderInt(label, item.cast<Int>()::obj, exposeEditor.min.roundToInt(), exposeEditor.max.roundToInt())
                        }
                    }
                    is Float -> {
                        sliderFloat(label, item.cast<Float>()::obj, exposeEditor.min, exposeEditor.max, "%.1f")
                    }
                    is Prefab -> {
                        prefab(item.cast(), level, label)
                    }
                    is Size -> {
                        size(item.cast(), Size(exposeEditor.min.roundToInt(), exposeEditor.min.roundToInt()), Size(exposeEditor.min.roundToInt(), exposeEditor.min.roundToInt()))
                    }
                    is String -> {
                        when (exposeEditor.customType) {
                            CustomType.DEFAULT -> {
                                functionalProgramming.withItemWidth(100f) {
                                    if (inputText(label, value.toCharArray())) {
                                        item.obj = value.toString() as T
                                    }
                                }
                            }
                            CustomType.TAG_STRING -> {
                                gameObjectTag(item.cast(), level)
                            }
                            else -> {
                            }
                        }
                    }
                    is Enum<*> -> {
                        enum(label, item.cast())
                    }
                    else -> {
                        insertImguiExposeEditorField(item.obj, gameObject, level, editorSceneUI)
                    }
                }
            }
        }
    }

    fun inputText(label: String, value: KMutableProperty0<String>) {
        val buf = value.get().toCharArray()

        if (ImGui.inputText(label, buf))
            value.set(String(buf))
    }

    fun action(label: String, action: KMutableProperty0<Action>, gameObject: GameObject, level: Level, editorSceneUI: EditorScene.EditorSceneUI) {
        val item = Item(action())
        action(label, item, gameObject, level, editorSceneUI)
        action.set(item.obj)
    }

    fun action(label: String, action: Item<Action>, gameObject: GameObject, level: Level, editorSceneUI: EditorScene.EditorSceneUI) {
        with(ImGui) {
            val index = intArrayOf(PCGame.actionsClasses.indexOfFirst { it.isInstance(action.obj) })

            functionalProgramming.withItemWidth(150f) {
                if (combo("action", index, PCGame.actionsClasses.map { it.simpleName ?: "Nom inconnu" })) {
                    action.obj = ReflectionUtility.findNoArgConstructor(PCGame.actionsClasses[index[0]])!!.newInstance()
                }
            }

            functionalProgramming.withId("prop $label") {
                val requiredComponent = PCGame.actionsClasses[index[0]].findAnnotation<RequiredComponent>()
                val incorrectAction = let {
                    requiredComponent?.component?.forEach {
                        if (gameObject.getStates().elementAtOrNull(editorSceneUI.gameObjectCurrentStateIndex)?.hasComponent(it) == false)
                            return@let true
                    }
                    false
                }
                pushItemFlag(ItemFlags.Disabled.i, incorrectAction)
                if (treeNode("Propriétés")) {
                    insertImguiExposeEditorField(action.obj, gameObject, level, editorSceneUI)
                    treePop()
                }
                popItemFlag()
                if (incorrectAction && isMouseHoveringRect(itemRectMin, itemRectMax)) {
                    functionalProgramming.withTooltip {
                        text("Il manque le(s) component(s) : ${requiredComponent!!.component.map { it.simpleName }}")
                    }
                }
            }

        }
    }

    fun gameObjectTag(tag: Item<GameObjectTag>, level: Level, label: String = "tag") {
        val selectedIndex = intArrayOf(level.tags.indexOfFirst { it == tag.obj })
        functionalProgramming.withItemWidth(100f) {
            if (ImGui.combo(label, selectedIndex, level.tags)) {
                tag.obj = level.tags[selectedIndex[0]]
            }
        }
    }

    fun gameObjectTag(tag: KMutableProperty0<GameObjectTag>, level: Level, label: String = "tag") {
        val item = Item(tag.get())
        gameObjectTag(item, level, label)
        tag.set(item.obj)
    }

    fun prefab(prefab: Item<Prefab>, level: Level, label: String = "prefab") {
        val prefabs = level.resourcesPrefabs() + PrefabFactory.values().map { it.prefab }

        val selectedIndex = intArrayOf(prefabs.indexOfFirst { it.name == prefab.obj.name })
        functionalProgramming.withItemWidth(100f) {
            if (ImGui.combo(label, selectedIndex, prefabs.map { it.name })) {
                prefab.obj = prefabs[selectedIndex[0]]
            }
        }
    }

    fun point(point: KMutableProperty0<Point>, minPoint: Point, maxPoint: Point, editorSceneUI: EditorScene.EditorSceneUI) {
        functionalProgramming.withItemWidth(100f) {
            val pos = intArrayOf(point.get().x, point.get().y)
            if (ImGui.inputInt2("position", pos, 0)) {
                val x = pos[0]
                val y = pos[1]

                if (x >= minPoint.x && x <= maxPoint.x && y >= minPoint.y && y <= maxPoint.y)
                    point.set(Point(x, y))
            }
            ImGui.sameLine()
            if (ImGui.button("Sélect.")) {
                editorSceneUI.editorMode = EditorScene.EditorSceneUI.EditorMode.SELECT_POINT
                editorSceneUI.onSelectPoint.register(true) {
                    point.set(it)
                }
            }
        }
    }

    fun size(size: KMutableProperty0<Size>, minSize: Size, maxSize: Size) {
        val item = Item(size.get())
        size(item, minSize, maxSize)
        size.set(item.obj)
    }

    fun size(size: Item<Size>, minSize: Size, maxSize: Size) {
        val sizeArr = intArrayOf(size.obj.width, size.obj.height)
        functionalProgramming.withItemWidth(100f) {
            if (ImGui.inputInt2("taille", sizeArr, 0)) {
                val width = sizeArr[0]
                val height = sizeArr[1]

                if (width >= minSize.width && width <= maxSize.width && height >= minSize.height && height <= maxSize.height)
                    size.obj = Size(width, height)
            }
        }
    }

    private val keys = mutableMapOf<Int, Boolean>()
    fun gdxKey(key: KMutableProperty0<Int>) {
        val keyInt = key.get()
        with(ImGui) {
            if (button(if (keys[keyInt] == true) MenusText.MM_SETTINGS_PRESSKEY() else Input.Keys.toString(keyInt), Vec2(-1, 0))) {
                if (!keys.containsKey(keyInt) || keys[keyInt] == false) {
                    keys.forEach {
                        keys[it.key] = false
                        KeyDownSignalProcessor.keyDownSignal.clear()
                    }

                    keys[keyInt] = true

                    KeyDownSignalProcessor.keyDownSignal.register(true) {
                        key.set(it)
                        keys[keyInt] = false
                    }
                }
            }
        }
    }

    fun enum(label: String, enum: Item<Enum<*>>) {
        val enumConstants = enum.obj.javaClass.enumConstants
        val selectedIndex = intArrayOf(enumConstants.indexOfFirst { it == enum.obj })

        functionalProgramming.withItemWidth(100f) {
            if (ImGui.combo(label, selectedIndex, enumConstants.map { (it as Enum<*>).name })) {
                enum.obj = enumConstants[selectedIndex[0]] as Enum<*>
            }
        }
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

    fun insertImguiExposeEditorField(instance: Any, gameObject: GameObject, level: Level, editorSceneUI: EditorScene.EditorSceneUI) {
        if (instance is CustomEditorImpl) {
            instance.insertImgui(ReflectionUtility.simpleNameOf(instance), gameObject, level, editorSceneUI)
        }

        ReflectionUtility.getAllFieldsOf(instance.javaClass).filter { it.isAnnotationPresent(ExposeEditor::class.java) }.forEach { field ->
                    field.isAccessible = true

                    val exposeField = field.getAnnotation(ExposeEditor::class.java)
                    val item = Item(field.get(instance))
                    addImguiWidget(if (exposeField.customName.isBlank()) field.name else exposeField.customName, item, gameObject, level, exposeField, editorSceneUI)
                    field.set(instance, item.obj)
                }
    }
}