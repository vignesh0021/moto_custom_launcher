# Multi-Marvel Universe Theme Engine — Licensing & Asset Architecture

## 1. Overview & Compliance Statement

The **Multi-Marvel Universe Launcher** theme engine is built from the ground up to provide customizable superhero-inspired aesthetic experiences while maintaining strict compliance with intellectual property and copyright regulations:

> [!IMPORTANT]
> **Zero Unlicensed Assets**: This project does **not** package, distribute, or bundle any copyrighted Marvel artwork, character renders, movie stills, comic cutouts, trademarked logos, or proprietary soundtrack audio.
> All visual styles, geometries, telemetry HUDs, and ambient effects are **100% procedurally synthesized** via Jetpack Compose canvas drawing routines.

---

## 2. Procedural Visual Synthesis (Clean-Room Design)

Instead of relying on static bitmap assets (PNG/JPG/WEBP) that violate copyright and inflate APK size, all theme elements are mathematically rendered in real-time on the GPU:

| Theme | Inspired Archetype | Procedural Canvas Geometry (`CoreGeometry`) | Color Tokens |
|---|---|---|---|
| **Iron Man** | Tech Armor / Arc Reactor | `TRIANGULAR_ARC`: Nested concentric cyan rings, inverted delta reactor prism, segmented arc telemetry | `#00E5FF` Cyan, `#FFD700` Gold, `#D50000` Crimson |
| **Captain America** | Vibranium Shield / Tactical | `TACTICAL_STAR`: Concentric tactical rings with procedurally computed 5-point mathematical star | `#2979FF` Blue, `#FF1744` Red, `#ECEFF1` Titanium |
| **Thor** | Asgardian Cosmic Energy | `MJOLNIR_NORDIC`: Golden rune circle, lightning discharge path geometry, Bifrost auroral gradients | `#00E5FF` Storm Cyan, `#FFD700` Asgardian Gold |
| **Black Panther** | Wakandan Vibranium Tech | `CIRCULAR_VIBRANIUM`: Hexagonal micro-weave matrix, vibranium kinetic absorption ripple, tooth necklace arcs | `#C084FC` Vibranium Purple, `#7E22CE` Deep Violet |
| **Spider-Man** | Web Geometry / Motion | `ARACHNID_POLY`: 6-point radial web geometry with interconnecting tension vectors | `#FF1744` Stark Red, `#00B0FF` Web Blue |
| **Doctor Strange** | Mystic Kamar-Taj Energy | `MYSTIC_MANDALA`: Eldritch concentric fractal rings, rotating square mandala, spark emissions | `#FF9100` Eldritch Orange, `#FFD700` Mystic Gold |
| **Hulk** | Gamma Core Containment | `GAMMA_OCTAGON`: Bio-hazard octagonal containment perimeter, gamma emission core | `#00E676` Gamma Green, `#76FF03` Bio Lime |
| **Black Widow** | Tactical Red Room | `BLACK_WIDOW_HOURGLASS`: High-contrast tactical grid, geometric hourglass polygon | `#FF1744` Crimson, `#9E9E9E` Tactical Slate |
| **Avengers Unified** | Earth's Mightiest Initiative | `AVENGERS_CHEVRON`: Orbital command ring with tactical forward chevron vector | `#00E5FF` Cyan, `#FFD700` Gold |

---

## 3. Performance & Resource Guarantees

1. **Zero Runtime Bitmap Decoding**:
   Themes do not load or decode raster graphics from disk or network, avoiding Garbage Collection (GC) pauses, heap allocations, and memory spikes.
2. **Lifecycle-Aware Rendering**:
   Dynamic wallpapers and core telemetry animations pause immediately when the screen turns off or another application is in the foreground. Idle launcher state consumes **0% CPU**.
3. **Lazy Token Resolution**:
   `MarvelThemeRegistry` maintains lightweight immutable data classes. Only the active theme's tokens are resolved into the `LocalMarvelTheme` composition local.
4. **Safe Fallback**:
   If an unknown theme ID is encountered (e.g. from preference corruption or legacy upgrades), `MarvelThemeRegistry.getTheme()` safely falls back to the default theme (`iron_man`), preventing launcher crashes.

---

## 4. Audio & Haptics Guidelines

- **Default State**: In accordance with user preferences and acceptance criteria, sound effects are **disabled by default** (`soundEffectsEnabled = false`).
- **Haptic Feedback**: Haptic feedback leverages standard Android system key clicks (`HapticFeedbackConstants.KEYBOARD_TAP`), ensuring zero proprietary audio dependencies.
- **Custom Sound Integration**: If users choose to integrate audio clips, they must provide their own licensed or royalty-free audio files conforming to standard permissive licenses (e.g., CC0, MIT, Apache 2.0).

---

## 5. User Layout Preservation

Changing themes **never alters or resets**:
- Installed applications or package visibility
- Dock app selections (`KEY_DOCK_KEYS`)
- Quick access shelf selections (`KEY_QUICK_ACCESS_KEYS`)
- App search indexes or user database states
- Application permissions (e.g., Usage Stats, Query All Packages)
