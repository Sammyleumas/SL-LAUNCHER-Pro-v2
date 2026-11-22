# Phase 2 completion report

**PHASE:** 2, Design system and UI foundation.

**OBJECTIVE:** Build the complete, reusable SL design system before feature work, so every later screen shares one look, one set of components and one set of accessibility rules.

**IMPLEMENTED**
- **Typography:** Space Grotesk variable font bundled (133 KB, SIL OFL) for display, headings and labels; system sans-serif for body text. Seven type styles, all in `sp`.
- **Colour:** dark (brand) and light ("paper") palettes with semantic tokens. New `borderStrong` (`#737373`) for control outlines, because the brand border `#383838` only reaches 1.70:1 and WCAG needs 3:1. All ratios measured; see `DESIGN-SYSTEM.md`.
- **Spacing, shapes, sizes:** 4 dp scale, 2 dp borders, solid block shadows, 48 dp minimum touch targets.
- **Motion:** 120/220 ms tokens that collapse to instant changes when Android's "Remove animations" is on.
- **Responsive foundation:** window width classes (Compact, Medium, Expanded) and unit-tested floating-panel sizing.
- **Components (21):** button (4 styles), icon button, search field, floating panel, chip and chip row, segmented control, nav bar, icon tile, badge, dot badge, slider, switch, switch row, settings and navigation rows, dialog, bottom sheet, loading indicator, progress bar, empty and error states, media controls, assistant button, block, section header, plus a 15-icon set drawn in code.
- **Catalogue:** debug-only "SL Catalogue" app showing every component with sample data, dark/light and reduced-motion toggles, and a working sample floating drawer.
- Phase 1 screens moved onto the new tokens and components.

**FILES CREATED**
- `core/designsystem/src/main/java/.../component/`: `BlockShadow.kt`, `FloatingPanelSize.kt`, `SLAssistantButton.kt`, `SLBlock.kt`, `SLControls.kt`, `SLFeedback.kt`, `SLFloatingPanel.kt`, `SLIconTile.kt`, `SLMediaControls.kt`, `SLOverlays.kt`, `SLSearchField.kt`, `SLSelection.kt`, `SLSettingsRow.kt`, `SliderMath.kt`
- `.../icon/SLIcons.kt`, `.../layout/WindowWidthClass.kt`, `.../theme/Motion.kt`, `.../catalogue/SLComponentCatalogue.kt`
- `core/designsystem/src/main/res/font/space_grotesk.ttf`, `res/values/strings.xml`, `assets/licenses/space-grotesk-OFL.txt`
- Tests: `FloatingPanelSizeTest.kt`, `SliderMathTest.kt`, `BadgeTextTest.kt`, `WindowWidthClassTest.kt`, `SLComponentsTest.kt` (UI)
- `app/src/debug/`: `AndroidManifest.xml`, `res/values/strings.xml`, `java/.../catalogue/DesignCatalogueActivity.kt`
- `docs/DESIGN-SYSTEM.md`, `docs/THIRD_PARTY_NOTICES.md`, this report

**FILES MODIFIED:** `theme/Color.kt`, `theme/Type.kt`, `theme/Dimens.kt`, `theme/SLTheme.kt`, `component/SLButton.kt`, `core/designsystem/build.gradle.kts`, `app/.../home/HomeScreen.kt`, `app/.../setup/LauncherSetupScreen.kt`.

**DEPENDENCIES:** No new libraries. `androidx.activity:activity-compose` (already in the app) is now also used by the design system for Back handling on floating panels. Icons are drawn in code instead of adding Material Icons.

**BUILD STATUS:** Successful, GitHub Actions run #8, first attempt (debug APK, unit tests, instrumented-test APKs, lint, R8 release build). Debug APK 12.31 MB (was 12.04 MB).

**TEST STATUS**
- 22/22 JVM unit tests pass (13 new: panel sizing on 48 window sizes, slider snapping, badge capping, window breakpoints).
- 4 new Compose UI tests compile (search clear, switch row toggle, chip selection, panel close action). They need a device or emulator to run, as do the 3 from Phase 1.
- Not yet checked on a physical device.

**KNOWN ISSUES**
- No device testing yet, so visual details (font rendering, shadow offsets, panel animation smoothness on a low-end phone) are unverified.
- The slider supports touch and TalkBack but not yet D-pad or keyboard arrows.
- The light palette exists but the launcher still always uses dark. Theme choice comes in Phase 13.
- `LocalWindowInfo.containerSize` is used with an experimental opt-in; revisit when Compose marks it stable.

**NEXT PHASE:** Phase 3, onboarding and the real home screen: dock, pages, app shortcuts, widget hosting, folders and wallpaper.

**USER ACTION**
1. Install the debug APK from run #8 and open **SL Catalogue**. Try both palettes and the floating panel, and report anything that looks wrong.
2. Still open from Phase 1: your test phone (model, Android version, RAM), the SL assistant source, and the package name.
3. Approve Phase 2.
