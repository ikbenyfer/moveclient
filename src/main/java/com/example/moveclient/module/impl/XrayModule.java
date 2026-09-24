package com.example.moveclient.module.impl;

import com.example.moveclient.mixin.XrayOcclusionState;
import com.example.moveclient.module.Module;
import com.example.moveclient.module.ModuleCategory;
import com.example.moveclient.module.Setting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Two independent views of nearby ores, both toggled by this one module:
 *
 * 1. A HUD text readout (block type, direction, distance) — see {@code XrayHud}. Deliberately
 *    text, not a rendered 3D overlay: this client's world-rendering pipeline is a reworked
 *    "extraction" split (mirroring the GUI rendering rewrite documented on
 *    {@code ClickGuiScreen}) that was never verified against real behavior, so drawing into it
 *    was avoided after the scissor crash documented on {@code AntiKnockbackModule}/README.
 * 2. A real see-through-block resource pack bundled in this mod's own jar (`resourcepacks/xray/`,
 *    registered via Fabric's {@code ResourceLoader.registerBuiltinPack} in {@code ModClientInit}).
 *    Common stone/dirt-family blocks get a custom model overriding their vanilla one, with
 *    {@code "elements": []} — no geometry at all, so the block renders as nothing while its
 *    collision (a separate system from the render model) is completely untouched: you can still
 *    walk on/mine it normally, it's just invisible. This went through two more elaborate
 *    attempts first: (a) a fully-transparent whole-block texture, which looked "weird" at range
 *    (ores only visible very close, caves only very far — plausibly mip/depth/fog interactions a
 *    transparent-but-still-full-size cube has that this doesn't), and (b) a thin (0.5/16) shell
 *    model with cull-faced edges (reverse-engineered from a working third-party pack's file
 *    structure, not copied — it had no license and wasn't bundled), which fixed the shape issue
 *    but still looked wrong after also fixing a missing-particle-texture bug that came with it.
 *    Rather than keep chasing an exact geometric match to a pack this project can't visually
 *    test against, dropping the geometry entirely removes the whole category of "some render
 *    distance/angle looks subtly wrong" bug: there is nothing left to render incorrectly. Ore
 *    blocks are deliberately left with no override at all, so they render normally against the
 *    resulting empty space. Pure resource-pack data (JSON models, a `particle` texture reused
 *    from vanilla's own existing textures, no custom PNGs) using Minecraft's ordinary,
 *    long-stable resource pack system, so unlike (1) it isn't exposed to any of this build's
 *    rendering-API churn at all. Enabling/disabling this module adds/removes the pack from the
 *    active {@link PackRepository} selection and triggers a resource reload (the same "Reloading
 *    resources..." flash as toggling a pack by hand in the Options menu).
 *
 * Two further, independently-toggleable settings build on (2):
 *
 * 3. "Allowlist Mode" flips the hide logic around: instead of denylisting ~30 specific common
 *    terrain blocks, it hides every block that ISN'T an ore. A resource pack can't scale to
 *    "every non-ore block in the game" as individual model files, so this is done with a Mixin
 *    instead ({@code XrayAllowlistModelMixin}, injecting into {@code BlockModelResolver.update} -
 *    this build's renamed/restructured per-blockstate model resolution entry point - and
 *    redirecting non-ore blocks to vanilla's own {@code EmptyBlockModel.INSTANCE}). Needs the
 *    same face-culling fix as (2), generalized in {@code BlockOcclusionMixin} to "any adjacent
 *    block not in the ore allowlist" rather than one fixed covered-block set.
 * 4. "Fullbright Ores" makes ore blocks themselves glow: a third builtin pack
 *    (`resourcepacks/xray_ores/`) overrides each ore block with its normal full-cube geometry
 *    plus {@code light_emission: 15} and {@code ambientocclusion: false}, so they emit full
 *    brightness and stand out clearly whether or not a hide mode is also active. Kept as its own
 *    pack (not merged into (2)'s) so it can be toggled independently of which hide mode, if any,
 *    is in use.
 */
public class XrayModule extends Module {

    private static final String TEXTURE_PACK_ID = "moveclient:xray";
    private static final String FULLBRIGHT_PACK_ID = "moveclient:xray_ores";

    public record OreHit(BlockPos pos, Block block) {
    }

    private final Setting.DoubleSetting radius;
    private final Setting.DoubleSetting maxListed;
    private final Setting.DoubleSetting scanIntervalTicks;
    private final Setting.BooleanSetting includeGold;
    private final Setting.BooleanSetting includeCommonOres;
    private final Setting.BooleanSetting texturePack;
    private final Setting.BooleanSetting allowlistMode;
    private final Setting.BooleanSetting fullbrightOres;

    private final List<OreHit> nearestOres = new ArrayList<>();
    private int ticksUntilScan;

    public XrayModule() {
        super("Xray", "Lists nearby valuable ores (direction + distance) through walls", ModuleCategory.WORLD);
        radius = registerDouble("Scan Radius", 16, 8, 32, 4);
        maxListed = registerDouble("Max Listed", 5, 1, 10, 1);
        scanIntervalTicks = registerDouble("Scan Interval (ticks)", 40, 10, 200, 10);
        includeGold = registerBoolean("Include Gold", true);
        includeCommonOres = registerBoolean("Include Iron/Redstone/Lapis/Copper", false);
        texturePack = registerBoolean("Texture Pack", true);
        allowlistMode = registerBoolean("Allowlist Mode", false);
        fullbrightOres = registerBoolean("Fullbright Ores", true);
    }

    @Override
    protected void onEnable() {
        setTexturePackEnabled(texturePack.get());
        setAllowlistModeEnabled(allowlistMode.get());
        setFullbrightOresEnabled(fullbrightOres.get());
    }

    @Override
    protected void onDisable() {
        nearestOres.clear();
        ticksUntilScan = 0;
        setTexturePackEnabled(false);
        setAllowlistModeEnabled(false);
        setFullbrightOresEnabled(false);
    }

    private void setTexturePackEnabled(boolean enabled) {
        Minecraft client = Minecraft.getInstance();
        PackRepository repository = client.getResourcePackRepository();

        boolean changed = enabled ? repository.addPack(TEXTURE_PACK_ID) : repository.removePack(TEXTURE_PACK_ID);
        boolean selectedAfter = repository.getSelectedIds().contains(TEXTURE_PACK_ID);
        // Confirmed via an earlier diagnostic build that the pack itself was always being
        // selected/reloaded correctly (it really was becoming invisible/transparent) - the
        // remaining "still weird" report was fully-buried ore veins never rendering at all, a
        // face-culling issue this state flag lets BlockOcclusionMixin fix. Kept in sync with the
        // repository's actual state, not just the requested one, so a failed addPack/removePack
        // can't leave the mixin thinking Xray is active when the pack isn't really selected.
        XrayOcclusionState.setActive(selectedAfter);

        if (changed) {
            client.reloadResourcePacks();
        }
    }

    /**
     * Allowlist Mode: hides every block that isn't an ore (see {@code XrayAllowlistModelMixin}),
     * rather than denylisting ~30 specific "common terrain" blocks like the pack above. Pure
     * Mixin state, no resource pack involved - but already-built chunks won't reflect the new
     * value until re-meshed, so this reuses {@code reloadResourcePacks()} anyway purely as the
     * one mechanism already confirmed (via the denylist pack's own toggle, see the class doc's
     * account of the "still weird" saga) to force a full chunk rebuild in this build's renderer.
     */
    private void setAllowlistModeEnabled(boolean enabled) {
        if (XrayOcclusionState.isAllowlistActive() == enabled) {
            return;
        }
        XrayOcclusionState.setAllowlistActive(enabled);
        Minecraft.getInstance().reloadResourcePacks();
    }

    /**
     * Fullbright Ores: swaps ore blocks to full-cube models with {@code light_emission: 15}
     * (`resourcepacks/xray_ores/`) so they glow and stand out, whether or not a hide mode is also
     * active. Kept as its own builtin pack, independent of {@code TEXTURE_PACK_ID}, so it can be
     * toggled without pulling in (or requiring) the denylist pack's hide behavior.
     */
    private void setFullbrightOresEnabled(boolean enabled) {
        Minecraft client = Minecraft.getInstance();
        PackRepository repository = client.getResourcePackRepository();

        boolean changed = enabled ? repository.addPack(FULLBRIGHT_PACK_ID) : repository.removePack(FULLBRIGHT_PACK_ID);
        if (changed) {
            client.reloadResourcePacks();
        }
    }

    @Override
    protected void onTick() {
        // Reconciled every tick (cheap: a boolean/Set#contains compare, only touching the pack
        // repository or Mixin state on an actual mismatch) so toggling any of these settings from
        // the GUI takes effect immediately, the same way the other live settings do, rather than
        // only on the next full module enable/disable.
        PackRepository repository = Minecraft.getInstance().getResourcePackRepository();
        if (repository.getSelectedIds().contains(TEXTURE_PACK_ID) != texturePack.get()) {
            setTexturePackEnabled(texturePack.get());
        }
        if (XrayOcclusionState.isAllowlistActive() != allowlistMode.get()) {
            setAllowlistModeEnabled(allowlistMode.get());
        }
        if (repository.getSelectedIds().contains(FULLBRIGHT_PACK_ID) != fullbrightOres.get()) {
            setFullbrightOresEnabled(fullbrightOres.get());
        }

        if (ticksUntilScan > 0) {
            ticksUntilScan--;
            return;
        }
        ticksUntilScan = scanIntervalTicks.get().intValue();
        rescan();
    }

    private void rescan() {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        ClientLevel level = client.level;
        if (player == null || level == null) {
            return;
        }

        int r = radius.get().intValue();
        BlockPos center = player.blockPosition();
        BlockPos min = center.offset(-r, -r, -r);
        BlockPos max = center.offset(r, r, r);

        List<OreHit> found = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            if (!level.isLoaded(pos)) {
                continue;
            }
            Block block = level.getBlockState(pos).getBlock();
            if (!isTarget(block)) {
                continue;
            }
            found.add(new OreHit(pos.immutable(), block));
        }

        found.sort(Comparator.comparingDouble(hit -> distanceSquared(hit.pos(), center)));
        nearestOres.clear();
        int limit = maxListed.get().intValue();
        for (int i = 0; i < Math.min(limit, found.size()); i++) {
            nearestOres.add(found.get(i));
        }
    }

    private static double distanceSquared(BlockPos a, BlockPos b) {
        double dx = a.getX() - b.getX();
        double dy = a.getY() - b.getY();
        double dz = a.getZ() - b.getZ();
        return dx * dx + dy * dy + dz * dz;
    }

    private boolean isTarget(Block block) {
        if (block == Blocks.DIAMOND_ORE || block == Blocks.DEEPSLATE_DIAMOND_ORE
                || block == Blocks.EMERALD_ORE || block == Blocks.DEEPSLATE_EMERALD_ORE
                || block == Blocks.ANCIENT_DEBRIS) {
            return true;
        }
        if (includeGold.get() && (block == Blocks.GOLD_ORE || block == Blocks.DEEPSLATE_GOLD_ORE || block == Blocks.NETHER_GOLD_ORE)) {
            return true;
        }
        return includeCommonOres.get() && (block == Blocks.IRON_ORE || block == Blocks.DEEPSLATE_IRON_ORE
                || block == Blocks.REDSTONE_ORE || block == Blocks.DEEPSLATE_REDSTONE_ORE
                || block == Blocks.LAPIS_ORE || block == Blocks.DEEPSLATE_LAPIS_ORE
                || block == Blocks.COPPER_ORE || block == Blocks.DEEPSLATE_COPPER_ORE);
    }

    /** The nearest matching ores as of the last scan, closest first. Read by {@code XrayHud}. */
    public List<OreHit> getNearestOres() {
        return nearestOres;
    }

    /** The configured scan radius, so the HUD can report it even when nothing was found. */
    public double getRadius() {
        return radius.get();
    }
}
