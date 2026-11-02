package aaaa0ggmc.morphext.quark;

import aaaa0ggmc.morphext.integration.metamorph.SafeMobActAction;
import aaaa0ggmc.morphext.quark.profiler.ProfilerBridge;
import mchorse.metamorph.api.abilities.IAction;
import mchorse.metamorph.api.morphs.AbstractMorph;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.math.Vec3d;

import javax.annotation.Nullable;

/**
 * Quark 生物专属 Mob Act 原生技能适配器。
 */
public class QuarkMobActAdapter implements SafeMobActAction.IMobActAdapter {

    @Override
    public boolean handleMobAct(EntityLivingBase player, AbstractMorph morph, EntityLiving dummy, @Nullable EntityLivingBase targetEnemy, Vec3d lookTarget) {
        if (morph == null || morph.name == null) return false;
        String name = morph.name.toLowerCase();

        long start = ProfilerBridge.begin();
        try {
            // 1. 岩石灵
            if (name.contains("stoneling")) {
                String actId = player.isSneaking() ? "quark_stoneling_vanish" : "quark_stoneling_toss";
                return executeAction(actId, player, morph);
            }

            // 2. 青蛙
            if (name.contains("frog")) {
                String actId = player.isSneaking() ? "quark_frog_tongue" : "quark_frog_leap";
                return executeAction(actId, player, morph);
            }

            // 3. 巨蟹
            if (name.contains("crab")) {
                String actId;
                if (player.isSneaking()) {
                    boolean moving = Math.abs(player.motionX) > 0.05 || Math.abs(player.motionZ) > 0.05;
                    actId = moving ? "quark_crab_sidestep" : "quark_crab_shell_shield";
                } else {
                    actId = "quark_crab_pincer_snip";
                }
                return executeAction(actId, player, morph);
            }

            // 4. 怨灵
            if (name.contains("wraith")) {
                String actId = player.isSneaking() ? "quark_wraith_phase" : "quark_wraith_shriek";
                return executeAction(actId, player, morph);
            }

            // 5. 猎狐犬
            if (name.contains("foxhound")) {
                String actId = player.isSneaking() ? "quark_foxhound_flame_pounce" : "quark_foxhound_fire_breath";
                return executeAction(actId, player, morph);
            }

            // 6. 灰烬骷髅
            if (name.contains("ashen")) {
                return executeAction("quark_ashen_blind_shot", player, morph);
            }

            // 7. 潜影骷髅
            if (name.contains("dweller")) {
                return executeAction("quark_dweller_pierce_shot", player, morph);
            }

            // 8. 海盗骷髅
            if (name.contains("pirate")) {
                return executeAction("quark_pirate_flourish", player, morph);
            }

            return false;
        } finally {
            ProfilerBridge.end("quark:mob_act", start);
        }
    }

    private boolean executeAction(String actionId, EntityLivingBase player, AbstractMorph morph) {
        IAction act = mchorse.metamorph.api.MorphManager.INSTANCE.actions.get(actionId);
        if (act != null) {
            act.execute(player, morph);
            return true;
        }
        return false;
    }
}
