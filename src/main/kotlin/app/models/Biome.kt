package app.models

import app.utils.colorFromRGB
import javafx.scene.paint.Color

val biomes = listOf(
    Biome("Frozen wastes", colorFromRGB(255, 255, 250)),
    Biome("City", colorFromRGB(145, 145, 145)),
    Biome("Tundra", colorFromRGB(85, 85, 70)),

    Biome("Mountains", colorFromRGB(80, 80, 75)),
    Biome("Hills", colorFromRGB(195, 145, 115)),

    Biome("Deep Ocean", colorFromRGB(20, 35, 80)),
    Biome("Ocean", colorFromRGB(40, 65, 120)),
    Biome("Sea", colorFromRGB(75, 100, 150)),

    Biome("Forest", colorFromRGB(50, 65, 50)),
    Biome("Swamp", colorFromRGB(35, 55, 65)),
    Biome("Jungle", colorFromRGB(40, 65, 45)),

    Biome("Grassland", colorFromRGB(75, 90, 75)),
    Biome("Plains", colorFromRGB(140, 145, 110)),

    Biome("Desert", colorFromRGB(250, 200, 155)),
    Biome("Steppe", colorFromRGB(100, 100, 90)),
    Biome("Savanna", colorFromRGB(130, 125, 100))
)

data class Biome(
    val Name: String,
    val Color: Color
)