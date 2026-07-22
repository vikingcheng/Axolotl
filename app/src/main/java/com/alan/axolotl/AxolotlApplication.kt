package com.alan.axolotl

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application entry point for Hilt. [@HiltAndroidApp] triggers Hilt's code
 * generation and creates the app-level (Singleton) dependency container that
 * every other Hilt component is built from.
 */
@HiltAndroidApp
class AxolotlApplication : Application()
