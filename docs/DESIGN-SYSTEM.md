# SL design system

Module: `core/designsystem`. Every screen wraps its content in `SLTheme { }` and reads tokens through `SLTheme.colors`, `.typography`, `.spacing`, `.shapes`, `.sizes` and `.motion`. Feature code never uses raw hex values, ad-hoc `dp` sizes or Material defaults.

**See it live:** debug builds install a second icon, **SL Catalogue**, that shows every component with sample data, plus dark/light and reduced-motion toggles.

## Colour

| Token | Dark (brand default) | Light ("paper") | Use |
|---|---|---|---|
| `background` | `#090909` | `#F5F1E8` | Screen background |
| `surface` | `#151515` | `#EAE5D9` | Panels, rows, blocks |
| `surfaceElevated` | `#252525` | `#DED8CA` | Tracks, placeholders, raised blocks |
| `accent` | `#FFD400` | `#FFD400` | Fills only: primary buttons, selection, SL identity |
| `onAccent` | `#090909` | `#090909` | Text and icons on `accent` |
| `accentIndicator` | `#FFD400` | `#7A6000` | Thin lines and small marks on backgrounds |
| `accentOutline` | `#FFD400` | `#090909` | Outline around an `accent` fill |
| `textPrimary` | `#F5F1E8` | `#090909` | Main text |
| `textSecondary` | `#A5A5A5` | `#4A4A4A` | Supporting text |
| `border` | `#383838` | `#C9C3B5` | Decorative dividers, block shadows |
| `borderStrong` | `#737373` | `#737373` | Outlines of interactive controls |
| `error` / `onError` | `#FF5A4E` / `#090909` | `#B3261E` / `#F5F1E8` | Errors, destructive actions |
| `scrim` | black 60% | black 40% | Dims wallpaper behind overlays |

**Why `borderStrong` exists:** the brand border `#383838` measures only 1.70:1 against `#090909`. Accessibility guidelines (WCAG 1.4.11) need 3:1 for control boundaries, so input fields, switches, chips and similar controls use `#737373`. Measured ratios: 4.20 on `#090909`, 3.85 on `#151515`, 3.23 on `#252525`, and 4.21, 3.77 and 3.34 on the three paper surfaces. `#383838` stays the colour of decorative lines and shadows.

**Yellow discipline:** yellow marks what is active, important or SL. Use it for the selected chip, the primary action and the assistant, not for every heading. In the light palette yellow is never text or a thin line (1.27:1 on paper); use `accentIndicator` there.

## Typography

| Style | Face | Size / line | Use |
|---|---|---|---|
| `display` | Space Grotesk Bold | 64 / 64 sp | Home clock |
| `headline` | Space Grotesk Bold | 28 / 32 sp | Screen titles |
| `title` | Space Grotesk SemiBold | 18 / 24 sp | Section and item titles |
| `body` | System sans | 16 / 24 sp | Running text |
| `bodySmall` | System sans | 14 / 20 sp | Subtitles, descriptions |
| `label` | Space Grotesk Bold, +6% tracking, uppercase | 14 / 18 sp | Buttons, chips, section headers |
| `caption` | System sans Medium | 12 / 16 sp | App labels, metadata |

Space Grotesk ships as one 133 KB variable font (SIL OFL, see `THIRD_PARTY_NOTICES.md`). Body text uses the platform sans-serif: it adds nothing to the APK and is tuned for long reading. All sizes are `sp`, so they follow the user's font-size setting.

## Spacing, shape, size

- **Spacing:** 2, 4, 8, 12, 16, 24, 32, 48 dp (`xxs` to `xxxl`).
- **Shape:** 2–4 dp corners, 2 dp borders, and a solid **block shadow** offset 4 dp (6 dp for panels and dialogs) instead of blurred elevation. It is one rectangle draw, which is cheap on low-end GPUs.
- **Touch targets:** at least 48 dp for everything tappable.
- **Sizes:** icon 24, app icon 52, control height 48, row 56, nav bar 64, assistant button 64 dp.

## Motion

Short (120 ms) and medium (220 ms) transitions with a fast-out, firm-stop curve, and no bounce. `SLTheme.motion.reduced` is true when Android's **Remove animations** setting is on (SL's own low-performance mode will also set it in Phase 15). In that mode transitions are instant and looping animations (loading blocks, assistant level bars) show a still frame.

## Floating panel sizing

`floatingPanelSize()` (unit-tested):

| Window | Width | Height |
|---|---|---|
| Phone (e.g. 360×800 dp) | 80% (288 dp) | 70% (560 dp) |
| Narrow phone (≤ 350 dp) | grows to 280 dp minimum | 70% |
| Tablet / unfolded | capped at 560 dp | capped at 720 dp |
| Short window (< 480 dp tall: landscape, split screen) | 80%, capped | 90% |

The panel is always at least 16 dp inside the area between the system bars, so wallpaper stays visible around it.

## Components

| Component | Notes |
|---|---|
| `SLButton` | Primary, Secondary, Ghost, Destructive; optional leading icon; the face presses onto its shadow |
| `SLIconButton` | Filled, Outlined, Plain; 48 dp; `contentDescription` required |
| `SLSearchField` | Yellow outline when focused, clear button, Search key on the keyboard, trailing slot (voice input) |
| `SLFloatingPanel` | Scrim, open/close animation, Back / outside-tap / drag-handle dismiss, TalkBack "Close" action |
| `SLChip`, `SLChipRow` | Category selection (Tab role) |
| `SLSegmentedControl` | Grid/list style toggle (RadioButton role) |
| `SLNavBar` | Bottom navigation with badges |
| `SLIconTile`, `SLBadge`, `SLDotBadge` | App tile with icon slot, single-line label and long-press; badge caps at "99+" |
| `SLSlider` | Continuous or stepped; tap, drag, and TalkBack adjust |
| `SLSwitch`, `SLSwitchRow` | The whole row toggles |
| `SLSettingsRow`, `SLNavigationRow` | Settings lists |
| `SLDialog` | Confirmation, optional destructive styling |
| `SLBottomSheet` | Always fully expanded |
| `SLLoadingIndicator`, `SLProgressBar` | Indeterminate and determinate |
| `SLEmptyState`, `SLErrorState` | Say what happened and offer one action |
| `SLMediaControls` | Stateless; wired to media sessions in Phases 7 and 9 |
| `SLAssistantButton` | Idle, Listening, Processing, Speaking; announces state changes |
| `SLBlock`, `SLSectionHeader` | Use blocks sparingly, only for distinct objects |
| `SLIcons` | 15 icons drawn in code; no icon library dependency |

## Rules for feature code

1. Use a component if one exists. If a screen needs something new, add it here first, with a preview, then use it.
2. No new colours, fonts or shadow styles without updating this document and explaining why.
3. Every icon-only control has a `contentDescription`.
4. Animations go through `SLTheme.motion` so reduced motion keeps working.
5. Layouts adapt by window size (`currentWindowWidthClass()`), never by phone model.
