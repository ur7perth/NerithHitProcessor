
package com.nerith.hitprocessor;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public final class GhostHitEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player attacker;
    private final Player target;
    private final int detectedTick;
    private final double distance;

    public GhostHitEvent(
            Player attacker,
            Player target,
            int detectedTick,
            double distance
    ) {
        this.attacker = attacker;
        this.target = target;
        this.detectedTick = detectedTick;
        this.distance = distance;
    }

    public Player getAttacker() {
        return attacker;
    }

    public Player getTarget() {
        return target;
    }

    public int getDetectedTick() {
        return detectedTick;
    }

    public double getDistance() {
        return distance;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
