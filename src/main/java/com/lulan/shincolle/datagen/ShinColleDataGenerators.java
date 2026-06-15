package com.lulan.shincolle.datagen;


import com.lulan.shincolle.datagen.server.ShinColleGlobalLootModifierProvider;
import com.lulan.shincolle.reference.Reference;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ShinColleDataGenerators {
    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput packOutput = generator.getPackOutput();
        ExistingFileHelper existingFileHelper = event.getExistingFileHelper();
        generator.addProvider(event.includeClient(), new ShinColleItemModelProvider(packOutput,
                existingFileHelper));

        generator.addProvider(event.includeServer(), new ShinColleGlobalLootModifierProvider(packOutput));
    }

}
