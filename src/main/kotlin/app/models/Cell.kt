package app.models

import app.utils.sq
import kotlin.math.sqrt
import kotlin.math.abs

data class Cell(val pos: Pos, val biome: Biome)

data class Pos(val w: Int, val h: Int) {
    fun near(other: Pos): Boolean {
        return abs(w - other.w) <= 1 && abs(h - other.h) <= 1
    }

    fun distanceTo(other: Pos): Double {
        return sqrt(sq(this.w - other.w) + sq(this.h - other.h))
    }
}