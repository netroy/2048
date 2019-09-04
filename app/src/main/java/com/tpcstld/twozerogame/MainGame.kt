package com.tpcstld.twozerogame

import android.content.Context
import android.preference.PreferenceManager
import java.util.*
import kotlin.math.pow

class MainGame(private val mContext: Context, private val mView: MainView) {
    val dimensions = 4
    var gameState = GAME_NORMAL
    var lastGameState = GAME_NORMAL
    var grid: Grid = Grid(dimensions)
    var aGrid: AnimationGrid = AnimationGrid(dimensions)
    var canUndo: Boolean = false
    var score: Long = 0
    var highScore: Long = 0
    var lastScore: Long = 0
    private var bufferGameState = GAME_NORMAL
    private var bufferScore: Long = 0
    private var endingMaxValue: Int = 2.0.pow((mView.numCellTypes - 1).toDouble()).toInt()

    val isActive get() = !(gameWon() || gameLost())

    fun newGame() {
        prepareUndoState()
        saveUndoState()
        grid.clearGrid()
        highScore = getStoredHighScore()
        if (score >= highScore) {
            highScore = score
            recordHighScore()
        }
        score = 0
        gameState = GAME_NORMAL
        addStartTiles()
        mView.showHelp = firstRun()
        mView.refreshLastTime = true
        mView.resyncTime()
        mView.invalidate()
    }

    private fun addStartTiles() {
        val startTiles = 2
        for (xx in 0 until startTiles) {
            this.addRandomTile()
        }
    }

    private fun addRandomTile() {
        if (grid.areCellsAvailable()) {
            val value = if (Math.random() < 0.9) 2 else 4
            val p = grid.randomAvailablePosition()!!
            val tile = Tile(p, value)
            spawnTile(tile)
        }
    }

    private fun spawnTile(tile: Tile) {
        grid.insertTile(tile)
        aGrid.startAnimation(tile.position, SPAWN_ANIMATION,
                SPAWN_ANIMATION_TIME, MOVE_ANIMATION_TIME, null) //Direction: -1 = EXPANDING
    }

    private fun recordHighScore() {
        val settings = PreferenceManager.getDefaultSharedPreferences(mContext)
        val editor = settings.edit()
        editor.putLong(HIGH_SCORE, highScore)
        editor.apply()
    }

    private fun getStoredHighScore(): Long {
        val settings = PreferenceManager.getDefaultSharedPreferences(mContext)
        return settings.getLong(HIGH_SCORE, -1)
    }

    private fun firstRun(): Boolean {
        val settings = PreferenceManager.getDefaultSharedPreferences(mContext)
        if (settings.getBoolean(FIRST_RUN, true)) {
            val editor = settings.edit()
            editor.putBoolean(FIRST_RUN, false)
            editor.apply()
            return true
        }
        return false
    }

    private fun prepareTiles() {
        grid.field.availableTiles().forEach { tile -> tile.mergedFrom = null }
    }

    private fun moveTile(tile: Tile, p: Position) {
        grid.field.unset(tile.position)
        grid.field.set(p, tile)
        tile.updatePosition(p)
    }

    private fun saveUndoState() {
        grid.saveTiles()
        canUndo = true
        lastScore = bufferScore
        lastGameState = bufferGameState
    }

    private fun prepareUndoState() {
        grid.prepareSaveTiles()
        bufferScore = score
        bufferGameState = gameState
    }

    fun revertUndoState() {
        if (canUndo) {
            canUndo = false
            aGrid.cancelAnimations()
            grid.revertTiles()
            score = lastScore
            gameState = lastGameState
            mView.refreshLastTime = true
            mView.invalidate()
        }
    }

    fun gameWon(): Boolean {
        return gameState > 0 && gameState % 2 != 0
    }

    fun gameLost(): Boolean {
        return gameState == GAME_LOST
    }

    fun move(direction: Int) {
        aGrid.cancelAnimations()
        // 0: up, 1: right, 2: down, 3: left
        if (!isActive) return

        this.prepareUndoState()
        val vector = getVector(direction)
        val traversalsX = buildTraversalsX(vector)
        val traversalsY = buildTraversalsY(vector)
        var moved = false

        prepareTiles()

        for (xx in traversalsX) {
            for (yy in traversalsY) {
                val p = Position(xx, yy)
                val tile = grid.getCellContent(p)

                if (tile != null) {
                    val (previous, next) = findFarthestPosition(p, vector)
                    val nextTile = grid.getCellContent(next)

                    if (nextTile != null && nextTile.value == tile.value && nextTile.mergedFrom == null) {
                        val merged = Tile(next, tile.value * 2)
                        val temp = arrayOf(tile, nextTile)
                        merged.mergedFrom = temp

                        grid.insertTile(merged)
                        grid.removeTile(tile)

                        // Converge the two tiles' positions
                        tile.updatePosition(next)

                        val extras = intArrayOf(xx, yy)
                        aGrid.startAnimation(merged.position, MOVE_ANIMATION,
                                MOVE_ANIMATION_TIME, 0, extras) //Direction: 0 = MOVING MERGED
                        aGrid.startAnimation(merged.position, MERGE_ANIMATION,
                                SPAWN_ANIMATION_TIME, MOVE_ANIMATION_TIME, null)

                        // Update the score
                        score += merged.value
                        highScore = score.coerceAtLeast(highScore)

                        // The mighty 2048 tile
                        if (merged.value >= winValue() && !gameWon()) {
                            gameState += GAME_WIN // Set win state
                            endGame()
                        }
                    } else {
                        moveTile(tile, previous)
                        val extras = intArrayOf(xx, yy, 0)
                        aGrid.startAnimation(previous, MOVE_ANIMATION, MOVE_ANIMATION_TIME, 0, extras) //Direction: 1 = MOVING NO MERGE
                    }

                    if (!positionsEqual(p, tile.position)) {
                        moved = true
                    }
                }
            }
        }

        if (moved) {
            saveUndoState()
            addRandomTile()
            checkLose()
        }
        mView.resyncTime()
        mView.invalidate()
    }

