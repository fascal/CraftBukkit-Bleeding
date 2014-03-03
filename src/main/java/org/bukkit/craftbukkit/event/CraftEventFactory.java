package org.bukkit.craftbukkit.event;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.minecraft.server.ChunkCoordinates;
import net.minecraft.server.Container;
import net.minecraft.server.DamageSource;
import net.minecraft.server.Entity;
import net.minecraft.server.EntityAnimal;
import net.minecraft.server.EntityArrow;
import net.minecraft.server.EntityDamageSource;
import net.minecraft.server.EntityDamageSourceIndirect;
import net.minecraft.server.EntityGhast;
import net.minecraft.server.EntityGolem;
import net.minecraft.server.EntityHuman;
import net.minecraft.server.EntityInsentient;
import net.minecraft.server.EntityItem;
import net.minecraft.server.EntityLiving;
import net.minecraft.server.EntityMonster;
import net.minecraft.server.EntityPlayer;
import net.minecraft.server.EntityPotion;
import net.minecraft.server.EntitySlime;
import net.minecraft.server.EntityWaterAnimal;
import net.minecraft.server.Explosion;
import net.minecraft.server.FoodMetaData;
import net.minecraft.server.IInventory;
import net.minecraft.server.ISourceBlock;
import net.minecraft.server.InventoryCrafting;
import net.minecraft.server.ItemStack;
import net.minecraft.server.Items;
import net.minecraft.server.PacketPlayInCloseWindow;
import net.minecraft.server.PacketPlayOutSetSlot;
import net.minecraft.server.PacketPlayOutUpdateHealth;
import net.minecraft.server.PathfinderGoalDefendVillage;
import net.minecraft.server.PathfinderGoalHurtByTarget;
import net.minecraft.server.PathfinderGoalNearestAttackableTarget;
import net.minecraft.server.PathfinderGoalOwnerHurtByTarget;
import net.minecraft.server.PathfinderGoalOwnerHurtTarget;
import net.minecraft.server.PathfinderGoalTarget;
import net.minecraft.server.Slot;
import net.minecraft.server.TileEntityFurnace;
import net.minecraft.server.World;
import net.minecraft.server.WorldServer;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.Statistic.Type;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.CraftStatistic;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.block.CraftBlock;
import org.bukkit.craftbukkit.block.CraftBlockState;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.craftbukkit.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.inventory.CraftInventoryCrafting;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.craftbukkit.util.CraftDamageSource;
import org.bukkit.craftbukkit.util.CraftMagicNumbers;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Item;
import org.bukkit.entity.LightningStrike;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Pig;
import org.bukkit.entity.PigZombie;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Slime;
import org.bukkit.entity.ThrownExpBottle;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.block.*;
import org.bukkit.event.block.BlockIgniteEvent.IgniteCause;
import org.bukkit.event.entity.*;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.inventory.FurnaceBurnEvent;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.server.ServerListPingEvent;
import org.bukkit.event.weather.ThunderChangeEvent;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.util.Vector;

public class CraftEventFactory {
    public static final DamageSource MELTING = CraftDamageSource.copyOf(DamageSource.BURN);
    public static final DamageSource POISON = CraftDamageSource.copyOf(DamageSource.MAGIC);

    // helper methods
    private static boolean canBuild(CraftWorld world, Player player, int x, int z) {
        WorldServer worldServer = world.getHandle();
        int spawnSize = Bukkit.getServer().getSpawnRadius();

        if (world.getHandle().dimension != 0) return true;
        if (spawnSize <= 0) return true;
        if (((CraftServer) Bukkit.getServer()).getHandle().getOPs().isEmpty()) return true;
        if (player.isOp()) return true;

        ChunkCoordinates chunkcoordinates = worldServer.getSpawn();

        int distanceFromSpawn = Math.max(Math.abs(x - chunkcoordinates.x), Math.abs(z - chunkcoordinates.z));
        return distanceFromSpawn > spawnSize;
    }

    public static <T extends Event> T callEvent(T event) {
        Bukkit.getServer().getPluginManager().callEvent(event);
        return event;
    }

    /**
     * Block place methods
     */
    public static BlockPlaceEvent callBlockPlaceEvent(World world, EntityHuman who, BlockState replacedBlockState, int clickedX, int clickedY, int clickedZ) {
        CraftWorld craftWorld = world.getWorld();
        CraftServer craftServer = world.getServer();

        Player player = (who == null) ? null : (Player) who.getBukkitEntity();

        Block blockClicked = craftWorld.getBlockAt(clickedX, clickedY, clickedZ);
        Block placedBlock = replacedBlockState.getBlock();

        boolean canBuild = canBuild(craftWorld, player, placedBlock.getX(), placedBlock.getZ());

        BlockPlaceEvent event = new BlockPlaceEvent(placedBlock, replacedBlockState, blockClicked, player.getItemInHand(), player, canBuild);
        craftServer.getPluginManager().callEvent(event);

        return event;
    }

    /**
     * Bucket methods
     */
    public static PlayerBucketEmptyEvent callPlayerBucketEmptyEvent(EntityHuman who, int clickedX, int clickedY, int clickedZ, int clickedFace, ItemStack itemInHand) {
        return (PlayerBucketEmptyEvent) getPlayerBucketEvent(false, who, clickedX, clickedY, clickedZ, clickedFace, itemInHand, Items.BUCKET);
    }

