package app.models

class Region {
    val cells = mutableListOf<Cell>()

    val biome = if (cells.isEmpty()) {
        null
    } else {
        cells[0].biome
    }

    fun contains(cell: Cell): Boolean {
        if (cell.biome != this.biome) {
            return false
        }

        val borderCell = cells.find { it.pos == cell.pos }
        if (borderCell != null) {
            return true
        }

        val innerCell = cells.find { it.pos == cell.pos }
        return innerCell != null
    }

    fun mergeWith(other: Region) {
        if (other.biome != this.biome) {
            throw Exception("Attempt to merge with region $this, biome ${this.biome} with region $other of different biome ${other.biome}")
        }

        this.cells.addAll(other.cells)
    }

    fun calcBorderCells() {
        //todo
    }
}