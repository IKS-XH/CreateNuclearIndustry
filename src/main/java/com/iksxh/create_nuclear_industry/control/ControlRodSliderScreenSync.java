package com.iksxh.create_nuclear_industry.control;

import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBehaviour.ValueSettings;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec2;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** Keeps an already-open Create value panel aligned with a server response. */
public final class ControlRodSliderScreenSync {
    private static final Field POS = field("pos");
    private static final Field INITIAL_SETTINGS = field("initialSettings");
    private static final Field LAST_HOVERED = field("lastHovered");
    private static final Method SET_CURSOR = method("setCursor", Vec2.class);

    private ControlRodSliderScreenSync() {
    }

    public static void refresh(BlockPos drivePos, int depthPercent) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!(minecraft.screen instanceof ValueSettingsScreen screen)
                || drivePos == null
                || !drivePos.equals(read(POS, screen))) {
            return;
        }

        int clamped = Math.max(0, Math.min(100, depthPercent));
        ValueSettings settings = new ValueSettings(0, clamped);
        write(INITIAL_SETTINGS, screen, settings);
        write(LAST_HOVERED, screen, settings);

        if (SET_CURSOR != null) {
            try {
                SET_CURSOR.invoke(screen, screen.getCoordinateOfValue(0, clamped));
            } catch (ReflectiveOperationException ignored) {
                // The panel still receives the value; cursor movement is only visual polish.
            }
        }
    }

    private static Object read(Field field, Object instance) {
        if (field == null) {
            return null;
        }
        try {
            return field.get(instance);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static void write(Field field, Object instance, Object value) {
        if (field == null) {
            return;
        }
        try {
            field.set(instance, value);
        } catch (ReflectiveOperationException ignored) {
            // Create does not expose a public refresh API on this screen.
        }
    }

    private static Field field(String name) {
        try {
            Field field = ValueSettingsScreen.class.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException | SecurityException ignored) {
            return null;
        }
    }

    private static Method method(String name, Class<?>... parameterTypes) {
        try {
            Method method = ValueSettingsScreen.class.getDeclaredMethod(name, parameterTypes);
            method.setAccessible(true);
            return method;
        } catch (ReflectiveOperationException | SecurityException ignored) {
            return null;
        }
    }
}
