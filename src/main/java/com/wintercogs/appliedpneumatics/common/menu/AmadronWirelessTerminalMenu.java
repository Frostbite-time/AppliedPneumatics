package com.wintercogs.appliedpneumatics.common.menu;

import appeng.api.config.Actionable;
import appeng.api.inventories.InternalInventory;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.storage.MEStorage;
import appeng.me.storage.NullInventory;
import appeng.menu.SlotSemantics;
import appeng.menu.implementations.UpgradeableMenu;
import appeng.menu.slot.RestrictedInputSlot;
import com.wintercogs.appliedpneumatics.common.amadron.ImmutableBasketArg;
import com.wintercogs.appliedpneumatics.common.blocks.entitis.MEAmadronProcessStationBlockEntity;
import com.wintercogs.appliedpneumatics.common.init.APItems;
import com.wintercogs.appliedpneumatics.common.init.APMenus;
import com.wintercogs.appliedpneumatics.common.items.AmadronWirelessTerminalItem;
import com.wintercogs.appliedpneumatics.common.me.crafting.AmadronPatternDetails;
import com.wintercogs.appliedpneumatics.common.menu.host.AmadronWirelessTerminalMenuHost;
import me.desht.pneumaticcraft.api.crafting.recipe.AmadronRecipe;
import me.desht.pneumaticcraft.common.amadron.AmadronOfferManager;
import me.desht.pneumaticcraft.common.amadron.ShoppingBasket;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class AmadronWirelessTerminalMenu extends UpgradeableMenu<AmadronWirelessTerminalMenuHost>
{
    private static String submitOrderAction = "submit_amadron_order_action";
    private static String savePatternAction = "save_amadron_pattern_action";

    // 打开UI时所有可用的交易快照对应的Id（直到UI关闭前都不更新）
    // 无需同步，双端均有气动自行处理
    private final List<ResourceLocation> offerIdsSnapshot = AmadronOfferManager.getInstance()
            .getActiveOffers().stream()
            .map(AmadronRecipe::getId)
            .toList();

    // 构造：双端通用，由AE自行传递信息
    public AmadronWirelessTerminalMenu(int id, Inventory playerInv, @NotNull AmadronWirelessTerminalMenuHost host)
    {
        super(APMenus.AMADRON_WIRELESS_TERMINAL_MENU.get(), id, playerInv, host);

        registerClientAction(submitOrderAction, ImmutableBasketArg.class, immutableBasketArg -> onSubmitOrder(immutableBasketArg.basket()));
        registerClientAction(savePatternAction, String.class, offerId -> onSavePatternAction(new ResourceLocation(offerId)));
    }

    private void onSubmitOrder(ShoppingBasket basket)
    {
        MEStorage storage = getHost().getInventory();
        if (!getHost().rangeCheck() || storage == NullInventory.of())
        {
            getPlayer().sendSystemMessage(Component.translatable("amadron.appliedpneumatics.order_fail.me_disconnected"));
            return;
        }
        MEAmadronProcessStationBlockEntity processBE =
                AmadronWirelessTerminalItem.getLinkWithAmadronProcess(getHost().getItemStack(), getPlayer().level());
        if (processBE == null || processBE.isRemoved())
        {
            getPlayer().sendSystemMessage(Component.translatable("amadron.appliedpneumatics.order_fail.cant_find_process_station"));
            return;
        }
        if (!processBE.getMainNode().isActive())
        {
            getPlayer().sendSystemMessage(Component.translatable("amadron.appliedpneumatics.order_fail.process_station_not_active"));
            return;
        }
        if (basket.isEmpty())
        {
            getPlayer().sendSystemMessage(Component.translatable("amadron.appliedpneumatics.order_fail.basket_empty"));
            return;
        }

        // 汇总总需求（按 AEKey）+ 记录“每个报价的单份需求”
        Map<AEKey, Long> totalNeed = new HashMap<>();
        List<AbstractMap.SimpleEntry<ResourceLocation, GenericStack>> jobsToCreate = new ArrayList<>();
        int totalUnits = 0;

        for (ResourceLocation offerId : basket)
        {
            int units = basket.getUnits(offerId);
            if (units <= 0) continue;

            AmadronRecipe offer = AmadronOfferManager.getInstance().getOffer(offerId);
            if (offer == null || !AmadronOfferManager.getInstance().isActive(offerId))
            {
                getPlayer().sendSystemMessage(Component.translatable("amadron.appliedpneumatics.order_fail.order_invaild", offerId.toString()));
                return;
            }

            // 单份输入（物品或流体）
            GenericStack unitIn = !offer.getInput().getItem().isEmpty()
                    ? GenericStack.fromItemStack(offer.getInput().getItem())
                    : GenericStack.fromFluidStack(offer.getInput().getFluid());


            // 汇总总量
            long totalForOffer = Math.multiplyExact(unitIn.amount(), (long) units);
            totalNeed.merge(unitIn.what(), totalForOffer, Long::sum);

            // 预先生成“逐单位”的 job 资源描述（此时还未真正扣减，仅准备好列表）
            // 这里这么做主要是因为包装job在BE中能够原子化执行，防止因为单个job的所需的执行次数过大导致即使输入槽全空也无法容纳所需资源
            for (int i = 0; i < units; i++)
            {
                jobsToCreate.add(new AbstractMap.SimpleEntry<>(offerId, new GenericStack(unitIn.what(), unitIn.amount())));
            }
            totalUnits += units;
        }

        if (totalNeed.isEmpty())
        {
            getPlayer().sendSystemMessage(Component.translatable("amadron.appliedpneumatics.order_fail.have_not_vaild_order"));
            return;
        }

        if (jobsToCreate.size() + processBE.getJobAmount() > 512)
        {
            getPlayer().sendSystemMessage(Component.translatable("amadron.appliedpneumatics.order_fail.order_too_much"));
            return;
        }

        IActionSource src = IActionSource.ofPlayer(getPlayer());

        // 2) 原子模拟：一次性按 Key 模拟提取总量
        for (var entry : totalNeed.entrySet())
        {
            long got = storage.extract(entry.getKey(), entry.getValue(), Actionable.SIMULATE, src);
            if (got < entry.getValue())
            {
                getPlayer().sendSystemMessage(
                        Component.translatable("amadron.appliedpneumatics.order_fail.missing_materials", entry.getValue(), entry.getKey().getDisplayName()));
                return;
            }
        }

        // 3) 原子提交：一次性扣总量；任何异常→整体回滚
        ArrayList<Map.Entry<AEKey, Long>> committed = new ArrayList<>();
        try
        {
            // 扣总量
            for (var entry : totalNeed.entrySet())
            {
                long pulled = storage.extract(entry.getKey(), entry.getValue(), Actionable.MODULATE, src);
                if (pulled < entry.getValue())
                {
                    throw new IllegalStateException("提交过程中原料发生变动，无法完成原子扣减");
                }
                committed.add(Map.entry(entry.getKey(), entry.getValue()));
            }

            // 逐单位创建 Job
            for (var unitJob : jobsToCreate)
            {
                final ResourceLocation offerId = unitJob.getKey();
                final GenericStack reserved = unitJob.getValue();
                processBE.addJob(offerId, reserved, getPlayer().getUUID());
            }
        }
        catch (Exception ex)
        {
            // 回滚所有已扣资源
            for (var c : committed)
            {
                storage.insert(c.getKey(), c.getValue(), Actionable.MODULATE, src);
            }
            getPlayer().sendSystemMessage(Component.translatable("amadron.appliedpneumatics.order_fail.for_details"));
            return;
        }

        // 标记以保证持久化，BE 会在 tick 中处理job
        processBE.setChanged();
        getPlayer().sendSystemMessage(Component.translatable("amadron.appliedpneumatics.order_success", totalUnits));
    }

    public void sendSubmitOrderAction(ShoppingBasket basket)
    {
        sendClientAction(submitOrderAction, new ImmutableBasketArg(basket));
    }

    private void onSavePatternAction(ResourceLocation offerId)
    {
        InternalInventory patternInv = getHost().getPatternInv();
        if (!patternInv.getStackInSlot(1).isEmpty()) return;

        ItemStack extracted = patternInv.extractItem(0, 1, false);
        if (!extracted.isEmpty())
        {
            ItemStack encodedPattern = new ItemStack(APItems.AMADRON_PATTERN.get(), 1);
            AmadronPatternDetails.encode(encodedPattern, offerId);
            patternInv.insertItem(1, encodedPattern, false);
        }
    }

    public void sendSavePatternAction(ResourceLocation offerId)
    {
        sendClientAction(savePatternAction, offerId.toString());
    }

    public List<ResourceLocation> getOfferIdsSnapshot()
    {
        return offerIdsSnapshot;
    }

    // 放除了升级槽之外的其他真实库存
    // 注：玩家槽位已经由UpgradeableMenu处理，不必再写
    @Override
    protected void setupInventorySlots()
    {
        RestrictedInputSlot slot = new RestrictedInputSlot(RestrictedInputSlot.PlacableItemType.BLANK_PATTERN, getHost().getPatternInv(), 0);
        addSlot(slot, SlotSemantics.BLANK_PATTERN);

        RestrictedInputSlot outputPatternSlot = new RestrictedInputSlot(RestrictedInputSlot.PlacableItemType.ENCODED_PATTERN, getHost().getPatternInv(), 1);
        addSlot(outputPatternSlot, SlotSemantics.ENCODED_PATTERN);
    }

    @Override
    public boolean stillValid(@NotNull Player player)
    {
        AmadronWirelessTerminalMenuHost host = getHost();
        return super.stillValid(player) && host.rangeCheck() && host.getTerminalItem().getAECurrentPower(host.getItemStack()) > 0;
    }
}
