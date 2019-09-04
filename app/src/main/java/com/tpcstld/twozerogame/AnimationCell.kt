package com.tpcstld.twozerogame

import kotlin.math.max

class AnimationCell(position: Position, val animationType: Int, private val animationTime: Long, private val delayTime: Long, val extras: IntArray?) : Cell(position) {
    private var timeElapsed: Long = 0

    override fun clone(): AnimationCell = AnimationCell(this.position, this.animationType, this.animationTime, this.delayTime, this.extras)

    val percentageDone: Double
        get() = max(0.0, 1.0 * (timeElapsed - delayTime) / animationTime)

    val isActive: Boolean
        get() = timeElapsed >= delayTime

    fun tick(elapsed: Long) {
        timeElapsed += elapsed
    }

    fun animationDone(): Boolean =
            animationTime + delayTime < timeElapsed

}
