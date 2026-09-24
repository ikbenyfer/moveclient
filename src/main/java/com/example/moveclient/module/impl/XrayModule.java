package com.example.moveclient.module.impl;

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
 * 2. A real see-through-block texture pack: a resource pack bundled in this mod's own jar
 *    (`resourcepacks/xray/`, registered via Fabric's {@code ResourceLoader.registerBuiltinPack}
 *    in {@code ModClientInit}) that makes common stone/dirt-family block textures fully
 *    transparent, leaving ore blocks visible. This is pure resource-pack data — PNGs and a
 *    `pack.mcmeta` — using Minecraft's ordinary, long-stable resource pack system, so unlike (1)
 *    it isn't exposed to any of this build's rendering-API churn at all. Enabling/disabling this
 *    module adds/removes the pack from the active {@link PackRepository} selection and triggers
 *    a resource reload (the same "Reloading resources..." flash as toggling a pack by hand in
 *    the Options menu).
 */
public class XrayModule extends Module {

    private static final String TEXTURE_PACK_ID = "moveclient:xray";

    public record OreHit(BlockPos pos, Block block) {
    }

    private final Setting.DoubleSetting radius;
    private final Setting.DoubleSetting maxListed;
    private final Setting.DoubleSetting scanIntervalTicks;
    private final Setting.BooleanSetting includeGold;
    private final Setting.BooleanSetting includeCommonOres;
    private final Setting.BooleanSetting texturePack;

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
    }

    @Override
    protected void onEnable() {
        setTexturePackEnabled(texturePack.get());
    }

    @Override
    protected void onDisable() {
        nearestOres.clear();
        ticksUntilScan = 0;
        setTexturePackEnabled(false);
    }

    private void setTexturePackEnabled(boolean enabled) {
        Minecraft client = Minecraft.getInstance();
        PackRepository repository = client.getResourcePackRepository();
        boolean changed = enabled ? repository.addPack(TEXTURE_PACK_ID) : repository.removePack(TEXTURE_PACK_ID);
        if (changed) {
            client.reloadResourcePacks();
        }
    }

    @Override
    protected void onTick() {
        // Reconciled every tick (cheap: a boolean compare and a Set#contains, only touching the
        // pack repository on an actual mismatch) so toggling the "Texture Pack" setting from the
        // GUI takes effect immediately, the same way the other live settings do, rather than only
        // on the next full module enable/disable.
        boolean packActive = Minecraft.getInstance().getResourcePackRepository().getSelectedIds().contains(TEXTURE_PACK_ID);
        if (packActive != texturePack.get()) {
            setTexturePackEnabled(texturePack.get());
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
