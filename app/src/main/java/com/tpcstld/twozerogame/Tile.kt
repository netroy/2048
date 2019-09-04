package com.tpcstld.twozerogame

class Tile(position: Position, var value: Int) : Cell(position) {
    var mergedFrom: Array<Tile>? = null

    override fun clone(): Tile = Tile(this.position, this.value)

    fun updatePosition(p: Position) {
        this.position.x = p.x
        this.position.y = p.y
    }
}
