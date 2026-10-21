package com.github.edg_thexu.better_experience.init;

import com.github.edg_thexu.better_experience.Better_experience;
import com.github.edg_thexu.better_experience.data.component.ItemContainerComponent;
import com.github.edg_thexu.better_experience.data.component.AmmoChainSettings;
import com.github.edg_thexu.better_experience.data.component.AmmoChainCursor;
import com.github.edg_thexu.better_experience.data.component.FluidStaffSettings;
import com.github.edg_thexu.better_experience.data.component.FluidStaffStorage;
import com.github.edg_thexu.better_experience.data.component.SpaceStaffSettings;
import com.github.edg_thexu.better_experience.data.component.StaffSelectionShape;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.resources.ResourceLocation;
import com.github.edg_thexu.better_experience.module.locator.StructureMarker;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class ModDataComponentTypes {
    public static final DeferredRegister.DataComponents TYPES = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Better_experience.MODID);

    public static final Supplier<DataComponentType<ItemContainerComponent>> ITEM_CONTAINER_COMPONENT = TYPES.registerComponentType("item_container", builder ->
            builder.persistent(ItemContainerComponent.CODEC).networkSynchronized(ItemContainerComponent.STREAM_CODEC));

    public static final Supplier<DataComponentType<AmmoChainSettings>> AMMO_CHAIN_SETTINGS = TYPES.registerComponentType(
            "ammo_chain_settings", builder -> builder.persistent(AmmoChainSettings.CODEC).networkSynchronized(AmmoChainSettings.STREAM_CODEC));
    public static final Supplier<DataComponentType<AmmoChainCursor>> AMMO_CHAIN_CURSOR = TYPES.registerComponentType(
            "ammo_chain_cursor", builder -> builder.persistent(AmmoChainCursor.CODEC).networkSynchronized(AmmoChainCursor.STREAM_CODEC));

    public static final Supplier<DataComponentType<ResourceLocation>> LOCATOR_STRUCTURE = TYPES.registerComponentType(
            "locator_structure", builder -> builder.persistent(ResourceLocation.CODEC).networkSynchronized(ResourceLocation.STREAM_CODEC));
    public static final Supplier<DataComponentType<StructureMarker>> LOCATOR_MARKER = TYPES.registerComponentType(
            "locator_marker", builder -> builder.persistent(StructureMarker.CODEC).networkSynchronized(StructureMarker.STREAM_CODEC));
    public static final Supplier<DataComponentType<StaffSelectionShape>> STAFF_SELECTION_SHAPE = TYPES.registerComponentType("staff_selection_shape", builder ->
            builder.persistent(StaffSelectionShape.CODEC).networkSynchronized(StaffSelectionShape.STREAM_CODEC));
    public static final Supplier<DataComponentType<FluidStaffSettings>> FLUID_STAFF_SETTINGS = TYPES.registerComponentType(
            "fluid_staff_settings", builder -> builder.persistent(FluidStaffSettings.CODEC)
                    .networkSynchronized(FluidStaffSettings.STREAM_CODEC));
    public static final Supplier<DataComponentType<FluidStaffStorage>> FLUID_STAFF_STORAGE = TYPES.registerComponentType(
            "fluid_staff_storage", builder -> builder.persistent(FluidStaffStorage.CODEC)
                    .networkSynchronized(FluidStaffStorage.STREAM_CODEC));
    public static final Supplier<DataComponentType<SpaceStaffSettings>> SPACE_STAFF_SETTINGS = TYPES.registerComponentType(
            "space_staff_settings", builder -> builder.persistent(SpaceStaffSettings.CODEC)
                    .networkSynchronized(SpaceStaffSettings.STREAM_CODEC));
}
