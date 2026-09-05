package com.wintercogs.appliedpneumatics.common.blocks.entitis;

import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.networking.GridFlags;
import appeng.api.networking.IGrid;
import appeng.api.networking.energy.IEnergyService;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.storage.IStorageService;
import appeng.api.storage.MEStorage;
import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.IUpgradeableObject;
import appeng.api.upgrades.UpgradeInventories;
import appeng.blockentity.ServerTickingBlockEntity;
import appeng.blockentity.grid.AENetworkBlockEntity;
import appeng.core.definitions.AEItems;
import appeng.util.SettingsFrom;
import com.wintercogs.appliedpneumatics.common.init.APBlockEntities;
import com.wintercogs.appliedpneumatics.common.init.APBlockStates;
import com.wintercogs.appliedpneumatics.common.init.APBlocks;
import com.wintercogs.appliedpneumatics.common.init.APItems;
import com.wintercogs.appliedpneumatics.common.me.keys.AirKey;
import me.desht.pneumaticcraft.api.PNCCapabilities;
import me.desht.pneumaticcraft.api.heat.IHeatExchangerLogic;
import me.desht.pneumaticcraft.common.heat.HeatExchangerManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;

/**
 * 注意，这里所有的温度单位都是开尔文，而不是摄氏度
 */
