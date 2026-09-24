# Changelog

All notable changes to MoveClient, version by version.

## [1.5.2] — Xray: fixed missing particles + a leftover model bug

- **Fixed**: 1.5.1's thin-shell models never declared a `particle` texture variable (the
  reference pack's `cube_all.json` layer did this via `"textures": {"particle": "#all"}`, which
  was missed when reimplementing the technique independently). With ~30 extremely common blocks
  (stone, dirt, grass, sand, gravel, ...) all missing a resolvable particle texture at once, this
  is the most likely explanation for both the reported "still weird" rendering and **all
  particles disappearing entirely** while Xray was enabled. Added the missing declaration to the
  shared shell model so every covered block inherits it.

## [1.5.1] — Xray rebuilt with the thin-shell model technique

- **Fixed**: 1.5.0's flat-transparent-texture approach looked "weird" in practice — ores only
  visible very close, caves only visible very far. Rebuilt using a proper block-model technique
  instead: each covered block is replaced with a custom model that shrinks it to a thin (0.5/16)
  shell on each face, forced to full brightness (`light_emission: 15`,
  `ambientocclusion: false`), with normal `cullface` behavior so two adjacent shell blocks still
  hide their shared face — a solid mass of stone reads as empty, and only faces exposed to air
  (cave walls, ore veins) show as thin bright outlines. No custom textures needed; models
  reference vanilla's own existing block textures.
- Ore blocks are left with no override at all and render completely normally.
- The technique (and the `pack.mcmeta` `min_format`/`max_format`/`supported_formats` structure)
  was verified against a working third-party x-ray pack's file structure, then reimplemented
  from scratch with this mod's own files — the original pack had no license and wasn't bundled.

## [1.5.0] — Xray now applies a real see-through texture pack

- **Added**: Xray now bundles and auto-applies an actual resource pack (`resourcepacks/xray/`,
  built into the mod jar) that makes common stone/dirt-family block textures fully transparent,
  so ores are genuinely visible through terrain — not just listed as HUD text. Toggling the
  module adds/removes the pack from the resource pack repository and triggers a reload (the same
  "Reloading resources..." flash as switching a pack by hand). A new "Texture Pack" setting
  (default on) lets you disable this half and keep just the HUD text readout, and changes to it
  apply live without needing to re-toggle the module.
- **Why**: the previous HUD-text-only approach turned out not to be visible for at least one
  user despite passing every code review, and a proper texture pack sidesteps the whole
  uncertain 3D-rendering-pipeline question entirely — it's just PNGs and a `pack.mcmeta` through
  Minecraft's ordinary, decades-stable resource pack system, unrelated to any of this build's
  rendering-API churn.
- Covers stone, deepslate, dirt, grass, granite/diorite/andesite, tuff, calcite, gravel,
  sand/red sand, sandstone/red sandstone, netherrack, blackstone, basalt, end stone, and a few
  more — not an exhaustive list of every block in the game, but the common "digging through
  solid ground" case.

## [1.4.1] — Xray HUD fix

- **Fixed**: Xray's HUD text (the ore readout in the top-left corner) wasn't appearing at all,
  even the "no ores within Nm" fallback, despite the module showing as enabled in the module
  list. Consolidated Xray's HUD rendering onto the same `HudElementRegistry` registration as the
  module list (previously a separate one) and wrapped it in a try/catch that logs any exception
  instead of letting it disappear silently.

## [1.4.0] — In-GUI keybind picker

- **Added**: you can now bind a module's key directly from the ClickGUI — open a module's "Cfg"
  panel, click "Key: ...", then press the key or mouse button you want. Escape while waiting
  unbinds instead of closing the menu. Uses the same `KeyMapping.setKey` + `resetMapping()` +
  `options.save()` sequence vanilla's own Controls screen uses, so it's saved the same way.
- **Changed**: "Cfg" is now offered for every module, not just ones with tunable settings, since
  it's also where the keybind picker lives.

## [1.3.1] — Anti-Knockback and AutoTotem rewritten

- **Fixed (Anti-Knockback)**: tightened to a physics-aware model instead of a flat threshold.
  Horizontal tolerance lowered to just above ordinary WASD acceleration. Vertical now predicts
  the expected value each tick (gravity while falling, or the actual jump-strength attribute
  right as you leave the ground) instead of using one flat number, so real knockback/explosions
  get cancelled almost immediately while normal jumping and falling are unaffected. Known
  remaining edge case: a slime block bounce looks identical to external knock-up from this model
  and gets cancelled too.
