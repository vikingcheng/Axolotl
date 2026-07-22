package com.alan.axolotl.data

import com.alan.axolotl.ui.read.SentenceLevel
import com.alan.axolotl.ui.read.sentenceLevels
import javax.inject.Inject

/**
 * Source of the reading-practice sentences used by the "Read" feature.
 */
interface SentenceRepository {
    fun getLevels(): List<SentenceLevel>
}

class DefaultSentenceRepository @Inject constructor() : SentenceRepository {
    override fun getLevels(): List<SentenceLevel> = sentenceLevels
}
