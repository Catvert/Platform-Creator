package be.catvert.pc.utility

import be.catvert.pc.GameObject
import be.catvert.pc.GameObjectTag
import be.catvert.pc.PCGame
import be.catvert.pc.Prefab
import be.catvert.pc.actions.Action
import be.catvert.pc.actions.Actions
import be.catvert.pc.components.RequiredComponent
import be.catvert.pc.components.graphics.AtlasComponent
import be.catvert.pc.containers.Level
import be.catvert.pc.factories.PrefabFactory
import be.catvert.pc.i18n.MenusText
import be.catvert.pc.managers.ResourceManager
import be.catvert.pc.scenes.EditorScene
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.graphics.Color
import glm_.func.common.clamp
import glm_.vec2.Vec2
import glm_.vec4.Vec4
import imgui.*
import uno.kotlin.isPrintable
import kotlin.math.roundToInt
import kotlin.reflect.KMutableProperty0
import kotlin.reflect.full.findAnnotation

object ImGuiHelper {
    class Item<T>(var obj: T) {
        inline fun <reified T : Any> cast() = this as Item<T>
    }

    private val settingsBtnIconHandle: Int = ResourceManager.getTexture(Constants.uiDirPath.child("settings.png")).textureObjectHandle
    private val favBtnIconHandle: Int = ResourceManager.getTexture(Constants.uiDirPath.child("fav.png")).textureObjectHandle
    private val tickButtonSound: Sound? = ResourceManager.getSound(Constants.gameDirPath.child("tick.mp3"))

    private val hoveredTickButtons = mutableSetOf<Int>()

    private val searchBarBuffers = mutableMapOf<String, Item<String>>()

    fun <T : Any> addImguiWidgetsArray(label: String, array: ArrayList<T>, itemLabel: (item: T) -> String, createItem: () -> T, gameObject: GameObject, level: Level, editorSceneUI: EditorScene.EditorSceneUI, itemExposeEditor: ExposeEditor = ExposeEditorFactory.empty, endBlock: () -> Unit = {}) {
        addImguiWidgetsArray(label, array, itemLabel, createItem, {
            addImguiWidget(itemLabel(it.obj), it, gameObject, level, itemExposeEditor, editorSceneUI)
        }, endBlock)
    }

