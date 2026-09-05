package com.wintercogs.appliedpneumatics.common.blocks.entitis;

import appeng.api.AECapabilities;
import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.inventories.InternalInventory;
import appeng.api.networking.GridFlags;
import appeng.api.networking.IGrid;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.storage.IStorageService;
import appeng.api.storage.MEStorage;
import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.IUpgradeableObject;
import appeng.api.upgrades.UpgradeInventories;
import appeng.blockentity.ServerTickingBlockEntity;
import appeng.blockentity.grid.AENetworkedBlockEntity;
import appeng.util.SettingsFrom;
import appeng.util.inv.AppEngInternalInventory;
import appeng.util.inv.filter.IAEItemFilter;
import com.wintercogs.appliedpneumatics.common.init.APBlockEntities;
import com.wintercogs.appliedpneumatics.common.init.APBlocks;
import com.wintercogs.appliedpneumatics.common.init.APDataComponents;
import com.wintercogs.appliedpneumatics.common.init.APItems;
import com.wintercogs.appliedpneumatics.common.me.keys.AirKey;
import me.desht.pneumaticcraft.api.PNCCapabilities;
import me.desht.pneumaticcraft.api.tileentity.IAirHandler;
import me.desht.pneumaticcraft.api.tileentity.IAirHandlerMachine;
import me.desht.pneumaticcraft.api.tileentity.IAirListener;
import me.desht.pneumaticcraft.common.pressure.AirHandlerMachineFactory;
import me.desht.pneumaticcraft.common.util.PneumaticCraftUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;

/**
 * ME气压接口，作为ME网络与气动工艺的最佳通道使用
 */
