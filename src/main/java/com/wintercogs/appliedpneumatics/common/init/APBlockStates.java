package com.wintercogs.appliedpneumatics.common.init;

import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import org.jetbrains.annotations.NotNull;

public class APBlockStates
{
    public enum TemperatureState implements StringRepresentable
    {
        HIGH_TEMPERATURE("high_temperature"),
        LOW_TEMPERATURE("low_temperature"),
        ROOM_TEMPERATURE("room_temperature");

        private final String name;

        TemperatureState(String name)
        {
            this.name = name;
        }

        @Override
        public @NotNull String getSerializedName()
        {
            return name;
        }
    }

    public static final EnumProperty<TemperatureState> TEMP_STATE = EnumProperty.create("temperature_state", TemperatureState.class);
}