    public static PlayerBucketFillEvent callPlayerBucketFillEvent(EntityHuman who, int clickedX, int clickedY, int clickedZ, int clickedFace, ItemStack itemInHand, net.minecraft.server.Item bucket) {
        return (PlayerBucketFillEvent) getPlayerBucketEvent(true, who, clickedX, clickedY, clickedZ, clickedFace, itemInHand, bucket);
    }

    private static PlayerEvent getPlayerBucketEvent(boolean isFilling, EntityHuman who, int clickedX, int clickedY, int clickedZ, int clickedFace, ItemStack itemstack, net.minecraft.server.Item item) {
        Player player = (who == null) ? null : (Player) who.getBukkitEntity();
        CraftItemStack itemInHand = CraftItemStack.asNewCraftStack(item);
        Material bucket = CraftMagicNumbers.getMaterial(itemstack.getItem());

        CraftWorld craftWorld = (CraftWorld) player.getWorld();
        CraftServer craftServer = (CraftServer) player.getServer();

        Block blockClicked = craftWorld.getBlockAt(clickedX, clickedY, clickedZ);
        BlockFace blockFace = CraftBlock.notchToBlockFace(clickedFace);

        PlayerEvent event = null;
        if (isFilling) {
            event = new PlayerBucketFillEvent(player, blockClicked, blockFace, bucket, itemInHand);
            ((PlayerBucketFillEvent) event).setCancelled(!canBuild(craftWorld, player, clickedX, clickedZ));
        } else {
            event = new PlayerBucketEmptyEvent(player, blockClicked, blockFace, bucket, itemInHand);
            ((PlayerBucketEmptyEvent) event).setCancelled(!canBuild(craftWorld, player, clickedX, clickedZ));
        }

        craftServer.getPluginManager().callEvent(event);

        return event;
    }

    /**
     * Player Interact event
     */
    public static PlayerInteractEvent callPlayerInteractEvent(EntityHuman who, Action action, ItemStack itemstack) {
        if (action != Action.LEFT_CLICK_AIR && action != Action.RIGHT_CLICK_AIR) {
            throw new IllegalArgumentException();
        }
        return callPlayerInteractEvent(who, action, 0, 256, 0, 0, itemstack);
    }

    public static PlayerInteractEvent callPlayerInteractEvent(EntityHuman who, Action action, int clickedX, int clickedY, int clickedZ, int clickedFace, ItemStack itemstack) {
        Player player = (who == null) ? null : (Player) who.getBukkitEntity();
        CraftItemStack itemInHand = CraftItemStack.asCraftMirror(itemstack);

        CraftWorld craftWorld = (CraftWorld) player.getWorld();
        CraftServer craftServer = (CraftServer) player.getServer();

        Block blockClicked = craftWorld.getBlockAt(clickedX, clickedY, clickedZ);
        BlockFace blockFace = CraftBlock.notchToBlockFace(clickedFace);

        if (clickedY > 255) {
            blockClicked = null;
            switch (action) {
            case LEFT_CLICK_BLOCK:
                action = Action.LEFT_CLICK_AIR;
                break;
            case RIGHT_CLICK_BLOCK:
                action = Action.RIGHT_CLICK_AIR;
                break;
            }
        }

        if (itemInHand.getType() == Material.AIR || itemInHand.getAmount() == 0) {
            itemInHand = null;
        }

        PlayerInteractEvent event = new PlayerInteractEvent(player, action, itemInHand, blockClicked, blockFace);
        craftServer.getPluginManager().callEvent(event);

        return event;
    }

    /**
     * EntityShootBowEvent
     */
    public static EntityShootBowEvent callEntityShootBowEvent(EntityLiving who, ItemStack itemstack, EntityArrow entityArrow, float force) {
        LivingEntity shooter = (LivingEntity) who.getBukkitEntity();
        CraftItemStack itemInHand = CraftItemStack.asCraftMirror(itemstack);
        Arrow arrow = (Arrow) entityArrow.getBukkitEntity();

        if (itemInHand != null && (itemInHand.getType() == Material.AIR || itemInHand.getAmount() == 0)) {
            itemInHand = null;
        }

        EntityShootBowEvent event = new EntityShootBowEvent(shooter, itemInHand, arrow, force);
        Bukkit.getPluginManager().callEvent(event);

        return event;
    }

    /**
     * BlockDamageEvent
     */
    public static BlockDamageEvent callBlockDamageEvent(EntityHuman who, int x, int y, int z, ItemStack itemstack, boolean instaBreak) {
        return callEvent(new BlockDamageEvent((Player) who.getBukkitEntity(), who.world.getWorld().getBlockAt(x, y, z), CraftItemStack.asCraftMirror(itemstack), instaBreak));
    }

    /**
     * EntityTameEvent
     */
    public static EntityTameEvent callEntityTameEvent(EntityInsentient entity, EntityHuman tamer) {
        entity.persistent = true;

        return callEvent(new EntityTameEvent((LivingEntity) entity.getBukkitEntity(), (tamer != null ? tamer.getBukkitEntity() : null)));
    }

    /**
     * ItemDespawnEvent
     */
    public static ItemDespawnEvent callItemDespawnEvent(EntityItem entityitem) {
        org.bukkit.entity.Item entity = (org.bukkit.entity.Item) entityitem.getBukkitEntity();

        ItemDespawnEvent event = new ItemDespawnEvent(entity, entity.getLocation());

        entity.getServer().getPluginManager().callEvent(event);
        return event;
    }