- **Fixed (AutoTotem)**: now searches the *entire* inventory (not just the hotbar) and swaps a
  found totem directly into the offhand, via the same `ContainerInput.SWAP` / button 40 action
  vanilla's own inventory screen sends when you hover a slot and press F — verified against this
  build's actual bytecode before relying on it, since the inventory-click API was restructured in
  this Minecraft version.

## [1.3.0] — Killaura, Criticals, and three bug fixes

- **Added**: Killaura — auto-attacks the nearest hostile mob in range. `Player` is hard-excluded
  from targeting in code (not a setting), so it can never be used against another player no
  matter how it's configured.
- **Added**: Criticals — times a tiny hop the instant you attack while grounded, so your own
  attacks land as critical hits. Only reacts to your own attack key; never attacks anything by
  itself.
- **Fixed (Anti-Knockback)**: switched from a two-hook start/end-of-tick comparison to a single
  end-of-tick comparison against a self-maintained baseline, closing a timing gap where incoming
  knockback could already be applied before the "before" sample was taken. Also raised the
  default vertical tolerance above vanilla's jump velocity (0.42) — the previous default (0.35)
  was fighting the player's own jumps.
- **Fixed (AutoTotem)**: now searches the hotbar for a totem instead of requiring it already be
  in the main hand.
- **Fixed (Xray)**: the HUD now always shows a status line while enabled, even "no ores within
  Nm" when nothing is found — previously it rendered nothing at all in that case, which was
  indistinguishable from being broken.

## [1.2.0] — Xray, AutoTotem, and a NoFall efficiency fix

- **Added**: Xray — periodically scans a radius around you for valuable ores and lists the
  nearest ones on the HUD with direction and distance. A text readout, not a rendered
  see-through-wall overlay (this Minecraft build's 3D world-rendering API was never verified —
  see `XrayModule`'s class doc).
- **Added**: AutoTotem — swaps a Totem of Undying to your offhand when health drops low.
- **Improved (NoFall)**: the server-side fall-distance reset (for singleplayer/LAN) now fires
  once, exactly on the landing tick, instead of on every airborne tick of a fall — a long fall
  from the build limit previously scheduled dozens of redundant cross-thread resets.

## [1.1.0] — Typeable settings, scrolling, and a real AirJump fix

- **Added**: every numeric setting now shows a slider *and* a typeable number field, kept in
  sync with each other.
- **Added**: the ClickGUI now scrolls with the mouse wheel once its content exceeds a fixed
  viewport height. Implemented by hiding rows outside the viewport (`AbstractWidget#visible`),
  not by GPU-side clipping — a lesson from the 1.0.1 crash.
- **Fixed (AirJump)**: switched from per-tick `isDown()` polling to `KeyMapping.consumeClick()`.
  A plain "was it down last tick" comparison can miss a genuine key press entirely if it happens
  faster than one client tick (~50ms), which made the module feel unreliable.

## [1.0.1] — The scissor crash

- **Fixed**: a crash (`IllegalArgumentException: Scissor size must be >0`) that happened whenever
  the ClickGUI was open, traced via a user-supplied mclo.gs log to native `Tooltip` widget
  rendering — a deferred render-pipeline code path with no mod code in the stack trace, so it
  couldn't be caught with a try/catch. Removed the native tooltip (module descriptions are now
  drawn as a plain HUD status line instead) and never used `enableScissor`/native `Tooltip`
  again in this project.
- **Added**: dragging the ClickGUI panel is now clamped so it can never end up partially
  off-screen (a secondary precaution against the same class of crash).
- **Hardened**: any exception thrown inside a module's `onTick()`/`onEnable()`/`onDisable()` is
  now caught, logged, and disables just that module — instead of crashing the whole client.

## [1.0.0] — First versioned release

- **Fixed**: pressing `;` a second time now closes the ClickGUI (it previously only ever opened
  it).
- Renamed from the initial `0.1.0` jar.

## [0.1.0] — Initial release

Bhop, Flight, Anti-Knockback, NoFall, Speed, Step, HighJump, Gravity, AutoSprint, Spider,
AirJump, NoClip, and ElytraBoost, plus the draggable multi-column ClickGUI (colored ON/OFF
states, hover descriptions), the HUD module list, JSON config persistence, and per-module
keybinds. Also fixed a strafe-direction bug where A and D were swapped in both Flight and Bhop.
