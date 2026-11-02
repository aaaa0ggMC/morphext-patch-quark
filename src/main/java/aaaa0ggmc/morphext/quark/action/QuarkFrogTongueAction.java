package aaaa0ggmc.morphext.quark.action;

import aaaa0ggmc.morphext.integration.metamorph.ICooldownAction;
import mchorse.metamorph.api.abilities.IAction;
import mchorse.metamorph.api.morphs.AbstractMorph;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.potion.PotionEffect;
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
 * 青蛙长舌粘卷 (Quark Frog Tongue Action):
 * 伸出闪电般的黏滑长舌，将正前方 8 格内首个敌怪或掉落物瞬间拉扯到自身脚下，并施加 3 秒黏液迟缓 III！
 * 冷却 10 秒。
 */
public class QuarkFrogTongueAction implements IAction, ICooldownAction {

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
        World world = entity.world;
        world.playSound(null, entity.posX, entity.posY, entity.posZ,
                SoundEvents.ENTITY_SLIME_SQUISH, SoundCategory.NEUTRAL, 1.5F, 1.2F);

        if (world.isRemote) return;

        long now = world.getTotalWorldTime();
        Long next = this.nextUseAt.get(entity.getUniqueID());
        if (next != null && next > now) {
            return;
        }
        this.nextUseAt.put(entity.getUniqueID(), now + COOLDOWN);

        Vec3d look = entity.getLookVec();
        double range = 8.0D;
        AxisAlignedBB aabb = entity.getEntityBoundingBox().grow(range, 2.0D, range);

        // 优先拉取生物
        List<EntityLivingBase> targets = world.getEntitiesWithinAABB(EntityLivingBase.class, aabb,
                e -> e != null && e.isEntityAlive() && !e.equals(entity));

        EntityLivingBase pullTarget = null;
        double minAngle = 0.5D;

        for (EntityLivingBase t : targets) {
            Vec3d toTarget = new Vec3d(t.posX - entity.posX, 0, t.posZ - entity.posZ).normalize();
            double dot = look.x * toTarget.x + look.z * toTarget.z;
            if (dot > minAngle) {
                pullTarget = t;
                break;
            }
        }

        if (pullTarget != null) {
            Vec3d pullVector = new Vec3d(entity.posX - pullTarget.posX, 0.3D, entity.posZ - pullTarget.posZ).normalize();
            pullTarget.motionX = pullVector.x * 1.5D;
            pullTarget.motionY = 0.35D;
            pullTarget.motionZ = pullVector.z * 1.5D;
            pullTarget.velocityChanged = true;
            pullTarget.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 60, 2));
        } else {
            // 没有生物则尝试拉取物品掉落物
            List<EntityItem> items = world.getEntitiesWithinAABB(EntityItem.class, aabb,
                    i -> i != null && !i.isDead);
            for (EntityItem item : items) {
                item.setPositionAndUpdate(entity.posX, entity.posY + 0.2D, entity.posZ);
            }
        }

        if (world instanceof WorldServer) {
            WorldServer ws = (WorldServer) world;
            for (int i = 1; i <= 6; i++) {
                ws.spawnParticle(EnumParticleTypes.SLIME,
                        entity.posX + look.x * (i * 1.2D),
                        entity.posY + entity.getEyeHeight() + look.y * (i * 1.2D),
                        entity.posZ + look.z * (i * 1.2D),
                        3, 0.1D, 0.1D, 0.1D, 0.0D);
            }
        }
    }
}
