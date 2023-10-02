package org.example;

import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ProjectKorra;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.ElementalAbility;
import com.projectkorra.projectkorra.ability.LavaAbility;
import com.projectkorra.projectkorra.attribute.Attribute;
import com.projectkorra.projectkorra.configuration.Config;
import com.projectkorra.projectkorra.configuration.ConfigManager;
import com.projectkorra.projectkorra.earthbending.EarthTunnel;
import com.projectkorra.projectkorra.region.RegionProtection;
import com.projectkorra.projectkorra.util.ParticleEffect;
import com.projectkorra.projectkorra.util.TempBlock;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.util.Vector;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import static org.bukkit.Material.*;

public class VolcanicFlow extends LavaAbility implements AddonAbility {

    private static List<String> ignoredBlocks;
    @Attribute(Attribute.COOLDOWN)
    private long cooldown;
    @Attribute(Attribute.RANGE)
    private int range;
    @Attribute(Attribute.RADIUS)
    private double radius;
    @Attribute(Attribute.FIRE_TICK)
    private double fireTick;
    @Attribute(Attribute.DURATION)
    private long duration;
    private long magmaDuration;
    private long magmaDurationTicks;
    private long lavaDuration;
    private double lavaDamage;
    private boolean cancelMagmaDamage;
    private boolean cancelLavaWaveDamage;
    private long startTime;
    private double time;
    private boolean hasStarted;
    private int blockCounter;
    private int maxBlocks;
    private boolean enableWave;
    private boolean worksUnderWater;
    public Set<Block> lavaBlocks = new HashSet<Block>();
    public Set<Block> lavaWaveBlocks = new HashSet<Block>();
    public Set<Block> magmaBlocks = new HashSet<Block>();
    public static final String METADATA_KEY_MAGMABLOCK = "Vincentcf:/VolcanicFlow://Magma";
    public static final String METADATA_KEY_LAVAWAVEBLOCK = "Vincentcf:/VolcanicFlow://LavaWave";
    public static final String METADATA_KEY_LAVABLOCK = "Vincentcf:/VolcanicFlow://Lava";
    private static final FixedMetadataValue METADATA_VALUE = new FixedMetadataValue(ProjectKorra.plugin,1);
    private static final BlockFace[] BLOCK_FACES = new BlockFace[]{BlockFace.NORTH, BlockFace.EAST, BlockFace.UP, BlockFace.DOWN, BlockFace.SOUTH, BlockFace.WEST};


    public VolcanicFlow(Player player) {
        super(player);

        if (!bPlayer.canBendIgnoreBinds(this)) return;

        cooldown = ConfigManager.getConfig().getLong("ExtraAbilities.Vincentcf.VolcanicFlow.Cooldown");
        range = ConfigManager.getConfig().getInt("ExtraAbilities.Vincentcf.VolcanicFlow.Range");
        radius = ConfigManager.getConfig().getDouble("ExtraAbilities.Vincentcf.VolcanicFlow.Radius");
        fireTick = ConfigManager.getConfig().getDouble("ExtraAbilities.Vincentcf.VolcanicFlow.FireTick");
        magmaDuration = ConfigManager.getConfig().getLong("ExtraAbilities.Vincentcf.VolcanicFlow.MagmaDuration");
        lavaDuration = ConfigManager.getConfig().getLong("ExtraAbilities.Vincentcf.VolcanicFlow.LavaDuration");
        lavaDamage = ConfigManager.getConfig().getDouble("ExtraAbilities.Vincentcf.VolcanicFlow.LavaDamage");
        cancelMagmaDamage = ConfigManager.getConfig().getBoolean("ExtraAbilities.Vincentcf.VolcanicFlow.cancelMagmaDamage");
        cancelLavaWaveDamage = ConfigManager.getConfig().getBoolean("ExtraAbilities.Vincentcf.VolcanicFlow.cancelLavaWaveDamage");
        duration = ConfigManager.getConfig().getLong("ExtraAbilities.Vincentcf.VolcanicFlow.Duration");
        maxBlocks = ConfigManager.getConfig().getInt("ExtraAbilities.Vincentcf.VolcanicFlow.maxBlocks");
        enableWave = ConfigManager.getConfig().getBoolean("ExtraAbilities.Vincentcf.VolcanicFlow.enableWave");
        worksUnderWater = ConfigManager.getConfig().getBoolean("ExtraAbilities.Vincentcf.VolcanicFlow.worksUnderWater");
        ignoredBlocks = ConfigManager.getConfig().getStringList("ExtraAbilities.Vincentcf.VolcanicFlow.ignoredBlocks");


        magmaDurationTicks = (magmaDuration / 50);
        hasStarted = false;
        blockCounter = 0;


        start();

    }


