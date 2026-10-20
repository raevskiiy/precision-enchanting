package com.example.precisionenchanting;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.Identifier;

public class PrecisionEnchanting implements ModInitializer {
	public static final String MOD_ID = "precision_enchanting";

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		ModBlocks.register();
		PayloadTypeRegistry.serverboundPlay().register(ApplyEnchantsPayload.TYPE, ApplyEnchantsPayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(ApplyEnchantsPayload.TYPE,
				(payload, context) -> ServerHandler.handle(context.player(), payload));
	}
}
