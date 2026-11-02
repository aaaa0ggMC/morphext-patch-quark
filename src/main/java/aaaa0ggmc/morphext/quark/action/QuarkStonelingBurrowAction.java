package aaaa0ggmc.morphext.quark.action;

import aaaa0ggmc.morphext.integration.metamorph.ICooldownAction;
import mchorse.metamorph.api.abilities.IAction;
import mchorse.metamorph.api.morphs.AbstractMorph;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 岩石灵遁地石肤 (Quark Stoneling Burrow Action):
 * 对应 EntityStoneling 的遁地隐匿人设（惊遁噗灭消失 + 地底矿石人）：
 * 缩身遁入土石之中 5 秒，获得抗性提升 II 与缓慢的生命修复，代价是扎根原地（迟缓 I）。
 * 冷却 20 秒。
 */
public class QuarkStonelingBurrowAction implements IAction, ICooldownAction {

    public static final int COOLDOWN = 400;
    public static final int BURROW_TICKS = 100;

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
                SoundEvents.BLOCK_STONE_BREAK, SoundCategory.PLAYERS, 1.2F, 0.6F);
        world.playSound(null, entity.posX, entity.posY, entity.posZ,
                SoundEvents.BLOCK_GRAVEL_BREAK, SoundCategory.PLAYERS, 1.0F, 0.8F);

        if (world.isRemote) return;

        long now = world.getTotalWorldTime();
        Long next = this.nextUseAt.get(entity.getUniqueID());
        if (next != null && next > now) {
            return;
        }
        this.nextUseAt.put(entity.getUniqueID(), now + COOLDOWN);

        // 遁入土石：石肤抗性 + 大地缓慢修复，扎根原地
        entity.addPotionEffect(new PotionEffect(MobEffects.RESISTANCE, BURROW_TICKS, 1, false, true));
        entity.addPotionEffect(new PotionEffect(MobEffects.REGENERATION, BURROW_TICKS, 0, false, true));
        entity.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 60, 0, false, true));

        if (world instanceof WorldServer) {
            WorldServer ws = (WorldServer) world;
            ws.spawnParticle(EnumParticleTypes.BLOCK_CRACK,
                    entity.posX, entity.posY + 0.2D, entity.posZ, 30, 0.5D, 0.2D, 0.5D, 0.05D, 1);
            ws.spawnParticle(EnumParticleTypes.SMOKE_LARGE,
                    entity.posX, entity.posY + 0.8D, entity.posZ, 15, 0.4D, 0.3D, 0.4D, 0.02D);
        }
    }
}
