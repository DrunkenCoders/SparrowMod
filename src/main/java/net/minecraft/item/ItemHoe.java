package net.minecraft.item;

import net.canarymod.api.world.blocks.BlockFace;
import net.canarymod.api.world.blocks.CanaryBlock;
import net.canarymod.hook.player.ItemUseHook;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;

public class ItemHoe extends Item {

    protected ToolMaterial a;

    public ItemHoe(ToolMaterial item_toolmaterial) {
        this.a = item_toolmaterial;
        this.h = 1;
        this.f(item_toolmaterial.a());
        this.a(CreativeTabs.i);
    }

    public boolean a(ItemStack itemstack, EntityPlayer entityplayer, World world, int i0, int i1, int i2, int i3, float f0, float f1, float f2) {
        if (!entityplayer.a(i0, i1, i2, i3, itemstack)) {
            return false;
        }
        else {
            Block block = world.a(i0, i1, i2);
            Block block1 = Blocks.ak;
            // CanaryMod: ItemUse
            CanaryBlock clicked = (CanaryBlock) world.getCanaryWorld().getBlockAt(i0, i1, i2);
            clicked.setFaceClicked(BlockFace.fromByte((byte) i3));
            ItemUseHook hook = (ItemUseHook) new ItemUseHook(((EntityPlayerMP) entityplayer).getPlayer(), itemstack.getCanaryItem(), clicked).call();
            //

            if (i3 != 0 && world.a(i0, i1 + 1, i2).o() == Material.a && (block == Blocks.c || block == Blocks.d) && !hook.isCanceled()) {
                world.a((double) ((float) i0 + 0.5F), (double) ((float) i1 + 0.5F), (double) ((float) i2 + 0.5F), block1.H.e(), (block1.H.c() + 1.0F) / 2.0F, block1.H.d() * 0.8F);
                if (world.E) {
                    return true;
                }
                else {
                    world.b(i0, i1, i2, block1);
                    itemstack.a(1, (EntityLivingBase) entityplayer);
                    return true;
                }
            } else {
                return false;
            }
        }
    }

    public String i() {
        return this.a.toString();
    }
}
