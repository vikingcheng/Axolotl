package com.alan.axolotl.data

import com.alan.axolotl.ui.wordsearch.WordCategory
import com.alan.axolotl.ui.wordsearch.wordCategories
import javax.inject.Inject

/**
 * Source of the word-category data used by the "Word Search" puzzle.
 */
interface WordRepository {
    fun getCategories(): List<WordCategory>
}

class DefaultWordRepository @Inject constructor() : WordRepository {
    override fun getCategories(): List<WordCategory> = wordCategories
}
