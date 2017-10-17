package be.catvert.pc.utility

import com.badlogic.gdx.math.Rectangle
import com.fasterxml.jackson.annotation.JsonIgnore

data class Rect(@JsonIgnore var position: Point = Point(), @JsonIgnore var size: Size = Size()) {
    constructor(x: Int, y: Int, width: Int, height: Int): this(Point(x, y), Size(width, height))

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

    fun set(rect: Rect) = this.set(rect.size, rect.position)

    fun contains(rect: Rect): Boolean {
        val xmin = rect.x
        val xmax = xmin + rect.width

        val ymin = rect.y
        val ymax = ymin + rect.height

        return xmin > x && xmin < x + width && xmax > x && xmax < x + width && ymin > y && ymin < y + height && ymax > y && ymax < y + height
    }

    fun contains(point: Point): Boolean {
        return this.x <= point.x && this.x + this.width >= point.x && this.y <= point.y && this.y + this.height >= point.y
    }
}