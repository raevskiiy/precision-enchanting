package com.example.precisionenchanting;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client -> server: wanted enchantment levels (only the ones that differ from the item). */
public record ApplyEnchantsPayload(List<Entry> entries) implements CustomPacketPayload {
	public record Entry(String id, int level) {}

	public static final CustomPacketPayload.Type<ApplyEnchantsPayload> TYPE =
			new CustomPacketPayload.Type<>(PrecisionEnchanting.id("apply"));

	public static final StreamCodec<FriendlyByteBuf, ApplyEnchantsPayload> CODEC =
			StreamCodec.of(ApplyEnchantsPayload::write, ApplyEnchantsPayload::read);

	private static void write(FriendlyByteBuf buf, ApplyEnchantsPayload p) {
		buf.writeVarInt(p.entries.size());
		for (Entry e : p.entries) {
			buf.writeUtf(e.id());
			buf.writeVarInt(e.level());
		}
	}

	private static ApplyEnchantsPayload read(FriendlyByteBuf buf) {
		int n = buf.readVarInt();
		if (n < 0 || n > 256) throw new IllegalArgumentException("Too many entries: " + n);
		List<Entry> list = new ArrayList<>(n);
		for (int i = 0; i < n; i++) {
			list.add(new Entry(buf.readUtf(256), buf.readVarInt()));
		}
		return new ApplyEnchantsPayload(list);
	}

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