public class METemperatureInterfaceBlockEntity extends AENetworkBlockEntity implements IUpgradeableObject,
        ServerTickingBlockEntity
{
    private static final int SOFT_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;

    private final IUpgradeInventory upgrades = UpgradeInventories.forMachine(APBlocks.ME_TEMPERATURE_INTERFACE.get(), 5, this::onUpgradesChanged);

    // 温度接口----------------------------------------------------------------------------------
    private static final int BASE_HEAT_CAP = 1000; // 无任何升级下的基础热容
    // 以下两数值为使温控接口维持极端温度时消耗0.25L/t空气以及0.65KFE/t的数值
    private static final int AIR_COST_PER_1000J = 19145; // 每改变1000J热量所需要的空气量
    private static final int AE_ENERGY_COST_PER_1000J = 24889; // 每改变1000J热量所需要的AE能量

    private double expectedTemperature = 27f + 273;
    private final IHeatExchangerLogic heatHandler = HeatExchangerManager.getInstance().makeHeatExchangerLogic();

    private int maxTemperatureChangePerTick = 1; // 每tick与ME系统交互时，最大温度改变量
    private double lastTemperature = 0;

    // 能力缓存
    LazyOptional<IHeatExchangerLogic> heatOpt = LazyOptional.empty();


    public METemperatureInterfaceBlockEntity(BlockPos pos, BlockState blockState)
    {
        super(APBlockEntities.ME_TEMPERATURE_INTERFACE_BLOCK_ENTITY.get(), pos, blockState);

        getMainNode().setIdlePowerUsage(8.0) // 待机消耗
                .setFlags(GridFlags.REQUIRE_CHANNEL) // 需要频道
                .setExposedOnSides(EnumSet.allOf(Direction.class)); // 可以用于连接的方向
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side)
    {
        if (cap == PNCCapabilities.HEAT_EXCHANGER_CAPABILITY)
        {
            if (!heatOpt.isPresent())
                heatOpt = LazyOptional.of(() -> heatHandler);
            return heatOpt.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps()
    {
        super.invalidateCaps();

        if (heatOpt.isPresent()) heatOpt.invalidate();
        heatOpt = LazyOptional.empty();
    }

    public IHeatExchangerLogic getHeatHandler()
    {
        return heatHandler;
    }

    public double getExpectedTemperature()
    {
        return expectedTemperature;
    }

    public void setExpectedTemperature(double expectedTemperature)
    {
        this.expectedTemperature = Math.min(2273, Math.max(0, expectedTemperature));
        setChanged();
    }

    @Override
    public void exportSettings(SettingsFrom mode, CompoundTag tag, @Nullable Player player)
    {
        super.exportSettings(mode, tag, player);
        if (mode == SettingsFrom.MEMORY_CARD)
        {
            tag.putDouble("expected_temperature", expectedTemperature);
        }
    }

    @Override
    public void importSettings(SettingsFrom mode, CompoundTag tag, @Nullable Player player)
    {
        super.importSettings(mode, tag, player);
        if (mode == SettingsFrom.MEMORY_CARD && tag.contains("expected_temperature", Tag.TAG_DOUBLE))
        {
            setExpectedTemperature(tag.getDouble("expected_temperature"));
        }
    }

    /**
     * 取当前网络的MEStorage
     */
    public @Nullable MEStorage getNetworkInventory()
    {
        IGrid grid = getMainNode().getGrid();
        if (grid == null) return null;
        IStorageService ss = grid.getStorageService();
        return ss != null ? ss.getInventory() : null;
    }

    @Override
    public IUpgradeInventory getUpgrades()
    {
        return upgrades;
    }

    @Override
    public void serverTick()
    {
        if (level == null || level.isClientSide()) return;

        if (getMainNode().isActive())
            interactWithME();

        this.heatHandler.tick();

        if (lastTemperature != this.heatHandler.getTemperature())
        {
            this.lastTemperature = this.heatHandler.getTemperature();
            setChanged();

            // 顺便更新状态
            BlockState state = getBlockState();
            if (lastTemperature > 200 + 273)
                level.setBlock(worldPosition, state.setValue(APBlockStates.TEMP_STATE, APBlockStates.TemperatureState.HIGH_TEMPERATURE), SOFT_FLAGS);
            else if (lastTemperature < -100 + 273)
                level.setBlock(worldPosition, state.setValue(APBlockStates.TEMP_STATE, APBlockStates.TemperatureState.LOW_TEMPERATURE), SOFT_FLAGS);
            else
                level.setBlock(worldPosition, state.setValue(APBlockStates.TEMP_STATE, APBlockStates.TemperatureState.ROOM_TEMPERATURE), SOFT_FLAGS);
        }
    }

    private void interactWithME()
    {
        // 同时消耗空气与能量，尝试将温度改变到期望值，但每次运行的最大改变量不超过maxTemperatureChangePerTick
        MEStorage storage = getNetworkInventory();
        if (storage == null) return;
        double currentTemperature = this.heatHandler.getTemperature();
        if (this.expectedTemperature == currentTemperature) return;

        // 限幅后的目标温差（本 tick 至多改变 maxTemperatureChangePerTick）
        double diff = this.expectedTemperature - currentTemperature;
        double sign = Math.signum(diff); // 决定是增加还是减少
        double deltaWantedTemperature = Math.min(Math.abs(diff), this.maxTemperatureChangePerTick);

        // 需求热量 Q（焦耳）
        double capacity = Math.max(0.0, this.heatHandler.getThermalCapacity());
        if (capacity < 0.1) return; // 小热容时不处理
        double qWanted = deltaWantedTemperature * capacity; // J

        // 能量服务（AE 电力）
        IGrid grid = getMainNode().getGrid();
        if (grid == null) return;
        IEnergyService energyService = grid.getEnergyService();
        if (energyService == null) return;

        IActionSource actionSrc = IActionSource.ofMachine(this);

        // 试探网络中可用的“空气”与“AE能量”
        long airAvailable = storage.extract(AirKey.INSTANCE, Long.MAX_VALUE, Actionable.SIMULATE, actionSrc);
        double energyAvailable = energyService.getStoredPower();

        // 把“可用资源”换算为各自能提供的最大热量
        double qMaxByAir = (airAvailable <= 0) ? 0.0 : (airAvailable * 1000.0) / (double) AIR_COST_PER_1000J;
        double qMaxByAE = (energyAvailable <= 0.0) ? 0.0 : (energyAvailable * 1000.0) / (double) AE_ENERGY_COST_PER_1000J;

        // 实际可用的热量
        double qApply = Math.min(qWanted, Math.min(qMaxByAir, qMaxByAE));
        if (qApply <= 0.0) return;

        // 执行实际消耗（MODULATE）这里按向上取整避免热量不足，再与可用量取 min 以确保不超提
        long airToUse = (long) Math.min(airAvailable, Math.ceil(qApply / 1000.0 * (double) AIR_COST_PER_1000J));
        double energyToUse = Math.min(energyAvailable, (qApply / 1000.0) * (double) AE_ENERGY_COST_PER_1000J);

        long airExtracted = storage.extract(AirKey.INSTANCE, airToUse, Actionable.MODULATE, actionSrc);
        double energyExtracted = energyService.extractAEPower(energyToUse, Actionable.MODULATE, PowerMultiplier.CONFIG);

        // 根据实际消耗资源再计算本次可更改的热量
        double qByAir = (airExtracted <= 0) ? 0.0 : (airExtracted * 1000.0) / (double) AIR_COST_PER_1000J;
        double qByAE = (energyExtracted <= 0.0) ? 0.0 : (energyExtracted * 1000.0) / (double) AE_ENERGY_COST_PER_1000J;

        double qReal = Math.min(qApply, Math.min(qByAir, qByAE));
        if (qReal <= 0.0) return;

        // 施加热量（按符号方向），内部会做温度范围钳制
        this.heatHandler.addHeat(sign * qReal);
    }

    @Override
    public void loadTag(CompoundTag tag)
    {
        super.loadTag(tag);
        this.upgrades.readFromNBT(tag, "upgrades");
        this.heatHandler.deserializeNBT(tag.getCompound("heat_handler"));
        this.expectedTemperature = tag.getDouble("expected_temperature");
    }

    // load完成之后，且level被注入后
    @Override
    public void onLoad()
    {
        super.onLoad();
        onUpgradesChanged(); // 加载后刷新一次升级状态
        this.heatHandler.initializeAsHull(level, worldPosition, IHeatExchangerLogic.ALL_BLOCKS, Direction.values());
    }

    @Override
    public void saveAdditional(CompoundTag tag)
    {
        super.saveAdditional(tag);
        this.upgrades.writeToNBT(tag, "upgrades");
        tag.put("heat_handler", this.heatHandler.serializeNBT());
        tag.putDouble("expected_temperature", this.expectedTemperature);
    }

    private void onUpgradesChanged()
    {
        // 每tick最大温度更改量为2的n次方，n为加速卡数量
        int speedCards = this.upgrades.getInstalledUpgrades(AEItems.SPEED_CARD);
        this.maxTemperatureChangePerTick = (speedCards <= 0) ? 1 : (1 << speedCards);
        // 热容为基础热容值*2的n次方，n为容积卡数量
        int volume_cards = this.upgrades.getInstalledUpgrades(APItems.VOLUME_CARD.get());
        int mul = (volume_cards <= 0) ? 1 : (1 << volume_cards);
        this.heatHandler.setThermalCapacity(BASE_HEAT_CAP * (double) mul);
        interactWithME(); // 应用升级后立刻与ME系统进行一次交互
        setChanged();
    }

    @Override
    public void addAdditionalDrops(Level level, BlockPos pos, List<ItemStack> drops)
    {
        super.addAdditionalDrops(level, pos, drops);
        for (int i = 0; i < upgrades.size(); i++)
        {
            ItemStack slotContent = upgrades.getStackInSlot(i);
            if (slotContent.isEmpty()) continue;
            drops.add(slotContent.copy());
        }
    }

    @Override
    public void clearContent()
    {
        super.clearContent();
        upgrades.clear();
    }
}
