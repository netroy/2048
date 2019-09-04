package com.tpcstld.twozerogame

data class Position(var x: Int = 0, var y: Int = 0)

abstract class Cell(val position: Position) {
    abstract fun clone(): Cell
}
