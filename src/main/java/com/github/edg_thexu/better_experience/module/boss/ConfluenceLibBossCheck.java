package com.github.edg_thexu.better_experience.module.boss;

import net.minecraft.world.entity.LivingEntity;
import org.confluence.lib.api.entity.Boss;

/** Only loaded when Confluence Lib is present, keeping the boss type check optional. */
final class ConfluenceLibBossCheck {

    private ConfluenceLibBossCheck() {}

    static boolean isBoss(LivingEntity entity) {
        return entity instanceof Boss;
    }
}
