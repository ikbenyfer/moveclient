# Handoff: MoveClient (Fabric, Minecraft 26.2)

Context for whichever AI/developer picks this project up next. Read this before touching the
code — most of the pain in this project was reverse-engineering a Minecraft build with a very
different API surface from anything in typical training data, and re-learning it from scratch
would waste a lot of time.

## What this is

A client-side Fabric mod (package `com.example.moveclient`, mod id `moveclient`) with 18
toggleable modules for testing movement mechanics in a private/controlled world, plus a
draggable, scrollable in-game ClickGUI, per-module keybinds (including an in-GUI keybind
picker), and JSON config persistence. Current version: **1.4.0**. Full module list and
architecture are documented in [README.md](README.md) — this file covers what the README
doesn't: the environment gotchas and the reasoning behind non-obvious implementation choices.

## The one fact that matters most: this Minecraft build is unusually new

`minecraft_version=26.2` in `gradle.properties` is **not** a version this model (or probably any
model without live access to it) has training data for. Its APIs are renamed/restructured
relative to every Minecraft version most LLMs know well (1.20–1.21.x). Do not trust remembered
API shapes for anything in `net.minecraft.client.gui`, `net.minecraft.client.input`,
`net.minecraft.world.inventory`, or Fabric API's rendering/HUD packages — **verify against the
real jar before writing code that uses them.** Every module/movement-physics API
(`Entity`, `LivingEntity`, `Attributes`, `BlockPos`, `Level`) turned out to be exactly as
remembered; it was specifically the client-rendering, input, and inventory-click layers that had
changed.

### How to verify an API (the method used throughout this project)

1. You need a JDK 25 to build (Gradle itself needs 17+, but this project's
   `sourceCompatibility`/`targetCompatibility` is 25). If none is available, download Temurin 25
   for Windows x64 from `https://api.adoptium.net/v3/binary/latest/25/ga/windows/x64/jdk/hotspot/normal/eclipse`
   and extract it; set `JAVA_HOME` to the extracted `jdk-25.*` folder for Gradle invocations.
2. Run `./gradlew build` once (even if it fails on first pass) so Loom populates
   `.gradle/loom-cache/minecraftMaven/net/minecraft/minecraft-merged-*/26.2/*.jar` — this is the
   real, unobfuscated (Mojang-mapped) client jar. Fabric API's submodule jars are nested inside
   the resolved `fabric-api-0.161.0+26.2.jar` under `META-INF/jars/` and can be extracted with
   `jar xf`.
3. Inspect the real class before using it:
   `javac`'s sibling `javap -classpath <jar> -p <fully.qualified.ClassName>` lists every member
   (add `-c` to disassemble bytecode when you need to know parameter *meaning*, not just
   signature — e.g. figuring out that `GuiGraphicsExtractor.outline(x,y,w,h,color)` takes
   width/height, not a second corner, only came from reading its bytecode, which calls `fill`
   internally with `x+w`/`y+1`).
4. For anything where getting it wrong risks real damage (item loss, a crash with no stack trace
   reaching your code, a mod that fails to load at all), don't guess from the signature alone —
   disassemble the method body, or a caller of it, and read what it actually does. This is how
   the AutoTotem inventory-slot mapping and the Anti-Knockback scissor-crash root cause were
   found; guessing would very likely have shipped something broken or unsafe.

### Concrete API differences found this session (26.2 vs. "classic" Minecraft)

- `net.minecraft.client.gui.GuiGraphics` → `net.minecraft.client.gui.GuiGraphicsExtractor`.
  `drawString` → `text(...)`. `fill(x0,y0,x1,y1,color)` unchanged. `outline(x,y,w,h,color)` is
  width/height, not a second corner (verified via bytecode).
- `Screen.render(graphics, mouseX, mouseY, partialTick)` → `Screen.extractRenderState(...)`
  (same override point, renamed; default impl just iterates `renderables` and dispatches —
  no background darkening happens automatically, so no need to suppress it).
- `AbstractWidget.extractRenderState` is `final`; it checks `this.visible` before drawing at all.
  `visible = false` alone both hides a widget **and** disables its clicks (`isActive()` checks
  `visible && active`) — this is what `ClickGuiScreen`'s scroll implementation relies on instead
  of `enableScissor`.
- Input events are now objects, not raw primitives: `mouseClicked`/`mouseDragged`/`mouseReleased`
  take `MouseButtonEvent` (record: `x()`, `y()`, `button()`, ...); `keyPressed` takes `KeyEvent`
  (record: `key()`, `scancode()`, `modifiers()`, plus `isEscape()`). `mouseScrolled` is still raw
  `(double mouseX, double mouseY, double scrollX, double scrollY)`.