    /**
     * PotionSplashEvent
     */
    public static PotionSplashEvent callPotionSplashEvent(EntityPotion potion, Map<LivingEntity, Double> affectedEntities) {
        ThrownPotion thrownPotion = (ThrownPotion) potion.getBukkitEntity();

        PotionSplashEvent event = new PotionSplashEvent(thrownPotion, affectedEntities);
        Bukkit.getPluginManager().callEvent(event);
        return event;
    }

    /**
     * BlockFadeEvent
     */
    public static BlockFadeEvent callBlockFadeEvent(Block block, net.minecraft.server.Block type) {
        BlockState state = block.getState();
        state.setTypeId(net.minecraft.server.Block.b(type));

        BlockFadeEvent event = new BlockFadeEvent(block, state);
        Bukkit.getPluginManager().callEvent(event);
        return event;
    }

    public static void handleBlockFadeEvent(World world, int x, int y, int z, net.minecraft.server.Block type) {
        Block block = world.getWorld().getBlockAt(x, y, z);
        BlockState state = block.getState();
        state.setType(CraftMagicNumbers.getMaterial(type));

        if (callEvent(new BlockFadeEvent(block, state)).isCancelled()) {
            state.update(true);
        }
    }

    public static void handleBlockSpreadEvent(World world, int x, int y, int z, int sourceX, int sourceY, int sourceZ, net.minecraft.server.Block type, int data) {
        CraftWorld bukkitWorld = world.getWorld();

        handleBlockSpreadEvent(bukkitWorld.getBlockAt(x, y, z), bukkitWorld.getBlockAt(sourceX, sourceY, sourceZ), type, data);
    }

    public static void handleBlockSpreadEvent(Block block, Block source, net.minecraft.server.Block type, int data) {
        BlockState state = block.getState();
        state.setType(CraftMagicNumbers.getMaterial(type));
        state.setRawData((byte) data);

        if (!callEvent(new BlockSpreadEvent(block, source, state)).isCancelled()) {
            state.update(true);
        }
    }

    public static EntityDeathEvent callEntityDeathEvent(EntityLiving victim) {
        return callEntityDeathEvent(victim, new ArrayList<org.bukkit.inventory.ItemStack>(0));
    }

    public static EntityDeathEvent callEntityDeathEvent(EntityLiving victim, List<org.bukkit.inventory.ItemStack> drops) {
        CraftLivingEntity entity = (CraftLivingEntity) victim.getBukkitEntity();
        EntityDeathEvent event = callEvent(new EntityDeathEvent(entity, drops, victim.getExpReward()));
        victim.expToDrop = event.getDroppedExp();

        CraftWorld world = (CraftWorld) entity.getWorld();

        for (org.bukkit.inventory.ItemStack stack : event.getDrops()) {
            if (stack == null || stack.getType() == Material.AIR || stack.getAmount() == 0) {
                continue;
            }

            world.dropItemNaturally(entity.getLocation(), stack);
        }

        return event;
    }

    public static PlayerDeathEvent callPlayerDeathEvent(EntityPlayer victim, List<org.bukkit.inventory.ItemStack> drops, String deathMessage) {
        CraftPlayer entity = victim.getBukkitEntity();
        PlayerDeathEvent event = new PlayerDeathEvent(entity, drops, victim.getExpReward(), 0, deathMessage);
        org.bukkit.World world = entity.getWorld();
        Bukkit.getServer().getPluginManager().callEvent(event);

        victim.keepLevel = event.getKeepLevel();
        victim.newLevel = event.getNewLevel();
        victim.newTotalExp = event.getNewTotalExp();
        victim.expToDrop = event.getDroppedExp();
        victim.newExp = event.getNewExp();

        for (org.bukkit.inventory.ItemStack stack : event.getDrops()) {
            if (stack == null || stack.getType() == Material.AIR) continue;

            world.dropItemNaturally(entity.getLocation(), stack);
        }

        return event;
    }

    /**
     * Server methods
     */
    public static ServerListPingEvent callServerListPingEvent(Server craftServer, InetAddress address, String motd, int numPlayers, int maxPlayers) {
        ServerListPingEvent event = new ServerListPingEvent(address, motd, numPlayers, maxPlayers);
        craftServer.getPluginManager().callEvent(event);
        return event;
    }

    /**
     * EntityDamage(ByEntityEvent)
     */
    public static EntityDamageEvent callEntityDamageEvent(Entity damager, Entity damagee, DamageCause cause, double damage) {
        EntityDamageEvent event;
        if (damager != null) {
            event = new EntityDamageByEntityEvent(damager.getBukkitEntity(), damagee.getBukkitEntity(), cause, damage);
        } else {
            event = new EntityDamageEvent(damagee.getBukkitEntity(), cause, damage);
        }

        callEvent(event);

        if (!event.isCancelled()) {
            event.getEntity().setLastDamageCause(event);
        }

        return event;
    }

