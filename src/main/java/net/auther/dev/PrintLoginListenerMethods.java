package net.auther.dev;

import net.minecraft.server.network.ServerLoginPacketListenerImpl;
import java.lang.reflect.Method;
import java.lang.reflect.Field;

public class PrintLoginListenerMethods {
    public static void main(String[] args) {
        System.out.println("CLASS: " + ServerLoginPacketListenerImpl.class.getName());
        System.out.println("---METHODS---");
        for (Method m : ServerLoginPacketListenerImpl.class.getDeclaredMethods()) {
            System.out.println("METHOD: " + m.getName() + " " + m.toGenericString());
        }
        System.out.println("---FIELDS---");
        for (Field f : ServerLoginPacketListenerImpl.class.getDeclaredFields()) {
            System.out.println("FIELD: " + f.getName() + " " + f.getType().getName());
        }
    }
}
