package com.example;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

import com.example.client.CameraShakeTracker;
import com.example.client.render.RiftEntityRenderer;
// import com.example.client.render.RiftMinionEntityRenderer;
import com.example.client.render.RiftLightningEntityRenderer;
import com.example.entity.ModEntities;
// import com.example.network.RiftShakePayload;

public class ExampleModClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// com.example.network.ClientCitadelReceiver.init();
		EntityRendererRegistry.register(ModEntities.RIFT, RiftEntityRenderer::new);
		// EntityRendererRegistry.register(ModEntities.RIFT_MINION, RiftMinionEntityRenderer::new);
		EntityRendererRegistry.register(ModEntities.RIFT_LIGHTNING, RiftLightningEntityRenderer::new);

		net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(client -> {
			// com.example.world.EndIslandVisualManager.clientTick(isEnd);
		});

		// ClientPlayNetworking.registerGlobalReceiver(RiftShakePayload.ID, (payload, context) -> {
		// 	context.client().execute(() -> {
		// 		CameraShakeTracker.isShaking = payload.start();
		// 	});
		// });

	}
}
