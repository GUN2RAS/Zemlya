package com.example.util;

import net.minecraft.world.level.block.state.BlockBehaviour;
import java.lang.reflect.Method;

public class PrintBlockMethods {
    public static void main(String[] args) {
        for (Method m : BlockBehaviour.class.getDeclaredMethods()) {
            if (m.getName().toLowerCase().contains("collision") || m.getName().toLowerCase().contains("step")) {
                System.out.println(m.getName() + ": ");
                for (Class<?> p : m.getParameterTypes()) {
                    System.out.println("  " + p.getName());
                }
            }
        }
    }
}
