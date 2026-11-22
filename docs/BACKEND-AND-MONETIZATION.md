# Phases 17–18: monetization and backend

## Backend decision (Phase 18)

**Decision: no backend for version 1.** Every core feature works fully on the phone.

| Feature | Needs a backend? | Status |
|---|---|---|
| Home, drawer, search, island, notifications, music, productivity, customization | No | Done, offline |
| News | No (fetches publisher feeds directly) | Done; licensing to confirm |
| Backup | No (file export/import) | Done |
| Encrypted cloud sync | Yes (accounts and storage) | Not built |
| Theme marketplace | Yes (catalogue, payments to creators) | Not built |
| AI summaries, smart replies, advanced commands | Yes (to keep API keys off the phone) | Not built |
| Purchase verification | Yes (server-side checks against Google Play) | Not built |
| Crash reporting | Optional (third-party service) | Not built; Play Console's Android vitals covers basics |

**When a backend is added, recommended order:**

1. A small server (for example Supabase or Firebase Cloud Functions) that holds the AI provider key and verifies Play purchases.
2. Accounts only if sync or a marketplace are built.

**Trade-offs:**

- **Firebase:** fastest to start, strong Android tooling and Crashlytics, but ties SL to Google.
- **Supabase:** Postgres with row-level security, can self-host, slightly more setup.

**Rule:** API keys never go in the app.

## Monetization (Phase 17)

### What exists

- `EntitlementRepository`: the single source of truth for Free, Premium and AI tiers. It never grants a tier by itself; it is always Free today.
- `PremiumFeature`: the list of gated features. **Gated today:** extra accent colours and more than 3 saved profiles.
- **Premium screen:** lists the tiers and says plainly that purchases aren't available yet.
- **Never gated:** essential launcher functions (home, drawer, search, island, notifications, privacy, backup). There are **no ads** anywhere.

### What is needed to sell Premium

1. **Google Play Console:** a developer account ($25 one-time), the app created, and a merchant account (payments profile).
2. **Products:** create subscription and one-time products in Play Console (e.g. `sl_premium_monthly`, `sl_premium_lifetime`, `sl_ai_monthly`).
3. **Library:** add the Google Play Billing Library (check the current version and Play's required minimum at the time).
4. **App code:** implement `BillingClient` connection, product queries, the purchase flow and purchase acknowledgement, and feed verified purchases into `EntitlementRepository`.
5. **Verification:** confirm purchases server-side (Play Developer API) before granting AI features, which cost money per use.
6. **Testing:** test with Play Console license testers on an internal testing track.

This cannot be completed or tested without the Play Console account and products, which only the publisher can create.