    @Override
    public void progress() {

        if (!bPlayer.canBend(this)) {
            remove();
            return;
        }

        if (hasStarted && System.currentTimeMillis() > this.time + duration || blockCounter > maxBlocks) {
            remove();
            bPlayer.addCooldown(this);
            return;
        }

        if (!player.isSneaking()) {
            remove();
            if (hasStarted) {
            bPlayer.addCooldown(this);
            }
            return;
        }

        List<Block> locBlocks = GeneralMethods.getBlocksAroundPoint(getTargetLocation().add(-0.5, -0.5, -0.5), radius);

        for (Block element : locBlocks) {

            if (!worksUnderWater && element.getLocation().add(0, 1, 0).getBlock().getType() == WATER || element.getLocation().add(0, 1, 0).getBlock().getType() == BUBBLE_COLUMN) continue;

            if (!isEarthbendable(element) || element.getType() == LAVA || ignoredBlocks.contains(element.getType().name()) || TempBlock.isTempBlock(element) || !isTouchingAir(element) || RegionProtection.isRegionProtected(this, element.getLocation()))
                continue;
            if (!hasStarted) {
                this.time = System.currentTimeMillis();
                hasStarted = true;
            }
            blockCounter++;


            if (enableWave) {
                Block lavaWaveBlock = element.getLocation().add(0, 1, 0).getBlock();
                if (!lavaWaveBlock.getType().isSolid() && !lavaWaveBlock.isLiquid() && !(lavaWaveBlock.getType() == LAVA)) {
                    TempBlock lavaTB = new TempBlock(lavaWaveBlock, LAVA, GeneralMethods.getLavaData(5));
                    Block metaDataBlock = lavaTB.getBlock();
                    metaDataBlock.setMetadata(METADATA_KEY_LAVAWAVEBLOCK,METADATA_VALUE);
                    lavaWaveBlocks.add(lavaTB.getBlock());
                    Runnable lavaWaveBlockRemoveRunnable = new Runnable() {
                        @Override
                        public void run() {
                            lavaWaveBlocks.remove(lavaTB.getBlock());
                            metaDataBlock.removeMetadata(METADATA_KEY_LAVAWAVEBLOCK, ProjectKorra.plugin);
                        }
                    };
                    lavaTB.setRevertTime(250);
                    Bukkit.getScheduler().runTaskLater(ProjectKorra.plugin, lavaWaveBlockRemoveRunnable, 6);
                }
            }

            TempBlock tb = new TempBlock(element, Material.MAGMA_BLOCK);
            Block metaDataBlock = tb.getBlock();
            metaDataBlock.setMetadata(METADATA_KEY_MAGMABLOCK,METADATA_VALUE);
            magmaBlocks.add(tb.getBlock());

            Runnable magmaBlockRemoveRunnable = new Runnable() {
                @Override
                public void run() {
                    magmaBlocks.remove(tb.getBlock());
                    metaDataBlock.removeMetadata(METADATA_KEY_MAGMABLOCK, ProjectKorra.plugin);
                }
            };
            Bukkit.getScheduler().runTaskLater(ProjectKorra.plugin, magmaBlockRemoveRunnable, magmaDurationTicks);

            if (ThreadLocalRandom.current().nextInt(10) == 0) {
                element.getLocation().getWorld().playSound(element.getLocation(), Sound.BLOCK_LAVA_AMBIENT, 1, 1);
            } else if (ThreadLocalRandom.current().nextInt(3 ) == 0) {
                ParticleEffect.LAVA.display(tb.getLocation(), 2, 0.5, 0.5, 0.5, 0.1);
            }
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    tb.setType(LAVA);
                    Block metaDataBlock = tb.getBlock();
                    metaDataBlock.setMetadata(METADATA_KEY_LAVABLOCK,METADATA_VALUE);
                    lavaBlocks.add(tb.getBlock());
                    Runnable lavaBlockRemoveRunnable = new Runnable() {
                        @Override
                        public void run() {
                            lavaBlocks.remove(element);
                            metaDataBlock.removeMetadata(METADATA_KEY_LAVABLOCK, ProjectKorra.plugin);
                        }
                    };
                    lavaBlocks.add(element);
                    Bukkit.getScheduler().runTaskLater(ProjectKorra.plugin, lavaBlockRemoveRunnable, lavaDuration/50);
                    tb.setRevertTime(lavaDuration);
                }
            };
            Bukkit.getScheduler().runTaskLater(ProjectKorra.plugin, runnable, magmaDurationTicks);

        }

    }



    private boolean isTouchingAir(Block element) {

        for (BlockFace blockFace : BLOCK_FACES) {

            if (!element.getRelative(blockFace).getType().isSolid()) {
                return true;
            }
        }
        return false;
    }

    private Location getTargetLocation() {

        Location location = player.getEyeLocation();
        Vector newDir = location.getDirection().multiply(0.1);

        for (double i = 0; i <= range; i+=0.1) {

            if (location.getBlock().getType().isSolid()) {

                return location;
            }
            location.add(newDir);
        }
        return location;
    }


    @Override
    public boolean isSneakAbility() {
        return true;
    }

    @Override
    public boolean isHarmlessAbility() {
        return false;
    }

    @Override
    public long getCooldown() {
        return cooldown;
    }

    @Override
    public String getName() {
        return "VolcanicFlow";
    }

    @Override
    public String getInstructions() {
        return "Hold Sneak while looking at an earthbendable block";
    }

    @Override
    public String getDescription() {
        return "VolcanicFlow melts any earthbendable block and turns it into lava! Hold shift and drag your mouse over bendable blocks to create a lava source!";
    }

    @Override
    public Location getLocation() {
        return getTargetLocation();
    }

    @Override
    public void load() {
        ConfigManager.getConfig().addDefault("ExtraAbilities.Vincentcf.VolcanicFlow.Cooldown", 5000);
        ConfigManager.getConfig().addDefault("ExtraAbilities.Vincentcf.VolcanicFlow.Range", 5);
        ConfigManager.getConfig().addDefault("ExtraAbilities.Vincentcf.VolcanicFlow.Radius", 1.5);
        ConfigManager.getConfig().addDefault("ExtraAbilities.Vincentcf.VolcanicFlow.FireTick", 10);
        ConfigManager.getConfig().addDefault("ExtraAbilities.Vincentcf.VolcanicFlow.MagmaDuration", 1500);
        ConfigManager.getConfig().addDefault("ExtraAbilities.Vincentcf.VolcanicFlow.LavaDuration", 8000);
        ConfigManager.getConfig().addDefault("ExtraAbilities.Vincentcf.VolcanicFlow.LavaDamage", 1);
        ConfigManager.getConfig().addDefault("ExtraAbilities.Vincentcf.VolcanicFlow.Duration", 3000);
        ConfigManager.getConfig().addDefault("ExtraAbilities.Vincentcf.VolcanicFlow.cancelMagmaDamage", true);
        ConfigManager.getConfig().addDefault("ExtraAbilities.Vincentcf.VolcanicFlow.cancelLavaWaveDamage", true);
        ConfigManager.getConfig().addDefault("ExtraAbilities.Vincentcf.VolcanicFlow.maxBlocks", 100);
        ConfigManager.getConfig().addDefault("ExtraAbilities.Vincentcf.VolcanicFlow.enableWave", true);
        ConfigManager.getConfig().addDefault("ExtraAbilities.Vincentcf.VolcanicFlow.worksUnderWater", true);
        String[] blocksToIgnore = { Material.DIAMOND_ORE.name()};
        ConfigManager.getConfig().addDefault("ExtraAbilities.Vincentcf.VolcanicFlow.ignoredBlocks", blocksToIgnore);

        Listener listener = new VolcanicFlowListener();
        ProjectKorra.plugin.getServer().getPluginManager().registerEvents(listener, ProjectKorra.plugin);
        ConfigManager.defaultConfig.save();

        ProjectKorra.plugin.getLogger().info("Successfully enabled " + getName() + " " + getVersion() + " by " + getAuthor());
    }

    @Override
    public void stop() {
        remove();
    }

    @Override
    public String getAuthor() {
        return "Vincentcf";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }
}