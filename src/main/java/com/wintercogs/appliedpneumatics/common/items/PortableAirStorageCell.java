package com.wintercogs.appliedpneumatics.common.items;

import appeng.api.config.FuzzyMode;
import appeng.api.stacks.GenericStack;
import appeng.api.storage.cells.CellState;
import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.UpgradeInventories;
import appeng.api.upgrades.Upgrades;
import appeng.core.AEConfig;
import appeng.core.AppEng;
import appeng.core.localization.Tooltips;
import appeng.items.storage.StorageCellTooltipComponent;
import appeng.items.tools.powered.AbstractPortableCell;
import appeng.items.tools.powered.PoweredContainerItem;
import appeng.items.tools.powered.powersink.PoweredItemCapabilities;
import appeng.util.Platform;
import com.wintercogs.appliedpneumatics.common.air.PortableAirCellItemStackHandler;
import com.wintercogs.appliedpneumatics.common.init.APItems;
import com.wintercogs.appliedpneumatics.common.me.keys.AirKey;
import me.desht.pneumaticcraft.api.PNCCapabilities;
import me.desht.pneumaticcraft.api.tileentity.IAirHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import org.jetbrains.annotations.NotNull;

import java.util.*;

// 继承AbstractPortableCell可以获得一些效果，且不会继承到IBasicCellItem
// 这样我们可以避免被BasicCellHandler捕获，又能少写一些代码
public class PortableAirStorageCell extends AbstractPortableCell implements IAirStorageCell
{
    private final double idleDrain;
    private final int totalBytes;

    public PortableAirStorageCell(MenuType<?> menuType, Properties props, int defaultColor, double idleDrain, int kilobytes)
    {
        super(menuType, props, defaultColor);
        this.idleDrain = idleDrain;
        this.totalBytes = kilobytes * 1024;
    }

    public static void onRegisterCaps(RegisterCapabilitiesEvent event)
    {
        // 1K
        PortableAirStorageCell powerStorage1k = APItems.PORTABLE_AIR_CELL_1K.get();
        event.registerItem(Capabilities.EnergyStorage.ITEM,
                (o, unused) -> new PoweredItemCapabilities(o, powerStorage1k),
                APItems.PORTABLE_AIR_CELL_1K);
        event.registerItem(PNCCapabilities.AIR_HANDLER_ITEM,
                (o, unused) -> new PortableAirCellItemStackHandler(o),
                APItems.PORTABLE_AIR_CELL_1K);

        // 4K
        PortableAirStorageCell powerStorage4k = APItems.PORTABLE_AIR_CELL_4K.get();
        event.registerItem(Capabilities.EnergyStorage.ITEM,
                (o, unused) -> new PoweredItemCapabilities(o, powerStorage4k),
                APItems.PORTABLE_AIR_CELL_4K);
        event.registerItem(PNCCapabilities.AIR_HANDLER_ITEM,
                (o, unused) -> new PortableAirCellItemStackHandler(o),
                APItems.PORTABLE_AIR_CELL_4K);

        // 16K
        PortableAirStorageCell powerStorage16k = APItems.PORTABLE_AIR_CELL_16K.get();
        event.registerItem(Capabilities.EnergyStorage.ITEM,
                (o, unused) -> new PoweredItemCapabilities(o, powerStorage16k),
                APItems.PORTABLE_AIR_CELL_16K);
        event.registerItem(PNCCapabilities.AIR_HANDLER_ITEM,
                (o, unused) -> new PortableAirCellItemStackHandler(o),
                APItems.PORTABLE_AIR_CELL_16K);

        // 64K
        PortableAirStorageCell powerStorage64k = APItems.PORTABLE_AIR_CELL_64K.get();
        event.registerItem(Capabilities.EnergyStorage.ITEM,
                (o, unused) -> new PoweredItemCapabilities(o, powerStorage64k),
                APItems.PORTABLE_AIR_CELL_64K);
        event.registerItem(PNCCapabilities.AIR_HANDLER_ITEM,
                (o, unused) -> new PortableAirCellItemStackHandler(o),
                APItems.PORTABLE_AIR_CELL_64K);

        // 256K
        PortableAirStorageCell powerStorage256k = APItems.PORTABLE_AIR_CELL_256K.get();
        event.registerItem(Capabilities.EnergyStorage.ITEM,
                (o, unused) -> new PoweredItemCapabilities(o, powerStorage256k),
                APItems.PORTABLE_AIR_CELL_256K);
        event.registerItem(PNCCapabilities.AIR_HANDLER_ITEM,
                (o, unused) -> new PortableAirCellItemStackHandler(o),
                APItems.PORTABLE_AIR_CELL_256K);

        // 1M
        PortableAirStorageCell powerStorage1m = APItems.PORTABLE_AIR_CELL_1M.get();
        event.registerItem(Capabilities.EnergyStorage.ITEM,
                (o, unused) -> new PoweredItemCapabilities(o, powerStorage1m),
                APItems.PORTABLE_AIR_CELL_1M);
        event.registerItem(PNCCapabilities.AIR_HANDLER_ITEM,
                (o, unused) -> new PortableAirCellItemStackHandler(o),
                APItems.PORTABLE_AIR_CELL_1M);

        // 4M
        PortableAirStorageCell powerStorage4m = APItems.PORTABLE_AIR_CELL_4M.get();
        event.registerItem(Capabilities.EnergyStorage.ITEM,
                (o, unused) -> new PoweredItemCapabilities(o, powerStorage4m),
                APItems.PORTABLE_AIR_CELL_4M);
        event.registerItem(PNCCapabilities.AIR_HANDLER_ITEM,
                (o, unused) -> new PortableAirCellItemStackHandler(o),
                APItems.PORTABLE_AIR_CELL_4M);

        // 16M
        PortableAirStorageCell powerStorage16m = APItems.PORTABLE_AIR_CELL_16M.get();
        event.registerItem(Capabilities.EnergyStorage.ITEM,
                (o, unused) -> new PoweredItemCapabilities(o, powerStorage16m),
                APItems.PORTABLE_AIR_CELL_16M);
        event.registerItem(PNCCapabilities.AIR_HANDLER_ITEM,
                (o, unused) -> new PortableAirCellItemStackHandler(o),
                APItems.PORTABLE_AIR_CELL_16M);

        // 64M
        PortableAirStorageCell powerStorage64m = APItems.PORTABLE_AIR_CELL_64M.get();
        event.registerItem(Capabilities.EnergyStorage.ITEM,
                (o, unused) -> new PoweredItemCapabilities(o, powerStorage64m),
                APItems.PORTABLE_AIR_CELL_64M);
        event.registerItem(PNCCapabilities.AIR_HANDLER_ITEM,
                (o, unused) -> new PortableAirCellItemStackHandler(o),
                APItems.PORTABLE_AIR_CELL_64M);

        // 256M
        PortableAirStorageCell powerStorage256m = APItems.PORTABLE_AIR_CELL_256M.get();
        event.registerItem(Capabilities.EnergyStorage.ITEM,
                (o, unused) -> new PoweredItemCapabilities(o, powerStorage256m),
                APItems.PORTABLE_AIR_CELL_256M);
        event.registerItem(PNCCapabilities.AIR_HANDLER_ITEM,
                (o, unused) -> new PortableAirCellItemStackHandler(o),
                APItems.PORTABLE_AIR_CELL_256M);
    }

