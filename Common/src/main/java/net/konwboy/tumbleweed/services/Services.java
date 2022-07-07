package net.konwboy.tumbleweed.services;

import java.util.ServiceLoader;

public class Services {
	public static final IConfig CONFIG = load(IConfig.class);
	public static final INetwork NETWORK = load(INetwork.class);

	public static <T> T load(Class<T> clazz) {
		return ServiceLoader.load(clazz)
				.findFirst()
				.orElseThrow(() -> new NullPointerException("Failed to load service for " + clazz.getName()));
	}
}
