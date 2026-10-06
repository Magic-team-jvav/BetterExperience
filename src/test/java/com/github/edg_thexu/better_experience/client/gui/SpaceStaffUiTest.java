package com.github.edg_thexu.better_experience.client.gui;

import com.github.edg_thexu.better_experience.client.gui.widget.LiveSearchBox;
import net.minecraft.client.gui.components.events.AbstractContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public final class SpaceStaffUiTest {
    private SpaceStaffUiTest() {}

    public static void verify() {
        require(SpaceBlockSearch.parse("  ").matches("stone", "minecraft:stone"), "blank query shows all materials");
        require(SpaceBlockSearch.parse("  StOnE BRICKS ").matches("stone bricks", "minecraft:stone_bricks"),
                "case-insensitive AND search with whitespace");
        require(SpaceBlockSearch.parse("橡木").matches("橡木楼梯", "minecraft:oak_stairs"), "localized search");
        require(SpaceBlockSearch.parse("oak_stairs").matches("橡木楼梯", "minecraft:oak_stairs"), "registry ID search");
        require(SpaceBlockSearch.parse("@minecraft stairs").matches("oak stairs", "minecraft:oak_stairs")
                && !SpaceBlockSearch.parse("@other stairs").matches("oak stairs", "minecraft:oak_stairs"),
                "namespace filtering");
        require(!SpaceBlockSearch.parse("stone dirt").matches("stone", "minecraft:stone"), "no matches stays empty");

        AtomicInteger updates = new AtomicInteger();
        // Text edits don't need glyph rendering, so this uses the EditBox's null-font path.
        LiveSearchBox input = new LiveSearchBox(null, 0, 0, 128, 18, Component.literal("Search"), "", text -> updates.incrementAndGet());
        var container = new AbstractContainerEventHandler() {
            @Override
            public List<? extends GuiEventListener> children() { return List.of(input); }
        };
        // EditBox.onClick calls moveCursorTo before the parent assigns focus.
        input.moveCursorTo(0, false);
        container.setFocused(input);
        require(updates.get() == 0, "clicking an unchanged search doesn't refresh results");
        for (char letter : "stone".toCharArray()) require(container.charTyped(letter, 0), "typing reaches focused search");
        require(input.getValue().equals("stone") && updates.get() == 5, "continuous typing refreshes once per change");
        input.moveCursorTo(2, false);
        require(updates.get() == 5, "moving the caret preserves results and focus");
        require(container.charTyped('x', 0) && input.getValue().equals("stxone"), "inserting in the middle preserves caret");
        input.deleteChars(-1);
        require(input.getValue().equals("stone"), "deleting refreshes the existing input");
        require(input.mouseClicked(10, 8, 1) && input.getValue().isEmpty(), "right click clears search");
        input.insertText("石砖");
        require(input.getValue().equals("石砖") && updates.get() == 9, "paste and Unicode update only once");
        require(container.getFocused() == input && input.isFocused() && container.children().contains(input),
                "edited search remains the focused, attached widget");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
