package app.utils

import javafx.scene.paint.Color
import app.models.Biome
import kotlin.math.sqrt

fun findClosest(color: Color, biomes: List<Biome>): Biome {
    return biomes.minBy { it.Color.distanceTo(color) }!!
}

fun Color.distanceTo(other: Color): Double {
    return sqrt(sq(this.red - other.red) +
                sq(this.green - other.green) +
                sq(this.blue - other.blue))
}

/**
 * Expects [red], [green] and [blue] as Ints in 0..255.
 */
fun colorFromRGB(red: Int, green: Int, blue: Int): Color {
    val validColorVal = { value: Int -> value in 0..255 }
    if (!validColorVal(red) || !validColorVal(green) || !validColorVal(blue)) {
        throw Exception("Invalid color value passed!")
    }

    val colorValToDouble = { value: Int -> value / 255.0 }

    return Color(colorValToDouble(red), colorValToDouble(green), colorValToDouble(blue), 1.0)
}