    public static int getColor(ItemStack stack, int tintIndex)
    {
        if (tintIndex == 1) //led
        {
            if (stack.getItem() instanceof PoweredContainerItem poweredContainer)
            {
                if (poweredContainer.getAECurrentPower(stack) <= 0)
                    return CellState.ABSENT.getStateColor();
                ;
            }
            long stored = IAirStorageCell.getStoredAir(stack);
            CellState state = IAirStorageCell.calcState(((IAirStorageCell) stack.getItem()).getTotalBytes(), stored);
            return state.getStateColor();
        }
        else if (tintIndex == 0 && stack.getItem() instanceof AbstractPortableCell portableCell) // screen
        {
            return portableCell.getColor(stack); // 实际上是获取之前传入的默认颜色
        }
        return 0xFFFFFF; // 白
    }

    @Override
    public double getChargeRate(ItemStack stack)
    {
        return 80d + 80d * Upgrades.getEnergyCardMultiplier(getUpgrades(stack));
    }

    @Override
    public ResourceLocation getRecipeId()
    {
        return AppEng.makeId("tools/" + Objects.requireNonNull(getRegistryName()).getPath());
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines,
                                TooltipFlag advancedTooltips)
    {
        if (Platform.isClient())
        {
            // 基础容量/使用
            long stored = IAirStorageCell.getStoredAir(stack);
            long used = IAirStorageCell.usedBytes(stored);
            lines.add(Tooltips.bytesUsed(used, getTotalBytes()));
            // 单类型：0 或 1
            int typesUsed = stored > 0 ? 1 : 0;
            lines.add(Tooltips.typesUsed(typesUsed, 1));
            IAirHandler handler = stack.getCapability(PNCCapabilities.AIR_HANDLER_ITEM);
            if (handler != null)
                lines.add(Component.translatable("appliedpneumatics.portable_air_cell.bar", String.format(Locale.ROOT, "%.1f", handler.getPressure())).withStyle(ChatFormatting.DARK_GREEN));
        }
    }

    @Override
    public Optional<TooltipComponent> getTooltipImage(ItemStack stack)
    {
        var showUpg = AEConfig.instance().isTooltipShowCellUpgrades();
        var showCnt = AEConfig.instance().isTooltipShowCellContent();

        // 升级图标
        var upgrades = new ArrayList<ItemStack>();
        if (showUpg) getUpgrades(stack).forEach(upgrades::add);

        // 内容预览（只有 Air 一种）
        List<GenericStack> content = Collections.emptyList();
        boolean hasMore = false;
        if (showCnt)
        {
            long stored = IAirStorageCell.getStoredAir(stack);
            if (stored > 0)
            {
                content = List.of(new GenericStack(AirKey.INSTANCE, stored));
            }
        }

        return Optional.of(new StorageCellTooltipComponent(
                upgrades, content, hasMore, true  // 显示进度条
        ));
    }

