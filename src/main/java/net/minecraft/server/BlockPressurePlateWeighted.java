package net.minecraft.server;

import org.bukkit.craftbukkit.event.CraftEventFactory; // CraftBukkit

public class BlockPressurePlateWeighted extends BlockPressurePlateAbstract {
    private final int a;

    protected BlockPressurePlateWeighted(String s, Material material, int i) {
        super(s, material);
        this.a = i;
    }

    protected int e(World world, int i, int j, int k) {
        // CraftBukkit start
        int l = 0; /* Math.min(world.a(Entity.class, this.a(i, j, k)).size(), this.a); */
        java.util.Iterator iterator = world.a(Entity.class, this.a(i, j, k)).iterator();

        while (iterator.hasNext()) {
            Entity entity = (Entity) iterator.next();
            org.bukkit.event.Cancellable cancellable;

            if (entity instanceof EntityHuman) {
                cancellable = CraftEventFactory.callPlayerInteractEvent((EntityHuman) entity, org.bukkit.event.block.Action.PHYSICAL, i, j, k, -1, null);
            } else {
                cancellable = CraftEventFactory.callEntityInteractEvent(entity, world.getWorld().getBlockAt(i, j, k));
            }

            // We only want to block turning the plate on if all events are cancelled
            if (!cancellable.isCancelled()) {
                l++;
            }
        }

        l = Math.min(l, this.a);
        // CraftBukkit end

        if (l <= 0) {
            return 0;
        }

        float f = (float) Math.min(this.a, l) / (float) this.a;
        return MathHelper.f(f * 15.0F);
    }

    protected int c(int i) {
        return i;
    }

    protected int d(int i) {
        return i;
    }

    public int a(World world) {
        return 10;
    }
}
