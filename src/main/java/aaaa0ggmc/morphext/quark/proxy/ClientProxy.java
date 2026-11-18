package aaaa0ggmc.morphext.quark.proxy;

import aaaa0ggmc.morphext.client.hand.GenericHandRegistry;
import aaaa0ggmc.morphext.client.hand.HandPreset;
import aaaa0ggmc.morphext.quark.MorphExtQuark;
import aaaa0ggmc.morphext.quark.variant.QuarkVariantProvider;
import aaaa0ggmc.morphext.variant.MorphCreativeInjector;

public class ClientProxy extends CommonProxy {
    @Override
    public void init() {
        registerClientHands();
        MorphCreativeInjector.registerProvider(new QuarkVariantProvider());
        MorphExtQuark.LOGGER.info("[morphext-quark] QuarkVariantProvider registered into MorphCreativeInjector.");
    }

    private void registerClientHands() {
        try {
            HandPreset crabPreset = new HandPreset("rightArm", "leftArm");
            GenericHandRegistry.registerModel("ModelCrab", crabPreset);
            GenericHandRegistry.registerMorph("quark:crab", crabPreset);
            GenericHandRegistry.registerMorph("quark.crab", crabPreset);

            HandPreset frogPreset = new HandPreset("rightArm", "leftArm");
            GenericHandRegistry.registerModel("ModelFrog", frogPreset);
            GenericHandRegistry.registerMorph("quark:frog", frogPreset);
            GenericHandRegistry.registerMorph("quark.frog", frogPreset);

            HandPreset stonelingPreset = new HandPreset("arm_right", "arm_left", (float) Math.PI, 0, 0, 0, 0, 0);
            GenericHandRegistry.registerModel("ModelStoneling", stonelingPreset);
            GenericHandRegistry.registerMorph("quark:stoneling", stonelingPreset);
            GenericHandRegistry.registerMorph("quark.stoneling", stonelingPreset);

            HandPreset wraithPreset = new HandPreset("rightArm", "leftArm");
            GenericHandRegistry.registerModel("ModelWraith", wraithPreset);
            GenericHandRegistry.registerMorph("quark:wraith", wraithPreset);
            GenericHandRegistry.registerMorph("quark.wraith", wraithPreset);

            HandPreset foxhoundPreset = new HandPreset("rightFrontLeg", "leftFrontLeg");
            GenericHandRegistry.registerModel("ModelFoxhound", foxhoundPreset);
            GenericHandRegistry.registerMorph("quark:foxhound", foxhoundPreset);
            GenericHandRegistry.registerMorph("quark.foxhound", foxhoundPreset);

            MorphExtQuark.LOGGER.info("[morphext-quark] Injected Quark 3D model bone presets into GenericHandRegistry.");
        } catch (Throwable t) {
            MorphExtQuark.LOGGER.warn("[morphext-quark] Failed to register client hand presets", t);
        }
    }
}
