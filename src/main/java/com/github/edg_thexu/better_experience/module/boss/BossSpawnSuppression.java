package com.github.edg_thexu.better_experience.module.boss;

import com.github.edg_thexu.better_experience.Better_experience;
import com.github.edg_thexu.better_experience.config.CommonConfig;
import com.github.edg_thexu.better_experience.intergration.confluence_lib.ConfluenceLibHelper;
import com.github.edg_thexu.better_experience.intergration.terra_entity.TEHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Enemy;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

@EventBusSubscriber(modid = Better_experience.MODID)
public final class BossSpawnSuppression {
    public static final double RADIUS = 192.0;

    private BossSpawnSuppression() {}

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || event.loadedFromDisk()
                || !CommonConfig.NO_NATURAL_MONSTERS_DURING_BOSS.get()
                || !(event.getEntity() instanceof Mob mob)
                || !isSuppressedSpawnType(mob.getSpawnType())
                || !(mob instanceof Enemy || mob.getType().getCategory() == MobCategory.MONSTER)
                || isBoss(mob)) {
            return;
        }

        // Joining entities can belong to chunks still being promoted to FULL.
        // Iterate loaded entities only: do not query or load chunks from this event.
        // The final join also covers Confluence spawners that bypass PositionCheck.
        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof LivingEntity boss && boss.isAlive() && isBoss(boss)
                    && boss.distanceToSqr(mob) <= RADIUS * RADIUS) {
                event.setCanceled(true);
                return;
            }
        }
    }

    static boolean isSuppressedSpawnType(MobSpawnType type) {
        return type == MobSpawnType.NATURAL || type == MobSpawnType.CHUNK_GENERATION;
    }

    static boolean isBoss(LivingEntity entity) {
        return entity.getType().is(Tags.EntityTypes.BOSSES)
                || ConfluenceLibHelper.isLoaded() && ConfluenceLibBossCheck.isBoss(entity)
                || TEHelper.isLoaded() && TerraEntityBossCheck.isBoss(entity);
    }
}