- `Minecraft` no longer owns the current `Screen` directly. It's on `Minecraft.gui` (type `Gui`):
  `client.gui.screen()` / `client.gui.setScreen(screen)`.
- Fabric API's old `HudRenderCallback` is gone. HUD elements are now layered:
  `net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry.addLast(Identifier, HudElement)`
  where `HudElement.extractRenderState(GuiGraphicsExtractor, DeltaTracker)` is the render hook.
- World rendering (`WorldRenderEvents`, the old immediate-mode hook for drawing 3D overlays) is
  **also gone**, replaced by a `net.fabricmc.fabric.api.client.rendering.v1.level` package
  (`LevelRenderEvents`, `LevelExtractionEvents`, ...) mirroring the same extract/render split as
  the GUI. **This was never verified or used** — `XrayModule` deliberately reports ore positions
  as HUD text instead of a rendered see-through overlay, specifically to avoid guessing at this
  unfamiliar pipeline after the scissor crash (see below). If you want true world-space ESP
  rendering, this is where to start, but budget real time to reverse-engineer it the same way the
  GUI layer was handled here — don't assume `WorldRenderEvents` still exists.
- Inventory slot-clicking: `AbstractContainerMenu.clicked(int slotId, int button, ContainerInput,
  Player)` — `ContainerInput` replaces the old `ClickType` enum but the semantics are the same.
  Confirmed via bytecode: `ContainerInput.SWAP` accepts `button` in `[0,9)` (hotbar 0-8) or
  exactly `40` (`Inventory.SLOT_OFFHAND`) as the swap target. The player's own inventory menu slot
  numbering is unchanged from classic Minecraft: 0=craft result, 1-4=craft grid, 5-8=armor,
  **9-35=main inventory, 36-44=hotbar, 45=offhand** (`InventoryMenu.INV_SLOT_START/END`,
  `USE_ROW_SLOT_START/END`, `SHIELD_SLOT` constants — verified, not assumed). To apply a click
  from client code (not just locally predict it), call
  `Minecraft.getInstance().gameMode.handleContainerInput(containerId, slotId, button,
  ContainerInput, player)` — this both runs `menu.clicked(...)` locally and sends the real
  `ServerboundContainerClickPacket`; calling `menu.clicked()` directly only does the local half.
- `MultiPlayerGameMode.handleInventoryMouseClick` (the classic method name) doesn't exist in this
  build; it's `handleContainerInput` as above. `handleInventoryButtonClick` still exists but is
  for enchanting-table/beacon buttons, not item slots.
- Single-purpose player actions still go through `ServerboundPlayerActionPacket` with an `Action`
  enum, e.g. `Action.SWAP_ITEM_WITH_OFFHAND` (used by `AutoTotemModule` for the "totem already in
  main hand" fast path — though the module now primarily uses the `ContainerInput.SWAP`/button 40
  route above so it can pull a totem from anywhere in the inventory, not just the main hand).
  Confirmed via bytecode that the server-side handler (`ServerGamePacketListenerImpl.
  handlePlayerAction`) does a direct hand-swap for this action with no sequence-number validation.
- Keybinding: `KeyMapping.setKey(InputConstants.Key)` still exists; after changing a mapping's
  key you must call the static `KeyMapping.resetMapping()` (rebuilds an internal Key→List index)
  and `Minecraft.getInstance().options.save()` to persist it — same as vanilla's own Controls
  screen. `InputConstants.getKey(KeyEvent)` converts a captured keypress; for mouse buttons use
  `InputConstants.Type.MOUSE.getOrCreate(mouseButtonEvent.button())`.
- Attributes system unchanged in spirit but check exact `Holder<Attribute>` constants on
  `Attributes` before use: `MOVEMENT_SPEED`, `JUMP_STRENGTH`, `GRAVITY`, `STEP_HEIGHT` are all
  used here via `LivingEntity.getAttribute(...)` /
  `AttributeInstance.addOrUpdateTransientModifier(new AttributeModifier(id, amount, Operation))`
  + `removeModifier(id)` on disable — this is the pattern for any module that wants a reversible,
  vanilla-native stat change (see `SpeedModule`, `StepModule`, `HighJumpModule`, `GravityModule`).

### A real crash this session, and why: avoid `GuiGraphicsExtractor.enableScissor`

