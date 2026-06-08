package com.example.util;

import net.fabricmc.loader.api.FabricLoader;
import java.lang.reflect.Field;

public class VivecraftHelper {
    private static final boolean IS_VIVECRAFT_LOADED = FabricLoader.getInstance().isModLoaded("vivecraft");

    public static boolean isVRRunning() {
        if (!IS_VIVECRAFT_LOADED) return false;
        try {
            Class<?> vrStateClass = Class.forName("org.vivecraft.client_vr.VRState");
            Field vrRunningField = vrStateClass.getDeclaredField("VR_RUNNING");
            return vrRunningField.getBoolean(null);
        } catch (Exception e) {
            return false;
        }
    }
}