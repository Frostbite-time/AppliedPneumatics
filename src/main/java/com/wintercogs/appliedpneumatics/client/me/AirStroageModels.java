package com.wintercogs.appliedpneumatics.client.me;

import appeng.api.client.StorageCellModels;
import com.wintercogs.appliedpneumatics.AppliedPneumatics;
import com.wintercogs.appliedpneumatics.common.init.APItems;
import com.wintercogs.appliedpneumatics.common.items.AirStorageCell;
import com.wintercogs.appliedpneumatics.common.items.PortableAirStorageCell;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;


public class AirStroageModels
{
    public static final ResourceLocation AIR_CELL_MODEL_1K = AppliedPneumatics.makeId("block/drive/cells/air_cell_1k");
    public static final ResourceLocation AIR_CELL_MODEL_4K = AppliedPneumatics.makeId("block/drive/cells/air_cell_4k");
    public static final ResourceLocation AIR_CELL_MODEL_16K = AppliedPneumatics.makeId("block/drive/cells/air_cell_16k");
    public static final ResourceLocation AIR_CELL_MODEL_64K = AppliedPneumatics.makeId("block/drive/cells/air_cell_64k");
    public static final ResourceLocation AIR_CELL_MODEL_256K = AppliedPneumatics.makeId("block/drive/cells/air_cell_256k");
    public static final ResourceLocation AIR_CELL_MODEL_1M = AppliedPneumatics.makeId("block/drive/cells/air_cell_1m");
    public static final ResourceLocation AIR_CELL_MODEL_4M = AppliedPneumatics.makeId("block/drive/cells/air_cell_4m");
    public static final ResourceLocation AIR_CELL_MODEL_16M = AppliedPneumatics.makeId("block/drive/cells/air_cell_16m");
    public static final ResourceLocation AIR_CELL_MODEL_64M = AppliedPneumatics.makeId("block/drive/cells/air_cell_64m");
    public static final ResourceLocation AIR_CELL_MODEL_256M = AppliedPneumatics.makeId("block/drive/cells/air_cell_256m");

    public static void registerStorageModels()
    {
        StorageCellModels.registerModel(APItems.AIR_CELL_1K.get(), AIR_CELL_MODEL_1K);
        StorageCellModels.registerModel(APItems.AIR_CELL_4K.get(), AIR_CELL_MODEL_4K);
        StorageCellModels.registerModel(APItems.AIR_CELL_16K.get(), AIR_CELL_MODEL_16K);
        StorageCellModels.registerModel(APItems.AIR_CELL_64K.get(), AIR_CELL_MODEL_64K);
        StorageCellModels.registerModel(APItems.AIR_CELL_256K.get(), AIR_CELL_MODEL_256K);
        StorageCellModels.registerModel(APItems.AIR_CELL_1M.get(), AIR_CELL_MODEL_1M);
        StorageCellModels.registerModel(APItems.AIR_CELL_4M.get(), AIR_CELL_MODEL_4M);
        StorageCellModels.registerModel(APItems.AIR_CELL_16M.get(), AIR_CELL_MODEL_16M);
        StorageCellModels.registerModel(APItems.AIR_CELL_64M.get(), AIR_CELL_MODEL_64M);
        StorageCellModels.registerModel(APItems.AIR_CELL_256M.get(), AIR_CELL_MODEL_256M);

        StorageCellModels.registerModel(APItems.PORTABLE_AIR_CELL_1K.get(), AIR_CELL_MODEL_1K);
        StorageCellModels.registerModel(APItems.PORTABLE_AIR_CELL_4K.get(), AIR_CELL_MODEL_4K);
        StorageCellModels.registerModel(APItems.PORTABLE_AIR_CELL_16K.get(), AIR_CELL_MODEL_16K);
        StorageCellModels.registerModel(APItems.PORTABLE_AIR_CELL_64K.get(), AIR_CELL_MODEL_64K);
        StorageCellModels.registerModel(APItems.PORTABLE_AIR_CELL_256K.get(), AIR_CELL_MODEL_256K);
        StorageCellModels.registerModel(APItems.PORTABLE_AIR_CELL_1M.get(), AIR_CELL_MODEL_1M);
        StorageCellModels.registerModel(APItems.PORTABLE_AIR_CELL_4M.get(), AIR_CELL_MODEL_4M);
        StorageCellModels.registerModel(APItems.PORTABLE_AIR_CELL_16M.get(), AIR_CELL_MODEL_16M);
        StorageCellModels.registerModel(APItems.PORTABLE_AIR_CELL_64M.get(), AIR_CELL_MODEL_64M);
        StorageCellModels.registerModel(APItems.PORTABLE_AIR_CELL_256M.get(), AIR_CELL_MODEL_256M);
    }

    public static void registerItemColors(RegisterColorHandlersEvent.Item event)
    {
        event.register(AirStorageCell::getColor,
                APItems.AIR_CELL_1K.get(),
                APItems.AIR_CELL_4K.get(),
                APItems.AIR_CELL_16K.get(),
                APItems.AIR_CELL_64K.get(),
                APItems.AIR_CELL_256K.get(),
                APItems.AIR_CELL_1M.get(),
                APItems.AIR_CELL_4M.get(),
                APItems.AIR_CELL_16M.get(),
                APItems.AIR_CELL_64M.get(),
                APItems.AIR_CELL_256M.get());

        event.register(PortableAirStorageCell::getColor,
                APItems.PORTABLE_AIR_CELL_1K.get(),
                APItems.PORTABLE_AIR_CELL_4K.get(),
                APItems.PORTABLE_AIR_CELL_16K.get(),
                APItems.PORTABLE_AIR_CELL_64K.get(),
                APItems.PORTABLE_AIR_CELL_256K.get(),
                APItems.PORTABLE_AIR_CELL_1M.get(),
                APItems.PORTABLE_AIR_CELL_4M.get(),
                APItems.PORTABLE_AIR_CELL_16M.get(),
                APItems.PORTABLE_AIR_CELL_64M.get(),
                APItems.PORTABLE_AIR_CELL_256M.get()
        );
    }

}
