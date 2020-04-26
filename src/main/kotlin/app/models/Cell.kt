package app.models

data class Cell(val pos: Pos, val biome: Biome)

data class Pos(val w: Int, val h: Int) {
    fun near(other: Pos): Boolean {
        return kotlin.math.abs(w - other.w) <= 1 && kotlin.math.abs(h - other.h) <= 1
    }
}