package aaaa0ggmc.morphext.quark;

import aaaa0ggmc.morphext.faction.Faction;
import aaaa0ggmc.morphext.faction.FactionRegistry;
import aaaa0ggmc.morphext.integration.metamorph.SafeMobActAction;
import aaaa0ggmc.morphext.quark.action.*;
import mchorse.metamorph.api.MorphManager;
import mchorse.metamorph.api.abilities.IAction;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(
        modid = MorphExtQuark.MODID,
        name = MorphExtQuark.NAME,
        version = MorphExtQuark.VERSION,
        dependencies = "required-after:aaaa0ggmc_morph_extension;required-after:quark;required-after:metamorph",
        acceptableRemoteVersions = "*"
)
public class MorphExtQuark {
    public static final String MODID = "morphext_patch_quark";
    public static final String NAME = "Morph Extension - Quark Patch";
    public static final String VERSION = "1.0.0";
    public static final Logger LOGGER = LogManager.getLogger(NAME);

    @Mod.Instance(MODID)
    public static MorphExtQuark INSTANCE;

    @SidedProxy(
            clientSide = "aaaa0ggmc.morphext.quark.proxy.ClientProxy",
            serverSide = "aaaa0ggmc.morphext.quark.proxy.CommonProxy"
    )
    public static aaaa0ggmc.morphext.quark.proxy.CommonProxy proxy;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        LOGGER.info("[morphext-quark] Initializing {} v{}...", NAME, VERSION);
        registerActions();
        registerVariantHandlers();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init();
    }

    private void registerActions() {
        register("quark_stoneling_vanish", new QuarkStonelingVanishAction());
        register("quark_stoneling_toss", new QuarkStonelingTossAction());
        register("quark_stoneling_burrow", new QuarkStonelingBurrowAction());
        register("quark_stoneling_ore_magnet", new QuarkStonelingOreMagnetAction());
        register("quark_frog_leap", new QuarkFrogLeapAction());
        register("quark_frog_tongue", new QuarkFrogTongueAction());
        register("quark_frog_wednesday", new QuarkFrogWednesdayAction());
        register("quark_crab_pincer_snip", new QuarkCrabPincerAction());
        register("quark_crab_sidestep", new QuarkCrabSidestepAction());
        register("quark_crab_shell_shield", new QuarkCrabShellShieldAction());
        register("quark_crab_rave", new QuarkCrabRaveAction());
        register("quark_wraith_shriek", new QuarkWraithShriekAction());
        register("quark_wraith_phase", new QuarkWraithPhaseAction());
        register("quark_wraith_soul_grasp", new QuarkWraithSoulGraspAction());
        register("quark_foxhound_fire_breath", new QuarkFoxhoundFireBreathAction());
        register("quark_foxhound_flame_pounce", new QuarkFoxhoundFlamePounceAction());
        register("quark_foxhound_love_burn", new QuarkFoxhoundLoveBurnAction());
        register("quark_foxhound_smelt_touch", new QuarkFoxhoundSmeltTouchAction());
        register("quark_ashen_blind_shot", new QuarkAshenBlindShotAction());
        register("quark_ashen_ash_veil", new QuarkAshenAshVeilAction());
        register("quark_dweller_pierce_shot", new QuarkDwellerPierceAction());
        // 潜伏伏击需要服务端伤害事件支撑（受伤/出手破隐与破隐一击），动作实例同时注册为事件处理器
        QuarkDwellerLurkAction lurkAction = new QuarkDwellerLurkAction();
        register("quark_dweller_lurk", lurkAction);
        MinecraftForge.EVENT_BUS.register(lurkAction);
        register("quark_dweller_ambush_mark", new QuarkDwellerAmbushMarkAction());
        register("quark_pirate_flourish", new QuarkPirateFlourishAction());
        register("quark_pirate_captain", new QuarkPirateCaptainAction());
        register("quark_archaeologist_appraise", new QuarkArchaeologistAppraiseAction());

        LOGGER.info("[morphext-quark] Registered 26 Quark mob actions into MorphManager");
    }

    private void register(String id, IAction action) {
        MorphManager.INSTANCE.actions.put(id, action);
        MorphManager.INSTANCE.actions.put("morphext:" + id, action);
    }

    private void registerVariantHandlers() {
        try {
            // 海盗船长变体（EntityPirate 的 captain 字段，60 血）
            aaaa0ggmc.morphext.variant.VariantRegistry.register(new aaaa0ggmc.morphext.quark.variant.PirateCaptainVariantHandler());
            LOGGER.info("[morphext-quark] Registered PirateCaptainVariantHandler into VariantRegistry.");
        } catch (Throwable t) {
            LOGGER.warn("[morphext-quark] Failed to register pirate captain variant handler", t);
        }
    }
}
