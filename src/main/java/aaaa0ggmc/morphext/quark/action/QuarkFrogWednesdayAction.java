package aaaa0ggmc.morphext.quark.action;

import aaaa0ggmc.morphext.integration.metamorph.vanilla.ServerActionScheduler;
import aaaa0ggmc.morphext.integration.metamorph.ICooldownAction;
import mchorse.metamorph.api.abilities.IAction;
import mchorse.metamorph.api.morphs.AbstractMorph;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.IMob;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import vazkii.quark.base.sounds.QuarkSounds;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 周三青蛙鸣 (Quark Frog Wednesday Action):
 * 播放 Quark 招牌周三蛙鸣（ENTITY_FROG_WEDNESDAY，源自 EntityFrog 周三点击彩蛋），
 * 瞬时召唤 1-2 只青蛙幻影向外跳跃散开（粒子假身，无需真实实体），
 * 短暂吸引周围敌怪仇恨；周三当天幻影更多、嘲讽范围更大。
 * 冷却 8 秒。
 */
public class QuarkFrogWednesdayAction implements IAction, ICooldownAction {

    public static final int COOLDOWN = 160;

    private static final int HOP_TICKS = 36;

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
                QuarkSounds.ENTITY_FROG_WEDNESDAY, SoundCategory.NEUTRAL, 1.0F, 1.0F);
        world.playSound(null, entity.posX, entity.posY, entity.posZ,
                SoundEvents.ENTITY_SLIME_JUMP, SoundCategory.NEUTRAL, 1.2F, 1.5F);

        if (world.isRemote) return;

        long now = world.getTotalWorldTime();
        Long next = this.nextUseAt.get(entity.getUniqueID());
        if (next != null && next > now) {
            return;
        }
        this.nextUseAt.put(entity.getUniqueID(), now + COOLDOWN);

        boolean wednesday = world.getCurrentDate().get(Calendar.DAY_OF_WEEK) == Calendar.WEDNESDAY;

        // 1-2 只青蛙幻影（周三 +1），各自沿随机方向跳跃散开
        int phantoms = 1 + world.rand.nextInt(2) + (wednesday ? 1 : 0);
        List<double[]> hops = new ArrayList<>();
        for (int i = 0; i < phantoms; i++) {
            hops.add(new double[]{world.rand.nextDouble() * Math.PI * 2.0D});
        }

        // 2. 短暂吸引周围敌怪仇恨（"周三蛙骚动"）
        double aggroRadius = wednesday ? 12.0D : 10.0D;
        AxisAlignedBB aabb = entity.getEntityBoundingBox().grow(aggroRadius, 4.0D, aggroRadius);
        List<EntityLivingBase> hostiles = world.getEntitiesWithinAABB(EntityLivingBase.class, aabb,
                e -> e != null && e.isEntityAlive() && e instanceof EntityLiving && aaaa0ggmc.morphext.faction.FactionEnmity.isValidTargetFor(entity, e));
        for (EntityLivingBase hostile : hostiles) {
            ((EntityLiving) hostile).setAttackTarget(entity);
        }

        // 3. 幻影假身：三段跳跃粒子轨迹向外扩散
        if (world instanceof WorldServer) {
            WorldServer ws = (WorldServer) world;
            final double baseX = entity.posX;
            final double baseY = entity.posY;
            final double baseZ = entity.posZ;
            ServerActionScheduler.scheduleTicker(HOP_TICKS, remaining -> {
                int step;
                if (remaining == HOP_TICKS - 1) {
                    step = 0;
                } else if (remaining == HOP_TICKS - 13) {
                    step = 1;
                } else if (remaining == HOP_TICKS - 25) {
                    step = 2;
                } else {
                    return;
                }
                double dist = 1.5D + step * 1.5D;
                double arc = step == 0 ? 0.2D : (step == 1 ? 0.8D : 0.3D);
                for (double[] hop : hops) {
                    double angle = hop[0];
                    double x = baseX + Math.cos(angle) * dist;
                    double z = baseZ + Math.sin(angle) * dist;
                    ws.spawnParticle(EnumParticleTypes.SLIME, x, baseY + arc, z, 8, 0.15D, 0.1D, 0.15D, 0.0D);
                    ws.spawnParticle(EnumParticleTypes.WATER_SPLASH, x, baseY + 0.1D, z, 5, 0.15D, 0.05D, 0.15D, 0.01D);
                }
                ws.playSound(null, baseX, baseY, baseZ, SoundEvents.ENTITY_SLIME_JUMP, SoundCategory.NEUTRAL, 0.8F, 1.6F);
            }, null);
        }
    }
}
