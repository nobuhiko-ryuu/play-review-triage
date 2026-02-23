package app.playreviewtriage.core.time

import javax.inject.Inject

interface Clock {
    fun nowEpochSec(): Long
}

class SystemClock @Inject constructor() : Clock {
    override fun nowEpochSec(): Long = System.currentTimeMillis() / 1000
}
