package aaaa0ggmc.morphext.quark.action;

import aaaa0ggmc.morphext.integration.metamorph.ICooldownAction;
import mchorse.metamorph.api.abilities.IAction;
import mchorse.metamorph.api.morphs.AbstractMorph;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
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
 * 怨灵幽影穿梭 (Quark Wraith Phase Action):
 * 化身半透明幽魂白烟向前极速穿透 10 格，穿透沿途生物造成 6 点凋灵伤害并抽取 4 点生命值反哺自身！
 * 冷却 15 秒。
 */
public class QuarkWraithPhaseAction implements IAction, ICooldownAction {

    public static final int COOLDOWN = 300;

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

        // 幽魂穿透位移
        entity.motionX += look.x * 1.8D;
        entity.motionY += look.y * 1.2D;
        entity.motionZ += look.z * 1.8D;
        entity.fallDistance = 0.0F;
        entity.velocityChanged = true;

        world.playSound(null, entity.posX, entity.posY, entity.posZ,
                SoundEvents.ENTITY_ENDERMEN_TELEPORT, SoundCategory.HOSTILE, 1.4F, 0.6F);

        if (world.isRemote) return;

        AxisAlignedBB aabb = entity.getEntityBoundingBox().grow(4.0D, 2.0D, 4.0D);
        List<EntityLivingBase> targets = world.getEntitiesWithinAABB(EntityLivingBase.class, aabb,
                e -> e != null && e.isEntityAlive() && aaaa0ggmc.morphext.faction.FactionEnmity.isValidTargetFor(entity, e));

        DamageSource src = entity instanceof EntityPlayer ?
                DamageSource.causePlayerDamage((EntityPlayer) entity).setDamageBypassesArmor() :
                DamageSource.causeMobDamage(entity).setDamageBypassesArmor();

        float healed = 0.0F;
        for (EntityLivingBase target : targets) {
            target.attackEntityFrom(src, 6.0F);
            healed += 4.0F;
        }

        if (healed > 0.0F) {
            entity.heal(Math.min(healed, 12.0F));
        }

        if (world instanceof WorldServer) {
            ((WorldServer) world).spawnParticle(EnumParticleTypes.CLOUD,
                    entity.posX, entity.posY + 1.0D, entity.posZ, 25, 0.6D, 0.6D, 0.6D, 0.05D);
        }
    }
}