    public static EntityDamageEvent handleEntityDamageEvent(Entity entity, DamageSource source, float damage) {
        // Should be isExplosion
        if (source.c()) {
            return null;
        } else if (source instanceof EntityDamageSource) {
            Entity damager = source.getEntity();
            DamageCause cause = DamageCause.ENTITY_ATTACK;

            if (source instanceof EntityDamageSourceIndirect) {
                damager = ((EntityDamageSourceIndirect) source).getProximateDamageSource();
                if (damager.getBukkitEntity() instanceof ThrownPotion) {
                    cause = DamageCause.MAGIC;
                } else if (damager.getBukkitEntity() instanceof Projectile) {
                    cause = DamageCause.PROJECTILE;
                }
            } else if ("thorns".equals(source.translationIndex)) {
                cause = DamageCause.THORNS;
            }

            return callEntityDamageEvent(damager, entity, cause, damage);
        } else if (source == DamageSource.OUT_OF_WORLD) {
            return handleEntityDamageByBlockEvent(null, entity, source, damage);
        }

        DamageCause cause = null;
        if (source == DamageSource.FIRE) {
            cause = DamageCause.FIRE;
        } else if (source == DamageSource.STARVE) {
            cause = DamageCause.STARVATION;
        } else if (source == DamageSource.WITHER) {
            cause = DamageCause.WITHER;
        } else if (source == DamageSource.STUCK) {
            cause = DamageCause.SUFFOCATION;
        } else if (source == DamageSource.DROWN) {
            cause = DamageCause.DROWNING;
        } else if (source == DamageSource.BURN) {
            cause = DamageCause.FIRE_TICK;
        } else if (source == MELTING) {
            cause = DamageCause.MELTING;
        } else if (source == POISON) {
            cause = DamageCause.POISON;
        } else if (source == DamageSource.MAGIC) {
            cause = DamageCause.MAGIC;
        }

        if (cause != null) {
            return callEntityDamageEvent(null, entity, cause, damage);
        }

        // If an event was called earlier, we return null.
        // EG: Cactus, Lava, EntityEnderPearl "fall", FallingSand
        return null;
    }

    // Non-Living Entities such as EntityEnderCrystal need to call this
    public static boolean handleNonLivingEntityDamageEvent(Entity entity, DamageSource source, float damage) {
        if (!(source instanceof EntityDamageSource)) {
            return false;
        }
        EntityDamageEvent event = handleEntityDamageEvent(entity, source, damage);
        if (event == null) {
            return false;
        }
        return event.isCancelled() || event.getDamage() == 0;
    }

    public static EntityDamageEvent handleEntityDamageByBlockEvent(Block block, Entity entity, DamageSource source, float damage) {
        DamageCause cause = null;

        if (source == DamageSource.CACTUS) {
            cause = DamageCause.CONTACT;
        } else if (source == DamageSource.OUT_OF_WORLD) {
            cause = DamageCause.VOID;
        }

        EntityDamageEvent event = callEvent(new EntityDamageByBlockEvent(block, entity.getBukkitEntity(), cause, damage));

        if (!event.isCancelled()) {
            event.getEntity().setLastDamageCause(event);
        }

        return event;
    }

    public static PlayerLevelChangeEvent callPlayerLevelChangeEvent(Player player, int oldLevel, int newLevel) {
        return callEvent(new PlayerLevelChangeEvent(player, oldLevel, newLevel));
    }

    public static PlayerExpChangeEvent callPlayerExpChangeEvent(EntityHuman entity, int expAmount) {
        return callEvent(new PlayerExpChangeEvent((Player) entity.getBukkitEntity(), expAmount));
    }

    public static void handleBlockGrowEvent(World world, int x, int y, int z, net.minecraft.server.Block type, int data) {
        Block block = world.getWorld().getBlockAt(x, y, z);
        CraftBlockState state = (CraftBlockState) block.getState();
        state.setTypeId(net.minecraft.server.Block.b(type));
        state.setRawData((byte) data);

        if (!callEvent(new BlockGrowEvent(block, state)).isCancelled()) {
            state.update(true);
        }
    }

    public static void handleFoodLevelChangeEvent(EntityHuman entity, int level, float saturationLevel, boolean eating) {
        FoodMetaData foodData = entity.getFoodData();
        int oldFoodLevel = foodData.foodLevel;

        FoodLevelChangeEvent event;
        if (eating) {
            event = callEvent(new FoodLevelChangeEvent(entity.getBukkitEntity(), level + oldFoodLevel));
        } else {
            event = callEvent(new FoodLevelChangeEvent(entity.getBukkitEntity(), level));
        }

        if (!event.isCancelled()) {
            if (eating) {
                foodData.eat(event.getFoodLevel() - oldFoodLevel, saturationLevel);
            } else {
                foodData.foodLevel = event.getFoodLevel();
                foodData.saturationLevel = saturationLevel;
            }
        }

        EntityPlayer player = (EntityPlayer) entity;
        player.playerConnection.sendPacket(new PacketPlayOutUpdateHealth(player.getBukkitEntity().getScaledHealth(), foodData.foodLevel, foodData.saturationLevel));
    }

    public static PigZapEvent callPigZapEvent(Entity pig, Entity lightning, Entity pigzombie) {
        return callEvent(new PigZapEvent((Pig) pig.getBukkitEntity(), (LightningStrike) lightning.getBukkitEntity(), (PigZombie) pigzombie.getBukkitEntity()));
    }

    public static HorseJumpEvent callHorseJumpEvent(Entity horse, float power) {
        return callEvent(new HorseJumpEvent((Horse) horse.getBukkitEntity(), power));
    }

