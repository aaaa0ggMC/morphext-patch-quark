package aaaa0ggmc.morphext.quark.action;

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
 * 熔岩猎犬熔热爱意 (Quark Foxhound Love Burn Action):
 * 还原 EntityAIWantLove 求摸摸时点燃主人的调皮机制：
 * 自身热情迸发（周身烈焰与爱心粒子），获得 6 秒力量 II 与速度 II；
 * 周围 6 格内队友共享炉火温暖，获得 8 秒火焰抗性。
 * 冷却 18 秒。
 */
public class QuarkFoxhoundLoveBurnAction implements IAction, ICooldownAction {

    public static final int COOLDOWN = 360;

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
                SoundEvents.ENTITY_WOLF_PANT, SoundCategory.PLAYERS, 1.2F, 1.3F);
        world.playSound(null, entity.posX, entity.posY, entity.posZ,
                SoundEvents.BLOCK_FIRE_AMBIENT, SoundCategory.PLAYERS, 1.0F, 0.8F);

        if (world.isRemote) return;

        long now = world.getTotalWorldTime();
        Long next = this.nextUseAt.get(entity.getUniqueID());
        if (next != null && next > now) {
            return;
        }
        this.nextUseAt.put(entity.getUniqueID(), now + COOLDOWN);

        // 自身：燃烧的爱意（力量 II + 速度 II）
        entity.addPotionEffect(new PotionEffect(MobEffects.STRENGTH, 120, 1, false, true));
        entity.addPotionEffect(new PotionEffect(MobEffects.SPEED, 120, 1, false, true));

        // 队友：炉火温暖（火焰抗性）
        AxisAlignedBB aabb = entity.getEntityBoundingBox().grow(6.0D, 3.0D, 6.0D);
        List<EntityLivingBase> nearby = world.getEntitiesWithinAABB(EntityLivingBase.class, aabb,
                e -> e != null && e.isEntityAlive() && !e.equals(entity));
        for (EntityLivingBase target : nearby) {
            if (aaaa0ggmc.morphext.faction.FactionEnmity.isValidTargetFor(entity, target)) {
                continue;
            }
            if (aaaa0ggmc.morphext.faction.FactionEnmity.isAlly(entity, target) || target.isOnSameTeam(entity)) {
                target.addPotionEffect(new PotionEffect(MobEffects.FIRE_RESISTANCE, 160, 0, false, true));
            }
        }

        if (world instanceof WorldServer) {
            WorldServer ws = (WorldServer) world;
            ws.spawnParticle(EnumParticleTypes.FLAME,
                    entity.posX, entity.posY + 0.6D, entity.posZ, 35, 0.5D, 0.5D, 0.5D, 0.02D);
            ws.spawnParticle(EnumParticleTypes.HEART,
                    entity.posX, entity.posY + entity.height + 0.3D, entity.posZ, 6, 0.4D, 0.2D, 0.4D, 0.0D);
        }
    }
}
