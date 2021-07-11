package app.models

data class Cell(val pos: Pos, val biome: Biome)

data class Pos(val w: Int, val h: Int) {
    fun getNeighbors(): List<Pos> {
        return listOf(
            Pos(this.w + 1, this.h),
            Pos(this.w - 1, this.h),
            Pos(this.w, this.h + 1),
            Pos(this.w, this.h - 1)
        )
    }
}