package com.github.edg_thexu.better_experience.mixin.integration.confluence;

import com.github.edg_thexu.better_experience.module.lootpity.LootPity;
import com.github.edg_thexu.better_experience.module.lootpity.PityRolls;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.storage.loot.LootContext;
import org.confluence.mod.common.loot.PercentageLootEntry;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;
import java.util.function.DoubleSupplier;

@Pseudo
@Mixin(targets = "org.confluence.mod.common.loot.PercentageLootEntry", remap = false)
public abstract class PercentageLootPityMixin {
    @Shadow @Final private List<PercentageLootEntry.Rule> rules;

    @WrapOperation(method = "createItemStack", at = @At(value = "INVOKE",
            target = "Lorg/confluence/mod/common/loot/LootRuleRolls;roll([DZZLjava/util/function/DoubleSupplier;)[Z"))
    private boolean[] betterExperience$accumulate(double[] probabilities, boolean exclusive, boolean guarantee,
                                                  DoubleSupplier random, Operation<boolean[]> original,
                                                  @Local(argsOnly = true) LootContext context) {
        LootPity.Attempt attempt = LootPity.current(context);
        if (attempt == null) return original.call(probabilities, exclusive, guarantee, random);
        double[] scores = java.util.Arrays.copyOf(probabilities, probabilities.length);
        for (int i = 0; i < scores.length; i++) {
            Item item = LootPity.target(rules.get(i).entry());
            if (item != null && scores[i] > 0) scores[i] = attempt.score(item, scores[i]);
        }
        return original.call(PityRolls.probabilities(scores, exclusive), exclusive, guarantee, random);
    }
}
