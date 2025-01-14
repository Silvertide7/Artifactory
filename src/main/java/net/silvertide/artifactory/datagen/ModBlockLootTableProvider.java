package net.silvertide.artifactory.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.silvertide.artifactory.registry.BlockRegistry;
import net.minecraft.core.Holder;

import java.util.Set;

public class ModBlockLootTableProvider extends BlockLootSubProvider {
    protected ModBlockLootTableProvider(HolderLookup.Provider registries) {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
    }

    @Override
    protected void generate() {
        dropSelf(BlockRegistry.ATTUNEMENT_NEXUS_BLOCK.get());
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return BlockRegistry.blocks().stream().map(Holder::value)::iterator;
    }
}

//public class ModBlockLootTables extends BlockLootSubProvider {
//    public ModBlockLootTables() {
//        super(Set.of(), FeatureFlags.REGISTRY.allFlags());
//    }
//
//    @Override
//    protected void generate() {
//        this.dropSelf(BlockRegistry.ATTUNEMENT_NEXUS_BLOCK.get());
//    }
//
//    @Override
//    protected Iterable<Block> getKnownBlocks() {
//        return BlockRegistry.blocks().stream().map(DeferredHolder::get)::iterator;
//    }
//}