    public static EntityChangeBlockEvent callEntityChangeBlockEvent(Entity entity, int x, int y, int z, net.minecraft.server.Block type, int data, boolean cancelled) {
        EntityChangeBlockEvent event = new EntityChangeBlockEvent(entity.getBukkitEntity(), entity.world.getWorld().getBlockAt(x, y, z), CraftMagicNumbers.getMaterial(type), (byte) data);
        event.setCancelled(cancelled);

        return callEvent(event);
    }

    public static EntityChangeBlockEvent callEntityChangeBlockEvent(Entity entity, int x, int y, int z, net.minecraft.server.Block type, int data) {
        return callEntityChangeBlockEvent(entity, x, y, z, type, data, false);
    }

    public static CreeperPowerEvent callCreeperPowerEvent(Entity creeper, Entity lightning, CreeperPowerEvent.PowerCause cause) {
        if (lightning == null) {
            return callEvent(new CreeperPowerEvent((Creeper) creeper.getBukkitEntity(), cause));
        } else {
            return callEvent(new CreeperPowerEvent((Creeper) creeper.getBukkitEntity(), (LightningStrike) lightning.getBukkitEntity(), cause));
        }
    }

    public static EntityTargetEvent callEntityTargetEvent(Entity entity, Entity target, EntityTargetEvent.TargetReason reason) {
        return callEvent(new EntityTargetEvent(entity.getBukkitEntity(), target == null ? null : target.getBukkitEntity(), reason));
    }

    public static Entity handleEntityTargetEvent(Entity entity, Entity oldTarget, Entity newTarget, EntityTargetEvent.TargetReason reason) {
        EntityTargetEvent event = callEvent(new EntityTargetEvent(entity.getBukkitEntity(), newTarget == null ? null : newTarget.getBukkitEntity(), reason));

        if (event.isCancelled()) {
            return oldTarget;
        } else {
            org.bukkit.entity.Entity bukkitTarget = event.getTarget();
            return bukkitTarget == null ? null : ((CraftEntity) bukkitTarget).getHandle();
        }
    }

    public static boolean handleEntityTargetLivingEvent(PathfinderGoalTarget goalTarget, EntityLiving target) {
        EntityTargetEvent.TargetReason reason = EntityTargetEvent.TargetReason.RANDOM_TARGET;

        if (goalTarget instanceof PathfinderGoalDefendVillage) {
            reason = EntityTargetEvent.TargetReason.DEFEND_VILLAGE;
        } else if (goalTarget instanceof PathfinderGoalHurtByTarget) {
            reason = EntityTargetEvent.TargetReason.TARGET_ATTACKED_ENTITY;
        } else if (goalTarget instanceof PathfinderGoalNearestAttackableTarget) {
            if (target instanceof EntityHuman) {
                reason = EntityTargetEvent.TargetReason.CLOSEST_PLAYER;
            }
        } else if (goalTarget instanceof PathfinderGoalOwnerHurtByTarget) {
            reason = EntityTargetEvent.TargetReason.TARGET_ATTACKED_OWNER;
        } else if (goalTarget instanceof PathfinderGoalOwnerHurtTarget) {
            reason = EntityTargetEvent.TargetReason.OWNER_ATTACKED_TARGET;
        }

        EntityTargetLivingEntityEvent event = callEvent(new EntityTargetLivingEntityEvent(goalTarget.c.getBukkitEntity(), (LivingEntity) target.getBukkitEntity(), reason));
        LivingEntity eventTarget = event.getTarget();

        if (event.isCancelled() || eventTarget == null) {
            goalTarget.c.setGoalTarget(null);
            return false;
        } else if (target.getBukkitEntity() != eventTarget) {
            goalTarget.c.setGoalTarget((EntityLiving) ((CraftEntity) eventTarget).getHandle());
        }

        goalTarget.c.target = ((CraftEntity) eventTarget).getHandle();
        return true;
    }

    public static EntityBreakDoorEvent callEntityBreakDoorEvent(Entity entity, int x, int y, int z) {
        return callEvent(new EntityBreakDoorEvent((LivingEntity) entity.getBukkitEntity(), entity.world.getWorld().getBlockAt(x, y, z)));
    }

    public static Container callInventoryOpenEvent(EntityPlayer player, Container container) {
        if (player.activeContainer != player.defaultContainer) { // fire INVENTORY_CLOSE if one already open
            player.playerConnection.a(new PacketPlayInCloseWindow(player.activeContainer.windowId));
        }

        CraftServer server = player.world.getServer();
        CraftPlayer craftPlayer = player.getBukkitEntity();
        player.activeContainer.transferTo(container, craftPlayer);

        InventoryOpenEvent event = new InventoryOpenEvent(container.getBukkitView());
        server.getPluginManager().callEvent(event);

        if (event.isCancelled()) {
            container.transferTo(player.activeContainer, craftPlayer);
            return null;
        }

        return container;
    }

    public static ItemStack callPreCraftEvent(InventoryCrafting matrix, ItemStack result, InventoryView lastCraftView, boolean isRepair) {
        CraftInventoryCrafting inventory = new CraftInventoryCrafting(matrix, matrix.resultInventory);
        inventory.setResult(CraftItemStack.asCraftMirror(result));

        PrepareItemCraftEvent event = new PrepareItemCraftEvent(inventory, lastCraftView, isRepair);
        Bukkit.getPluginManager().callEvent(event);

        org.bukkit.inventory.ItemStack bitem = event.getInventory().getResult();

        return CraftItemStack.asNMSCopy(bitem);
    }

