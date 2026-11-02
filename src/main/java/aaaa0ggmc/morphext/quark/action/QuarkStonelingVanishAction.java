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
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 岩石灵受惊隐遁 (Quark Stoneling Vanish Action):
 * 炸开石灰烟雾，自身获得 6 秒隐形与速度 II，并在原地留下微型方块碎屑迷惑敌人。
 * 冷却 10 秒。
 */
public class QuarkStonelingVanishAction implements IAction, ICooldownAction {

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
                SoundEvents.BLOCK_STONE_BREAK, SoundCategory.NEUTRAL, 1.5F, 1.2F);
        world.playSound(null, entity.posX, entity.posY, entity.posZ,
                SoundEvents.ENTITY_FIREWORK_BLAST, SoundCategory.NEUTRAL, 1.2F, 1.5F);

        if (world.isRemote) return;

        long now = world.getTotalWorldTime();
        Long next = this.nextUseAt.get(entity.getUniqueID());
        if (next != null && next > now) {
            return;
        }
        this.nextUseAt.put(entity.getUniqueID(), now + COOLDOWN);

        entity.addPotionEffect(new PotionEffect(MobEffects.INVISIBILITY, 120, 0, false, false));
        entity.addPotionEffect(new PotionEffect(MobEffects.SPEED, 120, 1, false, false));

        if (world instanceof WorldServer) {
            WorldServer ws = (WorldServer) world;
            ws.spawnParticle(EnumParticleTypes.SMOKE_LARGE, entity.posX, entity.posY + 0.3D, entity.posZ, 20, 0.4D, 0.2D, 0.4D, 0.05D);
            ws.spawnParticle(EnumParticleTypes.BLOCK_CRACK, entity.posX, entity.posY + 0.2D, entity.posZ, 30, 0.5D, 0.2D, 0.5D, 0.05D, 1);
        }
    }
}
