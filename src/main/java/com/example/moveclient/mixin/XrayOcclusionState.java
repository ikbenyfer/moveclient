package com.example.moveclient.mixin;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.Set;

/**
 * Shared state read by {@link BlockOcclusionMixin}, kept in its own tiny class (rather than a
 * static field on {@code XrayModule}) so the mixin's dependency surface is as small as possible.
 * The block set here must match the block list overridden in {@code resourcepacks/xray/}.
 */
public final class XrayOcclusionState {

    public static final Set<Block> COVERED_BLOCKS = Set.of(
            Blocks.STONE, Blocks.COBBLESTONE, Blocks.DEEPSLATE, Blocks.COBBLED_DEEPSLATE,
            Blocks.DIRT, Blocks.COARSE_DIRT, Blocks.ROOTED_DIRT, Blocks.GRASS_BLOCK,
            Blocks.GRANITE, Blocks.DIORITE, Blocks.ANDESITE, Blocks.TUFF, Blocks.TUFF_BRICKS,
            Blocks.CALCITE, Blocks.GRAVEL, Blocks.SAND, Blocks.RED_SAND, Blocks.SANDSTONE,
            Blocks.RED_SANDSTONE, Blocks.NETHERRACK, Blocks.BLACKSTONE, Blocks.BASALT,
            Blocks.SMOOTH_BASALT, Blocks.END_STONE, Blocks.DEEPSLATE_BRICKS,
            Blocks.POLISHED_DEEPSLATE, Blocks.MYCELIUM, Blocks.PODZOL, Blocks.CLAY, Blocks.TERRACOTTA
    );

    /**
     * Every ore block Allowlist Mode leaves visible. Unlike {@code COVERED_BLOCKS} (an incomplete
     * "common terrain" denylist), this is meant to be exhaustive - it's the full set of blocks
     * that survive when everything else in the world is hidden. Kept in sync with
     * {@code resourcepacks/xray_ores/assets/minecraft/blockstates/*.json} (the fullbright glow
     * models) and with the vanilla ore blocks {@code XrayModule#isTarget} already knows about.
     */
    public static final Set<Block> ORE_BLOCKS = Set.of(
            Blocks.COAL_ORE, Blocks.DEEPSLATE_COAL_ORE,
            Blocks.IRON_ORE, Blocks.DEEPSLATE_IRON_ORE,
            Blocks.COPPER_ORE, Blocks.DEEPSLATE_COPPER_ORE,
            Blocks.GOLD_ORE, Blocks.DEEPSLATE_GOLD_ORE, Blocks.NETHER_GOLD_ORE,
            Blocks.REDSTONE_ORE, Blocks.DEEPSLATE_REDSTONE_ORE,
            Blocks.LAPIS_ORE, Blocks.DEEPSLATE_LAPIS_ORE,
            Blocks.DIAMOND_ORE, Blocks.DEEPSLATE_DIAMOND_ORE,
            Blocks.EMERALD_ORE, Blocks.DEEPSLATE_EMERALD_ORE,
            Blocks.NETHER_QUARTZ_ORE, Blocks.ANCIENT_DEBRIS
    );

    private static volatile boolean active;
    private static volatile boolean allowlistActive;

    private XrayOcclusionState() {
    }

    public static void setActive(boolean value) {
        active = value;
    }

    public static boolean isActive() {
        return active;
    }

    public static void setAllowlistActive(boolean value) {
        allowlistActive = value;
    }

    public static boolean isAllowlistActive() {
        return allowlistActive;
    }
}