    fun <T : Any> addImguiWidgetsArray(label: String, array: ArrayList<T>, itemLabel: (item: T) -> String, createItem: () -> T, itemBlock: (item: Item<T>) -> Unit, endBlock: () -> Unit = {}) {
        with(ImGui) {
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
                    if (collapsingHeader("")) {
                        sameLine(0f, style.itemInnerSpacing.x)
                        text(itemLabel(item.obj))

                        functionalProgramming.withIndent {
                            functionalProgramming.withId("array item $index") {
                                itemBlock(item)
                            }
                        }
                    } else {
                        sameLine(0f, style.itemInnerSpacing.x)
                        text(itemLabel(item.obj))
                    }

                    array[index] = item.obj
                }
            }
            functionalProgramming.withId("$label add btn") {
                if (button("Ajouter", Vec2(if (array.isEmpty()) Constants.defaultWidgetsWidth else -1f, 0))) {
                    array.add(createItem())
                }
            }
            endBlock()
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
                        insertImguiExposeEditorFields(value, gameObject, level, editorSceneUI)
                    }
                    is Boolean -> {
                        checkbox(label, item.cast<Boolean>()::obj)
                    }
                    is Int -> {
                        functionalProgramming.withItemWidth(Constants.defaultWidgetsWidth) {
                            val item = item.cast<Int>()
                            inputInt(label, item::obj)
                            item.obj = item.obj.clamp(exposeEditor.min.roundToInt(), exposeEditor.max.roundToInt())
                        }
                    }
                    is Float -> {
                        functionalProgramming.withItemWidth(Constants.defaultWidgetsWidth) {
                            sliderFloat(label, item.cast<Float>()::obj, exposeEditor.min, exposeEditor.max, "%.1f")
                        }
                    }
                    is Prefab -> {
                        prefab(item.cast(), level, label)
                    }
                    is Size -> {
                        size(item.cast(), Size(exposeEditor.min.roundToInt(), exposeEditor.min.roundToInt()), Size(exposeEditor.max.roundToInt(), exposeEditor.max.roundToInt()))
                    }
                    is String -> {
                        when (exposeEditor.customType) {
                            CustomType.DEFAULT -> {
                                ImGuiHelper.inputText(label, item.cast())
                            }
                            CustomType.TAG_STRING -> {
                                gameObjectTag(item.cast(), level)
                            }
                        }
                    }
                    is Enum<*> -> {
                        enum(label, item.cast())
                    }
                    else -> {
                        insertImguiExposeEditorFields(item.obj, gameObject, level, editorSceneUI)
                    }
                }

                if (!exposeEditor.description.isBlank() && isItemHovered()) {
                    functionalProgramming.withTooltip {
                        text(exposeEditor.description)
                    }
                }
            }
        }
    }

    fun inputText(label: String, text: Item<String>) {
        val buf = text.obj.toCharArray(CharArray(32))
        functionalProgramming.withItemWidth(Constants.defaultWidgetsWidth) {
            if (ImGui.inputText(label, buf))
                text.obj = String(buf.filter { it.isPrintable }.toCharArray())
        }
    }

    fun inputText(label: String, value: KMutableProperty0<String>) {
        val item = Item(value())
        inputText(label, item)
        value.set(item.obj)
    }

    fun action(label: String, action: KMutableProperty0<Action>, gameObject: GameObject, level: Level, editorSceneUI: EditorScene.EditorSceneUI) {
        val item = Item(action())
        action(label, item, gameObject, level, editorSceneUI)
        action.set(item.obj)
    }

    fun action(label: String, action: Item<Action>, gameObject: GameObject, level: Level, editorSceneUI: EditorScene.EditorSceneUI) {
        with(ImGui) {
            val index = intArrayOf(Actions.values().indexOfFirst { it.action.isInstance(action.obj) })

            functionalProgramming.withId("prop $label") {

                val actionClass = Actions.values()[index[0]].action

                val requiredComponent = actionClass.findAnnotation<RequiredComponent>()
                val incorrectAction = let {
                    requiredComponent?.component?.forEach {
                        if (!gameObject.getStateOrDefault(editorSceneUI.gameObjectCurrentStateIndex).hasComponent(it))
                            return@let true
                    }
                    false
                }

                if (comboWithSettingsButton(label, index, Actions.values().map { it.name }, {
                            if (incorrectAction) {
                                textColored(Color.RED, "\\!/ Il manque le component :")
                                text("${requiredComponent!!.component.map { it.simpleName }}")
                            }
                            insertImguiExposeEditorFields(action.obj, gameObject, level, editorSceneUI)
                        }, searchBar = true)) {
                    action.obj = ReflectionUtility.findNoArgConstructor(Actions.values()[index[0]].action)!!.newInstance()
                }

                val description = actionClass.findAnnotation<Description>()
                if (description != null) {
                    sameLine(0f, style.itemInnerSpacing.x)
                    text("(?)")

                    if (isItemHovered()) {
                        functionalProgramming.withTooltip {
                            text(description.description)
                        }
                    }
                }
            }
        }
    }

    fun comboWithSettingsButton(label: String, currentItem: IntArray, items: List<String>, popupBlock: () -> Unit, settingsBtnDisabled: Boolean = false, onSettingsBtnDisabled: () -> Unit = {}, searchBar: Boolean = false): Boolean {
        val popupTitle = "popup settings $label"

        var comboChanged = false

        with(ImGui) {
            functionalProgramming.withItemWidth(Constants.defaultWidgetsWidth - g.fontSize - g.style.itemInnerSpacing.x * 3f) {
                functionalProgramming.withId(label) {
                    if (searchBar) {
                        if (searchCombo("", currentItem, items))
                            comboChanged = true
                    } else if (combo("", currentItem, items))
                        comboChanged = true
                }
            }

            sameLine(0f, style.itemInnerSpacing.x)

            pushItemFlag(ItemFlags.Disabled.i, settingsBtnDisabled)
            if (settingsButton())
                openPopup(popupTitle)
            popItemFlag()

            if (settingsBtnDisabled)
                onSettingsBtnDisabled()

            sameLine(0f, style.itemInnerSpacing.x)

            text(label)

            functionalProgramming.popup(popupTitle) {
                popupBlock()
            }
        }

        return comboChanged
    }

    fun comboWithSettingsButton(label: String, currentItem: KMutableProperty0<Int>, items: List<String>, popupBlock: () -> Unit, settingsBtnDisabled: Boolean = false, onSettingsBtnDisabled: () -> Unit = {}, searchBar: Boolean = false) {
        val item = intArrayOf(currentItem.get())
        comboWithSettingsButton(label, item, items, popupBlock, settingsBtnDisabled, onSettingsBtnDisabled, searchBar)
        currentItem.set(item[0])
    }

    fun searchCombo(label: String, currentItem: IntArray, items: List<String>, selectableHovered: (index: Int) -> Unit = {}): Boolean {
        var changed = false

        with(ImGui) {
            if (beginCombo(label, items.elementAtOrNull(currentItem[0]))) {
                cursorPosX += style.itemInnerSpacing.x
                val buf = searchBarBuffers.getOrPut(label) { Item("") }
                inputText("", buf)
                cursorPosY += style.itemInnerSpacing.y

                separator()

                for (i in 0 until items.size) {
                    if (!items[i].startsWith(buf.obj, true))
                        continue
                    pushId(i)
                    val itemSelected = i == currentItem[0]
                    val itemText = items.getOrElse(i, { "*Unknown item*" })
                    if (selectable(itemText, itemSelected)) {
                        currentItem[0] = i
                        changed = true
                    }
                    if (itemSelected) setItemDefaultFocus()

                    if (isItemHovered())
                        selectableHovered(i)

                    popId()
                }

                endCombo()
            }
        }
        return changed
    }

    fun settingsButton(size: Vec2 = Vec2(g.fontSize)) = ImGui.imageButton(settingsBtnIconHandle, size, uv1 = Vec2(1, 1))
    fun favButton(size: Vec2 = Vec2(g.fontSize)) = ImGui.imageButton(favBtnIconHandle, size, uv1 = Vec2(1, 1))

    fun gameObjectTag(tag: Item<GameObjectTag>, level: Level, label: String = "tag") {
        val selectedIndex = intArrayOf(level.tags.indexOfFirst { it == tag.obj })
        functionalProgramming.withItemWidth(Constants.defaultWidgetsWidth) {
            if (ImGui.combo(label, selectedIndex, level.tags)) {
                tag.obj = level.tags[selectedIndex[0]]
            }
        }
    }

    fun gameObject(gameObject: KMutableProperty0<GameObject?>, level: Level, editorSceneUI: EditorScene.EditorSceneUI, label: String = "game object") {
        val favTitle = "set gameobject fav"

        with(ImGui) {
            if (button("Sélect. $label", Vec2(Constants.defaultWidgetsWidth - g.fontSize - g.style.itemInnerSpacing.x * 3f, 0))) {
                editorSceneUI.editorMode = EditorScene.EditorSceneUI.EditorMode.SELECT_GO
                editorSceneUI.onSelectGO.register(true) {
                    gameObject.set(it)
                }
            }

            if (isItemHovered()) {
                functionalProgramming.withTooltip {
                    val go = gameObject.get()
                    if (go == null)
                        textColored(Color.RED, "aucun game object sélectionner")
                    else
                        textPropertyColored(Color.ORANGE, "game object actuel :", go.name)
                }
            }

            sameLine(0f, style.itemInnerSpacing.x)

            if (favButton())
                openPopup(favTitle)

            val fav = favoritesPopup(favTitle, level)

            if (fav != null) {
                gameObject.set(fav)
            }
        }
    }

    private val favoritesIndex = mutableMapOf<String, IntArray>()
    fun favoritesPopup(id: String, level: Level): GameObject? {
        var gameObject: GameObject? = null

        with(ImGui) {
            functionalProgramming.popup(id) {
                if (level.favoris.isNotEmpty()) {
                    functionalProgramming.withItemWidth(Constants.defaultWidgetsWidth) {
                        searchCombo("favoris", favoritesIndex.getOrPut(id, { intArrayOf(0) }), level.favoris.map { it.name }, {
                            val fav = level.favoris[it]

                            atlasPreviewTooltip(fav)
                        })
                    }

                    if (button("Sélectionner", Vec2(-1, 0))) {
                        gameObject = level.favoris[favoritesIndex[id]!![0]]
                        closeCurrentPopup()
                    }
                } else {
                    text("Aucun favoris n'est présent dans votre collection.")
                }
            }
        }
        return gameObject
    }

    fun atlasPreviewTooltip(gameObject: GameObject) {
        val atlas = gameObject.getCurrentState().getComponent<AtlasComponent>()
        if (atlas != null) {
            functionalProgramming.withStyleColor(Col.PopupBg, ImGui.getStyleColorVec4(Col.PopupBg).apply { a = 0.5f }) {
                functionalProgramming.withTooltip {
                    val region = atlas.data[atlas.currentIndex].currentKeyFrame()
                    ImGui.image(region.texture.textureObjectHandle, Vec2(gameObject.box.width, gameObject.box.height), Vec2(region.u, region.v), Vec2(region.u2, region.v2))
                }
            }
        }
    }

    fun prefab(prefab: Item<Prefab>, level: Level, label: String = "prefab") {
        val prefabs = level.resourcesPrefabs() + PrefabFactory.values().map { it.prefab }

        val selectedIndex = intArrayOf(prefabs.indexOfFirst { it.name == prefab.obj.name })
        functionalProgramming.withItemWidth(Constants.defaultWidgetsWidth) {
            if (ImGui.combo(label, selectedIndex, prefabs.map { it.name })) {
                prefab.obj = prefabs[selectedIndex[0]]
            }
        }
    }

    fun point(point: KMutableProperty0<Point>, minPoint: Point, maxPoint: Point, editorSceneUI: EditorScene.EditorSceneUI) {
        functionalProgramming.withItemWidth(Constants.defaultWidgetsWidth) {
            val pos = floatArrayOf(point.get().x, point.get().y)
            if (ImGui.inputFloat2("position", pos, 0)) {
                val x = pos[0]
                val y = pos[1]

                if (x >= minPoint.x && x <= maxPoint.x && y >= minPoint.y && y <= maxPoint.y)
                    point.set(Point(x, y))
            }
            ImGui.sameLine()

            val moveYCursor = 0

            ImGui.cursorPosY += moveYCursor
            functionalProgramming.withStyleVar(StyleVar.FrameRounding, 15f) {
                if (ImGui.button("Sélect.")) {
                    editorSceneUI.editorMode = EditorScene.EditorSceneUI.EditorMode.SELECT_POINT
                    editorSceneUI.onSelectPoint.register(true) {
                        point.set(Point(it.x.clamp(minPoint.x, maxPoint.x), it.y.clamp(minPoint.y, maxPoint.y)))
                    }
                }
            }
            ImGui.cursorPosY -= moveYCursor
        }
    }

    fun size(size: KMutableProperty0<Size>, minSize: Size, maxSize: Size) {
        val item = Item(size.get())
        size(item, minSize, maxSize)
        size.set(item.obj)
    }

    fun size(size: Item<Size>, minSize: Size, maxSize: Size) {
        val sizeArr = intArrayOf(size.obj.width, size.obj.height)
        functionalProgramming.withItemWidth(Constants.defaultWidgetsWidth) {
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
                        PCInputProcessor.keyDownSignal.clear()
                    }

                    keys[keyInt] = true

                    PCInputProcessor.keyDownSignal.register(true) {
                        key.set(it)
                        keys[keyInt] = false
                    }
                }
            }
        }
    }

    fun enumWithSettingsButton(label: String, enum: Item<Enum<*>>, popupBlock: () -> Unit, settingsBtnDisabled: Boolean = false, onSettingsBtnDisabled: () -> Unit = {}) {
        val enumConstants = enum.obj.javaClass.enumConstants
        val selectedIndex = intArrayOf(enumConstants.indexOfFirst { it == enum.obj })

        if (ImGuiHelper.comboWithSettingsButton(label, selectedIndex, enumConstants.map { (it as Enum<*>).name }, popupBlock, settingsBtnDisabled, onSettingsBtnDisabled))
            enum.obj = enumConstants[selectedIndex[0]]
    }

    fun enum(label: String, enum: Item<Enum<*>>) {
        val enumConstants = enum.obj.javaClass.enumConstants
        val selectedIndex = intArrayOf(enumConstants.indexOfFirst { it == enum.obj })

        functionalProgramming.withItemWidth(Constants.defaultWidgetsWidth) {
            if (ImGui.combo(label, selectedIndex, enumConstants.map { (it as Enum<*>).name })) {
                enum.obj = enumConstants[selectedIndex[0]]
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

    fun textColored(color: Color, content: Any) {
        ImGui.textColored(Vec4(color.r, color.g, color.b, 1f), content.toString())
    }

    fun textPropertyColored(color: Color, propertyName: String, value: Any) {
        textColored(color, propertyName)
        ImGui.sameLine()

        value.cast<CustomEditorTextImpl>()?.insertText() ?: ImGui.text(value.toString())
    }

    fun tickSoundButton(label: String, size: Vec2): Boolean {
        val pressed = ImGui.button(label, size)

        val id = ImGui.currentWindow.dc.lastItemId

        if (ImGui.isItemHovered()) {
            if (!hoveredTickButtons.contains(id)) {
                tickButtonSound?.play(PCGame.soundVolume)
                hoveredTickButtons.add(id)
            }
        } else hoveredTickButtons.remove(id)

        return pressed
    }

    fun withMenuButtonsStyle(block: () -> Unit) {
        with(ImGui) {
            pushStyleColor(Col.WindowBg, ImGui.getStyleColorVec4(Col.WindowBg).apply { a = 0f })
            pushStyleColor(Col.Button, Vec4(0.2f, 0.5f, 0.9f, 1f))
            pushStyleColor(Col.Text, Vec4(1f))
            pushStyleColor(Col.Border, ImGui.getStyleColorVec4(Col.Border).apply { a = 0f })
            pushStyleVar(StyleVar.FrameRounding, 10f)
            pushStyleVar(StyleVar.FramePadding, Vec2(10f))
            pushFont(PCGame.imguiBigFont)

            block()

            ImGui.popStyleColor(4)
            ImGui.popStyleVar(2)
            popFont()
        }
    }

    fun insertImguiExposeEditorFields(instance: Any, gameObject: GameObject, level: Level, editorSceneUI: EditorScene.EditorSceneUI) {
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

    fun insertImguiTextExposeEditorFields(instance: Any) {
        with(ImGui) {
            instance.cast<CustomEditorTextImpl>()?.insertText()
                    ?: if (instance.toString().isNotBlank()) text(instance.toString())
            ReflectionUtility.getAllFieldsOf(instance.javaClass).filter { it.isAnnotationPresent(ExposeEditor::class.java) }.forEach { field ->
                field.isAccessible = true

                val exposeField = field.getAnnotation(ExposeEditor::class.java)
                val value = field.get(instance)

                textPropertyColored(Color.ORANGE, "${if (exposeField.customName.isBlank()) field.name else exposeField.customName} :", value)
            }
        }
    }
}