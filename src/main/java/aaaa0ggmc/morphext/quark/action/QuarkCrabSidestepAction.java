package aaaa0ggmc.morphext.quark.action;

import aaaa0ggmc.morphext.integration.metamorph.ICooldownAction;
import mchorse.metamorph.api.abilities.IAction;
import mchorse.metamorph.api.morphs.AbstractMorph;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 巨蟹侧身横行突刺 (Quark Crab Sidestep Action):
 * 还原螃蟹标志性横行步态，侧向爆发式滑行 5 格，闪避正面攻击与弹射物。
 * 冷却 6 秒。
 */
public class QuarkCrabSidestepAction implements IAction, ICooldownAction {

    public static final int COOLDOWN = 120;

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

        // 计算侧向向量 (垂直于视线)
        Vec3d side = new Vec3d(-look.z, 0, look.x).normalize();
        if (entity.isSneaking()) {
            side = side.scale(-1.0D); // 潜行向反方向侧移
        }

        entity.motionX += side.x * 1.25D;
        entity.motionY = 0.15D;
        entity.motionZ += side.z * 1.25D;
        entity.velocityChanged = true;

        world.playSound(null, entity.posX, entity.posY, entity.posZ,
                SoundEvents.ENTITY_SPIDER_STEP, SoundCategory.NEUTRAL, 1.2F, 1.4F);

        if (world.isRemote) return;

        if (world instanceof WorldServer) {
            ((WorldServer) world).spawnParticle(EnumParticleTypes.CLOUD,
                    entity.posX, entity.posY + 0.1D, entity.posZ, 12, 0.3D, 0.1D, 0.3D, 0.02D);
        }
    }
}
