package be.catvert.pc.utility

import be.catvert.pc.GameObject
import be.catvert.pc.containers.Level
import be.catvert.pc.scenes.EditorScene
import com.fasterxml.jackson.annotation.JsonIgnore
import imgui.ImGui
import imgui.functionalProgramming

class Rect(position: Point = Point(), size: Size = Size()) : CustomEditorImpl {
    constructor(x: Int, y: Int, width: Int, height: Int) : this(Point(x, y), Size(width, height))
    constructor(rect: Rect) : this(rect.x, rect.y, rect.width, rect.height)

    @JsonIgnore
    var position = position
        set(value) {
            field = value
            onPositionChange(value)
        }
    @JsonIgnore
    var size = size
        set(value) {
            field = value
            onSizeChange(value)
        }

    @JsonIgnore
    val onPositionChange = Signal<Point>()
    @JsonIgnore
    val onSizeChange = Signal<Size>()

    var x: Int
        get() = position.x
        set(value) {
            position = Point(value, y)
        }
    var y: Int
        get() = position.y
        set(value) {
            position = Point(x, value)
        }
    var width: Int
        get() = size.width
        set(value) {
            size = Size(value, height)
        }
    var height: Int
        get() = size.height
        set(value) {
            size = Size(width, value)
        }

    fun left() = x
    fun right() = x + width
    fun bottom() = y
    fun top() = y + height

    fun center() = Point(right() / 2, top() / 2)

    fun set(size: Size, position: Point) {
        this.size = size
        this.position = position
    }

    fun move(moveX: Int, moveY: Int) {
        x += moveX
        y += moveY
    }

    fun set(rect: Rect) = this.set(rect.size, rect.position)

    fun contains(rect: Rect, borderless: Boolean) =
            if (borderless)
                rect.left() >= this.left() && rect.right() <= this.right() && rect.bottom() >= this.bottom() && rect.top() <= this.top()
            else
                rect.left() > this.left() && rect.right() < this.right() && rect.bottom() > this.bottom() && rect.top() <= this.top()


    fun contains(point: Point) = this.x <= point.x && this.right() >= point.x && this.y <= point.y && this.top() >= point.y

    fun overlaps(rect: Rect) = this.x < rect.right() && this.right() > rect.x && this.y < rect.top() && this.top() > rect.y

    override fun insertImgui(label: String, gameObject: GameObject, level: Level, editorSceneUI: EditorScene.EditorSceneUI) {
        with(ImGui) {
            if (collapsingHeader(label)) {
                functionalProgramming.withIndent {
                    ImguiHelper.point(::position, Point(), Point(level.matrixRect.width, level.matrixRect.height), editorSceneUI)
                    ImguiHelper.addImguiWidget("taille", ::size, gameObject, level, ExposeEditorFactory.createExposeEditor(max = Constants.maxGameObjectSize.toFloat()), editorSceneUI)
                }
            }
        }
    }

    override fun toString(): String = "{ x: $x y: $y width: $width height: $height }"
}