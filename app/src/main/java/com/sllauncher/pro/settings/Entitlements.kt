package com.sllauncher.pro.settings

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

enum class Tier { Free, Premium, AI }

/**
 * Features above the free tier. Essential launcher functions (home screen,
 * drawer, search, basic SL commands, notifications, privacy controls) are
 * never gated.
 */
enum class PremiumFeature(val tier: Tier, val label: String) {
    ExtraAccents(Tier.Premium, "Extra accent colours"),
    CustomProfiles(Tier.Premium, "More than 3 saved profiles"),
    PremiumThemes(Tier.Premium, "Premium theme packs"),
    AiSummaries(Tier.AI, "AI news summaries"),
    AiSuggestions(Tier.AI, "AI reply suggestions"),
}

/**
 * Current entitlements. Today every user is on Free: purchases need Google
 * Play Billing connected to products in the Play Console, which only the
 * publisher can set up (see docs/MONETIZATION.md). Entitlements must come
 * from verified purchases; this class never grants anything on its own.
 */
@Singleton
class EntitlementRepository @Inject constructor() {
    private val _tier = MutableStateFlow(Tier.Free)
    val tier: StateFlow<Tier> = _tier.asStateFlow()

    val purchasesAvailable: Boolean = false

    fun has(feature: PremiumFeature): Boolean = _tier.value.ordinal >= feature.tier.ordinal
}
