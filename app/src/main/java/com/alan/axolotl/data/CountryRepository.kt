package com.alan.axolotl.data

import javax.inject.Inject

/**
 * Source of the country/flag data used by the "Countries" game.
 * Kept behind an interface so the data source can later move to a database or
 * network without touching the ViewModel, and so it can be faked in tests.
 */
interface CountryRepository {
    fun getCountries(): List<Country>
}

class DefaultCountryRepository @Inject constructor() : CountryRepository {
    override fun getCountries(): List<Country> = allCountries
}
