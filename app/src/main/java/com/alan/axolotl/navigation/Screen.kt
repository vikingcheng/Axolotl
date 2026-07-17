package com.alan.axolotl.navigation

import kotlinx.serialization.Serializable

@Serializable
object HomeRoute

@Serializable
object TimerRoute

@Serializable
object LockRoute

@Serializable
object BookRoute

@Serializable
data class BookReaderRoute(val fileName: String)

@Serializable
object CountriesRoute

@Serializable
object WordSearchRoute

@Serializable
object ReadRoute

@Serializable
object ProfileRoute

@Serializable
object PasswordGateRoute
