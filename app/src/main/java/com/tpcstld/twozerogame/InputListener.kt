package com.tpcstld.twozerogame

import android.annotation.SuppressLint
import android.support.v7.app.AlertDialog
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs

internal class InputListener(private val mView: MainView) : View.OnTouchListener {
    private var x: Float = 0.toFloat()
    private var y: Float = 0.toFloat()
    private var lastDx: Float = 0.toFloat()
    private var lastDy: Float = 0.toFloat()
    private var previousX: Float = 0.toFloat()
    private var previousY: Float = 0.toFloat()
    private var startingX: Float = 0.toFloat()
    private var startingY: Float = 0.toFloat()
    private var previousDirection = 1
    private var veryLastDirection = 1
    // Whether or not we have made a move, i.e. the blocks shifted or tried to shift.
    private var hasMoved = false
    // Whether or not we began the press on an icon. This is to disable swipes if the user began
    // the press on an icon.
    private var beganOnIcon = false

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(view: View, event: MotionEvent): Boolean {
        when (event.action) {

            MotionEvent.ACTION_DOWN -> {
                x = event.x
                y = event.y
                startingX = x
                startingY = y
                previousX = x
                previousY = y
                lastDx = 0f
                lastDy = 0f
                hasMoved = false
                beganOnIcon = iconPressed(mView.sXNewGame, mView.sYIcons) || iconPressed(mView.sXUndo, mView.sYIcons)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                x = event.x
                y = event.y
                if (mView.game.isActive && !beganOnIcon) {
                    val dx = x - previousX
                    if (abs(lastDx + dx) < abs(lastDx) + abs(dx) && abs(dx) > RESET_STARTING
                            && abs(x - startingX) > SWIPE_MIN_DISTANCE) {
                        startingX = x
                        startingY = y
                        lastDx = dx
                        previousDirection = veryLastDirection
                    }
                    if (lastDx == 0f) {
                        lastDx = dx
                    }
                    val dy = y - previousY
                    if (abs(lastDy + dy) < abs(lastDy) + abs(dy) && abs(dy) > RESET_STARTING
                            && abs(y - startingY) > SWIPE_MIN_DISTANCE) {
                        startingX = x
                        startingY = y
                        lastDy = dy
                        previousDirection = veryLastDirection
                    }
                    if (lastDy == 0f) {
                        lastDy = dy
                    }
                    if (pathMoved() > SWIPE_MIN_DISTANCE * SWIPE_MIN_DISTANCE && !hasMoved) {
                        var moved = false
                        //Vertical
                        if ((dy >= SWIPE_THRESHOLD_VELOCITY && Math.abs(dy) >= Math.abs(dx) || y - startingY >= MOVE_THRESHOLD) && previousDirection % 2 != 0) {
                            moved = true
                            previousDirection *= 2
                            veryLastDirection = 2
                            mView.game.move(2)
                        } else if ((dy <= -SWIPE_THRESHOLD_VELOCITY && Math.abs(dy) >= Math.abs(dx) || y - startingY <= -MOVE_THRESHOLD) && previousDirection % 3 != 0) {
                            moved = true
                            previousDirection *= 3
                            veryLastDirection = 3
                            mView.game.move(0)
                        }
                        //Horizontal
                        if ((dx >= SWIPE_THRESHOLD_VELOCITY && Math.abs(dx) >= Math.abs(dy) || x - startingX >= MOVE_THRESHOLD) && previousDirection % 5 != 0) {
                            moved = true
                            previousDirection *= 5
                            veryLastDirection = 5
                            mView.game.move(1)
                        } else if ((dx <= -SWIPE_THRESHOLD_VELOCITY && Math.abs(dx) >= Math.abs(dy) || x - startingX <= -MOVE_THRESHOLD) && previousDirection % 7 != 0) {
                            moved = true
                            previousDirection *= 7
                            veryLastDirection = 7
                            mView.game.move(3)
                        }
                        if (moved) {
                            hasMoved = true
                            startingX = x
                            startingY = y
                        }
                    }
                }
                previousX = x
                previousY = y
                return true
            }
            MotionEvent.ACTION_UP -> {
                x = event.x
                y = event.y
                previousDirection = 1
                veryLastDirection = 1
                //"Menu" inputs
                if (!hasMoved) {
                    if (iconPressed(mView.sXNewGame, mView.sYIcons)) {
                        if (!mView.game.gameLost()) {
                            AlertDialog.Builder(mView.context)
                                    .setPositiveButton(R.string.reset) { _, _ -> mView.game.newGame() }
                                    .setNegativeButton(R.string.continue_game, null)
                                    .setTitle(R.string.reset_dialog_title)
                                    .setMessage(R.string.reset_dialog_message)
                                    .show()
                        } else {
                            mView.game.newGame()
                        }

                    } else if (iconPressed(mView.sXUndo, mView.sYIcons)) {
                        mView.game.revertUndoState()
                    } else if (isTap(2) && inRange(mView.startingX.toFloat(), x, mView.endingX.toFloat())
                            && inRange(mView.startingY.toFloat(), x, mView.endingY.toFloat()) && mView.continueButtonEnabled) {
                        mView.game.setEndlessMode()
                    }
                }
            }
        }
        return true
    }

    private fun pathMoved(): Float =
            (x - startingX) * (x - startingX) + (y - startingY) * (y - startingY)

    private fun iconPressed(sx: Int, sy: Int): Boolean =
            (isTap(1) && inRange(sx.toFloat(), x, (sx + mView.iconSize).toFloat())
                    && inRange(sy.toFloat(), y, (sy + mView.iconSize).toFloat()))

    private fun inRange(starting: Float, check: Float, ending: Float): Boolean =
            check in starting..ending

    private fun isTap(factor: Int): Boolean =
            pathMoved() <= mView.iconSize * mView.iconSize * factor

    companion object {
        private const val SWIPE_MIN_DISTANCE = 0
        private const val SWIPE_THRESHOLD_VELOCITY = 25
        private const val MOVE_THRESHOLD = 250
        private const val RESET_STARTING = 10
    }
}
