package com.alan.axolotl.ui.read

import androidx.annotation.StringRes
import com.alan.axolotl.R

enum class Difficulty(@StringRes val labelRes: Int) {
    EASY(R.string.read_level_easy),
    MEDIUM(R.string.read_level_medium),
    HARD(R.string.read_level_hard)
}

data class SentenceLevel(
    val difficulty: Difficulty,
    val sentences: List<String>
)

val sentenceLevels = listOf(
    SentenceLevel(
        difficulty = Difficulty.EASY,
        sentences = listOf(
            "The cat is big.",
            "I like dogs.",
            "The sun is hot.",
            "My name is Alan.",
            "I can run fast.",
            "The fish can swim.",
            "I see a bird.",
            "The ball is red.",
            "I have a hat.",
            "We go to school."
        )
    ),
    SentenceLevel(
        difficulty = Difficulty.MEDIUM,
        sentences = listOf(
            "Alan went to the park with his dog.",
            "The little frog jumped into the pond.",
            "I like to eat apples and bananas.",
            "The blue bird is singing a song.",
            "My mom reads me a story every night.",
            "The happy children played in the garden.",
            "We saw three ducks at the lake.",
            "The big bear lives in the forest.",
            "I want to ride my red bicycle.",
            "The stars are shining in the sky."
        )
    ),
    SentenceLevel(
        difficulty = Difficulty.HARD,
        sentences = listOf(
            "The dolphins jumped over the waves happily.",
            "Alan and his friends built a sandcastle at the beach.",
            "The butterfly flew from flower to flower in the garden.",
            "We watched the fireworks light up the dark sky.",
            "The brave knight rode his horse through the forest.",
            "Penguins live in Antarctica where it is very cold.",
            "The astronaut floated in space looking at Earth below.",
            "My grandmother makes the best chocolate chip cookies.",
            "The rainbow appeared after the rain stopped falling.",
            "Octopuses have eight arms and live in the ocean."
        )
    )
)
