package com.wintercogs.appliedpneumatics.common.blocks.entitis;

import appeng.api.behaviors.GenericInternalInventory;
import appeng.api.config.Actionable;
import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.implementations.blockentities.PatternContainerGroup;
import appeng.api.inventories.InternalInventory;
import appeng.api.networking.GridFlags;
import appeng.api.networking.IGrid;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.storage.IStorageService;
import appeng.api.stacks.*;
import appeng.api.storage.MEStorage;
import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.IUpgradeableObject;
import appeng.api.upgrades.UpgradeInventories;
import appeng.blockentity.ServerTickingBlockEntity;
import appeng.blockentity.grid.AENetworkBlockEntity;
import appeng.capabilities.Capabilities;
import appeng.core.definitions.AEItems;
import appeng.core.localization.PlayerMessages;
import appeng.helpers.IPriorityHost;
import appeng.helpers.externalstorage.GenericStackInv;
import appeng.helpers.patternprovider.PatternContainer;
import appeng.menu.ISubMenu;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocators;
import appeng.util.SettingsFrom;
import appeng.util.inv.AppEngInternalInventory;
import appeng.util.inv.PlayerInternalInventory;
import com.wintercogs.appliedpneumatics.AppliedPneumatics;
import com.wintercogs.appliedpneumatics.api.GenericInv.CombinedGenericInternalInventory;
import com.wintercogs.appliedpneumatics.api.GenericInv.GenericStackInvWrapper;
import com.wintercogs.appliedpneumatics.common.init.APBlocks;
import com.wintercogs.appliedpneumatics.common.init.APItems;
import com.wintercogs.appliedpneumatics.common.init.APMenus;
import com.wintercogs.appliedpneumatics.common.me.crafting.AmadronPatternDetails;
import me.desht.pneumaticcraft.api.crafting.recipe.AmadronRecipe;
import me.desht.pneumaticcraft.common.amadron.AmadronOfferManager;
import me.desht.pneumaticcraft.common.amadron.AmadronUtil;
import me.desht.pneumaticcraft.common.config.subconfig.AmadronPlayerOffers;
import me.desht.pneumaticcraft.common.core.ModItems;
import me.desht.pneumaticcraft.common.drone.DroneRegistry;
import me.desht.pneumaticcraft.common.entity.drone.AmadroneEntity;
import me.desht.pneumaticcraft.common.network.NetworkHandler;
import me.desht.pneumaticcraft.common.network.PacketAmadronStockUpdate;
import me.desht.pneumaticcraft.common.recipes.amadron.AmadronPlayerOffer;
import me.desht.pneumaticcraft.common.util.GlobalPosHelper;
import me.desht.pneumaticcraft.lib.Log;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;

