package com.example.precisionenchanting.client;

import com.example.precisionenchanting.ApplyEnchantsPayload;
import com.example.precisionenchanting.EnchantRules;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;

/**
 * Widgets-only screen (no custom rendering code), so it is resilient to GUI API changes.
 * The item being enchanted is the one in the player's main hand.
 */
public class PrecisionEnchantScreen extends Screen {
	private static final int PER_PAGE = 5;

	private boolean loaded;
	private boolean empty;
	private List<Holder<Enchantment>> list = List.of();
	private Map<String, Integer> original = new HashMap<>();
	private Map<String, Integer> selected = new HashMap<>();
	private final List<LevelSlider> sliders = new ArrayList<>();
	private int page;
	private Component note = Component.empty();

	private StringWidget infoLine;
	private StringWidget noteLine;
	private Button applyButton;

	public PrecisionEnchantScreen() {
		super(Component.translatable("screen.precision_enchanting.title"));
	}

	private void load() {
		LocalPlayer player = this.minecraft.player;
		ItemStack stack = player.getMainHandItem();
		if (stack.isEmpty() || (!EnchantRules.isBook(stack) && stack.getCount() != 1)) {
			empty = true;
			return;
		}
		list = EnchantRules.applicable(player.level().registryAccess(), stack);
		if (list.isEmpty()) { empty = true; return; }
		original = EnchantRules.currentLevels(stack, list);
		selected = new HashMap<>(original);
	}

	@Override
	protected void init() {
		if (!loaded) { loaded = true; load(); }
		sliders.clear();

		int cx = this.width / 2;
		int top = Math.max(8, this.height / 2 - 105);

		this.addRenderableWidget(centered(this.title, top));

		if (empty) {
			this.addRenderableWidget(centered(Component.translatable("screen.precision_enchanting.hold_item"), top + 30));
			this.addRenderableWidget(Button.builder(Component.translatable("screen.precision_enchanting.close"), b -> this.onClose())
					.bounds(cx - 50, top + 60, 100, 20).build());
			return;
		}

		int pages = Math.max(1, (list.size() + PER_PAGE - 1) / PER_PAGE);
		page = Math.max(0, Math.min(page, pages - 1));

		int y = top + 18;
		for (int i = page * PER_PAGE; i < Math.min(list.size(), (page + 1) * PER_PAGE); i++) {
			Holder<Enchantment> h = list.get(i);
			LevelSlider s = new LevelSlider(cx - 100, y, 200, 20, h, selected.getOrDefault(EnchantRules.idOf(h), 0), this::onSlide);
			sliders.add(s);
			this.addRenderableWidget(s);
			y += 22;
		}

		int navY = top + 18 + PER_PAGE * 22 + 2;
		Button prev = Button.builder(Component.literal("<"), b -> { page--; this.rebuildWidgets(); })
				.bounds(cx - 100, navY, 20, 20).build();
		prev.active = page > 0;
		this.addRenderableWidget(prev);
		this.addRenderableWidget(centered(Component.translatable("screen.precision_enchanting.page", page + 1, pages), navY + 5));
		Button next = Button.builder(Component.literal(">"), b -> { page++; this.rebuildWidgets(); })
				.bounds(cx + 80, navY, 20, 20).build();
		next.active = page < pages - 1;
		this.addRenderableWidget(next);

		infoLine = centered(Component.empty(), navY + 26);
		noteLine = centered(Component.empty(), navY + 40);
		this.addRenderableWidget(infoLine);
		this.addRenderableWidget(noteLine);

		applyButton = Button.builder(Component.translatable("screen.precision_enchanting.apply"), b -> apply())
				.bounds(cx - 100, navY + 56, 98, 20).build();
		this.addRenderableWidget(applyButton);
		this.addRenderableWidget(Button.builder(Component.translatable("screen.precision_enchanting.close"), b -> this.onClose())
				.bounds(cx + 2, navY + 56, 98, 20).build());

		refreshInfo();
	}

	/** Text label centered horizontally (done manually: StringWidget has no alignCenter in 26.x). */
	private StringWidget centered(Component text, int y) {
		StringWidget w = new StringWidget(0, y, 10, 12, text, this.font);
		setCentered(w, text);
		return w;
	}

