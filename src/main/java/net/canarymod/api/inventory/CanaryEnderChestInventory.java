package net.canarymod.api.inventory;

import net.canarymod.api.entity.living.humanoid.Human;
import net.minecraft.inventory.InventoryEnderChest;
import net.minecraft.item.ItemStack;

import java.util.Arrays;

/**
 * EnderChest Inventory implementation
 *
 * @author Jason (darkdiplomat)
 */
public class CanaryEnderChestInventory extends CanaryEntityInventory implements EnderChestInventory {
    private Human human;

    public CanaryEnderChestInventory(InventoryEnderChest echest, Human human) {
        super(echest);
        this.human = human;
    }

    public InventoryType getInventoryType() {
        return InventoryType.CHEST;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Human getInventoryOwner() {
        return human;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clearContents() {
        Arrays.fill(getHandle().c, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Item[] clearInventory() {
        ItemStack[] items = Arrays.copyOf(getHandle().c, getSize());

        clearContents();
        return CanaryItem.stackArrayToItemArray(items);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Item[] getContents() {
        return CanaryItem.stackArrayToItemArray(getHandle().c);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setContents(Item[] items) {
        System.arraycopy(CanaryItem.itemArrayToStackArray(items), 0, getHandle().c, 0, getSize());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setInventoryName(String value) {
        getHandle().setName(value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void update() {
        getHandle().l_();
    }

    public InventoryEnderChest getHandle() {
        return (InventoryEnderChest) inventory;
    }
}
