package com.dohman.holdempucker.dagger

import android.app.Application
import com.dohman.holdempucker.repositories.AnalyticsRepository
import com.dohman.holdempucker.repositories.LobbyRepository
import com.dohman.holdempucker.repositories.OnlinePlayRepository
import com.dohman.holdempucker.repositories.ResourceRepository
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.database.DatabaseReference
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class RepositoryModule {
    @Provides
    @Singleton
    fun providesResourceRepository(application: Application) = ResourceRepository(application)

    @Provides
    @Singleton
    fun providesFirebaseRepository(firebaseRef: DatabaseReference) = OnlinePlayRepository(firebaseRef)

    @Provides
    @Singleton
    fun providesLobbyRepository(db: DatabaseReference) = LobbyRepository(db)

    @Provides
    @Singleton
    fun providesAnalyticsRepository(analytics: FirebaseAnalytics) = AnalyticsRepository(analytics)
}