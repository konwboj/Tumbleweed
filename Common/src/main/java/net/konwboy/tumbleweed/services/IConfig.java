package net.konwboy.tumbleweed.services;

public interface IConfig {

    boolean enableDrops();
    double spawnChance();
    int maxPerPlayer();
    boolean damageCrops();
    boolean dropOnlyByPlayer();

}
