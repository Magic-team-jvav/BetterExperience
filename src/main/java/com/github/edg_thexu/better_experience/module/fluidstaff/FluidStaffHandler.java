package com.github.edg_thexu.better_experience.module.fluidstaff;

import com.github.edg_thexu.better_experience.data.component.FluidStaffStorage;
import com.github.edg_thexu.better_experience.init.ModDataComponentTypes;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;

/** Standard item capability: simulation never changes components; no creative fluids leak into pipes. */
public final class FluidStaffHandler implements IFluidHandlerItem {
    private final ItemStack stack;
    private final DataComponentType<FluidStaffStorage> component;

    public FluidStaffHandler(ItemStack stack) { this(stack, ModDataComponentTypes.FLUID_STAFF_STORAGE.get()); }

    FluidStaffHandler(ItemStack stack, DataComponentType<FluidStaffStorage> component) {
        this.stack = stack;
        this.component = component;
    }

    private FluidStaffStorage storage() {
        return stack.getOrDefault(component, FluidStaffStorage.EMPTY);
    }
    @Override
    public ItemStack getContainer() { return stack; }
    @Override
    public int getTanks() { return storage().types() + 1; }
    @Override
    public FluidStack getFluidInTank(int tank) {
        return storage().fluidAt(tank);
    }
    @Override
    public int getTankCapacity(int tank) { return tank >= 0 && tank < getTanks() ? FluidStaffStorage.CAPACITY : 0; }
    @Override
    public boolean isFluidValid(int tank, FluidStack fluid) {
        if (tank < 0 || tank >= getTanks() || fluid.isEmpty()) return false;
        var stored = getFluidInTank(tank);
        return stored.isEmpty() || FluidStack.isSameFluidSameComponents(stored, fluid);
    }
    @Override
    public int fill(FluidStack resource, FluidAction action) {
        var storage = storage();
        int accepted = storage.accepted(resource);
        if (accepted > 0 && action.execute()) stack.set(component, storage.add(resource));
        return accepted;
    }
    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        if (resource.isEmpty()) return FluidStack.EMPTY;
        var storage = storage();
        int amount = Math.min(resource.getAmount(), storage.amount(resource));
        if (amount == 0) return FluidStack.EMPTY;
        var drained = resource.copyWithAmount(amount);
        if (action.execute()) stack.set(component, storage.remove(drained));
        return drained;
    }
    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        if (maxDrain <= 0) return FluidStack.EMPTY;
        var first = storage().fluidAt(0);
        return first.isEmpty() ? FluidStack.EMPTY : drain(first.copyWithAmount(maxDrain), action);
    }
}
