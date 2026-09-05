package com.wintercogs.appliedpneumatics.common.me;

import appeng.api.parts.RegisterPartCapabilitiesEvent;
import appeng.parts.automation.ExportBusPart;
import appeng.parts.automation.ImportBusPart;
import appeng.parts.storagebus.StorageBusPart;
import com.wintercogs.appliedpneumatics.AppliedPneumatics;
import com.wintercogs.appliedpneumatics.common.me.p2p.AirP2PTunnelPart;
import com.wintercogs.appliedpneumatics.common.me.p2p.HeatP2PTunnelPart;
import it.unimi.dsi.fastutil.floats.FloatPredicate;
import me.desht.pneumaticcraft.api.PNCCapabilities;
import me.desht.pneumaticcraft.api.tileentity.IAirHandlerMachine;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

@EventBusSubscriber(modid = AppliedPneumatics.MODID)
public class APPartCapabilities
{
    @SubscribeEvent
    public static void registerPartCaps(RegisterPartCapabilitiesEvent event)
    {
        event.register(
                PNCCapabilities.AIR_HANDLER_MACHINE,
                (part, side) -> part.getExposedApi(),
                AirP2PTunnelPart.class
        );

        event.register(
                PNCCapabilities.HEAT_EXCHANGER_BLOCK,
                (part, direction) -> part.getExposedApi(),
                HeatP2PTunnelPart.class
        );

        event.register(
                PNCCapabilities.AIR_HANDLER_MACHINE,
                (part, direction) -> new EmptyAirHandlerMachine(),
                ExportBusPart.class
        );

        event.register(
                PNCCapabilities.AIR_HANDLER_MACHINE,
                (part, direction) -> new EmptyAirHandlerMachine(),
                ImportBusPart.class
        );

        event.register(
                PNCCapabilities.AIR_HANDLER_MACHINE,
                (part, direction) -> new EmptyAirHandlerMachine(),
                StorageBusPart.class
        );
    }

    // 允许对方进行连接，自身不处理任何操作、返回极大气压确保外界不会对此处推送气体
    private static class EmptyAirHandlerMachine implements IAirHandlerMachine
    {
        // 输出端仅展示能力用于连接，返回50气压，防止邻居推气体
        @Override
        public float getPressure()
        {
            return 50f; // 绝对够了，创造压缩机都只能25压强
        }

        // 实际气体注入已经在输入端解决
        @Override
        public void addAir(int ml)
        {
        }

        @Override
        public int getBaseVolume()
        {
            return 1;
        }

        @Override
        public void setBaseVolume(int i)
        {
        }

        @Override
        public int getAir()
        {
            return 1;
        }

        @Override
        public int getVolume()
        {
            return 1;
        }

        @Override
        public float maxPressure()
        {
            return 0;
        }

        @Override
        public float getDangerPressure()
        {
            return Float.MAX_VALUE;
        }

        @Override
        public float getCriticalPressure()
        {
            return Float.MAX_VALUE;
        }

        @Override
        public void setPressure(float p)
        { /* no-op */ }

        @Override
        public void setVolumeUpgrades(int v)
        { /* no-op */ }

        @Override
        public void enableSafetyVenting(FloatPredicate c, Direction d)
        { /* no-op */ }

        @Override
        public void disableSafetyVenting()
        { /* no-op */ }

        @Override
        public void tick(BlockEntity ownerTE)
        { /* no-op */ }

        @Override
        public void setSideLeaking(@Nullable Direction dir)
        { /* no-op */ }

        @Override
        public @Nullable Direction getSideLeaking()
        {
            return null;
        }

        @Override
        public List<Connection> getConnectedAirHandlers(BlockEntity ownerTE)
        {
            return List.of();
        }

        @Override
        public void setConnectableFaces(Collection<Direction> sides)
        { /* no-op */ }

        @Override
        public Tag serializeNBT()
        {
            return new CompoundTag();
        }

        @Override
        public void deserializeNBT(CompoundTag tag)
        {
        }

        @Override
        public void addPendingAir(int pendingAir)
        { /* no-op */ }

        @Override
        public void printManometerMessage(Player p, List<Component> curInfo)
        {
            curInfo.add(Component.translatable("appliedpneumatics.cur.tooltip.nothing", String.format(Locale.ROOT, "%.2f", getPressure())));
        }
    }
}
