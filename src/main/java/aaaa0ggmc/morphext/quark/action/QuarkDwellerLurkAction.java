package aaaa0ggmc.morphext.quark.action;

import aaaa0ggmc.morphext.integration.metamorph.ICooldownAction;
import mchorse.metamorph.api.abilities.IAction;
import mchorse.metamorph.api.morphs.AbstractMorph;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 穴居僵尸潜伏伏击 (Quark Dweller Lurk Action):
 * 还原 EntityDweller 出生即永久隐身的招牌潜伏机制：进入 8 秒隐身；
 * 受伤或出手即破隐，破隐后的第一次近战（破隐后 2 秒窗口内）追加 4 点伤害与 3 秒迟缓——"受惊伏击"。
 * <p>
 * 执行侧：{@link #execute} 由 morphext 服务端动作链调用（内部仍防御性判空 isRemote）；
 * 破隐一击的伤害修正走服务端 {@link LivingHurtEvent}（本实例注册在 Forge 事件总线上）。
 * <p>
 * ambush_mark 的黑暗隐身经 {@link #armLurk} 并入同一潜伏状态机（受伤/出手破隐并武装伏击）。
 * 冷却 20 秒。
 */
public class QuarkDwellerLurkAction implements IAction, ICooldownAction {

    public static final int COOLDOWN = 400;
    public static final int LURK_TICKS = 160;
    public static final int AMBUSH_BONUS_DAMAGE = 4;
    public static final int AMBUSH_WINDOW_TICKS = 40;

    /** 潜伏状态（静态共享：ambush_mark 的黑暗隐身并入同一状态机）。 */
    private static final Map<UUID, Long> lurkUntil = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> ambushArmedUntil = new ConcurrentHashMap<>();

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

    /**
     * 并入潜伏状态机：注册隐身与破隐伏击武装窗口。
     * ambush_mark 的黑暗隐身也走本入口，保证“受伤/出手破隐并武装”语义一致。
     */
    public static void armLurk(EntityLivingBase entity, long now, int invisTicks) {
        UUID uuid = entity.getUniqueID();
        lurkUntil.put(uuid, now + invisTicks);
        ambushArmedUntil.put(uuid, now + invisTicks + AMBUSH_WINDOW_TICKS);
        entity.addPotionEffect(new PotionEffect(MobEffects.INVISIBILITY, invisTicks, 0, false, false));
    }

    @Override
    public void execute(EntityLivingBase entity, AbstractMorph morph) {
        if (entity == null || entity.world == null) {
            return;
        }
        World world = entity.world;
        entity.swingArm(EnumHand.MAIN_HAND);
        world.playSound(null, entity.posX, entity.posY, entity.posZ,
                SoundEvents.ENTITY_ZOMBIE_AMBIENT, SoundCategory.HOSTILE, 1.2F, 0.5F);
        world.playSound(null, entity.posX, entity.posY, entity.posZ,
                SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.NEUTRAL, 1.0F, 0.7F);

        if (world.isRemote) return;

        long now = world.getTotalWorldTime();
        Long next = this.nextUseAt.get(entity.getUniqueID());
        if (next != null && next > now) {
            return;
        }
        this.nextUseAt.put(entity.getUniqueID(), now + COOLDOWN);

        armLurk(entity, now, LURK_TICKS);
        cleanupStale(now);

        if (world instanceof WorldServer) {
            ((WorldServer) world).spawnParticle(EnumParticleTypes.SMOKE_LARGE,
                    entity.posX, entity.posY + 0.3D, entity.posZ, 25, 0.5D, 0.3D, 0.5D, 0.05D);
        }
    }

    /**
     * 服务端伤害事件：①潜伏者受伤 → 破隐并武装下一击；②潜伏者近战 → 破隐一击追加伤害与迟缓。
     */
    @SubscribeEvent
    public void onLivingHurt(LivingHurtEvent event) {
        EntityLivingBase hurt = event.getEntityLiving();
        if (hurt == null || hurt.world == null || hurt.world.isRemote || event.getAmount() <= 0.0F) {
            return;
        }
        long now = hurt.world.getTotalWorldTime();

        // ② 潜伏者近战出手：破隐一击（仅直接近战命中，箭矢等间接伤害不触发）
        Entity trueSource = event.getSource().getTrueSource();
        Entity immediate = event.getSource().getImmediateSource();
        if (trueSource instanceof EntityLivingBase && immediate == trueSource && trueSource != hurt) {
            UUID attackerId = trueSource.getUniqueID();
            Long armed = ambushArmedUntil.get(attackerId);
            if (armed != null && armed >= now) {
                event.setAmount(event.getAmount() + AMBUSH_BONUS_DAMAGE);
                hurt.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 60, 0, false, true));
                ambushArmedUntil.remove(attackerId);
                lurkUntil.remove(attackerId);
                ((EntityLivingBase) trueSource).removePotionEffect(MobEffects.INVISIBILITY);
                if (hurt.world instanceof WorldServer) {
                    ((WorldServer) hurt.world).spawnParticle(EnumParticleTypes.SMOKE_LARGE,
                            trueSource.posX, trueSource.posY + 1.0D, trueSource.posZ,
                            15, 0.4D, 0.4D, 0.4D, 0.05D);
                }
            }
        }

        // ① 潜伏者受伤破隐：破隐后 2 秒内的下一次近战仍可触发伏击加成
        UUID hurtId = hurt.getUniqueID();
        Long lurking = lurkUntil.get(hurtId);
        if (lurking != null && lurking >= now) {
            lurkUntil.remove(hurtId);
            hurt.removePotionEffect(MobEffects.INVISIBILITY);
            ambushArmedUntil.put(hurtId, now + AMBUSH_WINDOW_TICKS);
            if (hurt.world instanceof WorldServer) {
                ((WorldServer) hurt.world).spawnParticle(EnumParticleTypes.SMOKE_NORMAL,
                        hurt.posX, hurt.posY + 1.0D, hurt.posZ, 10, 0.3D, 0.3D, 0.3D, 0.02D);
            }
        }
    }

    private static void cleanupStale(long now) {
        lurkUntil.entrySet().removeIf(e -> e.getValue() < now - 200L);
        ambushArmedUntil.entrySet().removeIf(e -> e.getValue() < now - 200L);
    }
}
