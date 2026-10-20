package com.example.precisionenchanting.client;

import com.example.precisionenchanting.ModBlocks;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.Minecraft;

public class PrecisionEnchantingClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ModBlocks.openScreen = () -> Minecraft.getInstance().setScreen(new PrecisionEnchantScreen());
	}
}
