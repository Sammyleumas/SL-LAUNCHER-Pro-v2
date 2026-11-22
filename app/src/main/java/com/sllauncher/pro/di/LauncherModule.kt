package com.sllauncher.pro.di

import com.sllauncher.pro.assistant.DefaultLauncherActionBridge
import com.sllauncher.pro.assistant.LauncherActionBridge
import com.sllauncher.pro.commands.FeatureCommandHandlers
import com.sllauncher.pro.commands.FeatureCommandHandlersImpl
import com.sllauncher.pro.launcher.AndroidDefaultHomeChecker
import com.sllauncher.pro.launcher.DefaultHomeChecker
import com.sllauncher.pro.launcher.SystemTimeSource
import com.sllauncher.pro.launcher.TimeSource
import com.sllauncher.pro.news.NewsProvider
import com.sllauncher.pro.news.RssNewsProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Binds launcher-level interfaces to their Android implementations. */
@Module
@InstallIn(SingletonComponent::class)
abstract class LauncherModule {

    @Binds
    abstract fun bindDefaultHomeChecker(impl: AndroidDefaultHomeChecker): DefaultHomeChecker

    @Binds
    abstract fun bindTimeSource(impl: SystemTimeSource): TimeSource

    @Binds
    abstract fun bindFeatureCommands(impl: FeatureCommandHandlersImpl): FeatureCommandHandlers

    @Binds
    abstract fun bindNewsProvider(impl: RssNewsProvider): NewsProvider

    @Binds
    abstract fun bindAssistantBridge(impl: DefaultLauncherActionBridge): LauncherActionBridge
}
