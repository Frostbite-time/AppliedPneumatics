package com.wintercogs.appliedpneumatics.client.gui;

import appeng.api.config.Settings;
import appeng.api.config.TerminalStyle;
import appeng.client.gui.Icon;
import appeng.client.gui.implementations.UpgradeableScreen;
import appeng.client.gui.style.StyleManager;
import appeng.client.gui.widgets.AETextField;
import appeng.client.gui.widgets.IconButton;
import appeng.client.gui.widgets.Scrollbar;
import appeng.client.gui.widgets.SettingToggleButton;
import com.wintercogs.appliedpneumatics.AppliedPneumatics;
import com.wintercogs.appliedpneumatics.client.gui.widgets.AmadronOfferPanel;
import com.wintercogs.appliedpneumatics.common.menu.AmadronWirelessTerminalMenu;
import com.wintercogs.appliedpneumatics.util.AmadronOfferHelper;
import me.desht.pneumaticcraft.api.crafting.recipe.AmadronRecipe;
import me.desht.pneumaticcraft.common.amadron.AmadronOfferManager;
import me.desht.pneumaticcraft.common.amadron.ShoppingBasket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AmadronWirelessTerminalGUI extends UpgradeableScreen<AmadronWirelessTerminalMenu>
{
    // 背景纹理
    private static final ResourceLocation BG_TEX = AppliedPneumatics.makeId("textures/gui/amadron_wireless_terminal.png");

    // 背景切片（基于 BG_TEX 的 UV 与尺寸）
    private static final UVBounds HEADER = new UVBounds(0,   0, 195, 18);
    private static final UVBounds FIRST_ROW = new UVBounds(0,  18, 195, 23);
    private static final UVBounds MID_ROW = new UVBounds(0,  41, 195, 23);
    private static final UVBounds LAST_ROW = new UVBounds(0,  87, 195, 31);
    private static final UVBounds ENDER = new UVBounds(0, 118, 176,152);

    // 面板网格参数（与背景行视觉一致）
    private static final int PANELS_PER_ROW = 2; // 每行两个交易面板
    private static final int PANEL_W = 79;
    private static final int PANEL_H = 22;
    private static final int GAP_X = 1;
    private static final int GAP_Y = 1;

    // 网格左上角相对 GUI 顶点偏移（与纹理切片对齐）
    private static final int GRID_OFFSET_X = 9;
    private static final int GRID_OFFSET_Y = 19;

    // 隐藏停靠点
    private static final int HIDE_X = -10000;
    private static final int HIDE_Y = -10000;

    // 组件
    private final Scrollbar scrollbar;
    private final AETextField searchField;
    private final Button submitButton;
    private final IconButton savePatternButton;

    // 状态
    private int totalRows = 0; // 过滤后的总行数（数据规模决定）
    private int lastTopRow = -1; // 上一帧滚动首行
    private int visibleRows = 2; // 动态可见行数（至少 2）
    private final List<AmadronOfferPanel> pagePanels = new ArrayList<>();

    // 过滤
    private String searchQuery = "";
    private final List<Integer> filteredIndex = new ArrayList<>();

    // 样板制作辅助
    private @Nullable AmadronOfferPanel lastClickedPanel = null;

    /** 持久性组件必须在构造函数一次性添加 */
    public AmadronWirelessTerminalGUI(AmadronWirelessTerminalMenu menu, Inventory playerInventory, Component title)
    {
        super(menu, playerInventory, title, StyleManager.loadStyleDoc("/screens/amadron_wireless_terminal.json"));

        // 滚动条
        this.scrollbar = widgets.addScrollBar("scrollbar", Scrollbar.DEFAULT);

        // 提交订单按钮
        this.submitButton = Button.builder(
                Component.translatable("menu.appliedpneumatics.button.submit"),
                b -> onSubmit()
        ).build();
        widgets.add("submit_button", this.submitButton);

        // 保存样板按钮
        this.savePatternButton = new IconButton(btn -> {
            if (lastClickedPanel != null) {
                menu.sendSavePatternAction(lastClickedPanel.getOfferId());
            }
        }) {
            @Override
            protected Icon getIcon() { return Icon.ARROW_LEFT; }
        };
        widgets.add("save_pattern_button", this.savePatternButton);

        // 搜索框
        this.searchField = widgets.addTextField("search");
        this.searchField.setResponder(s -> {
            this.searchQuery = (s == null ? "" : s.trim().toLowerCase(Locale.ROOT));
            rebuildFilter();
            this.lastTopRow = -1;
            relayoutVisiblePanels();
        });

        // 添加和配置文件绑定的style，如同原版终端
        TerminalStyle terminalStyle = config.getTerminalStyle();
        this.addToLeftToolbar(new SettingToggleButton<>(Settings.TERMINAL_STYLE, terminalStyle, this::toggleTerminalStyle));
    }



    @Override
    protected void init()
    {
        // 先计算可见行数与 GUI 总高度，再 super.init()，确保 leftPos/topPos 按正确高度计算
        this.visibleRows = computeVisibleRows();
        this.imageWidth = Math.max(HEADER.width, ENDER.width);
        this.imageHeight = GRID_OFFSET_Y + backgroundRowsHeight(visibleRows) + ENDER.height;
        super.init();

        rebuildFilter(); // 将与搜索匹配的面板加入filteredIndex
        ensurePanelPoolUpToDate(); // 确保快照中的id都有一份对应的panel
        reattachPanelPool(); // 确保所有的panel都被添加为可用widget
        updateScrollbarFrameAndRange(); // 重设滚动条高度和滚动区域
        // 初始化后强制重排
        this.lastTopRow = -1;
        relayoutVisiblePanels(); // 排列所有亚马龙交易面板
    }

    /** 当终端风格切换 */
    private void toggleTerminalStyle(SettingToggleButton<TerminalStyle> btn, boolean backwards)
    {
        TerminalStyle next = btn.getNextValue(backwards);
        config.setTerminalStyle(next);
        btn.set(next);
        init();
    }

    /**
     * 根据终端样式与可用高度计算可见行数（至少 2 行）。
     * SMALL≈1/4，MEDIUM≈1/2，TALL≈3/4，FULL≈尽量占满。
     */
    private int computeVisibleRows()
    {
        // 可用空间：屏幕高 - 36 - HEADER - ENDER
        int usable = Math.max(0, this.height - 36 - HEADER.height - ENDER.height);
        TerminalStyle style = config.getTerminalStyle();
        int maxRows = usable / PANEL_H; // 算一个最接近的大小，但是去尾

        return Math.max(2, style.getRows(maxRows)); // 最低2行
    }

    /** 返回当前显示的所有行的高度总和，传入的rows最好大于等于2，但这里不做强制限制 */
    private static int backgroundRowsHeight(int rows)
    {
        if (rows <= 1) return FIRST_ROW.height;
        if (rows == 2) return FIRST_ROW.height + LAST_ROW.height;
        return FIRST_ROW.height + (rows - 2) * MID_ROW.height + LAST_ROW.height;
    }

    /** 重设滑动条高度以及滑动范围 */
    private void updateScrollbarFrameAndRange()
    {
        // 滚动条像素高度对齐可见网格高度（以面板高度为基准，手感更好）
        int gridPixelH = visibleRows * (PANEL_H + GAP_Y) - GAP_Y;
        gridPixelH = Math.max(1, gridPixelH);
        scrollbar.setHeight(gridPixelH);

        // 范围
        updateScrollbarRangeBySize(filteredIndex.size());
    }

    // —— 交互/滚动 ———————————————————————————————————————————

    @Override
    public void containerTick() {
        super.containerTick();
        int topRow = scrollbar.getCurrentScroll();
        if (topRow != lastTopRow) {
            lastTopRow = topRow;
            relayoutVisiblePanels();
        }
    }

    @Override
    public boolean mouseClicked(double x, double y, int btn) {
        for (var panel : pagePanels) {
            if (panel.isActive() && panel.isMouseOver(x, y)) {
                panel.onClicked(x, y, btn, hasShiftDown());
                updateLastClickedPanel(panel);
                return true;
            }
        }
        return super.mouseClicked(x, y, btn);
    }

    private void updateLastClickedPanel(AmadronOfferPanel clicked) {
        // 只允许对“静态且非玩家”的报价生成样板按钮
        if (clicked.getOfferType().isPlayer() || !clicked.getOfferType().isStatic()) return;

        if (lastClickedPanel != null) removeWidget(lastClickedPanel);
        lastClickedPanel = AmadronOfferPanel.fromOfferId(
                getGuiRight() - 183, getGuiBottom() - 125, clicked.getOfferId(), b -> {}
        );
        addRenderableWidget(lastClickedPanel);
    }


    /** 池：确保数量与完整快照一致 */
    private void ensurePanelPoolUpToDate()
    {
        var ids = menu.getOfferIdsSnapshot();

        // id池是一个快照，理论上在整个menu的生命周期不变，这里是防御性编程
        for (int i = pagePanels.size(); i < ids.size(); i++)
        {
            ResourceLocation id = ids.get(i);
            AmadronOfferPanel p = AmadronOfferPanel.fromOfferId(HIDE_X, HIDE_Y, id, b -> {});
            p.active = false;
            p.visible = false;
            addRenderableWidget(p);
            pagePanels.add(p);
        }

        // 缩容
        for (int i = ids.size(); i < pagePanels.size(); i++) {
            var p = pagePanels.get(i);
            p.active = false;
            p.visible = false;
            p.setX(HIDE_X);
            p.setY(HIDE_Y);
        }
    }

    /** 把所有交易面板加入到可渲染组件中 */
    private void reattachPanelPool()
    {
        for (var p : pagePanels)
        {
            if (!this.renderables.contains(p)) addRenderableWidget(p);
        }
    }

    /** 根据当前搜索状态重新排布交易面板 */
    private void relayoutVisiblePanels()
    {
        // 更新滚动条状态
        updateScrollbarRangeBySize(filteredIndex.size());
        ensurePanelPoolUpToDate();

        // 先全部隐藏
        for (var p : pagePanels) {
            p.active = false;
            p.visible = false;
            p.setX(HIDE_X);
            p.setY(HIDE_Y);
        }
        if (filteredIndex.isEmpty()) return;

        int maxTop = Math.max(0, totalRows - visibleRows);
        int startRow = Math.min(scrollbar.getCurrentScroll(), maxTop);
        int startIndex = startRow * PANELS_PER_ROW;
        int capacity = visibleRows * PANELS_PER_ROW;
        int endExclusive = Math.min(startIndex + capacity, filteredIndex.size()); // 计算可以显示的交易总数

        // 一个个排布面板，并设置显示和激活状态
        for (int index = startIndex; index < endExclusive; index++)
        {
            int local = index - startIndex;
            int row = local / PANELS_PER_ROW;
            int col = local % PANELS_PER_ROW;

            int x = leftPos + GRID_OFFSET_X + col * (PANEL_W + GAP_X);
            int y = topPos + GRID_OFFSET_Y + row * (PANEL_H + GAP_Y);

            int idxInSnapshot = filteredIndex.get(index);
            AmadronOfferPanel panel = pagePanels.get(idxInSnapshot);

            panel.setX(x);
            panel.setY(y);
            panel.active = true;
            panel.visible = true;
        }

        // 更新滚动条
        updateScrollbarFrameAndRange();
    }

    /** 将与搜索匹配的交易的索引添加到filteredIndex
     *  同时更新当前scrollbar的滚动范围 */
    private void rebuildFilter()
    {
        filteredIndex.clear();
        var ids = menu.getOfferIdsSnapshot();
        if (ids.isEmpty())
        {
            updateScrollbarRangeBySize(0);
            return;
        }

        if (searchQuery.isEmpty())
        {
            for (int i = 0; i < ids.size(); i++) filteredIndex.add(i);
        }
        else
        {
            for (int i = 0; i < ids.size(); i++)
            {
                if (offerMatches(ids.get(i), searchQuery)) filteredIndex.add(i);
            }
        }
        updateScrollbarRangeBySize(filteredIndex.size());
    }

    private boolean offerMatches(ResourceLocation id, String q)
    {
        String needle = q.toLowerCase(Locale.ROOT);
        if (id.toString().toLowerCase(Locale.ROOT).contains(needle)) return true;

        AmadronRecipe offer = AmadronOfferHelper.getActiveOffer(id);
        if (offer == null) return false;

        String inName = safeNameLower(offer, true);
        String outName = safeNameLower(offer, false);
        return (!inName.isEmpty() && inName.contains(needle))
                || (!outName.isEmpty() && outName.contains(needle));
    }

    private static String safeNameLower(AmadronRecipe offer, boolean input)
    {
        try {
            String name = input ? offer.getInput().getName() : offer.getOutput().getName();
            return name == null ? "" : name.toLowerCase(Locale.ROOT);
        } catch (Throwable t) {
            return "";
        }
    }

    /** 重设滑动范围 */
    private void updateScrollbarRangeBySize(int filteredSize)
    {
        // 计算总行数：每行 PANELS_PER_ROW 个面板，向上取整
        this.totalRows = (int) Math.ceil(filteredSize / (double) PANELS_PER_ROW);

        // 计算最大可滚到的首行索引
        // 能看到的行数是 visibleRows，总行数是 totalRows，
        // 所以最靠下的首行 = totalRows - visibleRows；不允许为负。
        int maxScroll = Math.max(0, totalRows - visibleRows);
        int pageStep = Math.max(1, Math.max(1, visibleRows / 6)); // 每次滑动选取1或者可见部分1/6的区域
        scrollbar.setRange(0, maxScroll, pageStep);
    }


    // 提交当前选中的所有交易
    private void onSubmit()
    {
        ShoppingBasket basket = new ShoppingBasket();
        for (AmadronOfferPanel panel : pagePanels) {
            int want = panel.getWantedStock();
            if (want > 0) basket.addUnitsToOffer(panel.getOfferId(), want);
        }
        menu.sendSubmitOrderAction(basket);
    }

    @Override
    public void drawFG(GuiGraphics gg, int ox, int oy, int mx, int my)
    {
        super.drawFG(gg, ox, oy, mx, my);
        for (var p : pagePanels) {
            if (p.isActive()) p.renderStackTooltip(gg, mx, my, ox, oy);
        }
    }

    // —— 背景绘制（动态） ———————————————————————————————————————————

    @Override
    public void drawBG(GuiGraphics gg, int x, int y, int mouseX, int mouseY, float partialTicks)
    {
        // 先让父类绘制它的基础层（槽位/样式等）


        // 手绘拼接背景
        final int x0 = this.leftPos;
        final int y0 = this.topPos;

        // 1) Header
        blit(gg, BG_TEX, x0, y0, HEADER);

        // 2) 行区域（从 GRID_OFFSET_Y 开始）

        int darwY = y0 + HEADER.height;
        if (visibleRows >= 2) {
            // 第一行背景
            blit(gg, BG_TEX, x0, darwY, FIRST_ROW);
            darwY += FIRST_ROW.height;

            // 中间行
            for (int i = 0; i < visibleRows - 2; i++) {
                blit(gg, BG_TEX, x0, darwY, MID_ROW);
                darwY += MID_ROW.height;
            }

            // 最后一行
            blit(gg, BG_TEX, x0, darwY, LAST_ROW);
            darwY += LAST_ROW.height;
        } else if (visibleRows == 1) {
            // 理论不会出现，但以防配置异常
            blit(gg, BG_TEX, x0, darwY, LAST_ROW);
            darwY += LAST_ROW.height;
        }

        // 3) Ender（玩家物品栏区域）：水平居中于 HEADER 宽度
        blit(gg, BG_TEX, x0, darwY, ENDER);

        super.drawBG(gg, x, y, mouseX, mouseY, partialTicks);
    }

    private int getGuiRight()
    {
        return getGuiLeft() + this.imageWidth;
    }

    private int getGuiBottom()
    {
        return getGuiTop() + this.imageHeight;
    }

    private static void blit(GuiGraphics gg, ResourceLocation tex, int x, int y, UVBounds uv) {
        // 使用 256x256 贴图尺寸（若你的贴图不是 256，请改成实际 texW/texH）
        gg.blit(tex, x, y, uv.x, uv.y, uv.width, uv.height, 512, 512);
    }

    private record UVBounds(int x, int y, int width, int height) {}

}