package com.sllauncher.pro.navigation

import kotlinx.serialization.Serializable

/*
 * Type-safe navigation routes. Floating panels (app drawer, search, folders,
 * Dynamic Island) are overlays on Home, not destinations.
 */

@Serializable data object HomeDestination
@Serializable data object LauncherSetupDestination
@Serializable data object OnboardingDestination
@Serializable data object SettingsDestination
@Serializable data object WidgetPickerDestination
@Serializable data object AssistantDestination
@Serializable data object NewsDestination
@Serializable data object MusicDestination
@Serializable data object NotificationCenterDestination
@Serializable data object QuickSettingsDestination
@Serializable data object DeviceInfoDestination
@Serializable data object ProductivityDestination
@Serializable data object CustomizationDestination
@Serializable data object PrivacyDestination
@Serializable data object PremiumDestination
@Serializable data object AboutDestination
