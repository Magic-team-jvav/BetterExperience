package com.github.edg_thexu.better_experience.client.gui.widget;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

/** Cursor-only changes must not refresh results or replace the focused input widget. */
public final class LiveSearchBox extends EditBox {
    private String lastValue;

    public LiveSearchBox(Font font, int x, int y, int width, int height, Component message,
                         String initial, Consumer<String> onTextChanged) {
        super(font, x, y, width, height, message);
        setMaxLength(64);
        setValue(initial);
        lastValue = getValue();
        setResponder(text -> {
            if (lastValue.equals(text)) return;
            lastValue = text;
            onTextChanged.accept(text);
        });
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 1 && isMouseOver(mouseX, mouseY)) {
            setValue("");
            setFocused(true);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
