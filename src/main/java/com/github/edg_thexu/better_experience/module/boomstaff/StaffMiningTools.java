package com.github.edg_thexu.better_experience.module.boomstaff;

import com.github.edg_thexu.better_experience.intergration.confluence.ConfluenceHelper;
import com.github.edg_thexu.better_experience.item.MagicBoomStaff;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.confluence.mod.common.data.map.DiggingPower;
import org.confluence.mod.common.init.ModTiers;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/** One inventory snapshot per preview update or blast, with tool checks cached by block state. */
public final class StaffMiningTools {
    private static final TagKey<Item> DRILLS = TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath("c", "tools/drill"));
    private final List<Candidate> tools;
    private final boolean confluence;
    private final Map<BlockState, HarvestTool> selections = new IdentityHashMap<>();

    private StaffMiningTools(List<Candidate> tools, boolean confluence) {
        this.tools = List.copyOf(tools);
        this.confluence = confluence;
    }

    public static StaffMiningTools forPlayer(Player player, MagicBoomStaff staff) {
        if (staff.usesInventoryTools()) {
            return fromInventory(player.getInventory().items, player.getOffhandItem(), ConfluenceHelper.isLoaded());
        }
        return new StaffMiningTools(List.of(new Candidate(player.getOffhandItem().copy(), -1)), false);
    }

    static StaffMiningTools fromInventory(Iterable<ItemStack> inventory, ItemStack offhand, boolean confluence) {
        List<Candidate> tools = new ArrayList<>();
        // Prefer an eligible offhand tool, allowing players to choose its loot enchantments.
        addCandidate(tools, offhand, confluence);
        for (ItemStack stack : inventory) addCandidate(tools, stack, confluence);
        return new StaffMiningTools(tools, confluence);
    }

    private static void addCandidate(List<Candidate> tools, ItemStack stack, boolean confluence) {
        if (!isMiningTool(stack)) return;
        ItemStack copy = stack.copy();
        tools.add(new Candidate(copy, confluence ? DiggingPower.getPower(copy) : -1));
    }

    private static boolean isMiningTool(ItemStack stack) {
        return !stack.isEmpty() && (stack.getItem() instanceof PickaxeItem
                || stack.is(ItemTags.PICKAXES) || stack.is(DRILLS));
    }

    public HarvestTool forBlock(BlockState state) {
        return selections.computeIfAbsent(state, this::selectTool);
    }

    private HarvestTool selectTool(BlockState state) {
        for (Candidate candidate : tools) {
            boolean correct = !state.requiresCorrectToolForDrops() || (confluence
                    ? ModTiers.isCorrectToolForDrops(candidate.power(), candidate.stack(), state)
                    : candidate.stack().isCorrectToolForDrops(state));
            if (correct) return new HarvestTool(candidate.stack(), true);
        }
        return new HarvestTool(ItemStack.EMPTY, !state.requiresCorrectToolForDrops());
    }

    public StaffBlockEligibility.Result evaluate(Level level, BlockPos pos, BlockState state) {
        return StaffBlockEligibility.evaluate(level, pos, state, forBlock(state).correct());
    }

    private record Candidate(ItemStack stack, int power) {}

    public record HarvestTool(ItemStack stack, boolean correct) {}
}
