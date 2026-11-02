package aaaa0ggmc.morphext.quark.action;

import aaaa0ggmc.morphext.integration.metamorph.ICooldownAction;
import mchorse.metamorph.api.abilities.IAction;
import mchorse.metamorph.api.morphs.AbstractMorph;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 熔岩猎犬熔炼之触 (Quark Foxhound Smelt Touch Action):
 * 还原 EntityFoxhound 坐在熔炉上加速烧炼的招牌机制：
 * 以体内炉火瞬间烧炼背包中最多 8 件可熔炼物（矿石→锭、沙→玻璃等原版熔炉配方），
 * 伴随炉火噼啪与烈焰粒子；无可烧炼物则原样取消（不进入冷却）。
 * 冷却 10 秒。
 */
public class QuarkFoxhoundSmeltTouchAction implements IAction, ICooldownAction {

    public static final int COOLDOWN = 200;
    public static final int MAX_SMELTED = 8;

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
                SoundEvents.BLOCK_FURNACE_FIRE_CRACKLE, SoundCategory.PLAYERS, 1.2F, 1.0F);

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

        // 逐格扫描背包，按原版熔炉配方烧炼
        List<ItemStack> results = new ArrayList<>();
        InventoryPlayer inventory = player.inventory;
        for (int i = 0; i < inventory.mainInventory.size() && results.size() < MAX_SMELTED; i++) {
            ItemStack stack = inventory.mainInventory.get(i);
            if (stack.isEmpty()) {
                continue;
            }
            ItemStack smelted = FurnaceRecipes.instance().getSmeltingResult(stack);
            if (smelted.isEmpty()) {
                continue;
            }
            int take = Math.min(stack.getCount(), MAX_SMELTED - results.size());
            for (int n = 0; n < take; n++) {
                results.add(smelted.copy());
            }
            stack.shrink(take);
            if (stack.isEmpty()) {
                inventory.mainInventory.set(i, ItemStack.EMPTY);
            }
        }

        if (results.isEmpty()) {
            // 无可烧炼物：原样取消且不进入冷却
            world.playSound(null, player.posX, player.posY, player.posZ,
                    SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.PLAYERS, 0.8F, 1.2F);
            return;
        }
        this.nextUseAt.put(player.getUniqueID(), now + COOLDOWN);

        for (ItemStack result : results) {
            if (!player.inventory.addItemStackToInventory(result)) {
                player.dropItem(result, false);
            }
        }

        world.playSound(null, player.posX, player.posY, player.posZ,
                SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 1.0F, 1.3F);

        if (world instanceof WorldServer) {
            WorldServer ws = (WorldServer) world;
            ws.spawnParticle(EnumParticleTypes.FLAME,
                    player.posX, player.posY + 0.8D, player.posZ, 25, 0.5D, 0.5D, 0.5D, 0.02D);
            ws.spawnParticle(EnumParticleTypes.SMOKE_LARGE,
                    player.posX, player.posY + 1.2D, player.posZ, 12, 0.4D, 0.3D, 0.4D, 0.02D);
        }
    }
}
