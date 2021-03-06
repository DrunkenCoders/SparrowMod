package net.minecraft.entity.item;

import net.canarymod.api.entity.CanaryEnderCrystal;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldProviderEnd;

public class EntityEnderCrystal extends Entity {

    public int a;
    public int b;

    public EntityEnderCrystal(World world) {
        super(world);
        this.l = true;
        this.a(2.0F, 2.0F);
        this.M = this.O / 2.0F;
        this.b = 5;
        this.a = this.aa.nextInt(100000);
        this.entity = new CanaryEnderCrystal(this); // CanaryMod: Wrap Entity
    }

    protected boolean g_() {
        return false;
    }

    protected void c() {
        this.ag.a(8, Integer.valueOf(this.b));
    }

    public void h() {
        this.q = this.t;
        this.r = this.u;
        this.s = this.v;
        ++this.a;
        this.ag.b(8, Integer.valueOf(this.b));
        int i0 = MathHelper.c(this.t);
        int i1 = MathHelper.c(this.u);
        int i2 = MathHelper.c(this.v);

        if (this.p.t instanceof WorldProviderEnd && this.p.a(i0, i1, i2) != Blocks.ab) {
            this.p.b(i0, i1, i2, (Block) Blocks.ab);
        }
    }

    protected void b(NBTTagCompound nbttagcompound) {
    }

    protected void a(NBTTagCompound nbttagcompound) {
    }

    public boolean R() {
        return true;
    }

    public boolean a(DamageSource damagesource, float f0) {
        if (this.aw()) {
            return false;
        }
        else {
            if (!this.L && !this.p.E) {
                // CanaryMod: Check if one hit can kill this entity
                if (((CanaryEnderCrystal) entity).isOneHitDetonate()) {
                    this.b = 0;
                }
                else {
                    this.b -= f0;
                }
                //
                if (this.b <= 0) {
                    this.B();
                    if (!this.p.E) {
                        this.p.a(this, this.t, this.u, this.v, ((CanaryEnderCrystal) entity).getPower(), true); // CanaryMod: configure Explosion power and set the entity properly
                    }
                }
            }

            return true;
        }
    }
}
