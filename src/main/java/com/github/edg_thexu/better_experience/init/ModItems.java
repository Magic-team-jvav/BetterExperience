package com.github.edg_thexu.better_experience.init;

import com.github.edg_thexu.better_experience.Better_experience;
import com.github.edg_thexu.better_experience.data.component.ItemContainerComponent;
import com.github.edg_thexu.better_experience.data.component.SpaceStaffSettings;
import com.github.edg_thexu.better_experience.item.DebugItem;
import com.github.edg_thexu.better_experience.item.MagicBoomStaff;
import com.github.edg_thexu.better_experience.item.PotionBag;
import com.github.edg_thexu.better_experience.item.SpaceStaff;
import com.github.edg_thexu.better_experience.item.UniversalController;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Better_experience.MODID);
    public static final DeferredRegister.Items TOOLS = DeferredRegister.createItems(Better_experience.MODID);

    public static final DeferredItem<Item> MAGIC_BOOM_STAFF = TOOLS.register("magic_boom_staff", () -> new MagicBoomStaff(new Item.Properties().stacksTo(1), 5));
    public static final DeferredItem<Item> STAR_BOOM_STAFF = TOOLS.register("star_boom_staff", () -> new MagicBoomStaff(new Item.Properties().stacksTo(1), 10, true));
    public static final DeferredItem<Item> SPACE_STAFF = TOOLS.register("space_staff",
            () -> new SpaceStaff(new Item.Properties().stacksTo(1)
                    .component(ModDataComponentTypes.SPACE_STAFF_SETTINGS.get(), SpaceStaffSettings.defaults())));
    public static final DeferredItem<Item> POTION_BAG = TOOLS.register("potion_bag",
            () -> new PotionBag(new Item.Properties().stacksTo(1)
                    .component(ModDataComponentTypes.ITEM_CONTAINER_COMPONENT, new ItemContainerComponent(18))));
    public static final DeferredItem<Item> DEBUG_ITEM = TOOLS.register("debug_item", () -> new DebugItem(new Item.Properties()));
    public static final DeferredItem<Item> UNIVERSAL_CONTROLLER = TOOLS.register("universal_controller", () -> new UniversalController(new Item.Properties()));


    public static void register(IEventBus bus) {
        ITEMS.register(bus);
        TOOLS.register(bus);
    }

}
