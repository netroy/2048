package com.tpcstld.twozerogame

import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v7.app.AppCompatActivity
import android.view.KeyEvent

class MainActivity : AppCompatActivity() {
    private var view: MainView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        view = MainView(this)

        val settings = PreferenceManager.getDefaultSharedPreferences(this)
        view!!.hasSaveState = settings.getBoolean("save_state", false)

        if (savedInstanceState != null) {
            if (savedInstanceState.getBoolean("hasState")) {
                load()
            }
        }
        setContentView(view)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_MENU -> //Do nothing
                return true
            KeyEvent.KEYCODE_DPAD_DOWN -> {
                view!!.game.move(2)
                return true
            }
            KeyEvent.KEYCODE_DPAD_UP -> {
                view!!.game.move(0)
                return true
            }
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                view!!.game.move(3)
                return true
            }
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                view!!.game.move(1)
                return true
            }
            else -> return super.onKeyDown(keyCode, event)
        }
    }

    public override fun onSaveInstanceState(savedInstanceState: Bundle) {
        super.onSaveInstanceState(savedInstanceState)
        savedInstanceState.putBoolean("hasState", true)
        save()
    }

    override fun onPause() {
        super.onPause()
        save()
    }

    private fun save() {
        val settings = PreferenceManager.getDefaultSharedPreferences(this)
        val editor = settings.edit()
        val field = view!!.game.grid.field
        val undoField = view!!.game.grid.undoField
        editor.putInt(WIDTH, field.size)
        editor.putInt(HEIGHT, field.size)
        field.forEach { p, tile ->
            val key = "$p.x $p.y"
            if (tile != null) {
                editor.putInt(key, tile.value)
            } else {
                editor.putInt(key, 0)
            }

            val undoTile = undoField.get(p)
            val undoKey = "$UNDO_GRID$p.x $p.y"
            if (undoTile != null) {
                editor.putInt(undoKey, undoTile.value)
            } else {
                editor.putInt(undoKey, 0)
            }
        }
        editor.putLong(SCORE, view!!.game.score)
        editor.putLong(HIGH_SCORE, view!!.game.highScore)
        editor.putLong(UNDO_SCORE, view!!.game.lastScore)
        editor.putBoolean(CAN_UNDO, view!!.game.canUndo)
        editor.putInt(GAME_STATE, view!!.game.gameState)
        editor.putInt(UNDO_GAME_STATE, view!!.game.lastGameState)
        editor.apply()
    }

    override fun onResume() {
        super.onResume()
        load()
    }

    private fun load() {
        //Stopping all animations
        view!!.game.aGrid.cancelAnimations()

        val settings = PreferenceManager.getDefaultSharedPreferences(this)
        val tileField = view!!.game.grid.field
        tileField.forEach { p, _ ->
            val value = settings.getInt("$p.x $p.y", -1)
            if (value > 0) {
                tileField.set(p, Tile(p, value))
            } else if (value == 0) {
                tileField.unset(p)
            }

            val undoField = view!!.game.grid.undoField
            val undoValue = settings.getInt("$UNDO_GRID$p.x $p.y", -1)
            if (undoValue > 0) {
                undoField.set(p, Tile(p, undoValue))
            } else if (value == 0) {
                undoField.unset(p)
            }
        }

        view!!.game.score = settings.getLong(SCORE, view!!.game.score)
        view!!.game.highScore = settings.getLong(HIGH_SCORE, view!!.game.highScore)
        view!!.game.lastScore = settings.getLong(UNDO_SCORE, view!!.game.lastScore)
        view!!.game.canUndo = settings.getBoolean(CAN_UNDO, view!!.game.canUndo)
        view!!.game.gameState = settings.getInt(GAME_STATE, view!!.game.gameState)
        view!!.game.lastGameState = settings.getInt(UNDO_GAME_STATE, view!!.game.lastGameState)
    }

    companion object {
        private const val WIDTH = "width"
        private const val HEIGHT = "height"
        private const val SCORE = "score"
        private const val HIGH_SCORE = "high score temp"
        private const val UNDO_SCORE = "undo score"
        private const val CAN_UNDO = "can undo"
        private const val UNDO_GRID = "undo"
        private const val GAME_STATE = "game state"
        private const val UNDO_GAME_STATE = "undo game state"
    }
}