public class MEPressureInterfaceBlockEntity extends AENetworkedBlockEntity implements IAirListener,
        IUpgradeableObject, ServerTickingBlockEntity
{

    /**
     * 与ME系统每交互1ml空气消耗1.25AE能量，期望气压非负时，此消耗为十分之一。
     * 这是为了保证使用其产生压缩气体时，能耗为满配通量压缩机的一半左右
     */
    private static final double AE_ENERGY_COST_PER_ML = 1.25;

    // 升级卡仓 5卡槽 包含四个容量卡和一个真空卡
    private final IUpgradeInventory upgrades = UpgradeInventories.forMachine(APBlocks.ME_PRESSURE_INTERFACE_BLOCK, 5, this::onUpgradesChanged);

    // 气动部分--------------------------------------------------------------------------------
    private float expectedPressure = 2.0f; // 期望气压值
    private static final int BASE_VOLUME_UNIT = 1000; // 未安装任何升级下的基本容量
    private final IAirHandlerMachine airHandler =
            AirHandlerMachineFactory.getInstance().createTierTwoAirHandler(BASE_VOLUME_UNIT); // 实际空气存储
    private int lastAir = 0;

    // 充气槽位--------------------------------------------------------------------------------
    private final AppEngInternalInventory inventory = new AppEngInternalInventory(1)
    {
        @Override
        protected void onContentsChanged(int slot)
        {
            super.onContentsChanged(slot);
            setChanged();
        }
    };

    public MEPressureInterfaceBlockEntity(BlockPos pos, BlockState state)
    {
        super(APBlockEntities.ME_PRESSURE_INTERFACE_BLOCK_ENTITY.get(), pos, state);

        getMainNode().setIdlePowerUsage(8.0) // 待机消耗
                .setFlags(GridFlags.REQUIRE_CHANNEL) // 需要频道
                .setExposedOnSides(EnumSet.allOf(Direction.class)); // 可以用于连接的方向

        airHandler.setConnectableFaces(EnumSet.allOf(Direction.class)); // 设置可散发空气的面
        inventory.setFilter(new IAEItemFilter()
        {
            @Override
            public boolean allowInsert(InternalInventory inv, int slot, ItemStack stack)
            {
                return !stack.isEmpty() && stack.getCapability(PNCCapabilities.AIR_HANDLER_ITEM) != null;
            }
        });
    }

    // 注册AE节点和空气容器能力
    public static void onRegisterCaps(RegisterCapabilitiesEvent event)
    {
        event.registerBlockEntity(
                AECapabilities.IN_WORLD_GRID_NODE_HOST,
                APBlockEntities.ME_PRESSURE_INTERFACE_BLOCK_ENTITY.get(),
                (be, unused) -> be
        );
        event.registerBlockEntity(
                PNCCapabilities.AIR_HANDLER_MACHINE,
                APBlockEntities.ME_PRESSURE_INTERFACE_BLOCK_ENTITY.get(),
                (be, direction) -> be.airHandler
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                APBlockEntities.ME_PRESSURE_INTERFACE_BLOCK_ENTITY.get(),
                (be, direction) -> be.inventory.toItemHandler()
        );
    }

    public AppEngInternalInventory getInventory()
    {
        return inventory;
    }

    public IAirHandlerMachine getAirHandler()
    {
        return airHandler;
    }

    public int getVolume()
    {
        return airHandler.getVolume();
    }

    public int getMaxVolume()
    {
        return (int) (getVolume() * (double) airHandler.getDangerPressure());
    }

    public float getExpectedPressure()
    {
        return expectedPressure;
    }

    public void setExpectedPressure(float expectedPressure)
    {
        expectedPressure = Math.min(20f, expectedPressure);
        float bottomPressure = isUpgradedWith(APItems.VACUUM_CARD) ? -1f : 0f;
        expectedPressure = Math.max(bottomPressure, expectedPressure);
        this.expectedPressure = expectedPressure;
        setChanged();
    }

    @Override
    public void exportSettings(SettingsFrom mode, DataComponentMap.Builder builder, @Nullable Player player)
    {
        super.exportSettings(mode, builder, player);
        if (mode == SettingsFrom.MEMORY_CARD)
        {
            builder.set(APDataComponents.EXPECTED_PRESSURE, expectedPressure);
        }
    }

    @Override
    public void importSettings(SettingsFrom mode, DataComponentMap input, @Nullable Player player)
    {
        super.importSettings(mode, input, player);
        if (mode == SettingsFrom.MEMORY_CARD)
        {
            var expected = input.get(APDataComponents.EXPECTED_PRESSURE.get());
            if (expected != null)
            {
                setExpectedPressure(expected);
            }
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

    /* 对外返回升级仓 */
    @Override
    public IUpgradeInventory getUpgrades()
    {
        return upgrades;
    }

    @Override
    public void loadTag(CompoundTag tag, HolderLookup.Provider registries)
    {
        super.loadTag(tag, registries);
        airHandler.deserializeNBT(tag.getCompound("air_handler"));
        this.inventory.readFromNBT(tag, "inv", registries);
        this.expectedPressure = tag.getFloat("expected_pressure");
        this.upgrades.readFromNBT(tag, "interface_upgrades", registries);
    }

    // load完成之后，且level被注入后
    @Override
    public void onLoad()
    {
        super.onLoad();
        onUpgradesChanged(); // 加载后刷新一次升级状态
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries)
    {
        super.saveAdditional(tag, registries);
        tag.put("air_handler", airHandler.serializeNBT());
        this.inventory.writeToNBT(tag, "inv", registries);
        tag.putFloat("expected_pressure", expectedPressure);
        this.upgrades.writeToNBT(tag, "interface_upgrades", registries);
    }

    private void onUpgradesChanged()
    {
        setExpectedPressure(expectedPressure); // 刷新一次期望气压，内部会应用虚空卡检查
        // 容积升级为4的n次方（n为容积卡数量，也就是最大64倍）
        int upgradeVolumeCount = 2 * getInstalledUpgrades(APItems.VOLUME_CARD);
        airHandler.setBaseVolume(BASE_VOLUME_UNIT * (1 << upgradeVolumeCount));
        interactWithMESystem(this.level, worldPosition, getBlockState(), this); // 重设升级卡后立刻与ME系统进行一次交互

        // 立刻重新设置有关安全卡的效果
        if (getUpgrades().isInstalled(APItems.SECURITY_CARD))
            airHandler.enableSafetyVenting(p -> p >= 20, Direction.UP);
        else
            airHandler.disableSafetyVenting();

        setChanged();
    }

    @Override
    public void serverTick()
    {
        if (level == null || level.isClientSide) return;

        // 与ME系统进行一次气体交换
        if (getMainNode().isActive())
            interactWithMESystem(level, worldPosition, getBlockState(), this);

        // 内部物品槽交互
        ItemStack containerItem = this.inventory.getStackInSlot(0);
        if (!containerItem.isEmpty())
        {
            IAirHandler itemAirHandler = containerItem.getCapability(PNCCapabilities.AIR_HANDLER_ITEM);
            if (itemAirHandler != null)
            {
                float bePressure = this.airHandler.getPressure();
                float itemPressure = itemAirHandler.getPressure();
                float itemVolume = itemAirHandler.getVolume();
                float delta = Math.abs(bePressure - itemPressure) / 2.0F; // 与充电站相同的“半差”限流
                int airInItem = itemAirHandler.getAir();

                // 基础传输率
                int airPerTick = 1000;

                // 充电器几乎为 0 压且差值很小：抽掉物品里的“最后一点点气”
                if (PneumaticCraftUtils.epsilonEquals(bePressure, 0f) && delta < 0.1f)
                {
                    if (airInItem != 0)
                    {
                        itemAirHandler.addAir(-airInItem);
                    }
                }
                // 物品气压更高：物品 -> 接口
                else if (itemPressure > bePressure + 0.01F && itemPressure > 0F)
                {
                    int move = Math.min(Math.min(airPerTick, airInItem),
                            (int) (delta * this.getVolume())); // 还要受接口自身体积*半差限制
                    if (move > 0)
                    {
                        itemAirHandler.addAir(-move);
                        this.airHandler.addAir(move);
                    }
                }
                // 物品气压更低且未达物品最大压：接口 -> 物品
                else if (itemPressure < bePressure - 0.01F && itemPressure < itemAirHandler.maxPressure())
                {
                    int maxAirInItem = (int) (itemAirHandler.maxPressure() * itemVolume);
                    // >15bar 时按充电站逻辑给一点加速
                    float boost = bePressure < 15f ? 1f : 1f + (bePressure - 15f) / 5f;

                    // 取速率、气体量、物品剩余空间的最小值
                    int move = Math.min(Math.min((int) (airPerTick * boost), this.airHandler.getAir()), maxAirInItem - airInItem);
                    // 加上半差限流
                    move = Math.min(move, (int) (delta * itemVolume));

                    if (move > 0)
                    {
                        itemAirHandler.addAir(move);
                        this.airHandler.addAir(-move);
                    }
                }
            }
        }

        // 与其他方块的空气交互
        airHandler.tick(this);

        // airHandler的回调很难覆盖所有路径
        // 这里直接使用tick比较，2个int比较极其轻量
        if (this.lastAir != this.airHandler.getAir())
        {
            this.lastAir = this.airHandler.getAir();
            this.setChanged();
        }
    }

    private static void interactWithMESystem(Level level, BlockPos pos, BlockState state, MEPressureInterfaceBlockEntity be)
    {
        // 取 ME 存储 电力服务
        final MEStorage storage = be.getNetworkInventory();
        final var grid = be.getMainNode().getGrid();
        if (storage == null || grid == null) return;
        final var energy = grid.getEnergyService();
        if (energy == null) return;

        // 压差
        final float currentPressure = be.airHandler.getPressure();
        final float targetPressure = be.expectedPressure;
        final float diffP = targetPressure - currentPressure;

        // 需要的空气量（以 air 单位计，1 bar ~= volume air）
        final int wantedAir = (int) (diffP * be.getVolume());
        final int demand = Math.abs(wantedAir);
        if (demand <= 0) return;

        // 成本：期望气压 >= 0 时成本降为十分之一
        final double costPerAir = AE_ENERGY_COST_PER_ML * (wantedAir >= 0f ? 0.1d : 1.0d);

        // 预估存储侧的可行量
        final IActionSource src = IActionSource.ofMachine(be);
        final int byStorage;
        if (wantedAir > 0)
        {
            // ME -> 接口
            byStorage = (int) storage.extract(AirKey.INSTANCE, demand, Actionable.SIMULATE, src);
        }
        else
        {
            // 接口 -> ME
            byStorage = (int) storage.insert(AirKey.INSTANCE, demand, Actionable.SIMULATE, src);
        }
        if (byStorage <= 0) return;

        // 预估能量侧的可行量
        final double aeAvail = energy.getStoredPower();
        final int byEnergy = (int) Math.floor(aeAvail / costPerAir);
        if (byEnergy <= 0) return;

        // 最终计划量
        int movePlan = Math.min(demand, Math.min(byStorage, byEnergy));

        // 先实际扣电，再按能量折算最终可移动量
        final double aeNeed = movePlan * costPerAir;
        final double aeSpent = energy.extractAEPower(aeNeed, Actionable.MODULATE, PowerMultiplier.CONFIG);
        final int moveByEnergy = (int) Math.floor(aeSpent / costPerAir);
        if (moveByEnergy <= 0) return;

        // 最终执行
        if (wantedAir > 0)
        {
            // ME -> 接口
            final int extracted = (int) storage.extract(AirKey.INSTANCE, moveByEnergy, Actionable.MODULATE, src);
            if (extracted > 0) be.airHandler.addAir(extracted);
        }
        else
        {
            // 接口 -> ME
            final int inserted = (int) storage.insert(AirKey.INSTANCE, moveByEnergy, Actionable.MODULATE, src);
            if (inserted > 0) be.airHandler.addAir(-inserted);
        }
    }

    /**
     * 由AEBasebBlock调用，把需要掉落的item放进列表即可，这里也能处理一些其他方块中onRemove时需要的操作
     */
    @Override
    public void addAdditionalDrops(Level level, BlockPos pos, List<ItemStack> drops)
    {
        super.addAdditionalDrops(level, pos, drops);
        for (int i = 0; i < inventory.size(); i++)
        {
            ItemStack slotContent = inventory.getStackInSlot(i);
            if (slotContent.isEmpty()) continue;
            drops.add(slotContent.copy());
        }
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
        inventory.clear();
        upgrades.clear();
    }
}
