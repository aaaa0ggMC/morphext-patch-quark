package aaaa0ggmc.morphext.quark.passive;

import aaaa0ggmc.morphext.ability.AbstractPassiveAbility;
import aaaa0ggmc.morphext.ability.MorphContext;
import mchorse.metamorph.api.abilities.IAbility;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 游魂惧光 (Quark Wraith Light Fear Passive):
 * 还原 EntityWraith 只在灵魂沙/下界黑暗处生成的幽魂习性：
 * 1. 畏光：强光（光照 >= 12）或直射日光下遭受虚弱 I 与迟缓 I，身冒阴烟；
 * 2. 暗影亲和：身处黑暗（光照 <= 7）获得夜视与速度 I。
 * 小幅增减益被动，服务端权威。
 * <p>
 * 同时兼备 MorphExtension IPassiveAbility 与 Metamorph 原生 IAbility 双接口。
 */
public class QuarkWraithLightFearPassive extends AbstractPassiveAbility implements IAbility {

    public static final String ID = "quark_wraith_light_fear";

    /** 同 tick 双通道去重（IPassiveAbility 与 IAbility 双注册，防止同 tick 双跑）。 */
    private static final Map<UUID, Integer> LAST_DRIVEN_TICK = new ConcurrentHashMap<>();

    public QuarkWraithLightFearPassive() {
        super(ID, "morphext.ability." + ID + ".name", "morphext.ability." + ID + ".desc", ItemStack.EMPTY);
    }

    // ==========================================
    //       MorphExtension IPassiveAbility 实现
    // ==========================================

    @Override
    public void onUpdate(EntityPlayer player, MorphContext context) {
        handleLightFearTick(player);
    }

    // ==========================================
    //       Metamorph 原生 IAbility 桥接实现
    // ==========================================

    @Override
    public void update(EntityLivingBase entity) {
        if (entity instanceof EntityPlayer) {
            handleLightFearTick((EntityPlayer) entity);
        }
    }

    @Override
    public void onMorph(EntityLivingBase entity) {
        // 变身瞬间无需初始化
    }

    @Override
    public void onDemorph(EntityLivingBase entity) {
        // 短时效效果（<=10s）自然消散，无需清理
    }

    // ==========================================
    //               核心逻辑封装
    // ==========================================

    public static void handleLightFearTick(EntityPlayer player) {
        if (player == null || player.world == null || player.world.isRemote
                || player.isCreative() || player.isSpectator()) {
            return;
        }
        if (player.ticksExisted % 20 != 0) {
            return;
        }
        Integer lastTick = LAST_DRIVEN_TICK.put(player.getUniqueID(), player.ticksExisted);
        if (lastTick != null && lastTick.intValue() == player.ticksExisted) {
            return;
        }
        if (LAST_DRIVEN_TICK.size() > 64) {
            LAST_DRIVEN_TICK.entrySet().removeIf(e -> e.getValue() < player.ticksExisted - 100);
        }
        World world = player.world;
        BlockPos pos = new BlockPos(player.posX, player.posY + player.getEyeHeight(), player.posZ);
        int light = world.getLight(pos);
        boolean inSunlight = world.isDaytime() && world.canSeeSky(pos)
                && world.getLightFor(EnumSkyBlock.SKY, pos) > 10;

        if (inSunlight || light >= 12) {
            // 畏光：虚弱 + 迟缓
            player.addPotionEffect(new PotionEffect(MobEffects.WEAKNESS, 60, 0, false, true));
            player.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 60, 0, false, true));

            if (world instanceof WorldServer) {
                ((WorldServer) world).spawnParticle(EnumParticleTypes.SMOKE_NORMAL,
                        player.posX, player.posY + player.height * 0.5D, player.posZ,
                        4, 0.25D, 0.35D, 0.25D, 0.02D);
            }
        } else if (light <= 7) {
            // 暗影亲和：夜视 + 速度
            player.addPotionEffect(new PotionEffect(MobEffects.NIGHT_VISION, 220, 0, false, false));
            player.addPotionEffect(new PotionEffect(MobEffects.SPEED, 60, 0, false, true));
        }
    }
}
