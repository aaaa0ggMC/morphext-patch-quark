package aaaa0ggmc.morphext.quark.action;

import aaaa0ggmc.morphext.integration.metamorph.ICooldownAction;
import mchorse.metamorph.api.abilities.IAction;
import mchorse.metamorph.api.morphs.AbstractMorph;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.EnumHand;
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
 * 岩石灵矿石搬运 (Quark Stoneling Ore Magnet Action):
 * 对应 EntityStoneling 双手搬运物品（CARRYING_ITEM）与被钻石诱惑的矿石人设：
 * 引力汇聚 12 格内所有掉落物飞向自身，同时获得 10 秒急迫 I（矿工之力）。
 * 冷却 10 秒，纯辅助无伤害。
 */
public class QuarkStonelingOreMagnetAction implements IAction, ICooldownAction {

    public static final int COOLDOWN = 200;

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
        entity.swingArm(EnumHand.MAIN_HAND);
        world.playSound(null, entity.posX, entity.posY, entity.posZ,
                SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.PLAYERS, 1.2F, 0.7F);
        world.playSound(null, entity.posX, entity.posY, entity.posZ,
                SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 1.0F, 1.2F);

        if (world.isRemote) return;

        long now = world.getTotalWorldTime();
        Long next = this.nextUseAt.get(entity.getUniqueID());
        if (next != null && next > now) {
            return;
        }
        this.nextUseAt.put(entity.getUniqueID(), now + COOLDOWN);

        // 搬运引力：12 格内掉落物飞向自身
        AxisAlignedBB aabb = entity.getEntityBoundingBox().grow(12.0D, 6.0D, 12.0D);
        List<EntityItem> items = world.getEntitiesWithinAABB(EntityItem.class, aabb,
                i -> i != null && !i.isDead);
        for (EntityItem item : items) {
            double dx = entity.posX - item.posX;
            double dy = (entity.posY + 0.2D) - item.posY;
            double dz = entity.posZ - item.posZ;
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (dist > 0.5D) {
                double speed = 0.45D;
                item.motionX = dx / dist * speed;
                item.motionY = dy / dist * speed + 0.08D;
                item.motionZ = dz / dist * speed;
                item.velocityChanged = true;
            }
        }

        // 矿工之力：10 秒急迫 I
        entity.addPotionEffect(new PotionEffect(MobEffects.HASTE, 200, 0, false, true));

        if (world instanceof WorldServer) {
            WorldServer ws = (WorldServer) world;
            ws.spawnParticle(EnumParticleTypes.VILLAGER_HAPPY,
                    entity.posX, entity.posY + 1.0D, entity.posZ, 25, 1.0D, 0.6D, 1.0D, 0.0D);
        }
    }
}
