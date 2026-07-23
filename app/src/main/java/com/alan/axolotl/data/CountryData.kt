package com.alan.axolotl.data

/**
 * The continents used by the geography quiz.
 *
 * [displayName] is what the child sees on the answer buttons; [emoji] is the
 * matching globe, kept for decoration.
 */
enum class Continent(val displayName: String, val emoji: String) {
    AFRICA("Africa", "🌍"),
    ASIA("Asia", "🌏"),
    EUROPE("Europe", "🌍"),
    NORTH_AMERICA("North America", "🌎"),
    SOUTH_AMERICA("South America", "🌎"),
    OCEANIA("Oceania", "🌏")
}

data class Country(
    val name: String,
    val flagEmoji: String,
    val continent: Continent
)

val allCountries = listOf(
    // North America (incl. Central America & the Caribbean)
    Country("United States", "🇺🇸", Continent.NORTH_AMERICA),
    Country("Canada", "🇨🇦", Continent.NORTH_AMERICA),
    Country("Mexico", "🇲🇽", Continent.NORTH_AMERICA),
    Country("Cuba", "🇨🇺", Continent.NORTH_AMERICA),
    Country("Jamaica", "🇯🇲", Continent.NORTH_AMERICA),
    Country("Panama", "🇵🇦", Continent.NORTH_AMERICA),
    Country("Costa Rica", "🇨🇷", Continent.NORTH_AMERICA),
    Country("Honduras", "🇭🇳", Continent.NORTH_AMERICA),

    // South America
    Country("Brazil", "🇧🇷", Continent.SOUTH_AMERICA),
    Country("Argentina", "🇦🇷", Continent.SOUTH_AMERICA),
    Country("Colombia", "🇨🇴", Continent.SOUTH_AMERICA),
    Country("Peru", "🇵🇪", Continent.SOUTH_AMERICA),
    Country("Chile", "🇨🇱", Continent.SOUTH_AMERICA),
    Country("Venezuela", "🇻🇪", Continent.SOUTH_AMERICA),
    Country("Ecuador", "🇪🇨", Continent.SOUTH_AMERICA),
    Country("Bolivia", "🇧🇴", Continent.SOUTH_AMERICA),
    Country("Uruguay", "🇺🇾", Continent.SOUTH_AMERICA),
    Country("Paraguay", "🇵🇾", Continent.SOUTH_AMERICA),

    // Europe
    Country("United Kingdom", "🇬🇧", Continent.EUROPE),
    Country("France", "🇫🇷", Continent.EUROPE),
    Country("Germany", "🇩🇪", Continent.EUROPE),
    Country("Italy", "🇮🇹", Continent.EUROPE),
    Country("Spain", "🇪🇸", Continent.EUROPE),
    Country("Russia", "🇷🇺", Continent.EUROPE),
    Country("Sweden", "🇸🇪", Continent.EUROPE),
    Country("Norway", "🇳🇴", Continent.EUROPE),
    Country("Greece", "🇬🇷", Continent.EUROPE),
    Country("Poland", "🇵🇱", Continent.EUROPE),
    Country("Netherlands", "🇳🇱", Continent.EUROPE),
    Country("Belgium", "🇧🇪", Continent.EUROPE),
    Country("Switzerland", "🇨🇭", Continent.EUROPE),
    Country("Austria", "🇦🇹", Continent.EUROPE),
    Country("Portugal", "🇵🇹", Continent.EUROPE),
    Country("Ireland", "🇮🇪", Continent.EUROPE),
    Country("Denmark", "🇩🇰", Continent.EUROPE),
    Country("Finland", "🇫🇮", Continent.EUROPE),
    Country("Czech Republic", "🇨🇿", Continent.EUROPE),
    Country("Hungary", "🇭🇺", Continent.EUROPE),
    Country("Romania", "🇷🇴", Continent.EUROPE),
    Country("Ukraine", "🇺🇦", Continent.EUROPE),
    Country("Croatia", "🇭🇷", Continent.EUROPE),

    // Africa
    Country("Egypt", "🇪🇬", Continent.AFRICA),
    Country("South Africa", "🇿🇦", Continent.AFRICA),
    Country("Nigeria", "🇳🇬", Continent.AFRICA),
    Country("Kenya", "🇰🇪", Continent.AFRICA),
    Country("Morocco", "🇲🇦", Continent.AFRICA),
    Country("Algeria", "🇩🇿", Continent.AFRICA),
    Country("Tanzania", "🇹🇿", Continent.AFRICA),
    Country("Ghana", "🇬🇭", Continent.AFRICA),
    Country("Ethiopia", "🇪🇹", Continent.AFRICA),
    Country("Congo", "🇨🇩", Continent.AFRICA),

    // Asia
    Country("China", "🇨🇳", Continent.ASIA),
    Country("Japan", "🇯🇵", Continent.ASIA),
    Country("South Korea", "🇰🇷", Continent.ASIA),
    Country("India", "🇮🇳", Continent.ASIA),
    Country("Turkey", "🇹🇷", Continent.ASIA),
    Country("Thailand", "🇹🇭", Continent.ASIA),
    Country("Vietnam", "🇻🇳", Continent.ASIA),
    Country("Indonesia", "🇮🇩", Continent.ASIA),
    Country("Philippines", "🇵🇭", Continent.ASIA),
    Country("Pakistan", "🇵🇰", Continent.ASIA),
    Country("Bangladesh", "🇧🇩", Continent.ASIA),
    Country("Sri Lanka", "🇱🇰", Continent.ASIA),
    Country("Malaysia", "🇲🇾", Continent.ASIA),
    Country("Singapore", "🇸🇬", Continent.ASIA),
    Country("Mongolia", "🇲🇳", Continent.ASIA),
    Country("Iran", "🇮🇷", Continent.ASIA),
    Country("Iraq", "🇮🇶", Continent.ASIA),
    Country("Saudi Arabia", "🇸🇦", Continent.ASIA),
    Country("Israel", "🇮🇱", Continent.ASIA),
    Country("UAE", "🇦🇪", Continent.ASIA),
    Country("Kazakhstan", "🇰🇿", Continent.ASIA),

    // Oceania
    Country("Australia", "🇦🇺", Continent.OCEANIA),
    Country("New Zealand", "🇳🇿", Continent.OCEANIA),
    Country("Fiji", "🇫🇯", Continent.OCEANIA),
    Country("Papua New Guinea", "🇵🇬", Continent.OCEANIA),
)
