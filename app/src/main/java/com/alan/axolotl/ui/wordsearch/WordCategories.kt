package com.alan.axolotl.ui.wordsearch

data class WordCategory(
    val name: String,
    val emoji: String,
    val words: List<String>
)

val wordCategories = listOf(
    WordCategory(
        name = "Numbers",
        emoji = "\uD83D\uDD22",
        words = listOf("ONE", "TWO", "THREE", "FOUR", "FIVE", "SIX", "SEVEN", "EIGHT", "NINE", "TEN")
    ),
    WordCategory(
        name = "Ocean Animals",
        emoji = "\uD83C\uDF0A",
        words = listOf("WHALE", "SHARK", "OCTOPUS", "DOLPHIN", "SEAL", "CRAB", "FISH", "TURTLE", "SQUID", "SHRIMP")
    ),
    WordCategory(
        name = "Countries",
        emoji = "\uD83C\uDF0D",
        words = listOf("CHINA", "JAPAN", "INDIA", "FRANCE", "SPAIN", "ITALY", "BRAZIL", "MEXICO", "CANADA", "EGYPT")
    ),
    WordCategory(
        name = "Planets",
        emoji = "\uD83E\uDE90",
        words = listOf("MERCURY", "VENUS", "EARTH", "MARS", "JUPITER", "SATURN", "URANUS", "NEPTUNE")
    ),
    WordCategory(
        name = "Animals",
        emoji = "\uD83D\uDC3E",
        words = listOf("CAT", "DOG", "LION", "TIGER", "BEAR", "HORSE", "EAGLE", "SNAKE", "FROG", "DEER")
    )
)
