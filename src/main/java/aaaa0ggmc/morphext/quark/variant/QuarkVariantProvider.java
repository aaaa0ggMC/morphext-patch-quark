package aaaa0ggmc.morphext.quark.variant;

import aaaa0ggmc.morphext.variant.CreativeVariantDef;
import aaaa0ggmc.morphext.variant.ICreativeVariantProvider;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

/**
 * Quark 专属变种与特异形态提供者 (Quark Creative Variant Provider)。
 * 
 * 核心变种：
 * 1. 岩石灵 (Stoneling) 7 种岩石地质变体 (石头、安山岩、闪长岩、花岗岩、石灰华、玄武岩、大理石) 与珍宝持有特异体；
 * 2. 螃蟹 (Crab) 巨型敌人螃蟹 (Giant Enemy Crab 2.0x)、狂欢之蟹 (Rave Crab) 与幼年蟹；
 * 3. 青蛙 (Frog) 微型跃蛙 (0.6x)、巨型巨蛙 (1.6x) 与幼年蛙；
 * 4. 狐犬 (Foxhound) 驯服狐犬、幼年狐犬；
 * 5. 怨灵 (Wraith)；
 * 6. 海盗 (Pirate) 船长特异体（captain，60 血）。
 */
public class QuarkVariantProvider implements ICreativeVariantProvider {

    private static final String[][] STONELING_TYPES = new String[][]{
            {"石头", "Stone"},
            {"安山岩", "Andesite"},
            {"闪长岩", "Diorite"},
            {"花岗岩", "Granite"},
            {"石灰华", "Limestone"},
            {"玄武岩", "Basalt"},
            {"大理石", "Marble"}
    };

    @Override
    public List<CreativeVariantDef> getVariants(World world) {
        List<CreativeVariantDef> list = new ArrayList<>();

        // 1. 岩石灵 (Stoneling) 7 种地质变体
        for (int i = 0; i < STONELING_TYPES.length; i++) {
            NBTTagCompound stoneData = new NBTTagCompound();
            stoneData.setByte("Variant", (byte) i);
            stoneData.setString("morphext_variant", "stoneling_rock_" + i);
            String displayName = "岩石灵 (" + STONELING_TYPES[i][0] + ") - " + STONELING_TYPES[i][1] + " Stoneling";
            list.add(new CreativeVariantDef("quark:stoneling", displayName, "quark", stoneData));
        }

        // 钻石持有岩石灵
        NBTTagCompound diamondStoneling = new NBTTagCompound();
        diamondStoneling.setByte("Variant", (byte) 0);
        NBTTagCompound diamondItem = new NBTTagCompound();
        new ItemStack(Items.DIAMOND).writeToNBT(diamondItem);
        diamondStoneling.setTag("carryingItem", diamondItem);
        diamondStoneling.setString("morphext_variant", "stoneling_carrying_diamond");
        list.add(new CreativeVariantDef("quark:stoneling", "岩石灵 (钻石持有) - Diamond Stoneling", "quark", diamondStoneling));

        // 2. 螃蟹 (Crab)
        // 巨型敌人螃蟹 (EnemyCrabRating 2.5F, 缩放 2.0x)
        NBTTagCompound giantCrab = new NBTTagCompound();
        giantCrab.setFloat("EnemyCrabRating", 2.5F);
        giantCrab.setString("morphext_variant", "giant_enemy_crab");
        list.add(new CreativeVariantDef("quark:crab", "螃蟹 (巨型重甲) - Giant Enemy Crab", "quark", giantCrab, 2.0F));

        // 狂欢之蟹 (Raving)
        NBTTagCompound raveCrab = new NBTTagCompound();
        raveCrab.setBoolean("Raving", true);
        raveCrab.setString("morphext_variant", "rave_crab");
        list.add(new CreativeVariantDef("quark:crab", "螃蟹 (狂欢模式) - Raving Crab", "quark", raveCrab));

        // 幼年螃蟹
        list.add(CreativeVariantDef.createBaby("quark:crab", "螃蟹", "Crab", "quark"));

        // 3. 青蛙 (Frog)
        // 微型青蛙
        NBTTagCompound tinyFrog = new NBTTagCompound();
        tinyFrog.setFloat("SizeModifier", 0.5F);
        tinyFrog.setString("morphext_variant", "tiny_frog");
        list.add(new CreativeVariantDef("quark:frog", "青蛙 (微型灵动) - Tiny Frog", "quark", tinyFrog, 0.6F));

        // 巨型青蛙
        NBTTagCompound giantFrog = new NBTTagCompound();
        giantFrog.setFloat("SizeModifier", 2.0F);
        giantFrog.setString("morphext_variant", "giant_frog");
        list.add(new CreativeVariantDef("quark:frog", "青蛙 (巨型蛮力) - Giant Frog", "quark", giantFrog, 1.6F));

        // 幼年青蛙
        list.add(CreativeVariantDef.createBaby("quark:frog", "青蛙", "Frog", "quark"));

        // 4. 狐犬 (Foxhound)
        NBTTagCompound tamedFoxhound = new NBTTagCompound();
        tamedFoxhound.setString("OwnerUUID", "00000000-0000-0000-0000-000000000000");
        tamedFoxhound.setByte("CollarColor", (byte) 14);
        tamedFoxhound.setString("morphext_variant", "tamed_foxhound");
        list.add(new CreativeVariantDef("quark:foxhound", "狐犬 (驯服地狱犬) - Tamed Foxhound", "quark", tamedFoxhound));

        list.add(CreativeVariantDef.createBaby("quark:foxhound", "狐犬", "Foxhound", "quark"));

        // 5. 海盗 (Pirate) 船长变体（EntityPirate 的 captain 字段，60 血船长）
        NBTTagCompound pirateCaptain = new NBTTagCompound();
        pirateCaptain.setBoolean("captain", true);
        pirateCaptain.setString("morphext_variant", "pirate_captain");
        list.add(new CreativeVariantDef("quark:pirate", "海盗 (船长) - Pirate Captain", "quark", pirateCaptain));

        return list;
    }
}
