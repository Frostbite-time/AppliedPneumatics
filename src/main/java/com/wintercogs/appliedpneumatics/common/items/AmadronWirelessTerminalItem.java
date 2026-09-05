package com.wintercogs.appliedpneumatics.common.items;

import appeng.api.config.FuzzyMode;
import appeng.api.config.RedstoneMode;
import appeng.api.config.Settings;
import appeng.api.config.TerminalStyle;
import appeng.api.implementations.items.IAEItemPowerStorage;
import appeng.api.util.IConfigManager;
import appeng.core.localization.GuiText;
import appeng.core.localization.PlayerMessages;
import appeng.items.tools.powered.WirelessTerminalItem;
import appeng.items.tools.powered.powersink.PoweredItemCapabilities;
import appeng.menu.MenuOpener;
import appeng.menu.locator.ItemMenuHostLocator;
import appeng.menu.locator.MenuLocators;
import com.wintercogs.appliedpneumatics.common.blocks.entitis.MEAmadronProcessStationBlockEntity;
import com.wintercogs.appliedpneumatics.common.init.APDataComponents;
import com.wintercogs.appliedpneumatics.common.init.APItems;
import com.wintercogs.appliedpneumatics.common.init.APMenus;
import com.wintercogs.appliedpneumatics.common.menu.host.AmadronWirelessTerminalMenuHost;
import me.desht.pneumaticcraft.api.item.IPositionProvider;
import me.desht.pneumaticcraft.common.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.DoubleSupplier;
import java.util.function.Supplier;

/**
 * 亚马龙无线终端
 * 能直接从链接的ME网络支付订单
 */
public class AmadronWirelessTerminalItem extends WirelessTerminalItem implements IPositionProvider
{

    public AmadronWirelessTerminalItem(DoubleSupplier powerCapacity, Properties props)
    {
        super(powerCapacity, props);
    }

    public static void onRegisterCaps(RegisterCapabilitiesEvent event)
    {
        IAEItemPowerStorage powerStorage = APItems.AMADRON_WIRELESS_TERMINAL.get();
        event.registerItem(Capabilities.EnergyStorage.ITEM,
                (o, unused) -> new PoweredItemCapabilities(o, powerStorage),
                APItems.AMADRON_WIRELESS_TERMINAL);
    }

    /**
     * 打开无线终端界面——我们把先验逻辑迁移到这里，防止某些蠢货疯狂调用getMenuHost
     */
    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
    {
        ItemStack is = player.getItemInHand(hand);

        if (!player.level().isClientSide() && checkPreconditions(is))
        {
            // 我们进行先验检查
            ItemMenuHostLocator locator = MenuLocators.forHand(player, hand);
            AmadronWirelessTerminalMenuHost menuHost = getMenuHost(player, locator, null);

            // 无能量拒绝打开
            if (getAECurrentPower(locator.locateItem(player)) <= 0)
            {
                player.sendSystemMessage(GuiText.OutOfPower.text());
                return new InteractionResultHolder<>(InteractionResult.PASS, is);
            }

            // 未链接拒绝打开
            if (!menuHost.getLinkStatus().connected())
            {
                player.sendSystemMessage(PlayerMessages.LinkedNetworkNotFound.text());
                return new InteractionResultHolder<>(InteractionResult.PASS, is);
            }

            // 如果成功打开，我们返回成功
            if (MenuOpener.open(getMenuType(), player, locator))
            {
                return new InteractionResultHolder<>(InteractionResult.sidedSuccess(level.isClientSide()), is);
            }
        }

        return new InteractionResultHolder<>(InteractionResult.FAIL, is);
    }

