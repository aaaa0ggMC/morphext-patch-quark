package aaaa0ggmc.morphext.quark.action;

import aaaa0ggmc.morphext.integration.metamorph.vanilla.ServerActionScheduler;
import aaaa0ggmc.morphext.integration.metamorph.ICooldownAction;
import mchorse.metamorph.api.abilities.IAction;
import mchorse.metamorph.api.morphs.AbstractMorph;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 螃蟹狂欢舞会 (Quark Crab Rave Action):
 * 对应 EntityCrab 的唱片机 crab rave 机制（EntityAIRave/jukebox）：
 * 原地开启 6 秒狂欢，持续喷发唱片机音符粒子与舞曲音效；
 * 自身获得速度 II，周围友军随节奏狂欢获得抗性提升 I。
 * 冷却 14 秒。
 */
public class QuarkCrabRaveAction implements IAction, ICooldownAction {

    public static final int COOLDOWN = 280;

    private static final int RAVE_TICKS = 120;

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
                SoundEvents.BLOCK_NOTE_BASS, SoundCategory.RECORDS, 2.0F, 0.8F);

        if (world.isRemote) return;

        long now = world.getTotalWorldTime();
        Long next = this.nextUseAt.get(entity.getUniqueID());
        if (next != null && next > now) {
            return;
        }
        this.nextUseAt.put(entity.getUniqueID(), now + COOLDOWN);

        // 6 秒狂欢：自身速度 II
        entity.addPotionEffect(new PotionEffect(MobEffects.SPEED, RAVE_TICKS, 1, false, true));

        // 周围友军获得抗性提升 I
        double radius = 8.0D;
        AxisAlignedBB aabb = entity.getEntityBoundingBox().grow(radius, 4.0D, radius);
        List<EntityLivingBase> nearby = world.getEntitiesWithinAABB(EntityLivingBase.class, aabb,
                e -> e != null && e.isEntityAlive() && !e.equals(entity));
        for (EntityLivingBase ally : nearby) {
            if (aaaa0ggmc.morphext.faction.FactionEnmity.isAlly(entity, ally) || ally.isOnSameTeam(entity)) {
                ally.addPotionEffect(new PotionEffect(MobEffects.RESISTANCE, RAVE_TICKS, 0, false, true));
            }
        }

        // 6 秒音符粒子与舞曲循环
        if (world instanceof WorldServer) {
            final WorldServer ws = (WorldServer) world;
            ServerActionScheduler.scheduleTicker(RAVE_TICKS, remaining -> {
                if (!entity.isEntityAlive()) {
                    return;
                }
                int elapsed = RAVE_TICKS - remaining;
                ws.spawnParticle(EnumParticleTypes.NOTE,
                        entity.posX, entity.posY + 1.2D, entity.posZ, 6, 1.2D, 0.6D, 1.2D, 0.0D);
                if (elapsed % 30 == 0) {
                    ws.spawnParticle(EnumParticleTypes.VILLAGER_HAPPY,
                            entity.posX, entity.posY + 0.8D, entity.posZ, 10, 1.5D, 0.8D, 1.5D, 0.0D);
                }
                if (elapsed % 20 == 0) {
                    boolean high = (elapsed / 20) % 2 == 0;
                    ws.playSound(null, entity.posX, entity.posY, entity.posZ,
                            high ? SoundEvents.BLOCK_NOTE_HARP : SoundEvents.BLOCK_NOTE_PLING,
                            SoundCategory.RECORDS, 1.2F, high ? 1.4F : 1.0F);
                }
            }, null);
        }
    }
}
