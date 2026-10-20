package com.example.precisionenchanting;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

/** Authoritative server-side validation and application. The client is never trusted. */
public final class ServerHandler {
	private static final int REACH_H = 6;
	private static final int REACH_V = 4;

	public static void handle(ServerPlayer player, ApplyEnchantsPayload payload) {
		if (!tableNearby(player)) { fail(player, "no_table"); return; }

		ItemStack stack = player.getMainHandItem();
		if (stack.isEmpty()) { fail(player, "no_item"); return; }
		boolean book = EnchantRules.isBook(stack);
		if (!book && stack.getCount() != 1) { fail(player, "no_item"); return; }

		List<Holder<Enchantment>> list = EnchantRules.applicable(player.level().registryAccess(), stack);
		Map<String, Holder<Enchantment>> byId = new HashMap<>();
		for (Holder<Enchantment> h : list) byId.put(EnchantRules.idOf(h), h);

		Map<String, Integer> original = EnchantRules.currentLevels(stack, list);
		Map<String, Integer> target = new HashMap<>(original);

		for (ApplyEnchantsPayload.Entry e : payload.entries()) {
			Holder<Enchantment> h = byId.get(e.id());
			if (h == null || e.level() < 0 || e.level() > EnchantRules.maxLevel(h)) { fail(player, "invalid"); return; }
			target.put(e.id(), e.level());
		}

		// Incompatible combinations (only checked for enchantments being raised)
		for (Holder<Enchantment> h : list) {
			String id = EnchantRules.idOf(h);
			if (target.getOrDefault(id, 0) <= original.getOrDefault(id, 0)) continue;
			for (Holder<Enchantment> other : list) {
				if (other == h || target.getOrDefault(EnchantRules.idOf(other), 0) <= 0) continue;
				if (!Enchantment.areCompatible(h, other)) { fail(player, "conflict"); return; }
			}
		}

		boolean changed = false;
		boolean anyPositive = false;
		for (Holder<Enchantment> h : list) {
			String id = EnchantRules.idOf(h);
			if (!target.getOrDefault(id, 0).equals(original.getOrDefault(id, 0))) changed = true;
			if (target.getOrDefault(id, 0) > 0) anyPositive = true;
		}
		if (!changed || (stack.is(Items.BOOK) && !anyPositive)) { fail(player, "nothing"); return; }

		int cost = EnchantRules.totalCost(list, original, target);
		boolean creative = player.getAbilities().instabuild;
		if (!creative && player.experienceLevel < cost) { fail(player, "no_xp"); return; }

		ItemStack result = stack.is(Items.BOOK) ? stack.transmuteCopy(Items.ENCHANTED_BOOK, 1) : stack;
		EnchantmentHelper.updateEnchantments(result, mutable -> {
			for (Holder<Enchantment> h : list) {
				String id = EnchantRules.idOf(h);
				int t = target.getOrDefault(id, 0);
				if (t != original.getOrDefault(id, 0)) mutable.set(h, t);
			}
		});

		if (stack.is(Items.BOOK)) {
			stack.shrink(1);
			if (stack.isEmpty()) {
				player.setItemInHand(InteractionHand.MAIN_HAND, result);
			} else if (!player.getInventory().add(result)) {
				net.minecraft.world.level.block.Block.popResource(player.level(), player.blockPosition(), result);
			}
		}

		if (!creative && cost != 0) player.giveExperienceLevels(-cost);
		player.level().playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
		if (!creative && cost < 0) {
			player.sendSystemMessage(Component.translatable("msg.precision_enchanting.refunded", -cost), true);
		} else {
			player.sendSystemMessage(Component.translatable("msg.precision_enchanting.success", Math.max(cost, 0)), true);
		}
	}

	private static boolean tableNearby(ServerPlayer player) {
		BlockPos p = player.blockPosition();
		for (BlockPos pos : BlockPos.betweenClosed(p.offset(-REACH_H, -REACH_V, -REACH_H), p.offset(REACH_H, REACH_V, REACH_H))) {
			if (player.level().getBlockState(pos).is(ModBlocks.TABLE)) return true;
		}
		return false;
	}

	private static void fail(ServerPlayer player, String key) {
		player.sendSystemMessage(Component.translatable("msg.precision_enchanting." + key), true);
	}

	private ServerHandler() {}
}
