package net.minecraft.inventory;

import net.canarymod.api.inventory.CanaryEnchantment;
import net.canarymod.api.inventory.Enchantment;
import net.canarymod.api.world.blocks.CanaryEnchantmentTable;
import net.canarymod.api.world.blocks.EnchantmentTable;
import net.canarymod.hook.player.EnchantHook;
import net.minecraft.enchantment.EnchantmentData;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ContainerEnchantment extends Container {

    public IInventory a = new InventoryBasic("Enchant", true, 1) {

        public int d() {
            return 1;
        }

        public void e() {
            super.e();
            ContainerEnchantment.this.a((IInventory) this);
        }
    };
    public World h; // CanaryMod: private => public
    public int i; // CanaryMod: private => public
    public int j; // CanaryMod: private => public
    public int k; // CanaryMod: private => public
    private Random l = new Random();
    public long f;
    public int[] g = new int[3];

    public ContainerEnchantment(InventoryPlayer inventoryplayer, World world, int i0, int i1, int i2) {
        this.h = world;
        this.i = i0;
        this.j = i1;
        this.k = i2;
        this.a(new Slot(this.a, 0, 25, 47) {

            public boolean a(ItemStack itemstack) {
                return true;
            }
        });

        int i3;

        for (i3 = 0; i3 < 3; ++i3) {
            for (int i4 = 0; i4 < 9; ++i4) {
                this.a(new Slot(inventoryplayer, i4 + i3 * 9 + 9, 8 + i4 * 18, 84 + i3 * 18));
            }
        }

        for (i3 = 0; i3 < 9; ++i3) {
            this.a(new Slot(inventoryplayer, i3, 8 + i3 * 18, 142));
        }

        this.inventory = new CanaryEnchantmentTable(this); // CanaryMod: Set inventory instance
    }

    public void a(ICrafting icrafting) {
        super.a(icrafting);
        icrafting.a(this, 0, this.g[0]);
        icrafting.a(this, 1, this.g[1]);
        icrafting.a(this, 2, this.g[2]);
    }

    public void b() {
        super.b();

        for (int i0 = 0; i0 < this.e.size(); ++i0) {
            ICrafting icrafting = (ICrafting) this.e.get(i0);

            icrafting.a(this, 0, this.g[0]);
            icrafting.a(this, 1, this.g[1]);
            icrafting.a(this, 2, this.g[2]);
        }
    }

    public void a(IInventory iinventory) {
        if (iinventory == this.a) {
            ItemStack itemstack = iinventory.a(0);
            int i0;

            if (itemstack != null && itemstack.x()) {
                this.f = this.l.nextLong();
                if (!this.h.E) {
                    i0 = 0;

                    int i1;

                    // CanaryMod: if fake cases are used, skip bookcase checks
                    if (!((CanaryEnchantmentTable) this.inventory).hasFakeCases()) {

                        for (i1 = -1; i1 <= 1; ++i1) {
                            for (int i2 = -1; i2 <= 1; ++i2) {
                                if ((i1 != 0 || i2 != 0) && this.h.c(this.i + i2, this.j, this.k + i1) && this.h.c(this.i + i2, this.j + 1, this.k + i1)) {
                                    if (this.h.a(this.i + i2 * 2, this.j, this.k + i1 * 2) == Blocks.X) {
                                        ++i0;
                                    }

                                    if (this.h.a(this.i + i2 * 2, this.j + 1, this.k + i1 * 2) == Blocks.X) {
                                        ++i0;
                                    }

                                    if (i2 != 0 && i1 != 0) {
                                        if (this.h.a(this.i + i2 * 2, this.j, this.k + i1) == Blocks.X) {
                                            ++i0;
                                        }

                                        if (this.h.a(this.i + i2 * 2, this.j + 1, this.k + i1) == Blocks.X) {
                                            ++i0;
                                        }

                                        if (this.h.a(this.i + i2, this.j, this.k + i1 * 2) == Blocks.X) {
                                            ++i0;
                                        }

                                        if (this.h.a(this.i + i2, this.j + 1, this.k + i1 * 2) == Blocks.X) {
                                            ++i0;
                                        }
                                    }
                                }
                            }
                        }
                    }
                    else {
                        // CanaryMod: set case count
                        i0 = ((CanaryEnchantmentTable) this.inventory).getFakeCaseCount();
                    }

                    for (i1 = 0; i1 < 3; ++i1) {
                        this.g[i1] = EnchantmentHelper.a(this.l, i1, i0, itemstack);
                    }

                    this.b();
                }
            }
            else {
                for (i0 = 0; i0 < 3; ++i0) {
                    this.g[i0] = 0;
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    public boolean a(EntityPlayer entityplayer, int i0) {
        ItemStack itemstack = this.a.a(0);

        if (this.g[i0] > 0 && itemstack != null && (entityplayer.bG >= this.g[i0] || entityplayer.bF.d)) {
            if (!this.h.E) {
                List<EnchantmentData> list = EnchantmentHelper.b(this.l, itemstack, this.g[i0]);
                boolean flag0 = itemstack.b() == Items.aG;

                if (list != null) {
                    // CanaryMod: Enchant
                    List<net.canarymod.api.inventory.Enchantment> cench = new ArrayList<Enchantment>();

                    for (EnchantmentData endat : list) {
                        cench.add(new CanaryEnchantment(endat));
                    }
                    EnchantHook hook = (EnchantHook) new EnchantHook(((EntityPlayerMP) entityplayer).getPlayer(), itemstack.getCanaryItem(), (EnchantmentTable) this.inventory, cench).call();
                    if (!hook.isCanceled() && hook.isValid(false)) {
                        list.clear();
                        for (net.canarymod.api.inventory.Enchantment ench : hook.getEnchantmentList()) {
                            list.add(((CanaryEnchantment) ench).getEnchantmentData());
                        }

                        entityplayer.a(-this.g[i0]);
                        if (flag0) {
                            itemstack.a((Item) Items.bR);
                        }

                        int i1 = flag0 && list.size() > 1 ? this.l.nextInt(list.size()) : -1;

                        for (int i2 = 0; i2 < list.size(); ++i2) {
                            EnchantmentData enchantmentdata = (EnchantmentData) list.get(i2);

                            if (!flag0 || i2 != i1) {
                                if (flag0) {
                                    Items.bR.a(itemstack, enchantmentdata);
                                }
                                else {
                                    itemstack.a(enchantmentdata.b, enchantmentdata.c);
                                }
                            }
                        }

                        this.a(this.a);
                    }
                    //
                }
            }

            return true;
        }
        else {
            return false;
        }
    }

    public void b(EntityPlayer entityplayer) {
        super.b(entityplayer);
        if (!this.h.E) {
            ItemStack itemstack = this.a.a_(0);

            if (itemstack != null) {
                entityplayer.a(itemstack, false);
            }
        }
    }

    public boolean a(EntityPlayer entityplayer) {
        // CanaryMod: remote inventories
        if (this.inventory.canOpenRemote()) {
            return true;
        }
        //

        return this.h.a(this.i, this.j, this.k) != Blocks.bn ? false : entityplayer.e((double) this.i + 0.5D, (double) this.j + 0.5D, (double) this.k + 0.5D) <= 64.0D;
    }

    public ItemStack b(EntityPlayer entityplayer, int i0) {
        ItemStack itemstack = null;
        Slot slot = (Slot) this.c.get(i0);

        if (slot != null && slot.e()) {
            ItemStack itemstack1 = slot.d();

            itemstack = itemstack1.m();
            if (i0 == 0) {
                if (!this.a(itemstack1, 1, 37, true)) {
                    return null;
                }
            }
            else {
                if (((Slot) this.c.get(0)).e() || !((Slot) this.c.get(0)).a(itemstack1)) {
                    return null;
                }

                if (itemstack1.p() && itemstack1.b == 1) {
                    ((Slot) this.c.get(0)).c(itemstack1.m());
                    itemstack1.b = 0;
                }
                else if (itemstack1.b >= 1) {
                    ((Slot) this.c.get(0)).c(new ItemStack(itemstack1.b(), 1, itemstack1.k()));
                    --itemstack1.b;
                }
            }

            if (itemstack1.b == 0) {
                slot.c((ItemStack) null);
            }
            else {
                slot.f();
            }

            if (itemstack1.b == itemstack.b) {
                return null;
            }

            slot.a(entityplayer, itemstack1);
        }

        return itemstack;
    }
}
