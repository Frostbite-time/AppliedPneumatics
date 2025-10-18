package com.wintercogs.appliedpneumatics.common.me;

import appeng.api.parts.IPart;
import appeng.api.parts.IPartHost;
import appeng.parts.automation.ExportBusPart;
import appeng.parts.automation.ImportBusPart;
import appeng.parts.storagebus.StorageBusPart;
import com.wintercogs.appliedpneumatics.AppliedPneumatics;
import it.unimi.dsi.fastutil.floats.FloatPredicate;
import me.desht.pneumaticcraft.api.PNCCapabilities;
import me.desht.pneumaticcraft.api.tileentity.IAirHandlerMachine;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.List;
import java.util.Locale;

@Mod.EventBusSubscriber(modid = AppliedPneumatics.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class APPartCapabilities
{
    private static final ResourceLocation KEY = AppliedPneumatics.makeId("air_handler_part_proxy");

    @SubscribeEvent
    public static void registerPartCaps(AttachCapabilitiesEvent<BlockEntity> event)
    {
        BlockEntity be = event.getObject();
        if (!(be instanceof IPartHost host)) return;

        AirHandlerProvider provider = new AirHandlerProvider(host);

        event.addCapability(KEY, provider);
        event.addListener(provider::invalidateAll);
    }

    private static final class AirHandlerProvider implements ICapabilityProvider
    {
        private final IPartHost host;
        private final EnumMap<Direction, LazyOptional<IAirHandlerMachine>> bySide = new EnumMap<>(Direction.class);

        public AirHandlerProvider(IPartHost host)
        {
            this.host = host;
        }

        @Override
        public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side)
        {
            if (cap != PNCCapabilities.AIR_HANDLER_MACHINE_CAPABILITY || side == null) return LazyOptional.empty();

            IPart part = host.getPart(side);
            boolean match = part instanceof ExportBusPart
                    || part instanceof ImportBusPart
                    || part instanceof StorageBusPart;

            // 这里，如果匹配，则返回map找到的值
            // 如果不匹配，则从map移除，并失效化
            if (match)
            {
                LazyOptional<IAirHandlerMachine> opt = bySide.get(side);
                if (opt == null || !opt.isPresent())
                {
                    opt = LazyOptional.of(EmptyAirHandlerMachine::new);
                    bySide.put(side, opt);
                }
                return opt.cast();
            }
            else
            {
                LazyOptional<IAirHandlerMachine> old = bySide.remove(side);
                if (old != null) old.invalidate();
                return LazyOptional.empty();
            }
        }

        void invalidateAll()
        {
            bySide.values().forEach(LazyOptional::invalidate);
            bySide.clear();
        }
    }


    // 允许对方进行连接，自身不处理任何操作、返回极大气压确保外界不会对此处推送气体
    public static class EmptyAirHandlerMachine implements IAirHandlerMachine
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
        public void setConnectedFaces(List<Direction> list)
        {
        }

        @Override
        public CompoundTag serializeNBT()
        {
            return new CompoundTag();
        }

        @Override
        public void deserializeNBT(CompoundTag tag)
        {
        }

        @Override
        public void printManometerMessage(Player p, List<Component> curInfo)
        {
            curInfo.add(Component.translatable("appliedpneumatics.cur.tooltip.nothing", String.format(Locale.ROOT, "%.2f", getPressure())));
        }
    }
}
