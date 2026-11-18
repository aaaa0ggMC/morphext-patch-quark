package aaaa0ggmc.morphext.quark;

import aaaa0ggmc.morphext.faction.Faction;
import aaaa0ggmc.morphext.quark.passive.QuarkWraithLightFearPassive;
import aaaa0ggmc.morphext.spi.IMorphextPlugin;
import aaaa0ggmc.morphext.spi.MorphextPluginContext;
import mchorse.metamorph.api.MorphManager;

/**
 * Quark 适配补丁 SPI 插件入口（ServiceLoader 自动发现）。
 */
public final class QuarkMorphextPlugin implements IMorphextPlugin {

    @Override
    public String id() {
        return MorphExtQuark.MODID;
    }

    @Override
    public void onPreInit(MorphextPluginContext ctx) {
        QuarkWraithLightFearPassive lightFear = new QuarkWraithLightFearPassive();
        ctx.registerAbility(lightFear);
        MorphManager.INSTANCE.abilities.put(lightFear.getId(), lightFear);

        injectFactions(ctx);
    }

    @Override
    public void onInit(MorphextPluginContext ctx) {
        ctx.registerMobActAdapter(new QuarkMobActAdapter());
    }

    private static void injectFactions(MorphextPluginContext ctx) {
        Faction underworld = ctx.getFaction("underworld");
        if (underworld == null) {
            underworld = ctx.registerFaction(new Faction("underworld",
                    "morphext.faction.underworld.name", "morphext.faction.underworld.desc",
                    new net.minecraft.item.ItemStack(net.minecraft.init.Blocks.COBBLESTONE)));
        }
        underworld.addPredicate(e -> {
            if (e == null) return false;
            String cls = e.getClass().getName();
            return cls.startsWith("vazkii.quark.")
                    && (cls.contains("Stoneling") || cls.contains("Dweller") || cls.contains("Wraith"));
        });

        Faction aquatic = ctx.getFaction("aquatic");
        if (aquatic != null) {
            aquatic.addPredicate(e -> {
                if (e == null) return false;
                String cls = e.getClass().getName();
                return cls.startsWith("vazkii.quark.") && (cls.contains("Crab") || cls.contains("Frog"));
            });
        }

        Faction nether = ctx.getFaction("nether");
        if (nether != null) {
            nether.addPredicate(e -> {
                if (e == null) return false;
                String cls = e.getClass().getName();
                return cls.startsWith("vazkii.quark.") && (cls.contains("Foxhound") || cls.contains("Ashen"));
            });
        }

        Faction undead = ctx.getFaction("undead");
        if (undead != null) {
            undead.addPredicate(e -> {
                if (e == null) return false;
                String cls = e.getClass().getName();
                return cls.startsWith("vazkii.quark.")
                        && (cls.contains("Dweller") || cls.contains("Ashen")
                        || cls.contains("Pirate") || cls.contains("Wraith"));
            });
        }
    }
}
