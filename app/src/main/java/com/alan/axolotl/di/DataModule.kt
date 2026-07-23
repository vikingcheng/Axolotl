package com.alan.axolotl.di

import android.content.Context
import android.content.SharedPreferences
import com.alan.axolotl.data.AndroidSpeechPlayer
import com.alan.axolotl.data.BookRepository
import com.alan.axolotl.data.CountryRepository
import com.alan.axolotl.data.DefaultCountryRepository
import com.alan.axolotl.data.DefaultSentenceRepository
import com.alan.axolotl.data.DefaultSettingsRepository
import com.alan.axolotl.data.DefaultWordRepository
import com.alan.axolotl.data.PdfBookRepository
import com.alan.axolotl.data.SentenceRepository
import com.alan.axolotl.data.SettingsRepository
import com.alan.axolotl.data.SpeechPlayer
import com.alan.axolotl.data.WordRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Wires each repository interface to its implementation and provides the
 * app-wide SharedPreferences. Installed in [SingletonComponent] so everything
 * here lives for the whole application.
 *
 * `@Binds` (abstract) is the efficient way to say "when someone needs the
 * interface, give them this implementation". `@Provides` (concrete) is used for
 * types Hilt can't construct itself, like SharedPreferences.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    @Singleton
    abstract fun bindCountryRepository(impl: DefaultCountryRepository): CountryRepository

    @Binds
    @Singleton
    abstract fun bindWordRepository(impl: DefaultWordRepository): WordRepository

    @Binds
    @Singleton
    abstract fun bindSentenceRepository(impl: DefaultSentenceRepository): SentenceRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: DefaultSettingsRepository): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindBookRepository(impl: PdfBookRepository): BookRepository

    /**
     * Deliberately unscoped: each ViewModel gets its own speech engine and
     * releases it in `onCleared()`, so no TTS service connection is held open
     * for the whole app lifetime.
     */
    @Binds
    abstract fun bindSpeechPlayer(impl: AndroidSpeechPlayer): SpeechPlayer

    companion object {
        @Provides
        @Singleton
        fun provideSharedPreferences(
            @ApplicationContext context: Context
        ): SharedPreferences =
            context.getSharedPreferences(
                DefaultSettingsRepository.PREFS_NAME,
                Context.MODE_PRIVATE
            )
    }
}
