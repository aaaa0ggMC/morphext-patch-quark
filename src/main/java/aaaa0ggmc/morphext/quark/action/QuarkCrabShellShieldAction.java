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
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 巨蟹甲壳格挡护体 (Quark Crab Shell Shield Action):
 * 缩入厚重坚韧的几丁质甲壳进行绝对防御，获得 5 秒抗性提升 III 与吸收伤害护盾，
 * 震退身前近战敌人并大幅削减受到的伤害。
 * 冷却 20 秒。
 */
public class QuarkCrabShellShieldAction implements IAction, ICooldownAction {

    public static final int COOLDOWN = 400;

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
                SoundEvents.ITEM_SHIELD_BLOCK, SoundCategory.PLAYERS, 1.5F, 0.9F);
        world.playSound(null, entity.posX, entity.posY, entity.posZ,
                SoundEvents.BLOCK_ANVIL_LAND, SoundCategory.PLAYERS, 0.6F, 1.8F);

        if (world.isRemote) return;

        long now = world.getTotalWorldTime();
        Long next = this.nextUseAt.get(entity.getUniqueID());
        if (next != null && next > now) {
            return;
        }
        this.nextUseAt.put(entity.getUniqueID(), now + COOLDOWN);

        // 赋予 5 秒抗性提升 III (-60% 伤害) 与 吸收护盾 II
        entity.addPotionEffect(new PotionEffect(MobEffects.RESISTANCE, 100, 2, false, false));
        entity.addPotionEffect(new PotionEffect(MobEffects.ABSORPTION, 100, 1, false, false));

        // 身前反冲震退近战敌人
        Vec3d look = entity.getLookVec();
        AxisAlignedBB aabb = entity.getEntityBoundingBox().grow(2.5D, 1.0D, 2.5D);
        List<EntityLivingBase> nearby = world.getEntitiesWithinAABB(EntityLivingBase.class, aabb,
                e -> e != null && e.isEntityAlive() && !e.equals(entity));

        for (EntityLivingBase target : nearby) {
            Vec3d toTarget = new Vec3d(target.posX - entity.posX, 0, target.posZ - entity.posZ).normalize();
            if (look.x * toTarget.x + look.z * toTarget.z > 0.1D) {
                target.knockBack(entity, 0.8F, -toTarget.x, -toTarget.z);
            }
        }

        if (world instanceof WorldServer) {
            WorldServer ws = (WorldServer) world;
            ws.spawnParticle(EnumParticleTypes.CRIT_MAGIC,
                    entity.posX, entity.posY + 0.3D, entity.posZ,
                    20, 0.5D, 0.2D, 0.5D, 0.05D);
            ws.spawnParticle(EnumParticleTypes.BLOCK_DUST,
                    entity.posX, entity.posY + 0.1D, entity.posZ,
                    15, 0.4D, 0.1D, 0.4D, 0.02D, 1);
        }
    }
}
