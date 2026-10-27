package com.github.edg_thexu.better_experience.module.reforge;

import com.github.edg_thexu.better_experience.data.component.ReforgeHistory;
import com.github.edg_thexu.better_experience.init.ModDataComponentTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.confluence.mod.common.component.prefix.ModPrefix;
import org.confluence.mod.common.component.prefix.PrefixComponent;
import org.confluence.mod.common.component.prefix.PrefixType;
import org.confluence.mod.common.init.entity.NpcEntities;
import org.confluence.mod.common.menu.NPCReforgeMenu;
import org.confluence.mod.util.PlayerUtils;
import org.confluence.mod.util.PrefixUtils;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public final class ReforgeHistoryManager {
    private ReforgeHistoryManager() {}

    /** One prefix rolled by the goblin, kept for the client-side queue. */
    public record Rolled(ItemStack stack, String name) {}

    private static final Deque<Rolled> ROLLED = new ArrayDeque<>();

    /** Records a freshly rolled result; polled results exist on the client only. */
    public static void pushRolled(ItemStack stack) {
        var prefix = PrefixUtils.getPrefix(stack);
        if (prefix != null && prefix.type() != PrefixType.UNKNOWN) {
            ROLLED.addLast(new Rolled(stack.copy(), prefix.name()));
        }
    }

    public static List<Rolled> drainRolled() {
        if (ROLLED.isEmpty()) return List.of();
        List<Rolled> rolled = new ArrayList<>(ROLLED);
        ROLLED.clear();
        return rolled;
    }

    public static ReforgeHistory history(ItemStack stack) {
        return stack.getOrDefault(ModDataComponentTypes.REFORGE_HISTORY.get(), ReforgeHistory.EMPTY);
    }

    public static void record(ItemStack stack) {
        var prefix = PrefixUtils.getPrefix(stack);
        if (prefix != null && prefix.type() != PrefixType.UNKNOWN) {
            stack.set(ModDataComponentTypes.REFORGE_HISTORY.get(), history(stack).record(prefix.name()));
        }
    }

    public static boolean select(ServerPlayer player, int containerId, ItemStack expected, String name) {
        if (!(player.containerMenu instanceof NPCReforgeMenu menu) || menu.containerId != containerId
                || !menu.stillValid(player) || menu.getNPC() == null
                || menu.getNPC().getType() != NpcEntities.GOBLIN_TINKERER.get() || menu.getCooldownTicks() > 0) return false;
        ItemStack stack = menu.getReforgeItem();
        // 只比对物品与数量：客户端同步过来的组件可能与服务端有细微差别（饰品尤其明显）。
        if (stack.isEmpty() || stack.getCount() != expected.getCount()
                || !ItemStack.isSameItem(stack, expected)) return false;
        if (!PrefixUtils.couldReforge(stack) && !ReforgeAccessories.isReforgeable(stack)) return false;
        PrefixType type = PrefixUtils.getPrefixType(stack);
        if (type == PrefixType.UNKNOWN && ReforgeAccessories.isAccessory(stack)) type = PrefixType.ACCESSORY;
        if (type == PrefixType.UNKNOWN) {
            PrefixComponent current = PrefixUtils.getPrefix(stack);
            if (current == null) return false;
            type = current.type();
        }
        if (type == PrefixType.UNKNOWN) return false;
        ModPrefix prefix = java.util.Arrays.stream(type.getAvailable()).filter(value -> value.name().equals(name)).findFirst().orElse(null);
        if (prefix == null) return false;
        int cost = PrefixUtils.getReforgeCost(player, stack);
        if (cost >= 0x3F3F3F3F || !PlayerUtils.tryCostMoney(player, cost, true)) return false;
        PrefixUtils.setAndUpdate(stack, type, prefix);
        menu.container.setChanged();
        menu.slotsChanged(menu.container);
        menu.broadcastChanges();
        return true;
    }
}