    @Override
    public int getTotalBytes()
    {
        return this.totalBytes;
    }

    @Override
    public double getIdleDrain()
    {
        return this.idleDrain;
    }

    @Override
    public IUpgradeInventory getUpgrades(ItemStack is)
    {
        return UpgradeInventories.forItem(is, 5, super::onUpgradesChanged);
    }

    @Override
    public FuzzyMode getFuzzyMode(ItemStack is)
    {
        return FuzzyMode.IGNORE_ALL;
    }

    @Override
    public void setFuzzyMode(ItemStack is, FuzzyMode fzMode)
    {
    }

    @Override
    public void inventoryTick(@NotNull ItemStack stack, @NotNull Level level, @NotNull Entity entity, int slotId, boolean isSelected)
    {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        // 仅服务端执行，且需要安装充气卡与自身还有电
        if (level.isClientSide) return;
        if (level.getGameTime() % 20 != 0) return;
        if (!(entity instanceof Player player)) return;
        if (!getUpgrades(stack).isInstalled(APItems.CHARGING_CARD)) return;
        if (this.getAECurrentPower(stack) <= 0) return;

        // 本便携空气单元的空气接口
        final IAirHandler src = stack.getCapability(PNCCapabilities.AIR_HANDLER_ITEM);
        if (src == null) return;

        final float DEAD_BAND_BAR = 0.05f;     // 压差死区，防抖
        final int BASE_AIR_PER_TICK = 20000;  // 基础每 tick 最大传输空气量（单位：air）
        final double AE_PER_AIR = 0.02d;       // 每 1 air 消耗的 AE 能量

        // 逐个物品尝试充气：主物品栏 + 盔甲栏 + 副手
        // 注意：不对其它便携空气存储单元充气（避免环路/套娃）
        Iterable<ItemStack> loops = () -> new Iterator<>()
        {
            final List<ItemStack> all = new ArrayList<>();

            {   // 主物品栏
                all.addAll(player.getInventory().items);
                // 盔甲栏
                all.addAll(player.getInventory().armor);
                // 副手
                all.addAll(player.getInventory().offhand);
            }

            int idx = 0;

            @Override
            public boolean hasNext()
            {
                return idx < all.size();
            }

            @Override
            public ItemStack next()
            {
                return all.get(idx++);
            }
        };

        for (ItemStack other : loops)
        {
            if (other.isEmpty()) continue;
            if (other == stack) continue; // 自身
            if (other.getItem() instanceof IAirStorageCell) continue; // 不给其它空气“存储单元/元件”充气

            IAirHandler dst = other.getCapability(PNCCapabilities.AIR_HANDLER_ITEM);
            if (dst == null) continue;

            // 单向：只有当本单元气压高于目标物品气压，且超出死区时，才推进空气
            float pSrc = src.getPressure();
            float pDst = dst.getPressure();
            float diff = pSrc - pDst;
            if (diff <= DEAD_BAND_BAR) continue;

            // 目标可用空间 maxAir - currentAir
            int dstMaxAir = (int) (dst.maxPressure() * dst.getVolume());
            int dstAir = dst.getAir();
            int dstFreeAir = Math.max(0, dstMaxAir - dstAir);
            if (dstFreeAir <= 0) continue;

            // 源可用空气
            int srcAir = src.getAir();
            if (srcAir <= 0) break; // 源没气了，直接结束

            // 速率限制 + 压差限流（半差 * 目标体积）
            int byRate = BASE_AIR_PER_TICK;
            int byDelta = (int) ((diff / 2.0f) * dst.getVolume());
            if (byDelta <= 0) continue;

            // 能量限制（先模拟可用 AE，再折算成能支持的 air）
            double aeAvail = this.extractAEPower(stack, Double.MAX_VALUE, appeng.api.config.Actionable.SIMULATE);
            int byEnergy = (int) Math.floor(aeAvail / AE_PER_AIR);
            if (byEnergy <= 0) break; // 没电了

            // 计算本次可移动的空气量
            int moveWant = Math.min(Math.min(byRate, byDelta), Math.min(srcAir, dstFreeAir));
            moveWant = Math.min(moveWant, byEnergy);

            // 真正扣电：按 moveWant 对应的 AE
            double aeNeed = moveWant * AE_PER_AIR;
            double aeSpent = this.extractAEPower(stack, aeNeed, appeng.api.config.Actionable.MODULATE);
            int move = (int) Math.floor(aeSpent / AE_PER_AIR);
            if (move <= 0) continue;

            // 执行充气
            dst.addAir(move);
            src.addAir(-move);

            // 如果电或气已经基本用尽，就提前结束
            if (this.getAECurrentPower(stack) <= 0) break;
            if (src.getAir() <= 0) break;
        }
    }
}
