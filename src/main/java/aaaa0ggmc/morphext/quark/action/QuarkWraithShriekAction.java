package aaaa0ggmc.morphext.quark.action;

import aaaa0ggmc.morphext.integration.metamorph.ICooldownAction;
import mchorse.metamorph.api.abilities.IAction;
import mchorse.metamorph.api.morphs.AbstractMorph;
import net.minecraft.entity.EntityLivingBase;
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
 * 怨灵灵魂尖叫 (Quark Wraith Shriek Action):
 * 张开虚空怨念之口，爆发灵魂撕裂尖啸，周围 12 格敌人受到 6 秒虚弱 II 与迟缓 II！
 * 冷却 12 秒。
 */
public class QuarkWraithShriekAction implements IAction, ICooldownAction {

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
                SoundEvents.ENTITY_GHAST_SCREAM, SoundCategory.HOSTILE, 1.8F, 0.6F);

        if (world.isRemote) return;

        long now = world.getTotalWorldTime();
        Long next = this.nextUseAt.get(entity.getUniqueID());
        if (next != null && next > now) {
            return;
        }
        this.nextUseAt.put(entity.getUniqueID(), now + COOLDOWN);

        double radius = 12.0D;
        AxisAlignedBB aabb = entity.getEntityBoundingBox().grow(radius, 4.0D, radius);
        List<EntityLivingBase> targets = world.getEntitiesWithinAABB(EntityLivingBase.class, aabb,
                e -> e != null && e.isEntityAlive() && !e.equals(entity) && !e.isEntityUndead());

        for (EntityLivingBase target : targets) {
            target.addPotionEffect(new PotionEffect(MobEffects.WEAKNESS, 120, 1));
            target.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 120, 1));
        }

        if (world instanceof WorldServer) {
            ((WorldServer) world).spawnParticle(EnumParticleTypes.SMOKE_LARGE,
                    entity.posX, entity.posY + entity.getEyeHeight(), entity.posZ,
                    30, 0.8D, 0.5D, 0.8D, 0.05D);
        }
    }
}
