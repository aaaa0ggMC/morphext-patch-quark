package aaaa0ggmc.morphext.quark.variant;

import aaaa0ggmc.morphext.variant.IVariantHandler;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Locale;

/**
 * Quark 海盗船长变种处理器 (Pirate Captain Variant Handler)。
 * 对应 EntityPirate 的 captain 字段（NBT "captain"，船长变体 60 血）：
 * morphext_variant = "pirate_captain" 或 captain NBT 为 true 时最大生命值 60。
 */
public class PirateCaptainVariantHandler implements IVariantHandler {

    public static final String VARIANT_CAPTAIN = "pirate_captain";

    @Override
    public boolean appliesTo(String morphName, EntityLivingBase entity) {
        if (entity != null && entity.getClass().getName().startsWith("vazkii.quark.")
                && entity.getClass().getName().contains("Pirate")) {
            return true;
        }
        if (morphName == null) {
            return false;
        }
        String lower = morphName.toLowerCase(Locale.ROOT);
        return (lower.startsWith("quark:") || lower.startsWith("quark.")) && lower.endsWith("pirate");
    }

    @Override
    public String getVariantKey(EntityLivingBase entity, NBTTagCompound entityData) {
        if (entityData != null) {
            if (VARIANT_CAPTAIN.equals(entityData.getString("morphext_variant"))) {
                return VARIANT_CAPTAIN;
            }
            if (entityData.getBoolean("captain")) {
                return VARIANT_CAPTAIN;
            }
        }
        return "";
    }

    @Override
    public String getVariantDisplayName(String variantKey, NBTTagCompound entityData) {
        if (VARIANT_CAPTAIN.equals(variantKey)) {
            if (FMLCommonHandler.instance().getEffectiveSide() == Side.CLIENT) {
                String localized = formatClientVariant();
                if (localized != null) {
                    return localized;
                }
            }
            return " (船长)";
        }
        return "";
    }

    @SideOnly(Side.CLIENT)
    private static String formatClientVariant() {
        try {
            if (net.minecraft.client.resources.I18n.hasKey("morphext.variant.pirate_captain")) {
                return " " + net.minecraft.client.resources.I18n.format("morphext.variant.pirate_captain");
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    @Override
    public float getMaxHealth(String variantKey, NBTTagCompound entityData, float defaultHealth) {
        return VARIANT_CAPTAIN.equals(variantKey) ? 60.0F : defaultHealth;
    }
}
