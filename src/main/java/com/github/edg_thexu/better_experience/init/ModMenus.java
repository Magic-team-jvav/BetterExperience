package com.github.edg_thexu.better_experience.init;

import com.github.edg_thexu.better_experience.Better_experience;
import com.github.edg_thexu.better_experience.client.event.ClientModEvent;
import com.github.edg_thexu.better_experience.menu.AutoFishMenu;
import com.github.edg_thexu.better_experience.menu.ChronopilotMenu;
import com.github.edg_thexu.better_experience.menu.PotionBagMenu;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;


/**
 * 注册屏幕: {@link ClientModEvent#registerMenuScreens})}
 */
public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> TYPES = DeferredRegister.create(BuiltInRegistries.MENU, Better_experience.MODID);

    public static final Supplier<MenuType<ChronopilotMenu>> CHRONOPILOT_MENU = TYPES.register("chronopilot",
            () -> IMenuTypeExtension.create((id, inventory, buffer) ->
                    new ChronopilotMenu(id, inventory, buffer.readBlockPos())));

    public static final Supplier<MenuType<AutoFishMenu>> AUTO_FISH_MENU = TYPES.register("auto_fish", () -> new MenuType<>(AutoFishMenu::new, FeatureFlags.VANILLA_SET));
    public static final Supplier<MenuType<PotionBagMenu>> POTION_BAG_MENU = TYPES.register("potion_bag", () -> new MenuType<>(PotionBagMenu::new, FeatureFlags.VANILLA_SET));


}
