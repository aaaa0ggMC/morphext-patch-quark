package aaaa0ggmc.morphext.quark.action;

import aaaa0ggmc.morphext.integration.metamorph.ICooldownAction;
import mchorse.metamorph.api.abilities.IAction;
import mchorse.metamorph.api.morphs.AbstractMorph;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.projectile.EntityArrow;
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
 * 潜影骷髅深底穿刺箭 (Quark Dweller Pierce Shot Action):
 * 射出淬有地底枯槁寒毒的穿刺重箭，命中施加 8 秒饥饿 II 与挖掘疲劳 II！
 * 冷却 10 秒。
 */
public class QuarkDwellerPierceAction implements IAction, ICooldownAction {

    public static final int COOLDOWN = 200;

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
                SoundEvents.ENTITY_ARROW_SHOOT, SoundCategory.HOSTILE, 1.2F, 0.7F);

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
                    EntityLivingBase hit = (EntityLivingBase) raytraceResultIn.entityHit;
                    hit.addPotionEffect(new PotionEffect(MobEffects.HUNGER, 160, 1));
                    hit.addPotionEffect(new PotionEffect(MobEffects.MINING_FATIGUE, 160, 1));
                }
                if (!this.world.isRemote && this.world instanceof WorldServer) {
                    ((WorldServer) this.world).spawnParticle(EnumParticleTypes.SUSPENDED_DEPTH,
                            this.posX, this.posY, this.posZ, 20, 0.5D, 0.5D, 0.5D, 0.05D);
                }
            }
        };

        arrow.shootingEntity = entity;
        arrow.setDamage(arrow.getDamage() + 2.0D);
        arrow.shoot(look.x, look.y + 0.05D, look.z, 2.0F, 0.8F);
        world.spawnEntity(arrow);
    }
}
