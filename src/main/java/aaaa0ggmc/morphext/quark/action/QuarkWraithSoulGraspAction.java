package aaaa0ggmc.morphext.quark.action;

import aaaa0ggmc.morphext.integration.metamorph.ICooldownAction;
import mchorse.metamorph.api.abilities.IAction;
import mchorse.metamorph.api.morphs.AbstractMorph;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
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
 * 怨灵灵魂攫取 (Quark Wraith Soul Grasp Action):
 * 还原 EntityWraith 的灵魂沙/灵魂宝珠人设与近战迟缓：自虚空探出幽魂之手，
 * 攫取正前方 10 格内首个目标——造成 4 点伤害、3 秒迟缓 II 并将目标拽向自身。
 * 冷却 14 秒。
 */
public class QuarkWraithSoulGraspAction implements IAction, ICooldownAction {

    public static final int COOLDOWN = 280;

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
                SoundEvents.ENTITY_VEX_AMBIENT, SoundCategory.HOSTILE, 1.2F, 0.6F);
        world.playSound(null, entity.posX, entity.posY, entity.posZ,
                SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.NEUTRAL, 1.0F, 0.7F);

        if (world.isRemote) return;

        long now = world.getTotalWorldTime();
        Long next = this.nextUseAt.get(entity.getUniqueID());
        if (next != null && next > now) {
            return;
        }

        Vec3d look = entity.getLookVec();
        double range = 10.0D;
        AxisAlignedBB aabb = entity.getEntityBoundingBox().grow(range, 3.0D, range);
        List<EntityLivingBase> targets = world.getEntitiesWithinAABB(EntityLivingBase.class, aabb,
                e -> e != null && e.isEntityAlive() && !e.equals(entity));

        EntityLivingBase graspTarget = null;
        double minAngle = 0.6D;
        for (EntityLivingBase t : targets) {
            Vec3d toTarget = new Vec3d(t.posX - entity.posX, 0, t.posZ - entity.posZ).normalize();
            if (look.x * toTarget.x + look.z * toTarget.z > minAngle) {
                graspTarget = t;
                break;
            }
        }

        if (graspTarget == null) {
            // 未攫取到目标不进入冷却
            return;
        }
        this.nextUseAt.put(entity.getUniqueID(), now + COOLDOWN);

        DamageSource src = entity instanceof EntityPlayer ?
                DamageSource.causePlayerDamage((EntityPlayer) entity) :
                DamageSource.causeMobDamage(entity);
        graspTarget.attackEntityFrom(src, 4.0F);
        graspTarget.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 60, 1));

        // 灵魂拽引：把目标拖向怨灵
        Vec3d pullVector = new Vec3d(entity.posX - graspTarget.posX, 0.3D, entity.posZ - graspTarget.posZ).normalize();
        graspTarget.motionX = pullVector.x * 1.2D;
        graspTarget.motionY = 0.3D;
        graspTarget.motionZ = pullVector.z * 1.2D;
        graspTarget.velocityChanged = true;

        if (world instanceof WorldServer) {
            WorldServer ws = (WorldServer) world;
            for (int i = 1; i <= 8; i++) {
                double px = entity.posX + look.x * (i * 1.1D);
                double py = entity.posY + entity.getEyeHeight() * 0.6D + look.y * (i * 1.1D);
                double pz = entity.posZ + look.z * (i * 1.1D);
                ws.spawnParticle(EnumParticleTypes.SPELL_WITCH, px, py, pz, 2, 0.1D, 0.1D, 0.1D, 0.05D);
            }
            ws.spawnParticle(EnumParticleTypes.SMOKE_LARGE,
                    graspTarget.posX, graspTarget.posY + 1.0D, graspTarget.posZ, 12, 0.3D, 0.4D, 0.3D, 0.02D);
        }
    }
}
