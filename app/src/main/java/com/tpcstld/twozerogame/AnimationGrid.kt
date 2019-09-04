package com.tpcstld.twozerogame

import java.util.*

typealias AnimationCellList = ArrayList<AnimationCell>
typealias AnimationField = Array<Array<AnimationCellList>>

class AnimationGrid(size: Int) {
    val globalAnimation = AnimationCellList()
    private val field: AnimationField = AnimationField(size) { Array(size) { AnimationCellList() } }
    private var activeAnimations = 0
    private var oneMoreFrame = false

    val isAnimationActive: Boolean
        get() {
            return when {
                activeAnimations != 0 -> {
                    oneMoreFrame = true
                    true
                }
                oneMoreFrame -> {
                    oneMoreFrame = false
                    true
                }
                else -> false
            }
        }

    fun startAnimation(p: Position, animationType: Int, length: Long, delay: Long, extras: IntArray?) {
        val animationToAdd = AnimationCell(p, animationType, length, delay, extras)
        if (p.x == -1 && p.y == -1) {
            globalAnimation.add(animationToAdd)
        } else {
            field[p.x][p.y].add(animationToAdd)
        }
        activeAnimations += 1
    }

    fun tickAll(timeElapsed: Long) {
        val cancelledAnimations = AnimationCellList()
        for (animation in globalAnimation) {
            animation.tick(timeElapsed)
            if (animation.animationDone()) {
                cancelledAnimations.add(animation)
                activeAnimations -= 1
            }
        }

        for (array in field) {
            for (list in array) {
                for (animation in list) {
                    animation.tick(timeElapsed)
                    if (animation.animationDone()) {
                        cancelledAnimations.add(animation)
                        activeAnimations -= 1
                    }
                }
            }
        }

        for (animation in cancelledAnimations) {
            cancelAnimation(animation)
        }
    }

    fun getAnimationCell(p: Position): AnimationCellList = field[p.x][p.y]

    fun cancelAnimations() {
        field.forEach { array -> array.forEach { list -> list.clear() } }
        globalAnimation.clear()
        activeAnimations = 0
    }

    private fun cancelAnimation(animation: AnimationCell) {
        val p = animation.position
        if (p.x == -1 && p.y == -1) {
            globalAnimation.remove(animation)
        } else {
            field[p.x][p.y].remove(animation)
        }
    }

}
