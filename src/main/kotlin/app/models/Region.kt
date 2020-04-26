package app.models

class Region {
    val cells = mutableListOf<Cell>()

    val biome = if (cells.isEmpty()) {
        null
    } else {
        cells[0].biome
    }

    private var cellHashCodeCache = cells.hashCode()
    private var _borderCellsCache = listOf<Cell>()
    val borderCells: List<Cell>
        get() {
            return if (cellHashCodeCache == cells.hashCode()) {
                _borderCellsCache
            } else {
                cellHashCodeCache = cells.hashCode()
                _borderCellsCache = cells.filter {
                    val w = it.pos.w
                    val h = it.pos.h
                    val positionsAround = listOf(Pos(w + 1, h), Pos(w, h + 1), Pos(w - 1, h), Pos(w, h - 1))
                    positionsAround.fold(true) {
                        found, position -> found && contains(position)
                    }
                }
                _borderCellsCache
            }
        }

    fun contains(cell: Cell): Boolean {
        if (cell.biome != this.biome) {
            return false
        }

        return contains(cell.pos)
    }

    fun contains(pos: Pos): Boolean {
        val regionCell = cells.find { it.pos == pos }
        return regionCell != null
    }
}