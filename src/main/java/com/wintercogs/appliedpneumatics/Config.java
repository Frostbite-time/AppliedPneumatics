package com.wintercogs.appliedpneumatics;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

@Mod.EventBusSubscriber(modid = AppliedPneumatics.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config
{
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.BooleanValue ALWAYS_SHOW_EXTENDED_CONTENT = BUILDER
            .comment("总是显示联动内容，如1m~256m存储元件以及扩展亚马龙处理站，注意，这不会添加配方，只会使其出现在创造模式菜单。")
            .define("always_show_extended_content", false);

    static final ForgeConfigSpec SPEC = BUILDER.build();

    public static boolean alwaysShowExtendedContent;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event)
    {
        alwaysShowExtendedContent = ALWAYS_SHOW_EXTENDED_CONTENT.get();
    }

}
