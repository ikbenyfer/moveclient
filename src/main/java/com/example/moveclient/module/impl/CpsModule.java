package com.example.moveclient.module.impl;

import com.example.moveclient.module.Module;
import com.example.moveclient.module.ModuleCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Tracks clicks per second for the attack and use keys, for display by {@code InfoHud}. Counts a
 * click on every up-to-down transition of the key (read-only {@code isDown()} edge detection -
 * same as {@code CriticalsModule}/{@code ClickAuraModule}, not {@code consumeClick()}, so normal
 * attacking/using is never affected), keeping a rolling one-second window of press timestamps per
 * key so the displayed number is always "clicks in the last real second," not a per-tick estimate
 * (which would be misleadingly coarse at 20 ticks/second).
 */
public class CpsModule extends Module {

    private static final long WINDOW_MILLIS = 1000;

    private final Deque<Long> attackClicks = new ArrayDeque<>();
    private final Deque<Long> useClicks = new ArrayDeque<>();
    private boolean wasAttackDown;
    private boolean wasUseDown;

    public CpsModule() {
        super("CPS", "Shows clicks per second for attack and use", ModuleCategory.PLAYER);
    }

    @Override
    protected void onDisable() {
        attackClicks.clear();
        useClicks.clear();
        wasAttackDown = false;
        wasUseDown = false;
    }

    @Override
    protected void onTick() {
        Options options = Minecraft.getInstance().options;
        long now = System.currentTimeMillis();

        boolean attackDown = options.keyAttack.isDown();
        if (attackDown && !wasAttackDown) {
            attackClicks.addLast(now);
        }
        wasAttackDown = attackDown;

        boolean useDown = options.keyUse.isDown();
        if (useDown && !wasUseDown) {
            useClicks.addLast(now);
        }
        wasUseDown = useDown;

        prune(attackClicks, now);
        prune(useClicks, now);
    }

    private static void prune(Deque<Long> clicks, long now) {
        while (!clicks.isEmpty() && now - clicks.peekFirst() > WINDOW_MILLIS) {
            clicks.pollFirst();
        }
    }

    /** Attack (left-click) clicks counted in the last real second. Read by {@code InfoHud}. */
    public int getAttackCps() {
        return attackClicks.size();
    }

    /** Use (right-click) clicks counted in the last real second. Read by {@code InfoHud}. */
    public int getUseCps() {
        return useClicks.size();
    }
}