    private fun checkLose() {
        if (!movesAvailable() && !gameWon()) {
            gameState = GAME_LOST
            endGame()
        }
    }

    private fun endGame() {
        aGrid.startAnimation(Position(-1, -1), FADE_GLOBAL_ANIMATION, NOTIFICATION_ANIMATION_TIME, NOTIFICATION_DELAY_TIME, null)
        if (score >= highScore) {
            highScore = score
            recordHighScore()
        }
    }

    private fun getVector(direction: Int): Position {
        return directions[direction]
    }

    private fun buildTraversalsX(vector: Position): List<Int> {
        val traversals = ArrayList<Int>()

        for (xx in 0 until dimensions) {
            traversals.add(xx)
        }
        if (vector.x == 1) {
            traversals.reverse()
        }

        return traversals
    }

    private fun buildTraversalsY(vector: Position): List<Int> {
        val traversals = ArrayList<Int>()

        for (xx in 0 until dimensions) {
            traversals.add(xx)
        }
        if (vector.y == 1) {
            traversals.reverse()
        }

        return traversals
    }

    private fun findFarthestPosition(p: Position, vector: Position): Pair<Position, Position> {
        var previous: Position
        var next = p
        do {
            previous = next
            next = Position(previous.x + vector.x, previous.y + vector.y)
        } while (grid.isCellWithinBounds(next) && grid.isCellAvailable(next))

        return Pair(previous, next)
    }

    private fun movesAvailable(): Boolean {
        return grid.areCellsAvailable() || tileMatchesAvailable()
    }

    private fun tileMatchesAvailable(): Boolean {
        var tile: Tile?

        for (xx in 0 until dimensions) {
            for (yy in 0 until dimensions) {
                tile = grid.getCellContent(Position(xx, yy))

                if (tile != null) {
                    for (direction in 0..3) {
                        val vector = getVector(direction)
                        val p = Position(xx + vector.x, yy + vector.y)

                        val other = grid.getCellContent(p)

                        if (other != null && other.value == tile.value) {
                            return true
                        }
                    }
                }
            }
        }

        return false
    }

    private fun positionsEqual(first: Position, second: Position) =
            first.x == second.x && first.y == second.y

    private fun winValue(): Int {
        return if (!canContinue()) {
            endingMaxValue
        } else {
            startingMaxValue
        }
    }

    fun setEndlessMode() {
        gameState = GAME_ENDLESS
        mView.invalidate()
        mView.refreshLastTime = true
    }

    fun canContinue(): Boolean {
        return !(gameState == GAME_ENDLESS || gameState == GAME_ENDLESS_WON)
    }

    companion object {
        const val SPAWN_ANIMATION = -1
        const val MOVE_ANIMATION = 0
        const val MERGE_ANIMATION = 1

        const val FADE_GLOBAL_ANIMATION = 0
        private const val MOVE_ANIMATION_TIME = MainView.BASE_ANIMATION_TIME.toLong()
        private const val SPAWN_ANIMATION_TIME = MainView.BASE_ANIMATION_TIME.toLong()
        private const val NOTIFICATION_DELAY_TIME = MOVE_ANIMATION_TIME + SPAWN_ANIMATION_TIME
        private const val NOTIFICATION_ANIMATION_TIME = (MainView.BASE_ANIMATION_TIME * 5).toLong()
        private const val startingMaxValue = 2048
        //Odd state = game is not active
        //Even state = game is active
        //Win state = active state + 1
        private const val GAME_WIN = 1
        private const val GAME_LOST = -1
        private const val GAME_NORMAL = 0
        private const val GAME_ENDLESS = 2
        private const val GAME_ENDLESS_WON = 3
        private const val HIGH_SCORE = "high score"
        private const val FIRST_RUN = "first run"

        private val directions = arrayOf(
                Position(0, -1), // up
                Position(1, 0),  // right
                Position(0, 1),  // down
                Position(-1, 0)  // left
        )
    }
}