    public static boolean handleEntitySpawnEvent(Entity entity, SpawnReason spawnReason) {
        org.bukkit.event.Cancellable event = null;
        org.bukkit.entity.Entity bukkitEntity = entity.getBukkitEntity();

        if (entity instanceof EntityLiving && !(entity instanceof EntityPlayer)) {
            boolean isAnimal = entity instanceof EntityAnimal || entity instanceof EntityWaterAnimal || entity instanceof EntityGolem;
            boolean isMonster = entity instanceof EntityMonster || entity instanceof EntityGhast || entity instanceof EntitySlime;

            if (spawnReason != SpawnReason.CUSTOM) {
                if (isAnimal && !entity.world.allowAnimals || isMonster && !entity.world.allowMonsters)  {
                    entity.dead = true;
                    return false;
                }
            }

            event = callEvent(new CreatureSpawnEvent((LivingEntity) bukkitEntity, spawnReason));
        } else if (entity instanceof EntityItem) {
            event = callEvent(new ItemSpawnEvent((Item) bukkitEntity, bukkitEntity.getLocation()));
        } else if (bukkitEntity instanceof Projectile) {
            // Not all projectiles extend EntityProjectile, so check for Bukkit interface instead
            event = callEvent(new ProjectileLaunchEvent(bukkitEntity));
        }

        if (event != null && (event.isCancelled() || entity.dead)) {
            entity.dead = true;
            return false;
        }

        return true;
    }

    public static ProjectileHitEvent callProjectileHitEvent(Entity entity) {
        ProjectileHitEvent event = new ProjectileHitEvent((Projectile) entity.getBukkitEntity());
        entity.world.getServer().getPluginManager().callEvent(event);
        return event;
    }

    public static ExpBottleEvent callExpBottleEvent(Entity entity, int exp) {
        ThrownExpBottle bottle = (ThrownExpBottle) entity.getBukkitEntity();
        ExpBottleEvent event = new ExpBottleEvent(bottle, exp);
        Bukkit.getPluginManager().callEvent(event);
        return event;
    }

    public static BlockRedstoneEvent callRedstoneChange(World world, int x, int y, int z, int oldCurrent, int newCurrent) {
        return callEvent(new BlockRedstoneEvent(world.getWorld().getBlockAt(x, y, z), oldCurrent, newCurrent));
    }

    public static BlockRedstoneEvent callRedstoneChange(World world, int x, int y, int z) {
        Block block = world.getWorld().getBlockAt(x, y, z);
        int power = block.getBlockPower();

        return callEvent(new BlockRedstoneEvent(world.getWorld().getBlockAt(x, y, z), power, power));
    }

    public static NotePlayEvent callNotePlayEvent(World world, int x, int y, int z, byte instrument, byte note) {
        return callEvent(new NotePlayEvent(world.getWorld().getBlockAt(x, y, z), org.bukkit.Instrument.getByType(instrument), new org.bukkit.Note(note)));
    }

    public static void callPlayerItemBreakEvent(EntityHuman human, ItemStack brokenItem) {
        CraftItemStack item = CraftItemStack.asCraftMirror(brokenItem);
        PlayerItemBreakEvent event = new PlayerItemBreakEvent((Player) human.getBukkitEntity(), item);
        Bukkit.getPluginManager().callEvent(event);
    }

    public static BlockIgniteEvent callBlockIgniteEvent(World world, int x, int y, int z, int igniterX, int igniterY, int igniterZ) {
        org.bukkit.World bukkitWorld = world.getWorld();
        Block igniter = bukkitWorld.getBlockAt(igniterX, igniterY, igniterZ);
        IgniteCause cause;
        switch (igniter.getType()) {
            case LAVA:
            case STATIONARY_LAVA:
                cause = IgniteCause.LAVA;
                break;
            case DISPENSER:
                cause = IgniteCause.FLINT_AND_STEEL;
                break;
            case FIRE: // Fire or any other unknown block counts as SPREAD.
            default:
                cause = IgniteCause.SPREAD;
        }

        BlockIgniteEvent event = new BlockIgniteEvent(bukkitWorld.getBlockAt(x, y, z), cause, igniter);
        world.getServer().getPluginManager().callEvent(event);
        return event;
    }

    public static BlockIgniteEvent callBlockIgniteEvent(World world, int x, int y, int z, Entity igniter) {
        org.bukkit.World bukkitWorld = world.getWorld();
        org.bukkit.entity.Entity bukkitIgniter = igniter.getBukkitEntity();
        IgniteCause cause;
        switch (bukkitIgniter.getType()) {
        case ENDER_CRYSTAL:
            cause = IgniteCause.ENDER_CRYSTAL;
            break;
        case LIGHTNING:
            cause = IgniteCause.LIGHTNING;
            break;
        case SMALL_FIREBALL:
        case FIREBALL:
            cause = IgniteCause.FIREBALL;
            break;
        default:
            cause = IgniteCause.FLINT_AND_STEEL;
        }

        BlockIgniteEvent event = new BlockIgniteEvent(bukkitWorld.getBlockAt(x, y, z), cause, bukkitIgniter);
        world.getServer().getPluginManager().callEvent(event);
        return event;
    }

