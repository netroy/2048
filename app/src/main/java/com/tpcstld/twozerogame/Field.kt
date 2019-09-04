package com.tpcstld.twozerogame

class Field(val size: Int) {
    private val bound = size - 1
    private var values: Array<Array<Tile?>> = empty()

    fun get(p: Position) = values[p.x][p.y]

    fun set(p: Position, value: Tile) {
        values[p.x][p.y] = value
    }

    fun unset(p: Position) {
        values[p.x][p.y] = null
    }

    fun clear() {
        values = empty()
    }

    fun hasContent(p: Position): Boolean = isWithinBounds(p) && notNullAt(p)

    fun isWithinBounds(p: Position): Boolean = p.x in 0..bound && p.y in 0..bound

    private fun notNullAt(p: Position): Boolean = values[p.x][p.y] != null

    fun copyFrom(source: Field) {
        source.forEach { p, tile ->
            if (tile == null) {
                unset(p)
            } else {
                set(p, tile.clone())
            }
        }
    }

    fun availableTiles(): ArrayList<Tile> {
        val entries = ArrayList<Tile>()
        forEach{ _, tile -> if (tile != null) entries.add(tile) }
        return entries
    }

    fun availablePositions(): ArrayList<Position> {
        val entries = ArrayList<Position>()
        forEach{ p, tile -> if (tile == null) entries.add(p) }
        return entries
    }

    fun forEach(fn: (Position, Tile?) -> Unit) {
        for (x in 0..bound) {
            for (y in 0..bound) {
                val p = Position(x, y)
                fn(p, get(p))
            }
        }
    }

    private fun empty(): Array<Array<Tile?>> = Array(size) { arrayOfNulls<Tile>(size) }
}