
package com.nerith.hitprocessor;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

public final class NerithHitProcessor extends JavaPlugin {

    private HitProcessor hitProcessor;

    @Override
    public void onEnable() {

        saveDefaultConfig();

        hitProcessor = new HitProcessor(this);

        Bukkit.getPluginManager().registerEvents(
                hitProcessor,
                this
        );

        getLogger().info("--------------------------------");
        getLogger().info("NerithHitProcessor enabled");
        getLogger().info("Paper 1.21 compatible");
        getLogger().info("Waiting for NerithHitSync requests");
        getLogger().info("--------------------------------");
    }

    /**
     * Public bridge called by NerithHitSync.
     */
    public void submitGhostHit(
            UUID attackerId,
            UUID targetId,
            int detectedTick,
            double distance
    ) {

        if (!isEnabled() || hitProcessor == null) {
            return;
        }

        Runnable task = () -> {

            Player attacker = Bukkit.getPlayer(attackerId);
            Player target = Bukkit.getPlayer(targetId);

            if (attacker == null || target == null) {
                return;
            }

            GhostHitEvent event = new GhostHitEvent(
                    attacker,
                    target,
                    detectedTick,
                    distance
            );

            Bukkit.getPluginManager().callEvent(event);

            hitProcessor.process(event);
        };

        if (Bukkit.isPrimaryThread()) {
            task.run();
        } else {
            Bukkit.getScheduler().runTask(this, task);
        }
    }

    public HitProcessor getHitProcessor() {
        return hitProcessor;
    }

    @Override
    public void onDisable() {

        if (hitProcessor != null) {
            hitProcessor.shutdown();
        }

        getLogger().info("NerithHitProcessor disabled");
    }
}
