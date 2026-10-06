package com.github.edg_thexu.better_experience.data.gen;

import com.github.edg_thexu.better_experience.Better_experience;
import com.github.edg_thexu.better_experience.init.ModItems;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.List;



public class ModItemModelProvider extends ItemModelProvider {

    private static final ResourceLocation MISSING_ITEM = Better_experience.space("item/item_icon");
    public ModItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, Better_experience.MODID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        List<DeferredRegister.Items> ALL = new ArrayList<>();
        ALL.add(ModItems.ITEMS);

        ALL.forEach(registry -> registry.getEntries().forEach(item -> {
            String path = item.getId().getPath().toLowerCase();
            try {
                withExistingParent("item/"+path, "item/generated").texture("layer0", Better_experience.space("item/"+path));
            }catch (Exception e){
                withExistingParent("item/"+path,MISSING_ITEM);
            }
        }));

        List<DeferredRegister.Items> TOOLS = new ArrayList<>();
        TOOLS.add(ModItems.TOOLS);

        TOOLS.forEach(registry -> registry.getEntries().forEach(item -> {
            String path = item.getId().getPath().toLowerCase();
            try {
                String texture = path.equals("space_staff") ? "star_boom_staff" : path;
                withExistingParent("item/"+path, "item/handheld").texture("layer0", Better_experience.space("item/"+texture));
                }
            catch (Exception e){
                withExistingParent("item/"+path, MISSING_ITEM);
            }
        }));

    }
}