	private void setCentered(StringWidget w, Component text) {
		int tw = this.font.width(text) + 2;
		w.setMessage(text);
		w.setWidth(tw);
		w.setX(this.width / 2 - tw / 2);
	}

	private void onSlide(LevelSlider slider, int level) {
		String id = slider.id;
		if (selected.getOrDefault(id, 0) == level) return;
		selected.put(id, level);

		MutableComponent removed = Component.empty();
		boolean any = false;
		if (level > 0) {
			for (Holder<Enchantment> other : list) {
				String oid = EnchantRules.idOf(other);
				if (oid.equals(id) || selected.getOrDefault(oid, 0) <= 0) continue;
				if (!Enchantment.areCompatible(slider.holder, other)) {
					selected.put(oid, 0);
					if (any) removed.append(Component.literal(", "));
					removed.append(other.value().description());
					any = true;
				}
			}
		}
		note = any ? Component.translatable("screen.precision_enchanting.removed", removed).withStyle(ChatFormatting.YELLOW)
				: Component.empty();
		for (LevelSlider s : sliders) s.setLevel(selected.getOrDefault(s.id, 0));
		refreshInfo();
	}

	private void refreshInfo() {
		LocalPlayer player = this.minecraft.player;
		int cost = EnchantRules.totalCost(list, original, selected);
		boolean creative = player.getAbilities().instabuild;
		boolean enough = creative || player.experienceLevel >= cost;
		MutableComponent c;
		if (cost < 0 && !creative) {
			c = Component.translatable("screen.precision_enchanting.refund", -cost).withStyle(ChatFormatting.GREEN);
		} else {
			c = Component.translatable("screen.precision_enchanting.cost", Math.max(cost, 0), player.experienceLevel);
			if (!enough) c = c.withStyle(ChatFormatting.RED);
		}
		setCentered(infoLine, c);
		setCentered(noteLine, note);
		applyButton.active = !selected.equals(original) && enough;
	}

	private void apply() {
		List<ApplyEnchantsPayload.Entry> entries = new ArrayList<>();
		for (Holder<Enchantment> h : list) {
			String id = EnchantRules.idOf(h);
			int t = selected.getOrDefault(id, 0);
			if (t != original.getOrDefault(id, 0)) entries.add(new ApplyEnchantsPayload.Entry(id, t));
		}
		if (!entries.isEmpty()) ClientPlayNetworking.send(new ApplyEnchantsPayload(entries));
		this.onClose();
	}

	/** Slider with discrete levels 0..max. Levels above the vanilla maximum are shown in aqua. */
	private static class LevelSlider extends AbstractSliderButton {
		final Holder<Enchantment> holder;
		final String id;
		final int max;
		private final java.util.function.BiConsumer<LevelSlider, Integer> onChange;

		LevelSlider(int x, int y, int w, int h, Holder<Enchantment> holder, int current,
					java.util.function.BiConsumer<LevelSlider, Integer> onChange) {
			super(x, y, w, h, Component.empty(), 0);
			this.holder = holder;
			this.id = EnchantRules.idOf(holder);
			this.max = Math.max(1, EnchantRules.maxLevel(holder));
			this.onChange = onChange;
			setLevel(current);
		}

		int level() {
			return (int) Math.round(this.value * max);
		}

		void setLevel(int level) {
			this.value = Math.max(0, Math.min(max, level)) / (double) max;
			updateMessage();
		}

		@Override
		protected void updateMessage() {
			if (holder == null) return;
			int lvl = level();
			MutableComponent lv = lvl == 0
					? Component.translatable("screen.precision_enchanting.off").withStyle(ChatFormatting.GRAY)
					: Component.literal(String.valueOf(lvl));
			if (lvl > EnchantRules.vanillaMax(holder)) lv = lv.withStyle(ChatFormatting.AQUA);
			setMessage(Component.empty().append(holder.value().description()).append(Component.literal(": ")).append(lv));
		}

		@Override
		protected void applyValue() {
			int lvl = level();
			this.value = lvl / (double) max;
			onChange.accept(this, lvl);
		}
	}
}
