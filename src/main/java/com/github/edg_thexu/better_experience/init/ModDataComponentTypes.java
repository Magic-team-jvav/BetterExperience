package com.github.edg_thexu.better_experience.init;

import com.github.edg_thexu.better_experience.Better_experience;
import com.github.edg_thexu.better_experience.data.component.ItemContainerComponent;
import com.github.edg_thexu.better_experience.data.component.SpaceStaffSettings;
import com.github.edg_thexu.better_experience.data.component.StaffSelectionShape;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class ModDataComponentTypes {
    public static final DeferredRegister.DataComponents TYPES = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Better_experience.MODID);

    public static final Supplier<DataComponentType<ItemContainerComponent>> ITEM_CONTAINER_COMPONENT = TYPES.registerComponentType("item_container", builder ->
            builder.persistent(ItemContainerComponent.CODEC).networkSynchronized(ItemContainerComponent.STREAM_CODEC));

    public static final Supplier<DataComponentType<StaffSelectionShape>> STAFF_SELECTION_SHAPE = TYPES.registerComponentType("staff_selection_shape", builder ->
            builder.persistent(StaffSelectionShape.CODEC).networkSynchronized(StaffSelectionShape.STREAM_CODEC));
    public static final Supplier<DataComponentType<SpaceStaffSettings>> SPACE_STAFF_SETTINGS = TYPES.registerComponentType(
            "space_staff_settings", builder -> builder.persistent(SpaceStaffSettings.CODEC)
                    .networkSynchronized(SpaceStaffSettings.STREAM_CODEC));
}
