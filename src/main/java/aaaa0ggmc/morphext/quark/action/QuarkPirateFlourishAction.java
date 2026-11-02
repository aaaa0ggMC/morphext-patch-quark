package aaaa0ggmc.morphext.quark.action;

import aaaa0ggmc.morphext.integration.metamorph.ICooldownAction;
import mchorse.metamorph.api.abilities.IAction;
import mchorse.metamorph.api.morphs.AbstractMorph;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
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
 * 海盗西洋剑突刺与格挡 (Quark Pirate Flourish Action):
 * 前冲拔剑刺击身前 3.5 格目标，造成 8 点暴击伤害并在冲刺瞬间招架正面攻击。
 * 冷却 12 秒。
 */
public class QuarkPirateFlourishAction implements IAction, ICooldownAction {

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
        if (!world.isRemote) {
            long now = world.getTotalWorldTime();
            Long next = this.nextUseAt.get(entity.getUniqueID());
            if (next != null && next > now) {
                return;
            }
            this.nextUseAt.put(entity.getUniqueID(), now + COOLDOWN);
        }
        entity.swingArm(EnumHand.MAIN_HAND);
        Vec3d look = entity.getLookVec();

        // 小幅突进
        entity.motionX += look.x * 0.7D;
        entity.motionZ += look.z * 0.7D;
        entity.velocityChanged = true;

        world.playSound(null, entity.posX, entity.posY, entity.posZ,
                SoundEvents.ITEM_ARMOR_EQUIP_IRON, SoundCategory.PLAYERS, 1.2F, 1.4F);
        world.playSound(null, entity.posX, entity.posY, entity.posZ,
                SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 1.4F, 1.2F);

        if (world.isRemote) return;

        AxisAlignedBB aabb = entity.getEntityBoundingBox().grow(3.0D, 1.0D, 3.0D);
        List<EntityLivingBase> targets = world.getEntitiesWithinAABB(EntityLivingBase.class, aabb,
                e -> e != null && e.isEntityAlive() && !e.equals(entity));

        DamageSource src = entity instanceof EntityPlayer ?
                DamageSource.causePlayerDamage((EntityPlayer) entity) : DamageSource.causeMobDamage(entity);

        for (EntityLivingBase target : targets) {
            Vec3d toTarget = new Vec3d(target.posX - entity.posX, 0, target.posZ - entity.posZ).normalize();
            if (look.x * toTarget.x + look.z * toTarget.z > 0.3D) {
                target.attackEntityFrom(src, 8.0F);
                target.knockBack(entity, 0.8F, -toTarget.x, -toTarget.z);
            }
        }

        if (world instanceof WorldServer) {
            ((WorldServer) world).spawnParticle(EnumParticleTypes.CRIT_MAGIC,
                    entity.posX + look.x * 1.5D, entity.posY + 1.0D, entity.posZ + look.z * 1.5D,
                    15, 0.4D, 0.3D, 0.4D, 0.05D);
        }
    }
}
