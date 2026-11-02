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
 * 青蛙蓄力蛙跃 (Quark Frog Leap Action):
 * 蓄力起跳爆发式前跃 4 格高跳，伴随欢快蛙鸣，空中展开脚蹼免坠落伤害。
 * 冷却 6 秒。
 */
public class QuarkFrogLeapAction implements IAction, ICooldownAction {

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

        entity.motionX += look.x * 1.1D;
        entity.motionY = Math.max(entity.motionY, 0.85D);
        entity.motionZ += look.z * 1.1D;
        entity.fallDistance = 0.0F;
        entity.velocityChanged = true;

        world.playSound(null, entity.posX, entity.posY, entity.posZ,
                SoundEvents.ENTITY_SLIME_JUMP, SoundCategory.NEUTRAL, 1.3F, 1.4F);

        if (world.isRemote) return;

        if (world instanceof WorldServer) {
            ((WorldServer) world).spawnParticle(EnumParticleTypes.WATER_SPLASH,
                    entity.posX, entity.posY, entity.posZ, 20, 0.5D, 0.1D, 0.5D, 0.1D);
        }
    }
}
