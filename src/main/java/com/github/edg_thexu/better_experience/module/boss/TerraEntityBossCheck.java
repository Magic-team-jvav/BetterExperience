package com.github.edg_thexu.better_experience.module.boss;

import net.minecraft.world.entity.LivingEntity;
import org.confluence.terraentity.entity.boss.AbstractTerraBossBase;

/** Only loaded when Terra Entity is present, keeping the boss type check optional. */
final class TerraEntityBossCheck {

    private TerraEntityBossCheck() {}

    static boolean isBoss(LivingEntity entity) {
        return entity instanceof AbstractTerraBossBase;
    }
}
