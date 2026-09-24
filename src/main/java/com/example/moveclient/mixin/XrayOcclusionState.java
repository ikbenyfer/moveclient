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

    private static volatile boolean active;

    private XrayOcclusionState() {
    }

    public static void setActive(boolean value) {
        active = value;
    }

    public static boolean isActive() {
        return active;
    }
}
