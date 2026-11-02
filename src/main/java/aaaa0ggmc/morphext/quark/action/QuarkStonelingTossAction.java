package aaaa0ggmc.morphext.quark.action;

import aaaa0ggmc.morphext.integration.metamorph.ICooldownAction;
import mchorse.metamorph.api.abilities.IAction;
import mchorse.metamorph.api.morphs.AbstractMorph;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntitySnowball;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 岩石灵矿石碎块投掷 (Quark Stoneling Toss Action):
 * 朝准星视线方向投掷一枚矿石碎屑，造成 5 点伤害与击退。
 * 冷却 12 秒。
 */
public class QuarkStonelingTossAction implements IAction, ICooldownAction {

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
                SoundEvents.ENTITY_SNOWBALL_THROW, SoundCategory.PLAYERS, 1.0F, 0.7F);

        if (world.isRemote) return;

        long now = world.getTotalWorldTime();
        Long next = this.nextUseAt.get(entity.getUniqueID());
        if (next != null && next > now) {
            return;
        }
        this.nextUseAt.put(entity.getUniqueID(), now + COOLDOWN);

        Vec3d look = entity.getLookVec();
        double spawnX = entity.posX + look.x * 1.5D;
        double spawnY = entity.posY + entity.getEyeHeight() * 0.75D + look.y * 1.5D;
        double spawnZ = entity.posZ + look.z * 1.5D;

        EntitySnowball stone = new EntitySnowball(world, spawnX, spawnY, spawnZ) {
            @Override
            protected void onImpact(RayTraceResult result) {
                if (result.entityHit != null && !result.entityHit.equals(entity)) {
                    DamageSource src = entity instanceof EntityPlayer ?
                            DamageSource.causePlayerDamage((EntityPlayer) entity) : DamageSource.causeMobDamage(entity);
                    result.entityHit.attackEntityFrom(src, 5.0F);
                }
                if (!this.world.isRemote && this.world instanceof WorldServer) {
                    ((WorldServer) this.world).spawnParticle(EnumParticleTypes.BLOCK_CRACK,
                            this.posX, this.posY, this.posZ, 15, 0.2D, 0.2D, 0.2D, 0.05D, 1);
                }
                this.setDead();
            }
        };

        stone.shoot(look.x, look.y + 0.05D, look.z, 1.4F, 1.0F);
        world.spawnEntity(stone);
    }
}
