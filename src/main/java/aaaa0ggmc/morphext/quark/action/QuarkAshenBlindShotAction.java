package aaaa0ggmc.morphext.quark.action;

import aaaa0ggmc.morphext.integration.metamorph.ICooldownAction;
import mchorse.metamorph.api.abilities.IAction;
import mchorse.metamorph.api.morphs.AbstractMorph;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityTippedArrow;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 灰烬骷髅盲烟火矢 (Quark Ashen Blind Shot Action):
 * 朝准星视线方向射出一支燃烧的灰烬火矢，命中爆发 4 格浓厚黑烟迷雾，致盲敌人 6 秒！
 * 冷却 12 秒。
 */
public class QuarkAshenBlindShotAction implements IAction, ICooldownAction {

    public static final int COOLDOWN = 240;

    private final Map<UUID, Long> nextUseAt = new ConcurrentHashMap<>();

    @Override
    public int getCooldownTicks(EntityLivingBase entity) {
        if (entity == null || entity.world == null) {
            return COOLDOWN;
        }
        Long next = this.nextUseAt.get(entity.getUniqueID());
        if (next == null) {
            return 0;
        }
        long remaining = next - entity.world.getTotalWorldTime();
        return remaining > 0L ? (int) Math.min(remaining, COOLDOWN) : 0;
    }

    @Override
    public void execute(EntityLivingBase entity, AbstractMorph morph) {
        World world = entity.world;
        entity.swingArm(EnumHand.MAIN_HAND);
        world.playSound(null, entity.posX, entity.posY, entity.posZ,
                SoundEvents.ENTITY_SKELETON_SHOOT, SoundCategory.HOSTILE, 1.2F, 0.8F);

        if (world.isRemote) return;

        long now = world.getTotalWorldTime();
        Long next = this.nextUseAt.get(entity.getUniqueID());
        if (next != null && next > now) {
            return;
        }
        this.nextUseAt.put(entity.getUniqueID(), now + COOLDOWN);

        Vec3d look = entity.getLookVec();
        double spawnX = entity.posX + look.x * 1.5D;
        double spawnY = entity.posY + entity.getEyeHeight() * 0.85D + look.y * 1.5D;
        double spawnZ = entity.posZ + look.z * 1.5D;

        EntityTippedArrow arrow = new EntityTippedArrow(world, spawnX, spawnY, spawnZ) {
            @Override
            protected void onHit(RayTraceResult raytraceResultIn) {
                super.onHit(raytraceResultIn);
                if (raytraceResultIn.entityHit instanceof EntityLivingBase && !raytraceResultIn.entityHit.equals(entity)) {
                    ((EntityLivingBase) raytraceResultIn.entityHit).addPotionEffect(new PotionEffect(MobEffects.BLINDNESS, 120, 0));
                    ((EntityLivingBase) raytraceResultIn.entityHit).setFire(4);
                }
                if (!this.world.isRemote && this.world instanceof WorldServer) {
                    ((WorldServer) this.world).spawnParticle(EnumParticleTypes.SMOKE_LARGE,
                            this.posX, this.posY, this.posZ, 25, 1.2D, 1.0D, 1.2D, 0.05D);
                }
            }
        };

        arrow.shootingEntity = entity;
        arrow.setFire(100);
        arrow.shoot(look.x, look.y + 0.05D, look.z, 1.8F, 1.0F);
        world.spawnEntity(arrow);
    }
}
