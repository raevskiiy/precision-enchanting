package com.example.precisionenchanting;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class PrecisionEnchantingTableBlock extends Block {
	public static final MapCodec<PrecisionEnchantingTableBlock> CODEC = simpleCodec(PrecisionEnchantingTableBlock::new);

	public PrecisionEnchantingTableBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected MapCodec<? extends Block> codec() {
		return CODEC;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (level.isClientSide()) {
			ModBlocks.openScreen.run();
		}
		return InteractionResult.SUCCESS;
	}
}