    public static BlockIgniteEvent callBlockIgniteEvent(World world, int x, int y, int z, Explosion explosion) {
        org.bukkit.World bukkitWorld = world.getWorld();
        org.bukkit.entity.Entity igniter = explosion.source == null ? null : explosion.source.getBukkitEntity();

        BlockIgniteEvent event = new BlockIgniteEvent(bukkitWorld.getBlockAt(x, y, z), IgniteCause.EXPLOSION, igniter);
        world.getServer().getPluginManager().callEvent(event);
        return event;
    }

    public static BlockIgniteEvent callBlockIgniteEvent(World world, int x, int y, int z, IgniteCause cause, Entity igniter) {
        BlockIgniteEvent event = new BlockIgniteEvent(world.getWorld().getBlockAt(x, y, z), cause, igniter.getBukkitEntity());
        world.getServer().getPluginManager().callEvent(event);
        return event;
    }

    public static void handleInventoryCloseEvent(EntityHuman human) {
        InventoryCloseEvent event = new InventoryCloseEvent(human.activeContainer.getBukkitView());
        human.world.getServer().getPluginManager().callEvent(event);
        human.activeContainer.transferTo(human.defaultContainer, human.getBukkitEntity());
    }

    public static void handleEditBookEvent(EntityPlayer player, ItemStack newBookItem) {
        int itemInHandIndex = player.inventory.itemInHandIndex;

        PlayerEditBookEvent editBookEvent = new PlayerEditBookEvent(player.getBukkitEntity(), player.inventory.itemInHandIndex, (BookMeta) CraftItemStack.getItemMeta(player.inventory.getItemInHand()), (BookMeta) CraftItemStack.getItemMeta(newBookItem), newBookItem.getItem() == Items.WRITTEN_BOOK);
        player.world.getServer().getPluginManager().callEvent(editBookEvent);
        ItemStack itemInHand = player.inventory.getItem(itemInHandIndex);

        // If they've got the same item in their hand, it'll need to be updated.
        if (itemInHand.getItem() == Items.BOOK_AND_QUILL) {
            if (!editBookEvent.isCancelled()) {
                CraftItemStack.setItemMeta(itemInHand, editBookEvent.getNewBookMeta());
                if (editBookEvent.isSigning()) {
                    itemInHand.setItem(Items.WRITTEN_BOOK);
                }
            }

            // Client will have updated its idea of the book item; we need to overwrite that
            Slot slot = player.activeContainer.a((IInventory) player.inventory, itemInHandIndex);
            player.playerConnection.sendPacket(new PacketPlayOutSetSlot(player.activeContainer.windowId, slot.rawSlotIndex, itemInHand));
        }
    }

    public static PlayerUnleashEntityEvent callPlayerUnleashEntityEvent(EntityInsentient entity, EntityHuman player) {
        PlayerUnleashEntityEvent event = new PlayerUnleashEntityEvent(entity.getBukkitEntity(), (Player) player.getBukkitEntity());
        entity.world.getServer().getPluginManager().callEvent(event);
        return event;
    }

    public static PlayerLeashEntityEvent callPlayerLeashEntityEvent(EntityInsentient entity, Entity leashHolder, EntityHuman player) {
        PlayerLeashEntityEvent event = new PlayerLeashEntityEvent(entity.getBukkitEntity(), leashHolder.getBukkitEntity(), (Player) player.getBukkitEntity());
        entity.world.getServer().getPluginManager().callEvent(event);
        return event;
    }

    public static EntityInteractEvent callEntityInteractEvent(Entity entity, Block block) {
        return callEvent(new EntityInteractEvent(entity.getBukkitEntity(), block));
    }

    public static Cancellable handleStatisticsIncrease(EntityHuman entityHuman, net.minecraft.server.Statistic statistic, int current, int incrementation) {
        Player player = ((EntityPlayer) entityHuman).getBukkitEntity();
        Event event;
        if (statistic instanceof net.minecraft.server.Achievement) {
            if (current != 0) {
                return null;
            }
            event = new PlayerAchievementAwardedEvent(player, CraftStatistic.getBukkitAchievement((net.minecraft.server.Achievement) statistic));
        } else {
            org.bukkit.Statistic stat = CraftStatistic.getBukkitStatistic(statistic);
            switch (stat) {
                case FALL_ONE_CM:
                case BOAT_ONE_CM:
                case CLIMB_ONE_CM:
                case DIVE_ONE_CM:
                case FLY_ONE_CM:
                case HORSE_ONE_CM:
                case MINECART_ONE_CM:
                case PIG_ONE_CM:
                case PLAY_ONE_TICK:
                case SWIM_ONE_CM:
                case WALK_ONE_CM:
                    // Do not process event for these - too spammy
                    return null;
                default:
            }
            if (stat.getType() == Type.UNTYPED) {
                event = new PlayerStatisticIncrementEvent(player, stat, current, current + incrementation);
            } else if (stat.getType() == Type.ENTITY) {
                EntityType entityType = CraftStatistic.getEntityTypeFromStatistic(statistic);
                event = new PlayerStatisticIncrementEvent(player, stat, current, current + incrementation, entityType);
            } else {
                Material material = CraftStatistic.getMaterialFromStatistic(statistic);
                event = new PlayerStatisticIncrementEvent(player, stat, current, current + incrementation, material);
            }
        }
        entityHuman.world.getServer().getPluginManager().callEvent(event);
        return (Cancellable) event;
    }