A native `Tooltip` widget (`AbstractWidget.setTooltip(Tooltip.create(...))`) caused
`java.lang.IllegalArgumentException: Scissor size must be >0, was 64x0` deep inside
`GuiRenderer` — a crash with **zero mod code in the stack trace**, because it happens during a
*deferred* render pass (`GameRenderer.render` → `GuiRenderer.render`) that runs after
`extractRenderState()` has already returned. **A try/catch in the Screen cannot catch this** —
by the time the exception fires, control has left mod code entirely. The fix was removing the
native tooltip (replaced with a plain HUD status-line description using
`AbstractWidget#isHovered()`) and never calling `enableScissor`/using native `Tooltip` widgets
again in this project. If you need clipped/scrollable content, use the `visible` flag technique
in `ClickGuiScreen` instead, not scissor rects — this build's scissor/tooltip render path is
demonstrably fragile and any crash there is uncatchable and mostly unreproducible without a real
GPU/driver (the report that surfaced this had AMD driver info in the crash log).

## Architecture (see README.md "Architecture" section for the diagram)

- `Module` (base class): `onEnable()`/`onDisable()`/`onTick()` lifecycle, exception-safe (a
  module that throws during any of these logs it and disables itself rather than crashing the
  client — this matters because modules keep ticking while the ClickGUI is open, since
  `isPauseScreen()` returns `false` by design).
- `ModuleManager`: singleton registry, ticks everything, handles `disableAll()` on shutdown and
  `reapplyEnabledOnJoin()` when the player joins a new world (re-runs `onEnable()` for already-
  enabled modules, since side effects like Flight's `setNoGravity(true)` were applied to the
  *previous* player entity, which is now discarded).
- `ConfigManager`: flat Gson JSON, `{"ModuleName": {"enabled": bool, "settings": {...}}}`, no
  reflection — settings are looked up by name string, so renaming a setting loses its saved
  value (acceptable; nothing currently depends on migration).
- `ClickGuiScreen`: draggable (clamped to stay on-screen), scrollable (via `visible`, not
  scissor — see crash note above), multi-column module grid, per-module settings panel with a
  slider **and** a typeable number field per `DoubleSetting` (kept in sync via mutual callbacks),
  and an in-GUI keybind picker (click "Key: ..." then press a key/mouse button; Escape while
  waiting unbinds instead of closing the menu).

## Known limitations (deliberate, not oversights)

- **Mixins are avoided by default; one exception exists.** Every module except Xray works
  through public vanilla/Fabric API only. This was a deliberate choice given how much of the
  "expected" API surface turned out to be wrong for this build — a wrong Mixin target doesn't
  fail gracefully like a wrong method call does, it can fail the whole mod's load. The one
  exception, added in 1.6.0: `BlockOcclusionMixin` targets `Block.shouldRenderFace` to fix
  Xray's texture pack not revealing fully-buried ores — face culling is decided by the adjacent
  block's real, server-shared shape, which a resource pack cannot override no matter what
  geometry it uses, so there was no non-Mixin way to fix it. It's registered `"required": false`
  in `moveclient.mixins.json` specifically so a future-version target mismatch degrades to a
  logged warning instead of failing the whole mod's load — keep that pattern for any *new*
  Mixin this project adds. If another feature seems to need a Mixin (true see-through-wall
  ESP beyond occlusion, spoofing outgoing movement packets for NoFall on a remote server,
  intercepting incoming knockback packets for a truly zero-tolerance Anti-Knockback), that
  remains a deliberate scope boundary, documented in the relevant module's class doc — not a
  TODO — unless you're prepared to verify the target as rigorously as `shouldRenderFace` was
  verified here (real bytecode inspection, not a remembered method name).
- `NoFallModule` only prevents damage authoritatively in singleplayer/LAN (resets the integrated
  server's copy of the player's fall distance via `MinecraftServer#execute`); against a remote
  dedicated server it only suppresses the client-side prediction.
- `AntiKnockbackModule` cannot reach literal zero tolerance (see its class doc for the physics
  reasoning) and has a known false-positive on slime block bounces (they look identical to an
  external upward launch from a velocity-only model).
- `XrayModule` is a HUD text readout, not a rendered overlay — see the world-rendering note
  above.
- `KillauraModule` hard-excludes `Player` from targeting **in code**, not behind a setting. This
  was an explicit scope decision after discussion with the user (a "Killaura" that could target
  players was declined; a mob-only version was built instead). Do not add a way to re-enable
  targeting players — that changes what the user actually agreed to.

## Everything else

Build/requirements/keybinds/full module table: [README.md](README.md). A Modrinth publish was
considered and explicitly declined by the user (this mod's feature set — Xray, Killaura, NoClip,
Speed, Flight, Anti-Knockback — is the kind of content most mod platforms' rules prohibit
hosting). A private download page (an Artifact, not a public listing) exists instead; ask the
user if they still have that link if a distribution page is needed again.
