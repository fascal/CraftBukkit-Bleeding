package net.minecraft.server;

import java.util.Random;

// CraftBukkit start
import org.bukkit.TreeType;
import org.bukkit.event.world.StructureGrowEvent;
// CraftBukkit end

public class BlockMushroom extends BlockPlant implements IBlockFragilePlantElement {

    protected BlockMushroom() {
        float f = 0.2F;

        this.a(0.5F - f, 0.0F, 0.5F - f, 0.5F + f, f * 2.0F, 0.5F + f);
        this.a(true);
    }

    public void a(World world, int i, int j, int k, Random random) {
        final int sourceX = i, sourceY = j, sourceZ = k; // CraftBukkit
        if (random.nextInt(25) == 0) {
            byte b0 = 4;
            int l = 5;

            int i1;
            int j1;
            int k1;

            for (i1 = i - b0; i1 <= i + b0; ++i1) {
                for (j1 = k - b0; j1 <= k + b0; ++j1) {
                    for (k1 = j - 1; k1 <= j + 1; ++k1) {
                        if (world.getType(i1, k1, j1) == this) {
                            --l;
                            if (l <= 0) {
                                return;
                            }
                        }
                    }
                }
            }

            i1 = i + random.nextInt(3) - 1;
            j1 = j + random.nextInt(2) - random.nextInt(2);
            k1 = k + random.nextInt(3) - 1;

            for (int l1 = 0; l1 < 4; ++l1) {
                if (world.isEmpty(i1, j1, k1) && this.j(world, i1, j1, k1)) {
                    i = i1;
                    j = j1;
                    k = k1;
                }

                i1 = i + random.nextInt(3) - 1;
                j1 = j + random.nextInt(2) - random.nextInt(2);
                k1 = k + random.nextInt(3) - 1;
            }

            if (world.isEmpty(i1, j1, k1) && this.j(world, i1, j1, k1)) {
                // CraftBukkit start
                /* world.setTypeAndData(i1, j1, k1, this, 0, 2); */
                org.bukkit.craftbukkit.event.CraftEventFactory.handleBlockSpreadEvent(world, i1, j1, k1, sourceX, sourceY, sourceZ, this, 0);
                // CraftBukkit end
            }
        }
    }

    public boolean canPlace(World world, int i, int j, int k) {
        return super.canPlace(world, i, j, k) && this.j(world, i, j, k);
    }

    protected boolean a(Block block) {
        return block.j();
    }

    public boolean j(World world, int i, int j, int k) {
        if (j >= 0 && j < 256) {
            Block block = world.getType(i, j - 1, k);

            return block == Blocks.MYCEL || block == Blocks.DIRT && world.getData(i, j - 1, k) == 2 || world.j(i, j, k) < 13 && this.a(block);
        } else {
            return false;
        }
    }

    // CraftBukkit - added bonemeal, player and itemstack
    public boolean grow(World world, int i, int j, int k, Random random, boolean bonemeal, org.bukkit.entity.Player player, ItemStack itemstack) {
        int l = world.getData(i, j, k);

        world.setAir(i, j, k);
        WorldGenHugeMushroom worldgenhugemushroom = null;

        // CraftBukkit start
        boolean grown = false;
        TreeType treeType = null;
        // CraftBukkit end

        if (this == Blocks.BROWN_MUSHROOM) {
            treeType = TreeType.BROWN_MUSHROOM; // CraftBukkit
            worldgenhugemushroom = new WorldGenHugeMushroom(0);
        } else if (this == Blocks.RED_MUSHROOM) {
            treeType = TreeType.RED_MUSHROOM; // CraftBukkit
            worldgenhugemushroom = new WorldGenHugeMushroom(1);
        }

        // CraftBukkit start
        StructureGrowEvent event = new StructureGrowEvent(new org.bukkit.Location(world.getWorld(), i, j, k), treeType, bonemeal, player, new java.util.ArrayList<org.bukkit.block.BlockState>());
        if (worldgenhugemushroom != null /*&& worldgenhugemushroom.a(world, random, i, j, k)*/) {
            grown = worldgenhugemushroom.grow(new org.bukkit.craftbukkit.CraftBlockChangeDelegate((org.bukkit.BlockChangeDelegate) world), random, i, j, k, event, itemstack, world.getWorld());
            if (event.isFromBonemeal() && itemstack != null) {
                --itemstack.count;
            }
        }
        if (grown && !event.isCancelled()) {
            return true;
            // CraftBukkit end
        } else {
            world.setTypeAndData(i, j, k, this, l, 3);
            return false;
        }
    }

    public boolean a(World world, int i, int j, int k, boolean flag) {
        return true;
    }

    public boolean a(World world, Random random, int i, int j, int k) {
        return (double) random.nextFloat() < 0.4D;
    }

    public void b(World world, Random random, int i, int j, int k) {
        this.grow(world, i, j, k, random, false, null, null); // CraftBukkit - Add bonemeal, player, and itemstack
    }
}