    public static BlockFromToEvent callBlockFromToEvent(World world, int x, int y, int z, int toX, int toY, int toZ) {
        CraftWorld bukkitWorld = world.getWorld();

        return callEvent(new BlockFromToEvent(bukkitWorld.getBlockAt(x, y, z), bukkitWorld.getBlockAt(toX, toY, toZ)));
    }

    public static BlockFromToEvent callBlockFromToEvent(Block block, BlockFace face) {
        return callEvent(new BlockFromToEvent(block, face));
    }

    public static InventoryMoveItemEvent callInventoryMoveItemEvent(Inventory sourceInventory, CraftItemStack itemStack, Inventory destinationInventory, boolean didSourceInitiate) {
        return callEvent(new InventoryMoveItemEvent(sourceInventory, itemStack, destinationInventory, didSourceInitiate));
    }

    public static InventoryDragEvent callInventoryDragEvent(InventoryView view, org.bukkit.inventory.ItemStack oldCursor, org.bukkit.inventory.ItemStack newCursor, boolean isRightClick, Map<Integer, org.bukkit.inventory.ItemStack> itemMap) {
        return callEvent(new InventoryDragEvent(view, oldCursor, newCursor, isRightClick, itemMap));
    }

    public static EntityPortalEnterEvent callEntityPortalEnterEvent(Entity entity, World world, int x, int y, int z) {
        return callEvent(new EntityPortalEnterEvent(entity.getBukkitEntity(), new Location(world.getWorld(), x, y, z)));
    }

    public static BlockBurnEvent callBlockBurnEvent(World world, int x, int y, int z) {
        return callEvent(new BlockBurnEvent(world.getWorld().getBlockAt(x, y, z)));
    }

    public static LeavesDecayEvent callLeavesDecayEvent(World world, int x, int y, int z) {
        return callEvent(new LeavesDecayEvent(world.getWorld().getBlockAt(x, y, z)));
    }

    public static BlockPistonEvent callBlockPistonEvent(World world, int x, int y, int z, int length, int blockFace) {
        if (length <= 0) {
            return callEvent(new BlockPistonRetractEvent(world.getWorld().getBlockAt(x, y, z), CraftBlock.notchToBlockFace(blockFace)));
        } else {
            return callEvent(new BlockPistonExtendEvent(world.getWorld().getBlockAt(x, y, z), length, CraftBlock.notchToBlockFace(blockFace)));
        }
    }

    public static FurnaceSmeltEvent callFurnaceSmeltEvent(World world, int x, int y, int z, ItemStack source, ItemStack result) {
        return callEvent(new FurnaceSmeltEvent(world.getWorld().getBlockAt(x, y, z), CraftItemStack.asCraftMirror(source), CraftItemStack.asBukkitCopy(result)));
    }

    public static FurnaceBurnEvent callFurnaceBurnEvent(World world, int x, int y, int z, ItemStack fuel) {
        return callEvent(new FurnaceBurnEvent(world.getWorld().getBlockAt(x, y, z), CraftItemStack.asCraftMirror(fuel), TileEntityFurnace.fuelTime(fuel)));
    }

    public static FurnaceExtractEvent callFurnaceExtractEvent(EntityHuman entity, TileEntityFurnace furnace, ItemStack itemStack, int xp) {
        return callEvent(new FurnaceExtractEvent((Player) entity.getBukkitEntity(), entity.world.getWorld().getBlockAt(furnace.x, furnace.y, furnace.z), CraftMagicNumbers.getMaterial(itemStack.getItem()), itemStack.count, xp));
    }

    public static BrewEvent callBrewEvent(World world, int x, int y, int z, BrewerInventory inventory) {
        return callEvent(new BrewEvent(world.getWorld().getBlockAt(x, y, z), inventory));
    }

    public static ThunderChangeEvent callThunderChangeEvent(CraftWorld world, boolean isThundering) {
        return callEvent(new ThunderChangeEvent(world, isThundering));
    }

    public static WeatherChangeEvent callWeatherChangeEvent(CraftWorld world, boolean isStorming) {
        return callEvent(new WeatherChangeEvent(world, isStorming));
    }

    public static EntityUnleashEvent callEntityUnleashEvent(Entity entity, EntityUnleashEvent.UnleashReason reason) {
        return callEvent(new EntityUnleashEvent(entity.getBukkitEntity(), reason));
    }

    public static SlimeSplitEvent callSlimeSplitEvent(Entity entity, int count) {
        return callEvent(new SlimeSplitEvent((Slime) entity.getBukkitEntity(), count));
    }

    public static BlockCanBuildEvent callBlockCanBuildEvent(World world, int x, int y, int z, net.minecraft.server.Block block, boolean canBuild) {
        return callEvent(new BlockCanBuildEvent(world.getWorld().getBlockAt(x, y, z), CraftMagicNumbers.getId(block), canBuild));
    }

    public static BlockDispenseEvent callBlockDispenseEvent(ISourceBlock isourceblock, CraftItemStack item, double vectorX, double vectorY, double vectorZ) {
        return callEvent(new BlockDispenseEvent(isourceblock.k().getWorld().getBlockAt(isourceblock.getBlockX(), isourceblock.getBlockY(), isourceblock.getBlockZ()), item.clone(), new Vector(vectorX, vectorY, vectorZ)));
    }
}
