package org.example;

import com.projectkorra.projectkorra.BendingPlayer;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ProjectKorra;
import com.projectkorra.projectkorra.ability.CoreAbility;
import com.projectkorra.projectkorra.configuration.Config;
import com.projectkorra.projectkorra.configuration.ConfigManager;
import com.projectkorra.projectkorra.util.TempBlock;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityCombustByBlockEvent;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class VolcanicFlowListener implements Listener {
    private static HashMap<Entity, BukkitTask> activeRunnableInstances = new HashMap<Entity, BukkitTask>();
    private Set<Entity> entitiesOnFire = new HashSet<Entity>();
    private int fireTick = ConfigManager.getConfig().getInt("ExtraAbilities.Vincentcf.VolcanicFlow.FireTick");

    @EventHandler
    public void whileSneaking(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        if (!event.isSneaking()) return;

        BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(player);

        String abil = bPlayer.getBoundAbilityName();
        if (abil.equalsIgnoreCase("VolcanicFlow")) {
            new VolcanicFlow(player);
        }
    }

    @EventHandler
    public void onEntityBlockDamage(EntityDamageByBlockEvent event) {

        Block block = event.getDamager();
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();
        boolean shouldCancel = ConfigManager.getConfig().getBoolean("ExtraAbilities.Vincentcf.VolcanicFlow.cancelLavaWaveDamage");
        boolean started = false;

        if (block.getType() == Material.LAVA) {
            if (block.hasMetadata(VolcanicFlow.METADATA_KEY_LAVABLOCK)) {
                double lavaReduction = ConfigManager.getConfig().getDouble("ExtraAbilities.Vincentcf.VolcanicFlow.LavaDamage") / 4;
                event.setDamage(event.getDamage() * lavaReduction);

            } else if (block.hasMetadata(VolcanicFlow.METADATA_KEY_LAVAWAVEBLOCK) && shouldCancel == true){
                event.setCancelled(true);
                player.setFireTicks(ConfigManager.getConfig().getInt("ExtraAbilities.Vincentcf.VolcanicFlow.FireTick"));
            }
        }

        if (block.getType() == Material.MAGMA_BLOCK && block.hasMetadata(VolcanicFlow.METADATA_KEY_MAGMABLOCK)) {
            event.setCancelled(ConfigManager.getConfig().getBoolean("ExtraAbilities.Vincentcf.VolcanicFlow.cancelMagmaDamage"));
        }

    }

    @EventHandler
    public void fireTickCheck(EntityDamageByBlockEvent event) {
        Block block = event.getDamager();
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();

        if (block.hasMetadata(VolcanicFlow.METADATA_KEY_LAVABLOCK)) {

            if (activeRunnableInstances.get(player) != null) {
                activeRunnableInstances.get(player).cancel();
            }

            if (fireTick < 10) {
                fireTick = 10;
            }

            BukkitTask bukkitRunnable = new BukkitRunnable() {
                @Override
                public void run() {
                    player.setFireTicks(0);
                }
            }.runTaskLater(ProjectKorra.plugin, fireTick);
            activeRunnableInstances.put(player, bukkitRunnable);
        }
    }


}