    @Override
    public @NotNull InteractionResult useOn(@NotNull UseOnContext context)
    {
        super.useOn(context);

        Player player = context.getPlayer();
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof MEAmadronProcessStationBlockEntity && player != null && player.isShiftKeyDown())
        {
            ItemStack tabletStack = player.getItemInHand(context.getHand());
            GlobalPos globalPos = GlobalPos.of(level.dimension(), pos);
            if (!level.isClientSide())
            {
                toggleLinkToAmadronProcess(tabletStack, globalPos);
            }
            else
            {
                player.playSound(ModSounds.CHIRP.get(), 1.0F, 1.5F);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        else
        {
            return InteractionResult.PASS;
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines,
                                TooltipFlag advancedTooltips)
    {
        // 父类添加连接状态描述
        super.appendHoverText(stack, context, lines, advancedTooltips);

        GlobalPos amadronStationPos = getLinkedAmadronPos(stack);
        if (amadronStationPos == null)
        {
            lines.add(Component.translatable("tooltip.appliedpneumatics.item.amadron.unlink")
                    .withStyle(ChatFormatting.RED));
        }
        else
        {
            ResourceKey<Level> dim = amadronStationPos.dimension();
            Component dimName = Component.translatable("dimension." + dim.location().getNamespace() + "." + dim.location().getPath());

            BlockPos pos = amadronStationPos.pos();
            lines.add(Component.translatable("tooltip.appliedpneumatics.item.amadron.linked",
                            dimName, pos.getX(), pos.getY(), pos.getZ())
                    .withStyle(ChatFormatting.GREEN));
        }
    }

    @Override
    public MenuType<?> getMenuType()
    {
        return APMenus.AMADRON_WIRELESS_TERMINAL_MENU.get();
    }

    @Override
    public @Nullable AmadronWirelessTerminalMenuHost getMenuHost(Player player, ItemMenuHostLocator locator, @Nullable BlockHitResult hitResult)
    {
        return new AmadronWirelessTerminalMenuHost(this, player, locator,
                (p, sm) -> openFromInventory(p, locator, true));
    }

    // 快速绑定、取消绑定方块
    private static void toggleLinkToAmadronProcess(ItemStack stack, GlobalPos pos)
    {
        if (stack.has(APDataComponents.AMADRON_PROCESS_POS) && Objects.equals(stack.get(APDataComponents.AMADRON_PROCESS_POS), pos))
        {
            stack.remove(APDataComponents.AMADRON_PROCESS_POS);
        }
        else
        {
            stack.set(APDataComponents.AMADRON_PROCESS_POS, pos);
        }
    }

    // 用于客户端渲染覆盖层
    public static @Nullable GlobalPos getLinkedAmadronPos(ItemStack stack)
    {
        return stack.get(APDataComponents.AMADRON_PROCESS_POS);
    }

    // 用于服务端获取数据
    public static @Nullable MEAmadronProcessStationBlockEntity getLinkWithAmadronProcess(ItemStack stack, Level level)
    {
        // 客户端不处理，直接返回 null
        if (level.isClientSide)
        {
            return null;
        }

        // 没有绑定则直接返回 null
        GlobalPos pos = stack.get(APDataComponents.AMADRON_PROCESS_POS);
        if (pos == null)
        {
            return null;
        }

        // 根据 GlobalPos 找对应的服务器维度
        ServerLevel serverLevel = level.getServer() != null ? level.getServer().getLevel(pos.dimension()) : null;
        if (serverLevel == null)
        {
            return null;
        }

        BlockEntity be = serverLevel.getBlockEntity(pos.pos());
        if (be instanceof MEAmadronProcessStationBlockEntity station)
        {
            return station;
        }
        return null;
    }

    @Override
    public @NotNull List<BlockPos> getStoredPositions(UUID player, @NotNull ItemStack itemStack)
    {
        GlobalPos amadronPos = getLinkedAmadronPos(itemStack);
        if (amadronPos != null)
        {
            return List.of(amadronPos.pos());
        }
        return List.of();
    }

    @Override
    public int getRenderColor(int index)
    {
        return 0x9003FF80; // 半透明绿色 - 与亚马龙终端风格一致
    }

    /**
     * 返回此无线终端的配置设置 这里无可用配置
     */
    public IConfigManager getConfigManager(Supplier<ItemStack> target)
    {
        return IConfigManager.builder(target)
                .registerSetting(Settings.REDSTONE_CONTROLLED, RedstoneMode.IGNORE)
                .registerSetting(Settings.FUZZY_MODE, FuzzyMode.IGNORE_ALL)
                .registerSetting(Settings.TERMINAL_STYLE, TerminalStyle.MEDIUM)
                .build();
    }
}
