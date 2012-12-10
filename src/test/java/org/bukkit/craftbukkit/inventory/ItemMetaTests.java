package org.bukkit.craftbukkit.inventory;

import static org.junit.Assert.*;
import static org.hamcrest.Matchers.*;

import java.util.Arrays;
import java.util.List;

import org.bukkit.DummyServer;
import org.bukkit.Material;
import org.bukkit.craftbukkit.inventory.ItemStackTests.StackProvider;
import org.bukkit.craftbukkit.inventory.ItemStackTests.StackWrapper;
import org.bukkit.craftbukkit.inventory.ItemStackTests.BukkitWrapper;
import org.bukkit.craftbukkit.inventory.ItemStackTests.CraftWrapper;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.junit.Test;

public class ItemMetaTests {

    static {
        DummyServer.setup();
    }

    @Test
    public void testCrazyEquality() {
        CraftItemStack craft = CraftItemStack.asCraftCopy(new ItemStack(1));
        craft.setItemMeta(craft.getItemMeta());
        ItemStack bukkit = new ItemStack(craft);
        assertThat(craft, is(bukkit));
        assertThat(bukkit, is((ItemStack) craft));
    }

    @Test
    public void testEachExtraData() {
        final List<StackProvider> providers = Arrays.asList(
            new StackProvider(Material.BOOK_AND_QUILL) {
                @Override ItemStack operate(final ItemStack cleanStack) {
                    final BookMeta meta = (BookMeta) cleanStack.getItemMeta();
                    meta.setAuthor("Some author");
                    meta.setPages("Page 1", "Page 2");
                    meta.setTitle("A title");
                    cleanStack.setItemMeta(meta);
                    return cleanStack;
                }
            },
            new StackProvider(Material.SKULL_ITEM) {
                @Override ItemStack operate(final ItemStack cleanStack) {
                    final SkullMeta meta = (SkullMeta) cleanStack.getItemMeta();
                    meta.setOwner("Notch");
                    cleanStack.setItemMeta(meta);
                    return cleanStack;
                }
            },
            new StackProvider(Material.POTION) {
                @Override ItemStack operate(final ItemStack cleanStack) {
                    final PotionMeta meta = (PotionMeta) cleanStack.getItemMeta();
                    // TODO
                    cleanStack.setItemMeta(meta);
                    return cleanStack;
                }
            },
            new StackProvider(Material.LEATHER_BOOTS) {
                @Override ItemStack operate(final ItemStack cleanStack) {
                    final LeatherArmorMeta meta = (LeatherArmorMeta) cleanStack.getItemMeta();
                    // TODO
                    cleanStack.setItemMeta(meta);
                    return cleanStack;
                }
            }
        );
        for (final StackProvider provider : providers) {
            downCastTest(new BukkitWrapper(provider));
            downCastTest(new CraftWrapper(provider));
        }
    }

    private void downCastTest(final StackWrapper provider) {
        final String name = provider.toString();
        final ItemStack blank = new ItemStack(1);
        final ItemStack craftBlank = CraftItemStack.asCraftCopy(blank);

        downCastTest(name, provider.stack(), blank);
        blank.setItemMeta(blank.getItemMeta());
        downCastTest(name, provider.stack(), blank);

        downCastTest(name, provider.stack(), craftBlank);
        craftBlank.setItemMeta(craftBlank.getItemMeta());
        downCastTest(name, provider.stack(), craftBlank);
    }

    private void downCastTest(final String name, final ItemStack stack, final ItemStack blank) {
        assertThat(name, stack, is(not(blank)));
        assertThat(name, stack.getItemMeta(), is(not(blank.getItemMeta())));

        stack.setTypeId(1);

        assertThat(name, stack, is(blank));
    }
}