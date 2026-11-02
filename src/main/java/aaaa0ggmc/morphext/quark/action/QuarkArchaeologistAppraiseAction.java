package aaaa0ggmc.morphext.quark.action;

import aaaa0ggmc.morphext.integration.metamorph.ICooldownAction;
import mchorse.metamorph.api.abilities.IAction;
import mchorse.metamorph.api.morphs.AbstractMorph;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 考古学家鉴宝交易 (Quark Archaeologist Appraise Action):
 * 对应 EntityArchaeologist 的村民交易机制（MerchantRecipe）：
 * 消耗 1 枚绿宝石进行专家鉴宝，随机产出 2-4 件矿物/宝物类鉴定品掉落，
 * 伴随交易音效与闪光粒子；冷却 60 秒，未持有绿宝石则原样取消（不进入冷却）。
 */
public class QuarkArchaeologistAppraiseAction implements IAction, ICooldownAction {

    public static final int COOLDOWN = 1200;

    private final Map<UUID, Long> nextUseAt = new ConcurrentHashMap<>();

    private static final ItemStack[] TREASURE_POOL = new ItemStack[]{
            new ItemStack(Items.COAL, 2),
            new ItemStack(Items.IRON_INGOT, 2),
            new ItemStack(Items.GOLD_INGOT, 1),
            new ItemStack(Items.REDSTONE, 3),
            new ItemStack(Items.DYE, 3, 4),
            new ItemStack(Items.QUARTZ, 3),
            new ItemStack(Items.GOLD_NUGGET, 3),
            new ItemStack(Items.DIAMOND, 1),
            new ItemStack(Items.EMERALD, 1)
    };

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
                SoundEvents.ENTITY_VILLAGER_TRADING, SoundCategory.NEUTRAL, 1.2F, 1.0F);

        if (world.isRemote) return;

        long now = world.getTotalWorldTime();
        Long next = this.nextUseAt.get(entity.getUniqueID());
        if (next != null && next > now) {
            return;
        }

        if (!(entity instanceof EntityPlayer)) {
            return;
        }
        EntityPlayer player = (EntityPlayer) entity;

        // 消耗 1 枚绿宝石（创造模式免消耗）；未持有时原样取消且不进入冷却
        if (!player.capabilities.isCreativeMode && !consumeEmerald(player)) {
            world.playSound(null, player.posX, player.posY, player.posZ,
                    SoundEvents.ENTITY_VILLAGER_NO, SoundCategory.NEUTRAL, 1.0F, 1.0F);
            return;
        }

        // 随机产出 2-4 件矿物/宝物类鉴定品
        int count = 2 + world.rand.nextInt(3);
        for (int i = 0; i < count; i++) {
            ItemStack template = TREASURE_POOL[world.rand.nextInt(TREASURE_POOL.length)];
            ItemStack drop = template.copy();
            drop.setCount(Math.max(1, template.getCount() / 2 + world.rand.nextInt(template.getCount() / 2 + 1)));
            player.dropItem(drop, false);
        }

        this.nextUseAt.put(player.getUniqueID(), now + COOLDOWN);

        world.playSound(null, player.posX, player.posY, player.posZ,
                SoundEvents.ENTITY_VILLAGER_YES, SoundCategory.NEUTRAL, 1.0F, 1.1F);
        world.playSound(null, player.posX, player.posY, player.posZ,
                SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.NEUTRAL, 1.0F, 1.4F);

        if (world instanceof WorldServer) {
            ((WorldServer) world).spawnParticle(EnumParticleTypes.VILLAGER_HAPPY,
                    player.posX, player.posY + 1.0D, player.posZ, 25, 0.8D, 0.6D, 0.8D, 0.0D);
        }
    }

    private static boolean consumeEmerald(EntityPlayer player) {
        InventoryPlayer inventory = player.inventory;
        for (int i = 0; i < inventory.mainInventory.size(); i++) {
            ItemStack stack = inventory.mainInventory.get(i);
            if (!stack.isEmpty() && stack.getItem() == Items.EMERALD) {
                stack.shrink(1);
                if (stack.isEmpty()) {
                    inventory.mainInventory.set(i, ItemStack.EMPTY);
                }
                return true;
            }
        }
        return false;
    }
}
