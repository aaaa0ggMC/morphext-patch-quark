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
 * 熔岩猎犬地狱火息 (Quark Foxhound Fire Breath Action):
 * 喷薄狂暴的地狱火炎，引燃身前 5 格扇面内所有敌人并附加 8 秒火焰灼烧。
 * 冷却 12 秒。
 */
public class QuarkFoxhoundFireBreathAction implements IAction, ICooldownAction {

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
        world.playSound(null, entity.posX, entity.posY, entity.posZ,
                SoundEvents.ENTITY_BLAZE_SHOOT, SoundCategory.HOSTILE, 1.5F, 1.1F);
        world.playSound(null, entity.posX, entity.posY, entity.posZ,
                SoundEvents.ENTITY_WOLF_GROWL, SoundCategory.HOSTILE, 1.4F, 0.7F);

        if (world.isRemote) return;

        long now = world.getTotalWorldTime();
        Long next = this.nextUseAt.get(entity.getUniqueID());
        if (next != null && next > now) {
            return;
        }
        this.nextUseAt.put(entity.getUniqueID(), now + COOLDOWN);

        Vec3d look = entity.getLookVec();
        double range = 5.0D;
        AxisAlignedBB aabb = entity.getEntityBoundingBox().grow(range, 2.0D, range);
        List<EntityLivingBase> targets = world.getEntitiesWithinAABB(EntityLivingBase.class, aabb,
                e -> e != null && e.isEntityAlive() && aaaa0ggmc.morphext.faction.FactionEnmity.isValidTargetFor(entity, e));

        DamageSource src = entity instanceof EntityPlayer ?
                DamageSource.causePlayerDamage((EntityPlayer) entity).setFireDamage() :
                DamageSource.causeMobDamage(entity).setFireDamage();

        for (EntityLivingBase target : targets) {
            Vec3d toTarget = new Vec3d(target.posX - entity.posX, 0, target.posZ - entity.posZ).normalize();
            if (look.x * toTarget.x + look.z * toTarget.z > 0.35D) {
                target.attackEntityFrom(src, 7.0F);
                target.setFire(8);
            }
        }

        if (world instanceof WorldServer) {
            WorldServer ws = (WorldServer) world;
            for (int i = 1; i <= 5; i++) {
                ws.spawnParticle(EnumParticleTypes.FLAME,
                        entity.posX + look.x * (i * 0.9D),
                        entity.posY + entity.getEyeHeight() * 0.7D + look.y * (i * 0.9D),
                        entity.posZ + look.z * (i * 0.9D),
                        10, 0.25D, 0.25D, 0.25D, 0.03D);
                ws.spawnParticle(EnumParticleTypes.SMOKE_LARGE,
                        entity.posX + look.x * (i * 0.9D),
                        entity.posY + entity.getEyeHeight() * 0.7D + look.y * (i * 0.9D),
                        entity.posZ + look.z * (i * 0.9D),
                        3, 0.15D, 0.15D, 0.15D, 0.01D);
            }
        }
    }
}
