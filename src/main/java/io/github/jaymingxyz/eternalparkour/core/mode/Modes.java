package io.github.jaymingxyz.eternalparkour.core.mode;

import io.github.jaymingxyz.eternalparkour.core.api.Registry;

public class Modes {

    public static DefaultMode DEFAULT;
    public static SpectatorMode SPECTATOR;

    public static void init() {
        DEFAULT = (DefaultMode) Registry.getMode("default");
        SPECTATOR = (SpectatorMode) Registry.getMode("spectator");
    }
}