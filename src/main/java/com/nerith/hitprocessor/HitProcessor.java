
package com.nerith.hitprocessor;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public final class HitProcessor implements Listener {

    private final NerithHitProcessor plugin;

    private final Map<String, Integer> processed = new HashMap<>();
    private final Map<UUID, Integer> statistics = new HashMap<>();

    public HitProcessor(NerithHitProcessor plugin) {
        this.plugin = plugin;
    }

    public void process(GhostHitEvent event) {

        if (!plugin.getConfig().getBoolean(
                "processor.enabled", true)) {
            return;
        }

        Player attacker = event.getAttacker();
        Player target = event.getTarget();

        if (attacker == null || target == null) {
            return;
        }

        if (!attacker.isOnline() || !target.isOnline()) {
            return;
        }

        if (attacker.equals(target)) {
            return;
        }

        if (attacker.isDead() || target.isDead()) {
            return;
        }

        if (attacker.getGameMode() != GameMode.SURVIVAL
                && attacker.getGameMode() != GameMode.ADVENTURE) {
            return;
        }

        if (target.getGameMode() == GameMode.SPECTATOR) {
            return;
        }

        if (plugin.getConfig().getBoolean(
                "processor.require-same-world", true)
                && !attacker.getWorld().equals(target.getWorld())) {
            return;
        }

        int now = Bukkit.getCurrentTick();

        int maxAge = plugin.getConfig().getInt(
                "processor.max-request-age-ticks", 3
        );

        if (now - event.getDetectedTick() > maxAge
                || now < event.getDetectedTick()) {
            return;
        }

        String key = attacker.getUniqueId()
                + ":" + target.getUniqueId();

        int duplicateWindow = plugin.getConfig().getInt(
                "processor.duplicate-window-ticks", 5
        );

        Integer previous = processed.get(key);

        if (previous != null
                && now - previous <= duplicateWindow) {
            return;
        }

        processed.put(key, now);

        statistics.merge(
                attacker.getUniqueId(),
                1,
                Integer::sum
        );

        sendAlert(attacker, target, "Verified");

        if (plugin.getConfig().getBoolean("debug", false)) {
            plugin.getLogger().info(
                    "Ghost request verified: "
                    + attacker.getName()
                    + " -> "
                    + target.getName()
                    + " | Distance: "
                    + event.getDistance()
            );
        }

        cleanup(now);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDamage(EntityDamageByEntityEvent event) {

        if (!(event.getDamager() instanceof Player attacker)) {
            return;
        }

        if (!(event.getEntity() instanceof Player target)) {
            return;
        }

        String key = attacker.getUniqueId()
                + ":" + target.getUniqueId();

        // A real hit is recorded so an incoming request
        // cannot cause the same hit to be processed again.
        processed.put(key, Bukkit.getCurrentTick());
    }

    private void sendAlert(
            Player attacker,
            Player target,
            String status
    ) {
        if (!plugin.getConfig().getBoolean(
                "alerts.enabled", true)) {
            return;
        }

        String prefix = plugin.getConfig().getString(
                "prefix", "&6NerithHitProcessor"
        );

        String format = plugin.getConfig().getString(
                "alerts.format",
                "{prefix} &8| &a{attacker} &7-> &f{target} &8| &a{status}"
        );

        String message = format
                .replace("{prefix}", prefix)
                .replace("{attacker}", attacker.getName())
                .replace("{target}", target.getName())
                .replace("{status}", status);

        message = message.replace("&", "§");

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.isOp()
                    || online.hasPermission(
                    "nerithhitprocessor.alerts")) {
                online.sendMessage(message);
            }
        }

        Bukkit.getConsoleSender().sendMessage(message);
    }

    private void cleanup(int now) {

        Iterator<Map.Entry<String, Integer>> iterator =
                processed.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<String, Integer> entry = iterator.next();

            if (now - entry.getValue() > 40) {
                iterator.remove();
            }
        }
    }

    public int getProcessed(UUID id) {
        return statistics.getOrDefault(id, 0);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();

        statistics.remove(id);

        processed.keySet().removeIf(
                key -> key.contains(id.toString())
        );
    }

    public void shutdown() {
        processed.clear();
        statistics.clear();
    }
}
