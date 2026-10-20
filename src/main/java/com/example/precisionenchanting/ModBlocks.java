package com.example.precisionenchanting;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public final class ModBlocks {
	public static final Identifier TABLE_ID = PrecisionEnchanting.id("precision_enchanting_table");

	public static Block TABLE;
	public static Item TABLE_ITEM;

	/** Set by the client entrypoint; opens the GUI. Does nothing on a dedicated server. */
	public static Runnable openScreen = () -> {};

	public static void register() {
		ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, TABLE_ID);
		TABLE = Registry.register(BuiltInRegistries.BLOCK, blockKey,
				new PrecisionEnchantingTableBlock(BlockBehaviour.Properties.of()
						.setId(blockKey)
						.mapColor(MapColor.COLOR_PURPLE)
						.requiresCorrectToolForDrops()
						.strength(5.0F, 1200.0F)
						.lightLevel(state -> 7)));

		ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, TABLE_ID);
		TABLE_ITEM = Registry.register(BuiltInRegistries.ITEM, itemKey,
				new BlockItem(TABLE, new Item.Properties().setId(itemKey).useBlockDescriptionPrefix()));

		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS)
				.register(entries -> entries.accept(TABLE_ITEM));
	}

	private ModBlocks() {}
}
