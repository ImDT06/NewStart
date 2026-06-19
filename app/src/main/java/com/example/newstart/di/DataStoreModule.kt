package com.example.newstart.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.dataStoreFile
import com.example.newstart.data.preferences.EncryptedSerializer
import com.example.newstart.data.preferences.UserPreferences
import com.example.newstart.util.CryptoManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

    @Provides
    @Singleton
    fun provideUserPreferencesDataStore(
        @ApplicationContext context: Context,
        cryptoManager: CryptoManager
    ): DataStore<UserPreferences> {
        return DataStoreFactory.create(
            serializer = EncryptedSerializer(
                cryptoManager = cryptoManager,
                kSerializer = UserPreferences.serializer(),
                defaultValue = UserPreferences()
            ),
            produceFile = { context.dataStoreFile("user_prefs.pb") } // Dùng đuôi pb hoặc json tùy ý
        )
    }
}
