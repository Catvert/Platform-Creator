package be.catvert.pc.utility

import com.badlogic.gdx.math.Rectangle
import com.fasterxml.jackson.annotation.JsonIgnore

class Rect(position: Point = Point(), size: Size = Size()) {
    constructor(x: Int, y: Int, width: Int, height: Int): this(Point(x, y), Size(width, height))
    constructor(rect: Rect): this(rect.x, rect.y, rect.width, rect.height)

    @JsonIgnore var position = position
        set(value) {
            field = value
            onPositionChange(value)
        }
    @JsonIgnore var size = size
        set(value) {
            field = value
            onSizeChange(value)
        }

    @JsonIgnore val onPositionChange = Signal<Point>()
    @JsonIgnore val onSizeChange = Signal<Size>()

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

    fun set(size: Size, position: Point) {
        this.size = size
        this.position = position
    }

    fun move(moveX: Int, moveY: Int) {
        x += moveX
        y += moveY
    }

    fun set(rect: Rect) = this.set(rect.size, rect.position)

    fun contains(rect: Rect): Boolean {
        val xmin = rect.x
        val xmax = xmin + rect.width

        val ymin = rect.y
        val ymax = ymin + rect.height

        return xmin > x && xmin < x + width && xmax > x && xmax < x + width && ymin > y && ymin < y + height && ymax > y && ymax < y + height
    }

    fun contains(point: Point) = this.x <= point.x && this.x + this.width >= point.x && this.y <= point.y && this.y + this.height >= point.y

    fun overlaps(rect: Rect) =  this.x < rect.x + rect.width && this.x + this.width > rect.x && this.y < rect.y + rect.height && this.y + this.height > rect.y

    override fun toString(): String {
        return "{ x: $x y: $y width: $width height: $height }"
    }
}