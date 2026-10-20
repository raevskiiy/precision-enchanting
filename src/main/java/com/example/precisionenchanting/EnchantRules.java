package com.example.precisionenchanting;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;

/**
 * Shared (client + server) rules: which enchantments are offered, level caps and XP cost.
 * Tweak the constants below to rebalance the mod.
 */
public final class EnchantRules {
	/** XP cost = (vanilla "min power" of the level / POWER_DIVISOR) levels. Lower = cheaper. */
	public static final double POWER_DIVISOR = 4.0;
	/** +10% cost per anvil-cost point above 1 (rare enchantments cost more). */
	public static final double RARITY_STEP = 0.10;
	/** Extra multiplier per level above the vanilla maximum (level 6 sharpness = x1.35, 7 = x1.70 ...). */
	public static final double OVERCAP_STEP = 0.35;
	/** Share of an enchantment's cost refunded when it is removed or lowered (0.5 = 50%). Keep below 1.0 to prevent XP farming. */
	public static final double REFUND_RATE = 0.5;
	/** +5% total cost per enchantment on the item beyond the 3rd. */
	public static final double STACK_STEP = 0.05;

	/** Enchantments that may go beyond the vanilla maximum, and their new caps. */
	private static final Map<String, Integer> EXTENDED = Map.ofEntries(
			Map.entry("minecraft:protection", 8),
			Map.entry("minecraft:fire_protection", 8),
			Map.entry("minecraft:blast_protection", 8),
			Map.entry("minecraft:projectile_protection", 8),
			Map.entry("minecraft:feather_falling", 8),
			Map.entry("minecraft:respiration", 6),
			Map.entry("minecraft:thorns", 6),
			Map.entry("minecraft:depth_strider", 6),
			Map.entry("minecraft:frost_walker", 4),
			Map.entry("minecraft:soul_speed", 6),
			Map.entry("minecraft:swift_sneak", 6),
			Map.entry("minecraft:sharpness", 10),
			Map.entry("minecraft:smite", 10),
			Map.entry("minecraft:bane_of_arthropods", 10),
			Map.entry("minecraft:knockback", 5),
			Map.entry("minecraft:fire_aspect", 5),
			Map.entry("minecraft:looting", 8),
			Map.entry("minecraft:sweeping_edge", 8),
			Map.entry("minecraft:efficiency", 10),
			Map.entry("minecraft:unbreaking", 10),
			Map.entry("minecraft:fortune", 8),
			Map.entry("minecraft:power", 10),
			Map.entry("minecraft:punch", 5),
			Map.entry("minecraft:luck_of_the_sea", 8),
			Map.entry("minecraft:lure", 8),
			Map.entry("minecraft:loyalty", 5),
			Map.entry("minecraft:impaling", 10),
			Map.entry("minecraft:riptide", 5),
			Map.entry("minecraft:quick_charge", 5),
			Map.entry("minecraft:piercing", 8),
			Map.entry("minecraft:density", 10),
			Map.entry("minecraft:breach", 8),
			Map.entry("minecraft:wind_burst", 5),
			Map.entry("minecraft:lunge", 5));

	public static String idOf(Holder<Enchantment> h) {
		return h.getRegisteredName();
	}

	public static int vanillaMax(Holder<Enchantment> h) {
		return h.value().getMaxLevel();
	}

	public static int maxLevel(Holder<Enchantment> h) {
		return Math.max(vanillaMax(h), EXTENDED.getOrDefault(idOf(h), 0));
	}

	public static boolean isBook(ItemStack s) {
		return s.is(Items.BOOK) || s.is(Items.ENCHANTED_BOOK);
	}

	public static List<Holder<Enchantment>> applicable(RegistryAccess access, ItemStack stack) {
		boolean book = isBook(stack);
		List<Holder<Enchantment>> out = new ArrayList<>();
		access.lookupOrThrow(Registries.ENCHANTMENT).listElements().forEach(ref -> {
			if (book || ref.value().canEnchant(stack)) out.add(ref);
		});
		out.sort(Comparator.comparing(EnchantRules::idOf));
		return out;
	}

	public static Map<String, Integer> currentLevels(ItemStack stack, List<Holder<Enchantment>> list) {
		ItemEnchantments ench = EnchantmentHelper.getEnchantmentsForCrafting(stack);
		Map<String, Integer> m = new HashMap<>();
		for (Holder<Enchantment> h : list) m.put(idOf(h), ench.getLevel(h));
		return m;
	}

	/** Cost (in XP levels, fractional) of having this enchantment at this level. */
	public static double rawCost(Holder<Enchantment> h, int level) {
		if (level <= 0) return 0;
		Enchantment e = h.value();
		double power = Math.max(1, e.getMinCost(level));
		double rarity = 1.0 + RARITY_STEP * Math.max(0, e.getAnvilCost() - 1);
		int over = Math.max(0, level - vanillaMax(h));
		double overcap = 1.0 + OVERCAP_STEP * over;
		return power / POWER_DIVISOR * rarity * overcap;
	}

	/**
	 * Net XP levels to go from {@code original} to {@code target}.
	 * Positive = you pay, negative = you get levels back. Refunds from removed/lowered
	 * enchantments are first spent on new/raised ones (the balance is simply added up).
	 */
	public static int totalCost(List<Holder<Enchantment>> list, Map<String, Integer> original, Map<String, Integer> target) {
		double add = 0;
		double refund = 0;
		int count = 0;
		for (Holder<Enchantment> h : list) {
			String id = idOf(h);
			int o = original.getOrDefault(id, 0);
			int t = target.getOrDefault(id, 0);
			if (t > 0) count++;
			if (t > o) add += rawCost(h, t) - rawCost(h, o);
			else if (t < o) refund += (rawCost(h, o) - rawCost(h, t)) * REFUND_RATE;
		}
		double mult = 1.0 + STACK_STEP * Math.max(0, count - 3);
		double net = add * mult - refund;
		if (net > 0) return (int) Math.ceil(net);
		if (net < 0) return -(int) Math.floor(-net);
		return 0;
	}

	private EnchantRules() {}
}
