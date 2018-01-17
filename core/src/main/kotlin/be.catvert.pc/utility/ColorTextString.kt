package be.catvert.pc.utility

import com.badlogic.gdx.graphics.Color
import glm_.vec4.Vec4
import imgui.ImGui
import java.util.regex.Pattern

class ColorTextString {
    companion object {
        fun toString(color: Vec4, content: String): String = "[(${color.r},${color.g},${color.b}):$content]"
        fun toString(color: Color, content: String) = toString(Vec4(color.r, color.g, color.b, 1f), content)
        fun toString(content: String) = toString(Color.WHITE, content)

        private val pattern = Pattern.compile("""\\[(.*?)\\]""")

        fun toImGui(format: String) {
            val colorStr = pattern.split(format).map { it.removePrefix("[").removeSuffix("]") }

            if (colorStr.isEmpty()) {
                ImGui.text(format)
            } else {
                colorStr.forEach {

                    val colorSplit = it.substringBefore(':').removePrefix("(").removeSuffix(")").split(',')
                    val content = it.substringAfter(':')
                    if(colorSplit.size == 3)
                        ImGui.textColored(Vec4(colorSplit[0].toFloat(), colorSplit[1].toFloat(), colorSplit[2].toFloat(), 1f), content)
                    else
                        ImGui.text(format)
                }
            }
        }
    }
}