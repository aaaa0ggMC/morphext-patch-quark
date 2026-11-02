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
 * 灰烬骷髅灰烬烟幕 (Quark Ashen Ash Veil Action):
 * 演绎自致盲箭人设的控场技：爆发 4 格浓厚灰烬黑烟，
 * 致盲烟中敌人 3 秒并施加 5 秒发光标记（透过烟幕锁定目标）；
 * 烟幕对自身完全豁免。
 * 冷却 16 秒。
 */
public class QuarkAshenAshVeilAction implements IAction, ICooldownAction {

    public static final int COOLDOWN = 320;

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
                SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.PLAYERS, 1.5F, 0.6F);
        world.playSound(null, entity.posX, entity.posY, entity.posZ,
                SoundEvents.BLOCK_GRAVEL_BREAK, SoundCategory.PLAYERS, 1.2F, 0.8F);

        if (world.isRemote) return;

        long now = world.getTotalWorldTime();
        Long next = this.nextUseAt.get(entity.getUniqueID());
        if (next != null && next > now) {
            return;
        }
        this.nextUseAt.put(entity.getUniqueID(), now + COOLDOWN);

        double radius = 4.0D;
        AxisAlignedBB aabb = entity.getEntityBoundingBox().grow(radius, 2.5D, radius);
        List<EntityLivingBase> targets = world.getEntitiesWithinAABB(EntityLivingBase.class, aabb,
                e -> e != null && e.isEntityAlive() && !e.equals(entity)
                        && (e instanceof IMob || (e instanceof EntityPlayer && !e.isOnSameTeam(entity))));

        for (EntityLivingBase target : targets) {
            target.addPotionEffect(new PotionEffect(MobEffects.BLINDNESS, 60, 0, false, true));
            target.addPotionEffect(new PotionEffect(MobEffects.GLOWING, 100, 0, false, true));
        }

        if (world instanceof WorldServer) {
            WorldServer ws = (WorldServer) world;
            ws.spawnParticle(EnumParticleTypes.SMOKE_LARGE,
                    entity.posX, entity.posY + 0.8D, entity.posZ, 60, 3.5D, 1.6D, 3.5D, 0.02D);
            ws.spawnParticle(EnumParticleTypes.CLOUD,
                    entity.posX, entity.posY + 0.5D, entity.posZ, 30, 3.0D, 1.2D, 3.0D, 0.01D);
        }
    }
}
