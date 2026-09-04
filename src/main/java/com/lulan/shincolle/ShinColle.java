package com.lulan.shincolle;

import com.lulan.shincolle.capability.CapabilityHandler;
import com.lulan.shincolle.client.AiDebugRenderer;
import com.lulan.shincolle.client.ClientConfigScreen;
import com.lulan.shincolle.command.CommandHandler;
import com.lulan.shincolle.config.ConfigMining;
import com.lulan.shincolle.handler.ConfigHandler;
import com.lulan.shincolle.init.*;
import com.lulan.shincolle.loot.ShinColleLootModifiers;
import com.lulan.shincolle.network.ModNetworking;
import com.lulan.shincolle.reference.Reference;
import com.lulan.shincolle.worldgen.ModWorldGen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(Reference.MOD_ID)
public class ShinColle {

    public static final Logger LOGGER = LoggerFactory.getLogger(Reference.MOD_ID);

    public ShinColle() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Register DeferredRegisters to the mod event bus
        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModEntities.ENTITIES.register(modEventBus);
        ModSounds.SOUNDS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModMenuTypes.MENUS.register(modEventBus);
        ModCreativeTabs.CREATIVE_TABS.register(modEventBus);
        ModRecipes.RECIPE_SERIALIZERS.register(modEventBus);
        ModParticles.PARTICLES.register(modEventBus);
        ShinColleLootModifiers.register(modEventBus);

        // Register config
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, ConfigHandler.COMMON_SPEC,
                "shincolle-common.toml");

        // Register config screen (client only)
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientConfigScreen.register();
        }
        // クライアント専用の登録はここでガードする
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientConfigScreen.register();
            MinecraftForge.EVENT_BUS.register(AiDebugRenderer.class);
        }
        // Register lifecycle event listeners
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::onConfigLoad);
        modEventBus.addListener(this::onConfigReload);
        modEventBus.addListener(CapabilityHandler::onRegisterCapabilities);

        // Register ourselves for server and other game events
        MinecraftForge.EVENT_BUS.register(this);

        // Register command handler for Brigadier commands
        MinecraftForge.EVENT_BUS.register(new CommandHandler());

        LOGGER.info("ShinColle: Mod loading initialized.");
    }

    /**
     * Common setup phase - register networking and other cross-side systems.
     */
    private void commonSetup(FMLCommonSetupEvent event) {
        ModNetworking.register();
        ModWorldGen.init();

        // Load mining loot table config (CSV file)
        ConfigMining.load(FMLPaths.CONFIGDIR.get().resolve("shincolle-mining.cfg").toFile());
    }

    /**
     * Called when config is first loaded. Syncs cached static fields.
     */
    private void onConfigLoad(ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() == ConfigHandler.COMMON_SPEC) {
            ConfigHandler.syncConfig();
            LOGGER.info("ShinColle: Config loaded.");
        }
    }

    /**
     * Called when config is reloaded (e.g. via in-game config editor).
     * Re-syncs cached static fields.
     */
    private void onConfigReload(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == ConfigHandler.COMMON_SPEC) {
            ConfigHandler.syncConfig();
            LOGGER.info("ShinColle: Config reloaded.");
        }
    }
}
