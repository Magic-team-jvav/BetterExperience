package com.github.edg_thexu.better_experience.data.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.NonNullList;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class ItemContainerComponent {

    public static final Codec<ItemContainerComponent> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ItemContainerContents.CODEC.fieldOf("container").forGetter(ins->ins.container),
            Codec.BOOL.optionalFieldOf("autoCollect").forGetter(ins->Optional.of(ins.autoCollect)),
            Codec.INT.fieldOf("size").forGetter(ins->ins.size)
    ).apply(instance, (container, autoCollect,size)->{
        boolean autoCollectValue = autoCollect.orElse(true);
        return new ItemContainerComponent(container, autoCollectValue,size);
    }));

    public static final StreamCodec<ByteBuf, ItemContainerComponent> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

    private final boolean autoCollect;
    public final ItemContainerContents container;

    public final int size;

    public ItemContainerComponent(int size) {
        this(ItemContainerContents.fromItems(List.of()),true,size);
    }

    public ItemContainerComponent(ItemContainerContents container, boolean autoCollect, int size) {
        this.autoCollect = autoCollect;
        this.container = Objects.requireNonNull(container);
        if (size < 9 || size > 54 || size % 9 != 0) {
            throw new IllegalArgumentException("Container size must be 9 to 54 in multiples of 9");
        }
        this.size = size;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof ItemContainerComponent that)) return false;
        return autoCollect == that.autoCollect && size == that.size && that.container.equals(container);
    }

    @Override
    public int hashCode() {
        return Objects.hash(autoCollect, size, container);
    }

    public boolean isAutoCollect() {
        return autoCollect;
    }

    public ItemContainerComponent withAutoCollect(boolean autoCollect) {
        return new ItemContainerComponent(container, autoCollect, size);
    }

    public List<ItemStack> getItems() {
        NonNullList<ItemStack> list = NonNullList.withSize(size, ItemStack.EMPTY);
        List<ItemStack> items = container.stream().toList();
        for (int i = 0; i < Math.min(size, items.size()); i++) {
            list.set(i, items.get(i).copy());
        }
        return list;
    }
}
