package com.wintercogs.appliedpneumatics;

import com.mojang.logging.LogUtils;
import com.wintercogs.appliedpneumatics.common.init.*;
import com.wintercogs.appliedpneumatics.common.me.AEPlugin;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLConstructModEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import org.slf4j.Logger;


@Mod(AppliedPneumatics.MODID)
public class AppliedPneumatics
{
    public static final String MODID = "appliedpneumatics";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final String MEGA_CELL_MODID = "megacells";
    public static boolean MEGA_CELL_LOADED = false;

    public static final String EAE_MODID = "extendedae";
    public static boolean EAE_LOADED = false;


    public AppliedPneumatics(IEventBus modEventBus, ModContainer modContainer)
    {
        modEventBus.addListener(this::constructMod);
        modEventBus.addListener(this::commonSetup);
        NeoForge.EVENT_BUS.register(this);
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        modEventBus.addListener((RegisterEvent event) -> {
            if (event.getRegistryKey().equals(Registries.BLOCK))
                AEPlugin.init();
        });

        APMenus.registerMenus(modEventBus);
        APCreativeModeTabs.register(modEventBus);
        APItems.register(modEventBus);
        APBlocks.register(modEventBus);
        APBlockEntities.register(modEventBus);
        APDataComponents.register(modEventBus);
    }

    private void constructMod(final FMLConstructModEvent event)
    {
        if (ModList.get().isLoaded(MEGA_CELL_MODID))
        {
            MEGA_CELL_LOADED = true;
        }
        if (ModList.get().isLoaded(EAE_MODID))
        {
            EAE_LOADED = true;
        }
    }

    private void commonSetup(FMLCommonSetupEvent event)
    {
        AEPlugin.register();
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event)
    {
        LOGGER.info("AppliedPneumatics server side setup");
    }

    public static ResourceLocation makeId(String path)
    {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
