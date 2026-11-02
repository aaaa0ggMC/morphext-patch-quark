package aaaa0ggmc.morphext.quark.action;

import aaaa0ggmc.morphext.integration.metamorph.ICooldownAction;
import mchorse.metamorph.api.abilities.IAction;
import mchorse.metamorph.api.morphs.AbstractMorph;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.IMob;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.potion.PotionEffect;
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
 * 海盗船长号令 (Quark Pirate Captain Command Action):
 * 还原 EntityPirate 船长变体的领袖气质：吹响船长号令，
 * 周围亡灵船员与队友获得 8 秒力量 I 与速度 I，自身获得 5 秒抗性提升；
 * 同时以威压震慑身前非亡灵敌怪，施加 4 秒虚弱并击退。
 * 冷却 14 秒。
 */
public class QuarkPirateCaptainAction implements IAction, ICooldownAction {

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
        if (entity == null || entity.world == null) {
            return;
        }
        World world = entity.world;
        entity.swingArm(net.minecraft.util.EnumHand.MAIN_HAND);
        world.playSound(null, entity.posX, entity.posY, entity.posZ,
                SoundEvents.ENTITY_PARROT_AMBIENT, SoundCategory.PLAYERS, 1.2F, 1.3F);
        world.playSound(null, entity.posX, entity.posY, entity.posZ,
                SoundEvents.BLOCK_NOTE_BASS, SoundCategory.PLAYERS, 2.0F, 0.6F);

        if (world.isRemote) return;

        long now = world.getTotalWorldTime();
        Long next = this.nextUseAt.get(entity.getUniqueID());
        if (next != null && next > now) {
            return;
        }
        this.nextUseAt.put(entity.getUniqueID(), now + COOLDOWN);

        // 船长自身：5 秒抗性提升
        entity.addPotionEffect(new PotionEffect(MobEffects.RESISTANCE, 100, 0, false, true));

        double radius = 12.0D;
        AxisAlignedBB aabb = entity.getEntityBoundingBox().grow(radius, 6.0D, radius);
        List<EntityLivingBase> nearby = world.getEntitiesWithinAABB(EntityLivingBase.class, aabb,
                e -> e != null && e.isEntityAlive() && !e.equals(entity));

        Vec3d look = entity.getLookVec();
        for (EntityLivingBase target : nearby) {
            if (target instanceof IMob && !target.isEntityUndead()) {
                // 威压震慑：非亡灵敌怪虚弱 + 轻微击退
                target.addPotionEffect(new PotionEffect(MobEffects.WEAKNESS, 80, 0, false, true));
                Vec3d toTarget = new Vec3d(target.posX - entity.posX, 0, target.posZ - entity.posZ).normalize();
                if (look.x * toTarget.x + look.z * toTarget.z > 0.1D) {
                    target.knockBack(entity, 0.5F, -toTarget.x, -toTarget.z);
                }
            } else if (target.isOnSameTeam(entity) || isCrew(target, entity)) {
                // 船员集结：亡灵船员与队友获得 8 秒力量 I 与速度 I
                target.addPotionEffect(new PotionEffect(MobEffects.STRENGTH, 160, 0, false, true));
                target.addPotionEffect(new PotionEffect(MobEffects.SPEED, 160, 0, false, true));
            }
        }

        if (world instanceof WorldServer) {
            WorldServer ws = (WorldServer) world;
            ws.spawnParticle(EnumParticleTypes.NOTE,
                    entity.posX, entity.posY + 1.5D, entity.posZ, 16, 1.0D, 0.5D, 1.0D, 0.0D);
            ws.spawnParticle(EnumParticleTypes.VILLAGER_HAPPY,
                    entity.posX, entity.posY + 1.0D, entity.posZ, 15, 1.2D, 0.6D, 1.2D, 0.0D);
        }
    }

    /**
     * 船员判定：亡灵生物（骷髅海盗船员同族）且当前没有把船长当作攻击目标。
     */
    private static boolean isCrew(EntityLivingBase target, EntityLivingBase captain) {
        return target.isEntityUndead()
                && (!(target instanceof EntityLiving) || ((EntityLiving) target).getAttackTarget() != captain);
    }
}
