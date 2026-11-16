package com.github.edg_thexu.better_experience.mixin;

import net.neoforged.fml.loading.FMLLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class OptionalMixinPlugin implements IMixinConfigPlugin {
	private String integrationPackage;
	private Set<String> loadedMods = Set.of();

	@Override
	public void onLoad(String mixinPackage) {
		integrationPackage = mixinPackage + ".integration.";
		// The runtime ModList is not initialized during mixin discovery.
		loadedMods = FMLLoader.getLoadingModList().getMods().stream()
				.map(mod -> mod.getModId())
				.collect(Collectors.toUnmodifiableSet());
	}

	@Override
	public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
		if (mixinClassName.startsWith(integrationPackage + "confluence.")) {
			return loadedMods.contains("confluence");
		}
		if (mixinClassName.startsWith(integrationPackage + "jei.")) {
			return loadedMods.contains("jei");
		}
		if (mixinClassName.startsWith(integrationPackage + "terra_gun.")) {
			return loadedMods.contains("terra_guns");
		}
		return true;
	}

	@Override
	public String getRefMapperConfig() {
		return null;
	}

	@Override
	public List<String> getMixins() {
		return null;
	}

	@Override
	public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}

	@Override
	public void preApply(
			String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixin) {}

	@Override
	public void postApply(
			String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixin) {}
}
