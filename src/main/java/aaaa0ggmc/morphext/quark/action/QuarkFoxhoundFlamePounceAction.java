package aaaa0ggmc.morphext.quark.action;

import aaaa0ggmc.morphext.integration.metamorph.ICooldownAction;
import mchorse.metamorph.api.abilities.IAction;
import mchorse.metamorph.api.morphs.AbstractMorph;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 熔岩猎犬烈焰扑咬与火环护体 (Quark Foxhound Flame Pounce Action):
 * 向准星视线方向爆发式烈焰飞扑，扑咬沿途目标造成 8 点火焰物理伤害并点燃敌人，
 * 同时在周身展开护体地狱火环，获得 4 秒抗性提升与击退保护。
 * 冷却 14 秒。
 */
public class QuarkFoxhoundFlamePounceAction implements IAction, ICooldownAction {

    public static final int COOLDOWN = 280;

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
        if (!world.isRemote) {
            long now = world.getTotalWorldTime();
            Long next = this.nextUseAt.get(entity.getUniqueID());
            if (next != null && next > now) {
                return;
            }
            this.nextUseAt.put(entity.getUniqueID(), now + COOLDOWN);
        }
        Vec3d look = entity.getLookVec();

        // 烈焰扑跃冲刺
        entity.motionX += look.x * 1.35D;
        entity.motionY = Math.max(entity.motionY, 0.42D);
        entity.motionZ += look.z * 1.35D;
        entity.fallDistance = 0.0F;
        entity.velocityChanged = true;

        world.playSound(null, entity.posX, entity.posY, entity.posZ,
                SoundEvents.ENTITY_WOLF_GROWL, SoundCategory.HOSTILE, 1.5F, 0.8F);
        world.playSound(null, entity.posX, entity.posY, entity.posZ,
                SoundEvents.ENTITY_BLAZE_SHOOT, SoundCategory.HOSTILE, 1.4F, 1.2F);

        if (world.isRemote) return;

        // 火环护体状态
        entity.addPotionEffect(new PotionEffect(MobEffects.RESISTANCE, 80, 1, false, false));

        // 扑咬判定
        AxisAlignedBB aabb = entity.getEntityBoundingBox().grow(3.0D, 1.5D, 3.0D);
        List<EntityLivingBase> targets = world.getEntitiesWithinAABB(EntityLivingBase.class, aabb,
                e -> e != null && e.isEntityAlive() && aaaa0ggmc.morphext.faction.FactionEnmity.isValidTargetFor(entity, e));

        DamageSource src = entity instanceof EntityPlayer ?
                DamageSource.causePlayerDamage((EntityPlayer) entity).setFireDamage() :
                DamageSource.causeMobDamage(entity).setFireDamage();

        for (EntityLivingBase target : targets) {
            Vec3d toTarget = new Vec3d(target.posX - entity.posX, 0, target.posZ - entity.posZ).normalize();
            if (look.x * toTarget.x + look.z * toTarget.z > 0.2D) {
                target.attackEntityFrom(src, 8.0F);
                target.setFire(6);
                target.knockBack(entity, 0.6F, -toTarget.x, -toTarget.z);
            }
        }

        // 周身绽放烈焰圆环粒子
        if (world instanceof WorldServer) {
            WorldServer ws = (WorldServer) world;
            for (int deg = 0; deg < 360; deg += 20) {
                double rad = Math.toRadians(deg);
                double px = entity.posX + Math.cos(rad) * 1.8D;
                double pz = entity.posZ + Math.sin(rad) * 1.8D;
                ws.spawnParticle(EnumParticleTypes.FLAME, px, entity.posY + 0.3D, pz, 2, 0.05D, 0.05D, 0.05D, 0.02D);
                ws.spawnParticle(EnumParticleTypes.LAVA, px, entity.posY + 0.2D, pz, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            }
        }
    }
}
