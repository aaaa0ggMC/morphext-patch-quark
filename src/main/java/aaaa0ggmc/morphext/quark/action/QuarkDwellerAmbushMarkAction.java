package aaaa0ggmc.morphext.quark.action;

import aaaa0ggmc.morphext.integration.metamorph.ICooldownAction;
import mchorse.metamorph.api.abilities.IAction;
import mchorse.metamorph.api.morphs.AbstractMorph;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.IMob;
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
 * 穴居僵尸伏击印记 (Quark Dweller Ambush Mark Action):
 * 发出潜伏者的低吼，标记周围 10 格内敌人（发光 + 迟缓 I）；
 * 夜间或低亮度环境下自身隐入黑暗额外获得 4 秒隐身（并入潜伏状态机：受伤/出手破隐并武装伏击），与潜伏伏击形成连招。
 * 冷却 12 秒。
 */
public class QuarkDwellerAmbushMarkAction implements IAction, ICooldownAction {

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
        if (entity == null || entity.world == null) {
            return;
        }
        World world = entity.world;
        world.playSound(null, entity.posX, entity.posY, entity.posZ,
                SoundEvents.ENTITY_ENDERMEN_STARE, SoundCategory.HOSTILE, 1.2F, 0.7F);
        world.playSound(null, entity.posX, entity.posY, entity.posZ,
                SoundEvents.ENTITY_WITHER_HURT, SoundCategory.HOSTILE, 0.6F, 1.4F);

        if (world.isRemote) return;

        long now = world.getTotalWorldTime();
        Long next = this.nextUseAt.get(entity.getUniqueID());
        if (next != null && next > now) {
            return;
        }
        this.nextUseAt.put(entity.getUniqueID(), now + COOLDOWN);

        boolean darkness = !world.isDaytime() || world.getLightFromNeighbors(entity.getPosition()) <= 7;

        double radius = 10.0D;
        AxisAlignedBB aabb = entity.getEntityBoundingBox().grow(radius, 4.0D, radius);
        List<EntityLivingBase> targets = world.getEntitiesWithinAABB(EntityLivingBase.class, aabb,
                e -> e != null && e.isEntityAlive() && !e.equals(entity) && e instanceof IMob);

        for (EntityLivingBase target : targets) {
            target.addPotionEffect(new PotionEffect(MobEffects.GLOWING, 100, 0, false, true));
            target.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 100, 0, false, true));
        }

        if (darkness) {
            // 并入潜伏状态机：黑暗隐身同样受伤/出手破隐并武装伏击（与 lurk 连招语义一致）
            QuarkDwellerLurkAction.armLurk(entity, world.getTotalWorldTime(), 80);
        }

        if (world instanceof WorldServer) {
            WorldServer ws = (WorldServer) world;
            ws.spawnParticle(EnumParticleTypes.SMOKE_NORMAL,
                    entity.posX, entity.posY + 1.0D, entity.posZ, 30, 3.0D, 1.0D, 3.0D, 0.02D);
            if (darkness) {
                ws.spawnParticle(EnumParticleTypes.SMOKE_LARGE,
                        entity.posX, entity.posY + 0.3D, entity.posZ, 15, 0.5D, 0.3D, 0.5D, 0.05D);
            }
        }
    }
}