// 大量的失败回退处理有重复代码，后续可以统一处理
public class MEAmadronProcessStationBlockEntity extends AENetworkBlockEntity implements IUpgradeableObject,
        ICraftingProvider, PatternContainer, ServerTickingBlockEntity, IPriorityHost
{

    // === 每台无人机的输出负载上限（Item/Fluid） 遵循气动原版限制
    private static final int MAX_ITEM_STACKS_PER_DRONE = 36;
    private static final int MAX_FLUID_MB_PER_DRONE = 576000;

    /** 样板槽 - 只允许UI存取 */
    private final AppEngInternalInventory patternInventory;
    /** 升级卡仓，最多四个加速卡 */
    private final IUpgradeInventory upgrades = UpgradeInventories.forMachine(APBlocks.ME_AMADRON_PROCESS_STATION.get(), 4, () -> {});
    /** 输入槽 - 供无人机拿取，允许能力系统对外输出 */
    private final GenericStackInv inputInv = new GenericStackInv(this::setChanged, 9);
    /** 输出槽 - 缓存，一旦收到物品，直接送回AE，允许能力系统输入 */
    private final GenericStackInv outputInv = new GenericStackInv(this::setChanged, 9);
    /** 样板优先级 */
    private int priority = 0;
    /** 当前所有运行中订单 */
    private final List<Job> jobs = new ArrayList<>();

    /** 用来判断亚马龙样版是否准备就绪，准备就绪后要求AE更新一次样板状态 */
    private boolean needAmadronRefresh = true;

    // 能力缓存
    private LazyOptional<GenericInternalInventory> invOpt = LazyOptional.empty();

    public MEAmadronProcessStationBlockEntity(BlockEntityType<? extends MEAmadronProcessStationBlockEntity> blockEntityType , BlockPos pos, BlockState blockState, int patternSize)
    {
        super(blockEntityType, pos, blockState);

        this.patternInventory = new AppEngInternalInventory(patternSize)
        {
            @Override
            public boolean isItemValid(int slot, ItemStack stack)
            {
                return super.isItemValid(slot, stack) && stack.getItem() == APItems.AMADRON_PATTERN.get();
            }

            @Override
            protected void onContentsChanged(int slot)
            {
                super.onContentsChanged(slot);
                ICraftingProvider.requestUpdate(getMainNode());
                setChanged();
            }
        };

        inputInv.useRegisteredCapacities();
        inputInv.setCapacity(AEKeyType.fluids(), 64000);
        outputInv.useRegisteredCapacities();
        outputInv.setCapacity(AEKeyType.fluids(), 64000);

        getMainNode().setIdlePowerUsage(8.0) // 待机消耗
                .setFlags(GridFlags.REQUIRE_CHANNEL) // 需要频道
                .setExposedOnSides(EnumSet.allOf(Direction.class)) // 可以用于连接的方向
                .addService(ICraftingProvider.class, this);
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side)
    {
        if(cap == Capabilities.GENERIC_INTERNAL_INV)
        {
            if(!invOpt.isPresent())
            {
                MEAmadronProcessStationBlockEntity be = this;
                GenericStackInvWrapper inputWrapper = new GenericStackInvWrapper(this.inputInv)
                {
                    // 防止无人机从外部塞入交易结果
                    @Override
                    public long insert(int slot, AEKey what, long amount, Actionable mode)
                    {
                        return 0;
                    }
                };
                GenericStackInvWrapper outputWrapper = new GenericStackInvWrapper(this.outputInv)
                {
                    @Override
                    public long insert(int slot, AEKey what, long amount, Actionable mode)
                    {
                        // 任何意图塞入的物品，首先会尝试直接塞入ae内，剩余物塞入outputWrapper
                        MEStorage storage = be.getNetworkInventory();
                        long remaining = amount;
                        long firstInsert = 0;
                        if(storage != null)
                        {
                            firstInsert = storage.insert(what, amount, mode, IActionSource.ofMachine(be));
                            remaining = amount - firstInsert;
                            if(remaining <= 0)
                                return amount;
                        }

                        return firstInsert + super.insert(slot, what, remaining, mode);
                    }

                    // 防止被无人机从中取出内容物
                    @Override
                    public long extract(int slot, AEKey what, long amount, Actionable mode)
                    {
                        return 0;
                    }
                };
                invOpt = LazyOptional.of(() -> new CombinedGenericInternalInventory(inputWrapper, outputWrapper));
            }
            return invOpt.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps()
    {
        super.invalidateCaps();

        if(invOpt.isPresent()) invOpt.invalidate();
        invOpt = LazyOptional.empty();
    }

    // getter----------------------------------------------------------------------------------

    /** 获取样板仓 */
    @Override
    public InternalInventory getTerminalPatternInventory()
    {
        return patternInventory;
    }
    /** 获取输入仓 */
    public GenericStackInv getInputInv()
    {
        return inputInv;
    }
    /** 获取输出仓 */
    public GenericStackInv getOutputInv()
    {
        return outputInv;
    }
    /** 获取升级卡仓 */
    @Override
    public IUpgradeInventory getUpgrades()
    {
        return upgrades;
    }
    /** 获取当前网络的MEStorage */
    public @Nullable MEStorage getNetworkInventory()
    {
        IGrid grid = getMainNode().getGrid();
        if (grid == null) return null;
        IStorageService ss = grid.getStorageService();
        return ss != null ? ss.getInventory() : null;
    }
    /** 获取当前节点网格 */
    @Override
    public @Nullable IGrid getGrid()
    {
        return getMainNode().isReady() ? getMainNode().getGrid() : null;
    }
    /** 获取样板仓组-即如何在样板管理终端中显示名称和图标 */
    @Override
    public PatternContainerGroup getTerminalGroup()
    {
        return new PatternContainerGroup(AEItemKey.of(APBlocks.ME_AMADRON_PROCESS_STATION.get()), APBlocks.ME_AMADRON_PROCESS_STATION.get().getName(), List.of());
    }
    /** 获取当前样板优先级 */
    @Override
    public int getPriority()
    {
        return priority;
    }
    /** 获取正在处理中的订单总数 */
    public int getJobAmount()
    {
        return jobs.size();
    }

    // setter -----------------------------------------------------------------------------------------------

    /** 设置优先级 */
    @Override
    public void setPriority(int priority)
    {
        this.priority = priority;
        ICraftingProvider.requestUpdate(getMainNode());
        setChanged();
    }

    /**
     * 按 AE 样板供应器的语义，把当前亚马龙样板保存到内存卡。
     */
    @Override
    public void exportSettings(SettingsFrom mode, CompoundTag tag, @Nullable Player player)
    {
        super.exportSettings(mode, tag, player);
        if (mode == SettingsFrom.MEMORY_CARD)
        {
            patternInventory.writeToNBT(tag, "patterns");
        }
    }

    /**
     * 从内存卡恢复亚马龙样板。和 PatternProviderLogic 一样，恢复每个编码样板消耗一个空白样板。
     */
    @Override
    public void importSettings(SettingsFrom mode, CompoundTag tag, @Nullable Player player)
    {
        super.importSettings(mode, tag, player);
        if (mode == SettingsFrom.MEMORY_CARD && player != null && !player.level().isClientSide)
        {
            importPatternsFromMemoryCard(tag, player);
        }
    }

    private void importPatternsFromMemoryCard(CompoundTag tag, Player player)
    {
        clearPatternInventoryForMemoryCard(player);

        var desiredPatterns = new AppEngInternalInventory(patternInventory.size());
        desiredPatterns.readFromNBT(tag, "patterns");

        var playerInventory = player.getInventory();
        int blankPatternsAvailable = player.getAbilities().instabuild
                ? Integer.MAX_VALUE
                : playerInventory.countItem(AEItems.BLANK_PATTERN.asItem());
        int blankPatternsUsed = 0;

        for (int i = 0; i < desiredPatterns.size(); i++)
        {
            ItemStack desiredPattern = desiredPatterns.getStackInSlot(i);
            if (desiredPattern.isEmpty() || desiredPattern.getItem() != APItems.AMADRON_PATTERN.get())
            {
                continue;
            }

            IPatternDetails pattern = PatternDetailsHelper.decodePattern(desiredPattern, level);
            if (!(pattern instanceof AmadronPatternDetails))
            {
                continue;
            }

            ++blankPatternsUsed;
            if (blankPatternsAvailable >= blankPatternsUsed)
            {
                ItemStack restoredPattern = desiredPattern.copy();
                restoredPattern.setCount(1);
                if (!patternInventory.addItems(restoredPattern).isEmpty())
                {
                    blankPatternsUsed--;
                }
            }
        }

        if (blankPatternsUsed > 0 && !player.getAbilities().instabuild)
        {
            new PlayerInternalInventory(playerInventory)
                    .removeItems(blankPatternsUsed, AEItems.BLANK_PATTERN.stack(), null);
        }

        if (blankPatternsUsed > blankPatternsAvailable)
        {
            player.sendSystemMessage(PlayerMessages.MissingBlankPatterns.text(
                    blankPatternsUsed - blankPatternsAvailable));
        }
    }

    /** 把当前样板还原为空白样板并交还玩家，仿照 AE 的 PatternProviderLogic。 */
    private void clearPatternInventoryForMemoryCard(Player player)
    {
        if (player.getAbilities().instabuild)
        {
            for (int i = 0; i < patternInventory.size(); i++)
            {
                patternInventory.setItemDirect(i, ItemStack.EMPTY);
            }
            return;
        }

        var playerInventory = player.getInventory();
        int blankPatternCount = 0;
        for (int i = 0; i < patternInventory.size(); i++)
        {
            ItemStack pattern = patternInventory.getStackInSlot(i);
            if (!pattern.isEmpty())
            {
                blankPatternCount += pattern.getCount();
            }
            patternInventory.setItemDirect(i, ItemStack.EMPTY);
        }

        if (blankPatternCount > 0)
        {
            playerInventory.placeItemBackInInventory(AEItems.BLANK_PATTERN.stack(blankPatternCount), false);
        }
    }

    // 终端状态实现 ----------------------------------------------------------------------------------------------------
    @Override
    public void returnToMainMenu(Player player, ISubMenu subMenu)
    {
        MenuOpener.returnTo(APMenus.ME_AMADRON_PROCESS_STATION_MENU.get(), player, MenuLocators.forBlockEntity(this));
    }

    @Override
    public ItemStack getMainMenuIcon()
    {
        return new ItemStack(getBlockState().getBlock());
    }

    // 持久化状态--------------------------------------------------------------------------------------------
    @Override
    public void saveAdditional(CompoundTag tag)
    {
        super.saveAdditional(tag);
        patternInventory.writeToNBT(tag,"pattern_inv");
        inputInv.writeToChildTag(tag,"input_inv");
        outputInv.writeToChildTag(tag,"output_inv");
        upgrades.writeToNBT(tag,"upgrade_inv");
        tag.putInt("priority", priority);

        ListTag jobList = new ListTag();
        for (Job j : this.jobs) {
            jobList.add(j.writeToSubTag());
        }
        tag.put("Jobs", jobList);
    }
    @Override
    public void loadTag(CompoundTag tag)
    {
        super.loadTag(tag);
        patternInventory.readFromNBT(tag,"pattern_inv");
        inputInv.readFromChildTag(tag,"input_inv");
        outputInv.readFromChildTag(tag,"output_inv");
        upgrades.readFromNBT(tag,"upgrade_inv");
        this.priority = tag.getInt("priority");

        this.jobs.clear();
        if (tag.contains("Jobs", Tag.TAG_LIST)) {
            ListTag jobList = tag.getList("Jobs", Tag.TAG_COMPOUND);
            for (int i = 0; i < jobList.size(); i++) {
                CompoundTag jt = jobList.getCompound(i);
                this.jobs.add(Job.readFromSubTag(jt));
            }
        }
    }
    /** 方块破坏后掉落物处理 */
    @Override
    public void addAdditionalDrops(Level level, BlockPos pos, List<ItemStack> drops)
    {
        super.addAdditionalDrops(level, pos, drops);

        // 收集掉落物：样板槽、升级槽、两个仓库位
        for (int i = 0; i < patternInventory.size(); i++)
        {
            ItemStack s = patternInventory.getStackInSlot(i);
            if (!s.isEmpty()) drops.add(s.copy());
        }
        for (int i = 0; i < upgrades.size(); i++)
        {
            ItemStack slotContent = upgrades.getStackInSlot(i);
            if (!slotContent.isEmpty()) drops.add(slotContent.copy());
        }
        Consumer<GenericStack> toDrops = (gs) -> {
            if (gs == null) return;
            if (gs.what() instanceof AEItemKey itemKey)
            {
                int amt = (int)Math.max(0, Math.min(gs.amount(), Integer.MAX_VALUE));
                if (amt > 0) drops.add(itemKey.toStack(amt));
            }
        };
        for (int i = 0; i < inputInv.size(); i++) toDrops.accept(inputInv.getStack(i));
        for (int i = 0; i < outputInv.size(); i++) toDrops.accept(outputInv.getStack(i));

        cancelAllJobs(Component.translatable("amadron.appliedpneumatics.process_fail.block_break", worldPosition.toShortString()));
    }

    @Override
    public void clearContent()
    {
        super.clearContent();
        patternInventory.clear();
        upgrades.clear();
        inputInv.clear();
        outputInv.clear();
    }

    // ICraftProvider实现---------------------------------------------------------------------------------------
    @Override
    public List<IPatternDetails> getAvailablePatterns()
    {
        List<IPatternDetails> result = new ArrayList<>();
        for(int i = 0; i < patternInventory.size(); i++)
        {
            ItemStack stack = patternInventory.getStackInSlot(i);
            if(stack.isEmpty()) continue;
            IPatternDetails patternDetails = PatternDetailsHelper.decodePattern(stack, level);
            if(patternDetails instanceof AmadronPatternDetails)
                result.add(patternDetails);
        }
        return result;
    }

    @Override
    public boolean pushPattern(IPatternDetails patternDetails, KeyCounter[] inputHolder)
    {
        if(isBusy()) return false;
        if(patternDetails instanceof AmadronPatternDetails details)
        {
            // 必然仅有一个input
            var entry = inputHolder[0].getFirstEntry();
            if(entry != null)
            {
                addJob(details.getOfferId(), new GenericStack(entry.getKey(), entry.getLongValue()));
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isBusy()
    {
        return jobs.size() >= 512;
    }

    @Override
    public int getPatternPriority()
    {
        return getPriority();
    }

    // 订单处理相关------------------------------------------------------------------------------------------
    @Override
    public void serverTick()
    {
        if (level == null || level.isClientSide) return;

        if (this.needAmadronRefresh
                && this.getMainNode().isReady()
                && (level.getGameTime() & 20) == 0) // 每 20 tick轻量检查一次
        {
            if (!AmadronOfferManager.getInstance().getActiveOffers().isEmpty()) {
                ICraftingProvider.requestUpdate(this.getMainNode());
                this.needAmadronRefresh = false; // 只刷一次，随后停掉轮询
            }
        }

        if(getMainNode().isActive())
        {
            // 每 tick 把 outputInv 往 ME 塞
            this.flushOutputToME();

            // 每 200 tick 下发订单
            if (level.getGameTime() % 200 == 0)
            {
                int maxDrones = 1 + Math.max(0, this.getInstalledUpgrades(AEItems.SPEED_CARD) - 1);
                int maxUnitsPerDrone = switch (this.getInstalledUpgrades(AEItems.SPEED_CARD))
                {
                    case 1 -> 32;
                    case 2 -> 64;
                    case 3 -> 128;
                    case 4 -> 256;

                    default -> 16;
                };
                this.doJob(maxDrones, maxUnitsPerDrone);
            }
        }
    }

    private void flushOutputToME()
    {
        MEStorage me = getNetworkInventory();
        if (me == null) return;
        IActionSource src = IActionSource.ofMachine(this);

        boolean moved = false;
        for (int i = 0; i < outputInv.size(); i++)
        {
            GenericStack gs = outputInv.getStack(i);
            if (gs == null) continue;
            long ins = me.insert(gs.what(), gs.amount(), Actionable.MODULATE, src);
            if (ins > 0) {
                outputInv.extract(gs.what(), ins, Actionable.MODULATE, src);
                moved = true;
            }
        }
        if (moved) setChanged();
    }

    // 每隔一定时间触发，通知无人机物流
    private void doJob(int maxDronesPerTick, int maxUnitsPerOfferPerDispatch)
    {
        if (this.level == null || this.level.isClientSide) return;

        IActionSource src = IActionSource.ofMachine(this);
        if (this.jobs.isEmpty())
        {
            return;
        }

        // 1 将job按offerId汇聚
        Map<ResourceLocation, List<Job>> withSelfByOffer = new HashMap<>();
        for (Job j : this.jobs)
        {
            withSelfByOffer.computeIfAbsent(j.offerId(), k -> new ArrayList<>()).add(j);
        }

        List<Job> pending = new ArrayList<>(); // 本轮无法完成的暂存在这里，等处理结束后下一轮继续
        Set<ResourceLocation> usedThisRound = new HashSet<>(); // 本轮已经处理过的offerId放在这，用于限制召唤的无人机总数
        int dispatched = 0;

        // 2 工具：把 outputInv 刷回 ME（本轮结束也会再刷一次）
        Consumer<Void> flushOutputs = v -> {
            MEStorage me = getNetworkInventory();
            if (me == null) return;
            for (int i = 0; i < outputInv.size(); i++)
            {
                GenericStack gs = outputInv.getStack(i);
                if (gs == null) continue;
                long ins = me.insert(gs.what(), gs.amount(), Actionable.MODULATE, src);
                if (ins > 0)
                {
                    outputInv.extract(gs.what(), ins, Actionable.MODULATE, src);
                }
            }
        };

        // 3 逐 job 插入资源，凑成一批后叫一台；失败则将资源整批送回ME，job不重入队
        for (var entry : withSelfByOffer.entrySet())
        {
            final ResourceLocation offerId = entry.getKey();
            final List<Job> list = entry.getValue();

            // 如果额度用尽，整组入队，等待下一轮，同时不break，确保剩下所有订单都能入队
            if (dispatched >= maxDronesPerTick)
            {
                pending.addAll(list);
                continue;
            }

            // 列表为空或者本轮已经扫过
            if (list.isEmpty() || usedThisRound.contains(offerId))
            {
                pending.addAll(list);
                continue;
            }

            // 报价失效，无法加入下一轮，直接送回资源并丢弃任务
            if (!AmadronOfferManager.getInstance().isActive(offerId))
            {
                // 报价失效：退款并丢弃
                MEStorage me = getNetworkInventory();

                // 合并失败消息：按玩家统计此次被退回的 job 数
                Map<UUID, Integer> refundedByPlayer = new HashMap<>();

                for (Job job : list)
                {
                    GenericStack res = job.selfResource();

                    long remain = res.amount();
                    if (me != null)
                    {
                        long inserted = me.insert(res.what(), remain, Actionable.MODULATE, src);
                        remain -= inserted;
                    }
                    // 插不回的余量：仅对物品掉落，其他Key无法掉落
                    if (remain > 0 && res.what() instanceof AEItemKey itemKey)
                    {
                        int drop = (int) Math.min(Integer.MAX_VALUE, remain);
                        Block.popResource(level, worldPosition, itemKey.toStack(drop));
                    }

                    if (job.player() != null)
                    {
                        refundedByPlayer.merge(job.player(), 1, Integer::sum);
                    }
                }

                // 告知相关玩家：该报价已失效，资源已返还
                if (level.getServer() != null && !refundedByPlayer.isEmpty())
                {
                    var offer = AmadronOfferManager.getInstance().getOffer(offerId); // 可能为 null（已被移除）
                    for (var playerEntry : refundedByPlayer.entrySet())
                    {
                        var player = level.getServer().getPlayerList().getPlayer(playerEntry.getKey());
                        if (player != null)
                        {
                            if (offer != null)
                            {
                                player.sendSystemMessage(
                                        Component.translatable("amadron.appliedpneumatics.process_fail",
                                                offer.getInput().getName(), offer.getOutput().getName(), playerEntry.getValue())
                                );
                            }
                            else
                            {
                                player.sendSystemMessage(
                                        Component.translatable("amadron.appliedpneumatics.process_fail.offer_invalid",
                                                offerId.toString(), playerEntry.getValue())
                                );
                            }
                        }
                    }
                }

                // 不加入 pending，直接跳过（相当于彻底移除这些 job）
                continue;
            }


            // 基于输出限制本次可派发的最大 units
            AmadronRecipe offer = AmadronOfferManager.getInstance().getOffer(offerId);
            int payloadUnitsLimit = computeMaxUnitsPerDrone(offer);
            if (payloadUnitsLimit <= 0) // 单订单超出可承载上限，此订单完全无法派发
            {
                pending.addAll(list);
                continue;
            }
            final int allowedUnits = Math.min(maxUnitsPerOfferPerDispatch, payloadUnitsLimit);

            // 逐 job 尝试插入资源；成功的累到 selected
            List<Job> selected = new ArrayList<>();
            Map<AEKey, Long> insertedTotals = new HashMap<>(); // 回滚用：汇总已插入的各 AEKey 数量

            // 记录访问了多少条，用于把“未访问尾部”补回 pending
            int visited = 0;

            for (int i = 0; i < list.size() && selected.size() < allowedUnits; i++)
            {
                Job job = list.get(i);
                visited++;

                GenericStack self = job.selfResource();

                long can = inputInv.insert(self.what(), self.amount(), Actionable.SIMULATE, src);
                if (can < self.amount())
                {
                    // 输入仓位不足：该 job 留下轮
                    pending.add(job);
                }
                else
                {
                    inputInv.insert(self.what(), self.amount(), Actionable.MODULATE, src);
                    selected.add(job);
                    insertedTotals.merge(self.what(), self.amount(), Long::sum);
                }
            }

            if (selected.isEmpty())
            {
                // 这一报价本轮没凑成批；visited 可能等于 list.size()，尾部为空
                // 把未访问的尾部补回 pending（如果有的话）
                for (int i = visited; i < list.size(); i++) pending.add(list.get(i));
                continue;
            }

            // 召唤无人机
            int units = selected.size();
            GlobalPos here = GlobalPos.of(this.level.dimension(), this.worldPosition);

            AmadroneEntity drone = retrieveOrder(AppliedPneumatics.MODID, offer, units, here, here);
            if (drone != null)
            {
                ItemStack tablet = new ItemStack(ModItems.AMADRON_TABLET.get(), 1);
                CompoundTag tag = tablet.getOrCreateTag();
                tag.put("itemPos", GlobalPosHelper.toNBT(here));
                tag.put("liquidPos", GlobalPosHelper.toNBT(here));
                drone.setHandlingOffer(offer.getId(), units, tablet, AppliedPneumatics.MODID,
                        AmadroneEntity.AmadronAction.TAKING_PAYMENT);

                usedThisRound.add(offerId);
                dispatched++;

                // 成功派发后，把“未访问的尾部”补回 pending
                for (int i = visited; i < list.size(); i++) pending.add(list.get(i));
            }
            else
            {
                // 无人机失败：整批回滚 + 合并消息（按玩家统计失败数量），不重入队
                // 1) 把已插入 inputInv 的资源抽回
                for (var it : insertedTotals.entrySet())
                {
                    inputInv.extract(it.getKey(), it.getValue(), Actionable.MODULATE, src);
                }

                // 2) 再尽量塞回 ME，不行的物品才掉落
                MEStorage me = getNetworkInventory();
                for (var it : insertedTotals.entrySet())
                {
                    long back = (me != null) ? me.insert(it.getKey(), it.getValue(), Actionable.MODULATE, src) : 0;
                    long remain = it.getValue() - back;
                    if (remain > 0 && it.getKey() instanceof AEItemKey itemKey)
                    {
                        int drop = (int) Math.min(Integer.MAX_VALUE, remain);
                        Block.popResource(level, worldPosition, itemKey.toStack(drop));
                    }
                }

                // 3) 合并失败消息：A -> B 交易失败，资源已返还，失败数量 x
                Map<UUID, Integer> failByPlayer = new HashMap<>();
                for (Job job : selected)
                {
                    if (job.player() != null) failByPlayer.merge(job.player(), 1, Integer::sum);
                }
                if (level.getServer() != null)
                {
                    for (var ent : failByPlayer.entrySet())
                    {
                        var p = level.getServer().getPlayerList().getPlayer(ent.getKey());
                        if (p != null)
                        {
                            p.sendSystemMessage(
                                    Component.translatable("amadron.appliedpneumatics.process_fail",
                                            offer.getInput().getName(), offer.getOutput().getName(), ent.getValue()));
                        }
                    }
                }

                // 失败时，同样把“未访问的尾部”补回 pending；selected 不重入队
                for (int i = visited; i < list.size(); i++) pending.add(list.get(i));
            }
        }

        // 4 刷一遍输出到 ME
        flushOutputs.accept(null);

        // 5 重建队列
        this.jobs.clear();
        this.jobs.addAll(pending);
        setChanged();
    }

    // 取一个亚马龙无人机
    public static AmadroneEntity retrieveOrder(String playerName, AmadronRecipe offer, int units, GlobalPos itemGPos, GlobalPos liquidGPos)
    {
        boolean isAmadronRestock = playerName == null;
        return offer.getInput().apply((itemStack) -> retrieveOrderItems(playerName, offer, units, itemGPos, isAmadronRestock), (fluidStack) -> retrieveOrderFluid(playerName, offer, units, liquidGPos, isAmadronRestock));
    }

    private static AmadroneEntity retrieveOrderFluid(String playerName, AmadronRecipe offer, int units, GlobalPos liquidGPos, boolean isAmadronRestock) {
        if (liquidGPos != null && validateStockLevel(playerName, offer, units, isAmadronRestock)) {
            FluidStack queryingFluid = AmadronUtil.buildFluidStack(offer.getInput().getFluid(), units);
            reduceStockLevel(offer, units, isAmadronRestock);
            return (AmadroneEntity)DroneRegistry.getInstance().retrieveFluidAmazonStyle(liquidGPos, queryingFluid);
        } else {
            return null;
        }
    }

    private static AmadroneEntity retrieveOrderItems(String playerName, AmadronRecipe offer, int units, GlobalPos itemGPos, boolean isAmadronRestock)
    {
        if (itemGPos != null && validateStockLevel(playerName, offer, units, isAmadronRestock)) {
            ItemStack queryingItems = offer.getInput().getItem();
            ItemStack[] stacks = AmadronUtil.buildStacks(queryingItems, units);
            if (stacks.length == 0) {
                Log.error("retrieveOrderItems: got empty itemstack list for offer {} x {} @ {}", new Object[]{units, queryingItems, itemGPos});
                return null;
            } else {
                reduceStockLevel(offer, units, isAmadronRestock);
                return (AmadroneEntity) DroneRegistry.getInstance().retrieveItemsAmazonStyle(itemGPos, stacks);
            }
        } else {
            return null;
        }
    }

    private static void reduceStockLevel(AmadronRecipe offer, int units, boolean isAmadronRestock)
    {
        if (!isAmadronRestock && (offer instanceof AmadronPlayerOffer || offer.getMaxStock() >= 0))
        {
            offer.setStock(offer.getStock() - units);
            if (offer instanceof AmadronPlayerOffer)
            {
                AmadronPlayerOffers.save();
            }

            NetworkHandler.sendNonLocal(new PacketAmadronStockUpdate(offer.getId(), offer.getStock()));
        }
    }

    public static boolean validateStockLevel(String playerName, AmadronRecipe offer, int units, boolean isAmadronRestock)
    {
        if (!isAmadronRestock && offer.getStock() >= 0 && units > offer.getStock()) {
            Log.warning("ignoring suspicious order from player [{}] for {} x {} - only {} in stock right now!", new Object[]{playerName, units, offer, offer.getStock()});
            return false;
        } else {
            return true;
        }
    }

    /** 计算“仅由输出负载上限”允许的一次派发最大 units（与速度卡上限取 min） */
    private static int computeMaxUnitsPerDrone(AmadronRecipe offer)
    {
        ItemStack outItem = offer.getOutput().getItem();
        if (!outItem.isEmpty())
        {
            int perUnitCount = Math.max(1, outItem.getCount());       // 单位产出的物品数量
            int maxStackSize = Math.max(1, outItem.getMaxStackSize()); // 该物品的最大堆叠
            long maxItemsCapacity = (long) MAX_ITEM_STACKS_PER_DRONE * (long) maxStackSize;
            long byUnits = maxItemsCapacity / perUnitCount; // floor
            return (int) Math.min(byUnits, Integer.MAX_VALUE);
        }
        else
        {
            // 流体：按 576,000 mB 的上限折算成 units
            FluidStack perUnit = offer.getOutput().getFluid();
            int mbPerUnit = Math.max(1, perUnit.getAmount());
            long byUnits = MAX_FLUID_MB_PER_DRONE / (long) mbPerUnit; // floor
            return (int) byUnits; // MAX_FLUID_MB_PER_DRONE仅576000，mbPerUnit最小为1，此处安全转换
        }
    }



    // 订单状态管理 -----------------------------------------------------------------------------------------------------
    public void addJob(ResourceLocation offerId, @NotNull GenericStack selfResource)
    {
        this.jobs.add(new Job(offerId, selfResource, null));
    }

    public void addJob(ResourceLocation offerId, @NotNull GenericStack selfResource, UUID player)
    {
        this.jobs.add(new Job(offerId, selfResource, player));
    }
    /** 将所有job携带的资源送回me网络或掉落，然后给相关玩家发生一次消息 */
    public void cancelAllJobs(Component message)
    {
        // 处理 Job 自带资源：尝试塞回 ME，否则掉落
        MEStorage me = getNetworkInventory();
        IActionSource src = IActionSource.ofMachine(this);

        Set<UUID> involvedPlayers = new HashSet<>();

        List<ItemStack> drops = new ArrayList<>();

        for (Job job : jobs)
        {
            GenericStack res = job.selfResource();

            long remain = res.amount();
            if (me != null)
            {
                long inserted = me.insert(res.what(), remain, Actionable.MODULATE, src);
                remain -= inserted;
            }

            if (remain > 0 && res.what() instanceof AEItemKey itemKey)
            {
                int drop = (int) Math.min(remain, Integer.MAX_VALUE);
                drops.add(itemKey.toStack(drop));
            }

            if (job.player() != null)
            {
                involvedPlayers.add(job.player());
            }
        }

        if(level != null && !drops.isEmpty())
        {
            for(ItemStack stack : drops)
            {
                Block.popResource(level, worldPosition, stack);
            }
        }


        // 统一给相关玩家发一次告警
        if (level != null && !involvedPlayers.isEmpty() && level.getServer() != null)
        {
            for (UUID uuid : involvedPlayers)
            {
                var p = level.getServer().getPlayerList().getPlayer(uuid);
                if (p != null) p.sendSystemMessage(message);
            }
        }

        jobs.clear();
        setChanged();
    }

    /** selfResource表示该Job自己携带了一部分资源，只有这部分资源被插入仓库才执行实际job */
    private record Job(ResourceLocation offerId, @NotNull GenericStack selfResource, @Nullable UUID player)
    {
        private CompoundTag writeToSubTag()
        {
            CompoundTag tag = new CompoundTag();
            tag.putString("offer", this.offerId.toString());
            tag.put("resource", GenericStack.writeTag(this.selfResource));

            if(this.player != null)
                tag.putString("player", this.player.toString());

            return tag;
        }

        public static Job readFromSubTag(CompoundTag tag)
        {
            ResourceLocation offer = new ResourceLocation(tag.getString("offer"));
            GenericStack resource = GenericStack.readTag(tag.getCompound("resource"));

            UUID player = null;
            if (tag.contains("player"))
                player = UUID.fromString(tag.getString("player"));

            return new Job(offer, Objects.requireNonNull(resource), player);
        }
    }
}
