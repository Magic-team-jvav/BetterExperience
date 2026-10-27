package com.github.edg_thexu.better_experience.init;

import com.github.edg_thexu.better_experience.Better_experience;
import com.github.edg_thexu.better_experience.data.component.AmmoChainCursor;
import com.github.edg_thexu.better_experience.data.component.AmmoChainSettings;
import com.github.edg_thexu.better_experience.data.component.FluidStaffSettings;
import com.github.edg_thexu.better_experience.data.component.FluidStaffStorage;
import com.github.edg_thexu.better_experience.data.component.PotionBagContents;
import com.github.edg_thexu.better_experience.data.component.SpaceStaffSettings;
import com.github.edg_thexu.better_experience.item.AmmoChain;
import com.github.edg_thexu.better_experience.item.DebugItem;
import com.github.edg_thexu.better_experience.item.FluidStaff;
import com.github.edg_thexu.better_experience.item.MagicBoomStaff;
import com.github.edg_thexu.better_experience.item.PotionBag;
import com.github.edg_thexu.better_experience.item.SpaceStaff;
import com.github.edg_thexu.better_experience.item.StructureLocatorOrb;
import com.github.edg_thexu.better_experience.item.UniversalController;
import com.github.edg_thexu.better_experience.item.VoidStaff;
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
    public static final DeferredItem<Item> FLUID_STAFF = TOOLS.register("fluid_staff",
            () -> new FluidStaff(new Item.Properties().stacksTo(1)
                    .component(ModDataComponentTypes.SPACE_STAFF_SETTINGS.get(), SpaceStaffSettings.defaults())
                    .component(ModDataComponentTypes.FLUID_STAFF_SETTINGS.get(), FluidStaffSettings.defaults())
                    .component(ModDataComponentTypes.FLUID_STAFF_STORAGE.get(), FluidStaffStorage.EMPTY)));
    public static final DeferredItem<Item> STRUCTURE_LOCATOR_ORB = TOOLS.register("structure_locator_orb",
            () -> new StructureLocatorOrb(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> VOID_STAFF = TOOLS.register("void_staff", () -> new VoidStaff(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> AMMO_CHAIN = TOOLS.register("ammo_chain", () -> new AmmoChain(new Item.Properties().stacksTo(1)
            .component(ModDataComponentTypes.AMMO_CHAIN_SETTINGS.get(), AmmoChainSettings.defaults())
            .component(ModDataComponentTypes.AMMO_CHAIN_CURSOR.get(), AmmoChainCursor.DEFAULT)));
    public static final DeferredItem<Item> POTION_BAG = TOOLS.register("potion_bag",
            () -> new PotionBag(new Item.Properties().stacksTo(1)
                    .component(ModDataComponentTypes.POTION_BAG_CONTENTS.get(), PotionBagContents.EMPTY)));
    public static final DeferredItem<Item> DEBUG_ITEM = TOOLS.register("debug_item", () -> new DebugItem(new Item.Properties()));
    public static final DeferredItem<Item> UNIVERSAL_CONTROLLER = TOOLS.register("universal_controller", () -> new UniversalController(new Item.Properties()));


    public static void register(IEventBus bus) {
        ITEMS.register(bus);
        TOOLS.register(bus);
    }

}
