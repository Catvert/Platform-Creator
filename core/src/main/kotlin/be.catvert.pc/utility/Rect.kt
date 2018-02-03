package be.catvert.pc.utility

import be.catvert.pc.GameObject
import be.catvert.pc.containers.Level
import be.catvert.pc.scenes.EditorScene
import com.fasterxml.jackson.annotation.JsonIgnore
import imgui.ImGui
import kotlin.math.roundToInt

class Rect(position: Point = Point(), size: Size = Size()) : CustomEditorImpl {
    constructor(x: Float, y: Float, width: Int, height: Int) : this(Point(x, y), Size(width, height))
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

    var x: Float
        get() = position.x
        set(value) {
            position = Point(value, y)
        }
    var y: Float
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

    fun center() = Point(x + width / 2f, y + height / 2f)

    fun set(size: Size, position: Point) {
        this.size = size
        this.position = position
    }

    fun move(moveX: Float, moveY: Float) {
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

    fun overlaps(rect: Rect) = this.left() < rect.right() && this.right() > rect.left() && this.bottom() < rect.top() && this.top() > rect.bottom()

    fun merge(rect: Rect): Rect {
        val minX = Math.min(x, rect.x)
        val maxX = Math.max(x + width, rect.x + rect.width)
        val x = minX
        val width = maxX - minX

        val minY = Math.min(y, rect.y)
        val maxY = Math.max(y + height, rect.y + rect.height)
        val y = minY
        val height = maxY - minY

        return Rect(x, y, width.roundToInt(), height.roundToInt())
    }

    override fun insertImgui(label: String, gameObject: GameObject, level: Level, editorSceneUI: EditorScene.EditorSceneUI) {
        with(ImGui) {
            ImGuiHelper.point(::position, Point(), Point(level.matrixRect.width.toFloat() - this@Rect.width, level.matrixRect.height.toFloat() - this@Rect.height), editorSceneUI)
            ImGuiHelper.size(::size, Size(1), Size(Constants.maxGameObjectSize))
        }
    }

    override fun equals(other: Any?): Boolean {
        if (other is Rect)
            return other.position == this.position && other.size == this.size
        return super.equals(other)
    }

    override fun toString(): String = "{ x: ${x.roundToInt()} y: ${y.roundToInt()} width: $width height: $height }"
}