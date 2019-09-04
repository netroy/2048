package com.tpcstld.twozerogame

import kotlin.math.floor

class Grid(size: Int) {
    val field: Field = Field(size)
    val undoField: Field = Field(size)
    private val bufferField: Field = Field(size)

    init {
        clearGrid()
        clearUndoGrid()
    }

    fun areCellsAvailable() = field.availablePositions().isNotEmpty()
    private fun availablePositions() = field.availablePositions()

    fun randomAvailablePosition(): Position? {
        val available = availablePositions()
        return if (available.isNotEmpty()) {
            available[floor(Math.random() * available.size).toInt()]
        } else null
    }

    fun isCellAvailable(p: Position) = !isCellOccupied(p)

    private fun isCellOccupied(p: Position): Boolean = field.hasContent(p)

    fun getCellContent(p: Position): Tile? =
            if (field.hasContent(p))
                field.get(p) as Tile
            else null

    fun isCellWithinBounds(p: Position): Boolean = field.isWithinBounds(p)

    fun insertTile(tile: Tile) {
        field.set(tile.position, tile)
    }

    fun removeTile(tile: Tile) {
        field.unset(tile.position)
    }

    fun saveTiles() {
        undoField.copyFrom(bufferField)
    }

    fun prepareSaveTiles() {
        bufferField.copyFrom(field)
    }

    fun revertTiles() {
        field.copyFrom(undoField)
    }

    fun clearGrid() {
        field.clear()
    }

    private fun clearUndoGrid() {
        undoField.clear()
    }
}
