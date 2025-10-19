package com.wintercogs.appliedpneumatics;

import com.wintercogs.appliedpneumatics.client.me.AEClientPlugin;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;

import java.nio.file.Path;

public class AppliedPneumaticsClient
{
    public static void clientInit()
    {

    }

    public static void clientCommonSetup()
    {
        AEClientPlugin.register();
        AppliedPneumatics.LOGGER.info("AppliedPneumatics client side setup");
    }

    public static void clientRegister(IEventBus modBus, IEventBus gameBus)
    {
        AEClientPlugin.registerStorageLED(modBus);
        modBus.addListener(AppliedPneumaticsClient::onAddPackFinders);
    }

    public static void onAddPackFinders(AddPackFindersEvent event)
    {
        if (event.getPackType() != PackType.CLIENT_RESOURCES) return;

        var modFileInfo = ModList.get().getModFileById(AppliedPneumatics.MODID);
        if (modFileInfo == null) return;

        Path packRoot = modFileInfo.getFile().findResource("resourcepacks/optional_textures"); // 指向包含 pack.mcmeta 的目录

        // 关键：直接提供 ResourcesSupplier（用 PathPackResources 构造包）
        Pack.ResourcesSupplier supplier = (name) -> new PathPackResources(name, packRoot, /* isBuiltin = */ false);

        Pack pack = Pack.readMetaAndCreate(
                AppliedPneumatics.MODID + ":optional_textures",
                Component.translatable("pack." + AppliedPneumatics.MODID + ".optional_textures.title"),
                /* required = */ false,
                supplier,
                PackType.CLIENT_RESOURCES,
                Pack.Position.TOP,
                PackSource.BUILT_IN
        );

        if (pack != null)
        {
            event.addRepositorySource(consumer -> consumer.accept(pack));
        }
    }
}
