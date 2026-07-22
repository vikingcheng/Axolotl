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
        words = listOf(
            "WHALE",
            "DOLPHIN",
            "ORCA",
            "SHARK",
            "HAMMERHEAD_SHARK",
            "SEAL",
            "SEA_LION",
            "WALRUS",
            "SEA_OTTER",
            "MANATEE",

            "OCTOPUS",
            "SQUID",
            "CUTTLEFISH",
            "JELLYFISH",
            "STARFISH",
            "SEA_URCHIN",
            "SEA_CUCUMBER",
            "CORAL",
            "SEA_ANEMONE",
            "NAUTILUS",

            "SEA_TURTLE",
            "FISH",
            "CLOWNFISH",
            "SEAHORSE",
            "STINGRAY",
            "EEL",
            "CRAB",
            "LOBSTER",
            "SHRIMP",
            "SCALLOP"
        )
    ),
    WordCategory(
        name = "Countries",
        emoji = "\uD83C\uDF0D",
        words = listOf(
            "CHINA",
            "UNITED_STATES",
            "CANADA",
            "MEXICO",
            "BRAZIL",
            "ARGENTINA",
            "CHILE",
            "COLOMBIA",
            "PERU",
            "UNITED_KINGDOM",

            "FRANCE",
            "GERMANY",
            "ITALY",
            "SPAIN",
            "PORTUGAL",
            "NETHERLANDS",
            "BELGIUM",
            "SWITZERLAND",
            "AUSTRIA",
            "POLAND",

            "SWEDEN",
            "NORWAY",
            "DENMARK",
            "FINLAND",
            "IRELAND",
            "GREECE",
            "TURKEY",
            "UKRAINE",
            "RUSSIA",
            "INDIA",

            "PAKISTAN",
            "BANGLADESH",
            "JAPAN",
            "SOUTH_KOREA",
            "THAILAND",
            "VIETNAM",
            "INDONESIA",
            "PHILIPPINES",
            "MALAYSIA",
            "SINGAPORE",

            "SAUDI_ARABIA",
            "UNITED_ARAB_EMIRATES",
            "ISRAEL",
            "EGYPT",
            "SOUTH_AFRICA",
            "NIGERIA",
            "KENYA",
            "AUSTRALIA",
            "NEW_ZEALAND",
            "MOROCCO"
        )
    ),
    WordCategory(
        name = "Planets",
        emoji = "\uD83E\uDE90",
        words = listOf("MERCURY", "VENUS", "EARTH", "MARS", "JUPITER", "SATURN", "URANUS", "NEPTUNE")
    ),
    WordCategory(
        name = "Animals",
        emoji = "\uD83D\uDC3E",
        words = listOf(
            "CAT",
            "DOG",
            "LION",
            "TIGER",
            "LEOPARD",
            "CHEETAH",
            "BEAR",
            "WOLF",
            "FOX",
            "HYENA",

            "ELEPHANT",
            "RHINOCEROS",
            "HIPPOPOTAMUS",
            "GIRAFFE",
            "ZEBRA",
            "HORSE",
            "DONKEY",
            "CAMEL",
            "COW",
            "GOAT",

            "SHEEP",
            "PIG",
            "DEER",
            "MOOSE",
            "KANGAROO",
            "PANDA",
            "GORILLA",
            "MONKEY",
            "RABBIT",
            "SQUIRREL",

            "EAGLE",
            "OWL",
            "PARROT",
            "PEACOCK",
            "FLAMINGO",
            "PENGUIN",
            "OSTRICH",
            "DUCK",
            "GOOSE",
            "CHICKEN",

            "SNAKE",
            "LIZARD",
            "TURTLE",
            "CROCODILE",
            "ALLIGATOR",
            "FROG",
            "TOAD",
            "SALAMANDER",
            "NEWT",
            "CHAMELEON"
        )
    )
)
