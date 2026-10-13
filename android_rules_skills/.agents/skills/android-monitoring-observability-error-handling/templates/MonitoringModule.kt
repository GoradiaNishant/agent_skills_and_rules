package core.monitoring.di

import core.monitoring.analytics.AnalyticsTracker
import core.monitoring.analytics.FirebaseAnalyticsTracker
import core.monitoring.crash.CrashReporter
import core.monitoring.crash.FirebaseCrashReporter
import core.monitoring.error.GlobalErrorHandler
import core.monitoring.error.GlobalErrorHandlerImpl
import core.monitoring.logger.AppLogger
import core.monitoring.logger.AppLoggerImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class MonitoringBindingModule {

    @Binds
    @Singleton
    abstract fun bindCrashReporter(
        impl: FirebaseCrashReporter
    ): CrashReporter

    @Binds
    @Singleton
    abstract fun bindAnalyticsTracker(
        impl: FirebaseAnalyticsTracker
    ): AnalyticsTracker

    @Binds
    @Singleton
    abstract fun bindGlobalErrorHandler(
        impl: GlobalErrorHandlerImpl
    ): GlobalErrorHandler
}

@Module
@InstallIn(SingletonComponent::class)
object MonitoringProviderModule {

    @Provides
    @Singleton
    fun provideAppLogger(): AppLogger {
        // Automatically link isDebug to BuildConfig.DEBUG in application project
        val isDebug = try {
            val buildConfigClass = Class.forName("com.bullvoy.BuildConfig")
            buildConfigClass.getField("DEBUG").getBoolean(null)
        } catch (_: Exception) {
            true // Safe fallback
        }
        return AppLoggerImpl(isDebug = isDebug)
    }
}
