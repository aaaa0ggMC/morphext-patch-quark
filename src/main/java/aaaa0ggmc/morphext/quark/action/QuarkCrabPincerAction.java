package aaaa0ggmc.morphext.quark.action;

import aaaa0ggmc.morphext.integration.metamorph.ICooldownAction;
import mchorse.metamorph.api.abilities.IAction;
import mchorse.metamorph.api.morphs.AbstractMorph;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.passive.EntitySheep;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
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
 * 巨蟹利钳剪击 (Quark Crab Pincer Snip Action):
 * 巨大铁钳闭合重剪，身前 3 格造成 9 点破甲物理重击并破盾！若对准羊类使用，直接如同剪刀剪取羊毛。
 * 冷却 12 秒。
 */
public class QuarkCrabPincerAction implements IAction, ICooldownAction {

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
        entity.swingArm(EnumHand.MAIN_HAND);
        world.playSound(null, entity.posX, entity.posY, entity.posZ,
                SoundEvents.ENTITY_SHEEP_SHEAR, SoundCategory.PLAYERS, 1.5F, 0.8F);
        world.playSound(null, entity.posX, entity.posY, entity.posZ,
                SoundEvents.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.PLAYERS, 1.2F, 0.9F);

        if (world.isRemote) return;

        long now = world.getTotalWorldTime();
        Long next = this.nextUseAt.get(entity.getUniqueID());
        if (next != null && next > now) {
            return;
        }
        this.nextUseAt.put(entity.getUniqueID(), now + COOLDOWN);

        Vec3d look = entity.getLookVec();
        AxisAlignedBB aabb = entity.getEntityBoundingBox().grow(2.5D, 1.0D, 2.5D);
        List<EntityLivingBase> targets = world.getEntitiesWithinAABB(EntityLivingBase.class, aabb,
                e -> e != null && e.isEntityAlive() && aaaa0ggmc.morphext.faction.FactionEnmity.isValidTargetFor(entity, e));

        DamageSource src = entity instanceof EntityPlayer ?
                DamageSource.causePlayerDamage((EntityPlayer) entity) : DamageSource.causeMobDamage(entity);

        for (EntityLivingBase target : targets) {
            Vec3d toTarget = new Vec3d(target.posX - entity.posX, 0, target.posZ - entity.posZ).normalize();
            if (look.x * toTarget.x + look.z * toTarget.z > 0.2D) {
                // 若目标是羊且未剪毛
                if (target instanceof EntitySheep) {
                    EntitySheep sheep = (EntitySheep) target;
                    if (!sheep.getSheared() && !sheep.isChild()) {
                        sheep.setSheared(true);
                        sheep.entityDropItem(new ItemStack(Blocks.WOOL, 2, sheep.getFleeceColor().getMetadata()), 1.0F);
                        world.playSound(null, sheep.posX, sheep.posY, sheep.posZ,
                                SoundEvents.ENTITY_SHEEP_SHEAR, SoundCategory.PLAYERS, 1.2F, 1.0F);
                        continue;
                    }
                }

                // 攻击并破盾
                target.attackEntityFrom(src, 9.0F);
                if (target instanceof EntityPlayer) {
                    EntityPlayer p = (EntityPlayer) target;
                    if (p.isActiveItemStackBlocking()) {
                        p.disableShield(true);
                    }
                }
            }
        }

        if (world instanceof WorldServer) {
            ((WorldServer) world).spawnParticle(EnumParticleTypes.CRIT,
                    entity.posX + look.x * 1.5D, entity.posY + 0.5D, entity.posZ + look.z * 1.5D,
                    15, 0.3D, 0.2D, 0.3D, 0.05D);
        }
    }
}
