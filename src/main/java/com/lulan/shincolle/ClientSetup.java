package com.lulan.shincolle;

import com.lulan.shincolle.client.gui.*;
import com.lulan.shincolle.client.model.*;
import com.lulan.shincolle.client.particle.ParticleSmoke;
import com.lulan.shincolle.client.particle.ParticleSpray;
import com.lulan.shincolle.client.render.*;
import com.lulan.shincolle.client.render.block.RenderDesk;
import com.lulan.shincolle.client.render.block.RenderLargeShipyard;
import com.lulan.shincolle.client.render.block.RenderSmallShipyard;
import com.lulan.shincolle.init.*;
import com.lulan.shincolle.item.BasicEquip;
import com.lulan.shincolle.item.PointerItem;
import com.lulan.shincolle.item.ShipSpawnEgg;
import com.lulan.shincolle.reference.Reference;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetup {

    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        // Base layers for generic ship and mob rendering
        event.registerLayerDefinition(ShipEntityModel.LAYER_LOCATION, ShipEntityModel::createBodyLayer);
        event.registerLayerDefinition(PlaceholderMobModel.LAYER_LOCATION, PlaceholderMobModel::createBodyLayer);

        // Ship model layers
        event.registerLayerDefinition(ModelDestroyerShimakaze.LAYER_LOCATION, ModelDestroyerShimakaze::createBodyLayer);
        event.registerLayerDefinition(ModelDestroyerAkatsuki.LAYER_LOCATION, ModelDestroyerAkatsuki::createBodyLayer);
        event.registerLayerDefinition(ModelDestroyerHibiki.LAYER_LOCATION, ModelDestroyerHibiki::createBodyLayer);
        event.registerLayerDefinition(ModelDestroyerIkazuchi.LAYER_LOCATION, ModelDestroyerIkazuchi::createBodyLayer);
        event.registerLayerDefinition(ModelDestroyerInazuma.LAYER_LOCATION, ModelDestroyerInazuma::createBodyLayer);
        event.registerLayerDefinition(ModelDestroyerI.LAYER_LOCATION, ModelDestroyerI::createBodyLayer);
        event.registerLayerDefinition(ModelDestroyerRo.LAYER_LOCATION, ModelDestroyerRo::createBodyLayer);
        event.registerLayerDefinition(ModelDestroyerHa.LAYER_LOCATION, ModelDestroyerHa::createBodyLayer);
        event.registerLayerDefinition(ModelDestroyerNi.LAYER_LOCATION, ModelDestroyerNi::createBodyLayer);
        event.registerLayerDefinition(ModelBBKongou.LAYER_LOCATION, ModelBBKongou::createBodyLayer);
        event.registerLayerDefinition(ModelBBHiei.LAYER_LOCATION, ModelBBHiei::createBodyLayer);
        event.registerLayerDefinition(ModelBBHaruna.LAYER_LOCATION, ModelBBHaruna::createBodyLayer);
        event.registerLayerDefinition(ModelBBKirishima.LAYER_LOCATION, ModelBBKirishima::createBodyLayer);
        event.registerLayerDefinition(ModelBattleshipNagato.LAYER_LOCATION, ModelBattleshipNagato::createBodyLayer);
        event.registerLayerDefinition(ModelBattleshipYamato.LAYER_LOCATION, ModelBattleshipYamato::createBodyLayer);
        event.registerLayerDefinition(ModelBattleshipRe.LAYER_LOCATION, ModelBattleshipRe::createBodyLayer);
        event.registerLayerDefinition(ModelBattleshipRu.LAYER_LOCATION, ModelBattleshipRu::createBodyLayer);
        event.registerLayerDefinition(ModelBattleshipTa.LAYER_LOCATION, ModelBattleshipTa::createBodyLayer);
        event.registerLayerDefinition(ModelCruiserTenryuu.LAYER_LOCATION, ModelCruiserTenryuu::createBodyLayer);
        event.registerLayerDefinition(ModelCruiserTatsuta.LAYER_LOCATION, ModelCruiserTatsuta::createBodyLayer);
        event.registerLayerDefinition(ModelCruiserAtago.LAYER_LOCATION, ModelCruiserAtago::createBodyLayer);
        event.registerLayerDefinition(ModelCruiserTakao.LAYER_LOCATION, ModelCruiserTakao::createBodyLayer);
        event.registerLayerDefinition(ModelHeavyCruiserNe.LAYER_LOCATION, ModelHeavyCruiserNe::createBodyLayer);
        event.registerLayerDefinition(ModelHeavyCruiserRi.LAYER_LOCATION, ModelHeavyCruiserRi::createBodyLayer);
        event.registerLayerDefinition(ModelCarrierAkagi.LAYER_LOCATION, ModelCarrierAkagi::createBodyLayer);
        event.registerLayerDefinition(ModelCarrierKaga.LAYER_LOCATION, ModelCarrierKaga::createBodyLayer);
        event.registerLayerDefinition(ModelCarrierWo.LAYER_LOCATION, ModelCarrierWo::createBodyLayer);
        event.registerLayerDefinition(ModelSubmRo500.LAYER_LOCATION, ModelSubmRo500::createBodyLayer);
        event.registerLayerDefinition(ModelSubmU511.LAYER_LOCATION, ModelSubmU511::createBodyLayer);
        event.registerLayerDefinition(ModelSubmYo.LAYER_LOCATION, ModelSubmYo::createBodyLayer);
        event.registerLayerDefinition(ModelSubmKa.LAYER_LOCATION, ModelSubmKa::createBodyLayer);
        event.registerLayerDefinition(ModelSubmSo.LAYER_LOCATION, ModelSubmSo::createBodyLayer);
        event.registerLayerDefinition(ModelTransportWa.LAYER_LOCATION, ModelTransportWa::createBodyLayer);
        event.registerLayerDefinition(ModelAirfieldHime.LAYER_LOCATION, ModelAirfieldHime::createBodyLayer);
        event.registerLayerDefinition(ModelBattleshipHime.LAYER_LOCATION, ModelBattleshipHime::createBodyLayer);
        event.registerLayerDefinition(ModelCAHime.LAYER_LOCATION, ModelCAHime::createBodyLayer);
        event.registerLayerDefinition(ModelCarrierHime.LAYER_LOCATION, ModelCarrierHime::createBodyLayer);
        event.registerLayerDefinition(ModelCarrierWDemon.LAYER_LOCATION, ModelCarrierWDemon::createBodyLayer);
        event.registerLayerDefinition(ModelDestroyerHime.LAYER_LOCATION, ModelDestroyerHime::createBodyLayer);
        event.registerLayerDefinition(ModelHarbourHime.LAYER_LOCATION, ModelHarbourHime::createBodyLayer);
        event.registerLayerDefinition(ModelIsolatedHime.LAYER_LOCATION, ModelIsolatedHime::createBodyLayer);
        event.registerLayerDefinition(ModelMidwayHime.LAYER_LOCATION, ModelMidwayHime::createBodyLayer);
        event.registerLayerDefinition(ModelNorthernHime.LAYER_LOCATION, ModelNorthernHime::createBodyLayer);
        event.registerLayerDefinition(ModelSSNH.LAYER_LOCATION, ModelSSNH::createBodyLayer);
        event.registerLayerDefinition(ModelSubmHime.LAYER_LOCATION, ModelSubmHime::createBodyLayer);

        // Mount model layers
        event.registerLayerDefinition(ModelMountAfH.LAYER_LOCATION, ModelMountAfH::createBodyLayer);
        event.registerLayerDefinition(ModelMountBaH.LAYER_LOCATION, ModelMountBaH::createBodyLayer);
        event.registerLayerDefinition(ModelMountCaH.LAYER_LOCATION, ModelMountCaH::createBodyLayer);
        event.registerLayerDefinition(ModelMountCaWD.LAYER_LOCATION, ModelMountCaWD::createBodyLayer);
        event.registerLayerDefinition(ModelMountHbH.LAYER_LOCATION, ModelMountHbH::createBodyLayer);
        event.registerLayerDefinition(ModelMountIsH.LAYER_LOCATION, ModelMountIsH::createBodyLayer);
        event.registerLayerDefinition(ModelMountMiH.LAYER_LOCATION, ModelMountMiH::createBodyLayer);
        event.registerLayerDefinition(ModelMountSuH.LAYER_LOCATION, ModelMountSuH::createBodyLayer);

        // Summon / airplane model layers
        event.registerLayerDefinition(ModelAirplane.LAYER_LOCATION, ModelAirplane::createBodyLayer);
        event.registerLayerDefinition(ModelAirplaneZero.LAYER_LOCATION, ModelAirplaneZero::createBodyLayer);
        event.registerLayerDefinition(ModelAirplaneT.LAYER_LOCATION, ModelAirplaneT::createBodyLayer);
        event.registerLayerDefinition(ModelTakoyaki.LAYER_LOCATION, ModelTakoyaki::createBodyLayer);
        event.registerLayerDefinition(ModelFloatingFort.LAYER_LOCATION, ModelFloatingFort::createBodyLayer);
        event.registerLayerDefinition(ModelRensouhou.LAYER_LOCATION, ModelRensouhou::createBodyLayer);
        event.registerLayerDefinition(ModelRensouhouS.LAYER_LOCATION, ModelRensouhouS::createBodyLayer);
        event.registerLayerDefinition(ModelAbyssMissile.LAYER_LOCATION, ModelAbyssMissile::createBodyLayer);

        // Misc model layers
        event.registerLayerDefinition(ModelBasicEntityItem.LAYER_LOCATION, ModelBasicEntityItem::createBodyLayer);
        event.registerLayerDefinition(ModelBlockDesk.LAYER_LOCATION, ModelBlockDesk::createBodyLayer);
        event.registerLayerDefinition(ModelLargeShipyard.LAYER_LOCATION, ModelLargeShipyard::createBodyLayer);
        event.registerLayerDefinition(ModelSmallShipyard.LAYER_LOCATION, ModelSmallShipyard::createBodyLayer);
        event.registerLayerDefinition(ModelVortex.LAYER_LOCATION, ModelVortex::createBodyLayer);
    }

    private static ResourceLocation tex(String name) {
        return new ResourceLocation(Reference.MOD_ID, "textures/entity/" + name.toLowerCase() + ".png");
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        // ========== Friendly Ships ==========

        // --- Destroyers ---
        event.registerEntityRenderer(ModEntities.DESTROYER_SHIMAKAZE.get(),
                ctx -> new ShipEntityRenderer<>(ctx,
                        new ModelDestroyerShimakaze(ctx.bakeLayer(ModelDestroyerShimakaze.LAYER_LOCATION)),
                        tex("EntityDestroyerShimakaze"), 0.5F));
        event.registerEntityRenderer(ModEntities.DESTROYER_AKATSUKI.get(),
                ctx -> new ShipEntityRenderer<>(ctx,
                        new ModelDestroyerAkatsuki(ctx.bakeLayer(ModelDestroyerAkatsuki.LAYER_LOCATION)),
                        tex("EntityDestroyerAkatsuki"), 0.5F));
        event.registerEntityRenderer(ModEntities.DESTROYER_HIBIKI.get(),
                ctx -> new ShipEntityRenderer<>(ctx,
                        new ModelDestroyerHibiki(ctx.bakeLayer(ModelDestroyerHibiki.LAYER_LOCATION)),
                        tex("EntityDestroyerHibiki"), 0.5F));
        event.registerEntityRenderer(ModEntities.DESTROYER_IKAZUCHI.get(),
                ctx -> new ShipEntityRenderer<>(ctx,
                        new ModelDestroyerIkazuchi(ctx.bakeLayer(ModelDestroyerIkazuchi.LAYER_LOCATION)),
                        tex("EntityDestroyerIkazuchi"), 0.5F));
        event.registerEntityRenderer(ModEntities.DESTROYER_INAZUMA.get(),
                ctx -> new ShipEntityRenderer<>(ctx,
                        new ModelDestroyerInazuma(ctx.bakeLayer(ModelDestroyerInazuma.LAYER_LOCATION)),
                        tex("EntityDestroyerInazuma"), 0.5F));
        event.registerEntityRenderer(ModEntities.DESTROYER_I.get(),
                ctx -> new ShipEntityRenderer<>(ctx, new ModelDestroyerI(ctx.bakeLayer(ModelDestroyerI.LAYER_LOCATION)),
                        tex("EntityDestroyerI"), 0.3F));
        event.registerEntityRenderer(ModEntities.DESTROYER_RO.get(),
                ctx -> new ShipEntityRenderer<>(ctx,
                        new ModelDestroyerRo(ctx.bakeLayer(ModelDestroyerRo.LAYER_LOCATION)), tex("EntityDestroyerRo"),
                        0.3F));
        event.registerEntityRenderer(ModEntities.DESTROYER_HA.get(),
                ctx -> new ShipEntityRenderer<>(ctx,
                        new ModelDestroyerHa(ctx.bakeLayer(ModelDestroyerHa.LAYER_LOCATION)), tex("EntityDestroyerHa"),
                        0.3F));
        event.registerEntityRenderer(ModEntities.DESTROYER_NI.get(),
                ctx -> new ShipEntityRenderer<>(ctx,
                        new ModelDestroyerNi(ctx.bakeLayer(ModelDestroyerNi.LAYER_LOCATION)), tex("EntityDestroyerNi"),
                        0.3F));

        // --- Battleships ---
        event.registerEntityRenderer(ModEntities.BB_KONGOU.get(),
                ctx -> new ShipEntityRenderer<>(ctx, new ModelBBKongou(ctx.bakeLayer(ModelBBKongou.LAYER_LOCATION)),
                        tex("EntityBBKongou"), 0.5F));
        event.registerEntityRenderer(ModEntities.BB_HIEI.get(),
                ctx -> new ShipEntityRenderer<>(ctx, new ModelBBHiei(ctx.bakeLayer(ModelBBHiei.LAYER_LOCATION)),
                        tex("EntityBBHiei"), 0.5F));
        event.registerEntityRenderer(ModEntities.BB_HARUNA.get(),
                ctx -> new ShipEntityRenderer<>(ctx, new ModelBBHaruna(ctx.bakeLayer(ModelBBHaruna.LAYER_LOCATION)),
                        tex("EntityBBHaruna"), 0.5F));
        event.registerEntityRenderer(ModEntities.BB_KIRISHIMA.get(),
                ctx -> new ShipEntityRenderer<>(ctx,
                        new ModelBBKirishima(ctx.bakeLayer(ModelBBKirishima.LAYER_LOCATION)), tex("EntityBBKirishima"),
                        0.5F));
        event.registerEntityRenderer(ModEntities.BB_NAGATO.get(),
                ctx -> new ShipEntityRenderer<>(ctx,
                        new ModelBattleshipNagato(ctx.bakeLayer(ModelBattleshipNagato.LAYER_LOCATION)),
                        tex("EntityBattleshipNagato"), 0.5F));
        event.registerEntityRenderer(ModEntities.BB_YAMATO.get(),
                ctx -> new ShipEntityRenderer<>(ctx,
                        new ModelBattleshipYamato(ctx.bakeLayer(ModelBattleshipYamato.LAYER_LOCATION)),
                        tex("EntityBattleshipYamato"), 0.5F));
        event.registerEntityRenderer(ModEntities.BB_RE.get(),
                ctx -> new ShipEntityRenderer<>(ctx,
                        new ModelBattleshipRe(ctx.bakeLayer(ModelBattleshipRe.LAYER_LOCATION)),
                        tex("EntityBattleshipRe"), 0.5F));
        event.registerEntityRenderer(ModEntities.BB_RU.get(),
                ctx -> new ShipEntityRenderer<>(ctx,
                        new ModelBattleshipRu(ctx.bakeLayer(ModelBattleshipRu.LAYER_LOCATION)),
                        tex("EntityBattleshipRu"), 0.5F));
        event.registerEntityRenderer(ModEntities.BB_TA.get(),
                ctx -> new ShipEntityRenderer<>(ctx,
                        new ModelBattleshipTa(ctx.bakeLayer(ModelBattleshipTa.LAYER_LOCATION)),
                        tex("EntityBattleshipTa"), 0.5F));

        // --- Light Cruisers ---
        event.registerEntityRenderer(ModEntities.CL_TENRYUU.get(),
                ctx -> new ShipEntityRenderer<>(ctx,
                        new ModelCruiserTenryuu(ctx.bakeLayer(ModelCruiserTenryuu.LAYER_LOCATION)),
                        tex("EntityCruiserTenryuu"), 0.5F));
        event.registerEntityRenderer(ModEntities.CL_TATSUTA.get(),
                ctx -> new ShipEntityRenderer<>(ctx,
                        new ModelCruiserTatsuta(ctx.bakeLayer(ModelCruiserTatsuta.LAYER_LOCATION)),
                        tex("EntityCruiserTatsuta"), 0.5F));

        // --- Heavy Cruisers ---
        event.registerEntityRenderer(ModEntities.CA_ATAGO.get(),
                ctx -> new ShipEntityRenderer<>(ctx,
                        new ModelCruiserAtago(ctx.bakeLayer(ModelCruiserAtago.LAYER_LOCATION)),
                        tex("EntityCruiserAtago"), 0.5F));
        event.registerEntityRenderer(ModEntities.CA_TAKAO.get(),
                ctx -> new ShipEntityRenderer<>(ctx,
                        new ModelCruiserTakao(ctx.bakeLayer(ModelCruiserTakao.LAYER_LOCATION)),
                        tex("EntityCruiserTakao"), 0.5F));
        event.registerEntityRenderer(ModEntities.CA_NE.get(),
                ctx -> new ShipEntityRenderer<>(ctx,
                        new ModelHeavyCruiserNe(ctx.bakeLayer(ModelHeavyCruiserNe.LAYER_LOCATION)),
                        tex("EntityHeavyCruiserNe"), 0.5F));
        event.registerEntityRenderer(ModEntities.CA_RI.get(),
                ctx -> new ShipEntityRenderer<>(ctx,
                        new ModelHeavyCruiserRi(ctx.bakeLayer(ModelHeavyCruiserRi.LAYER_LOCATION)),
                        tex("EntityHeavyCruiserRi"), 0.5F));

        // --- Carriers ---
        event.registerEntityRenderer(ModEntities.CV_AKAGI.get(),
                ctx -> new ShipEntityRenderer<>(ctx,
                        new ModelCarrierAkagi(ctx.bakeLayer(ModelCarrierAkagi.LAYER_LOCATION)),
                        tex("EntityCarrierAkagi"), 0.5F));
        event.registerEntityRenderer(ModEntities.CV_KAGA.get(),
                ctx -> new ShipEntityRenderer<>(ctx,
                        new ModelCarrierKaga(ctx.bakeLayer(ModelCarrierKaga.LAYER_LOCATION)), tex("EntityCarrierKaga"),
                        0.5F));
        event.registerEntityRenderer(ModEntities.CV_WO.get(),
                ctx -> new ShipEntityRenderer<>(ctx, new ModelCarrierWo(ctx.bakeLayer(ModelCarrierWo.LAYER_LOCATION)),
                        tex("EntityCarrierWo"), 0.5F));

        // --- Submarines ---
        event.registerEntityRenderer(ModEntities.SS_RO500.get(),
                ctx -> new ShipEntityRenderer<>(ctx, new ModelSubmRo500(ctx.bakeLayer(ModelSubmRo500.LAYER_LOCATION)),
                        tex("EntitySubmRo500"), 0.4F));
        event.registerEntityRenderer(ModEntities.SS_U511.get(),
                ctx -> new ShipEntityRenderer<>(ctx, new ModelSubmU511(ctx.bakeLayer(ModelSubmU511.LAYER_LOCATION)),
                        tex("EntitySubmU511"), 0.4F));
        event.registerEntityRenderer(ModEntities.SS_YO.get(),
                ctx -> new ShipEntityRenderer<>(ctx, new ModelSubmYo(ctx.bakeLayer(ModelSubmYo.LAYER_LOCATION)),
                        tex("EntitySubmYo"), 0.3F));
        event.registerEntityRenderer(ModEntities.SS_KA.get(),
                ctx -> new ShipEntityRenderer<>(ctx, new ModelSubmKa(ctx.bakeLayer(ModelSubmKa.LAYER_LOCATION)),
                        tex("EntitySubmKa"), 0.3F));
        event.registerEntityRenderer(ModEntities.SS_SO.get(),
                ctx -> new ShipEntityRenderer<>(ctx, new ModelSubmSo(ctx.bakeLayer(ModelSubmSo.LAYER_LOCATION)),
                        tex("EntitySubmSo"), 0.3F));

        // --- Transport ---
        event.registerEntityRenderer(ModEntities.AP_WA.get(),
                ctx -> new ShipEntityRenderer<>(ctx,
                        new ModelTransportWa(ctx.bakeLayer(ModelTransportWa.LAYER_LOCATION)), tex("EntityTransportWa"),
                        0.5F));

        // --- Hime / Boss Ships ---
        event.registerEntityRenderer(ModEntities.AIRFIELD_HIME.get(),
                ctx -> new ShipEntityRenderer<>(ctx,
                        new ModelAirfieldHime(ctx.bakeLayer(ModelAirfieldHime.LAYER_LOCATION)),
                        tex("EntityAirfieldHime"), 0.6F));
        event.registerEntityRenderer(ModEntities.BB_HIME.get(),
                ctx -> new ShipEntityRenderer<>(ctx,
                        new ModelBattleshipHime(ctx.bakeLayer(ModelBattleshipHime.LAYER_LOCATION)),
                        tex("EntityBattleshipHime"), 0.6F));
        event.registerEntityRenderer(ModEntities.CA_HIME.get(),
                ctx -> new ShipEntityRenderer<>(ctx, new ModelCAHime(ctx.bakeLayer(ModelCAHime.LAYER_LOCATION)),
                        tex("EntityCAHime"), 0.6F));
        event.registerEntityRenderer(ModEntities.CV_HIME.get(),
                ctx -> new ShipEntityRenderer<>(ctx,
                        new ModelCarrierHime(ctx.bakeLayer(ModelCarrierHime.LAYER_LOCATION)), tex("EntityCarrierHime"),
                        0.6F));
        event.registerEntityRenderer(ModEntities.CV_WD.get(),
                ctx -> new ShipEntityRenderer<>(ctx,
                        new ModelCarrierWDemon(ctx.bakeLayer(ModelCarrierWDemon.LAYER_LOCATION)),
                        tex("EntityCarrierWDemon"), 0.6F));
        event.registerEntityRenderer(ModEntities.DD_HIME.get(),
                ctx -> new ShipEntityRenderer<>(ctx,
                        new ModelDestroyerHime(ctx.bakeLayer(ModelDestroyerHime.LAYER_LOCATION)),
                        tex("EntityDestroyerHime"), 0.5F));
        event.registerEntityRenderer(ModEntities.HARBOUR_HIME.get(),
                ctx -> new ShipEntityRenderer<>(ctx,
                        new ModelHarbourHime(ctx.bakeLayer(ModelHarbourHime.LAYER_LOCATION)), tex("EntityHarbourHime"),
                        0.6F));
        event.registerEntityRenderer(ModEntities.ISOLATED_HIME.get(),
                ctx -> new ShipEntityRenderer<>(ctx,
                        new ModelIsolatedHime(ctx.bakeLayer(ModelIsolatedHime.LAYER_LOCATION)),
                        tex("EntityIsolatedHime"), 0.6F));
        event.registerEntityRenderer(ModEntities.MIDWAY_HIME.get(),
                ctx -> new ShipEntityRenderer<>(ctx, new ModelMidwayHime(ctx.bakeLayer(ModelMidwayHime.LAYER_LOCATION)),
                        tex("EntityMidwayHime"), 0.6F));
        event.registerEntityRenderer(ModEntities.NORTHERN_HIME.get(),
                ctx -> new ShipEntityRenderer<>(ctx,
                        new ModelNorthernHime(ctx.bakeLayer(ModelNorthernHime.LAYER_LOCATION)),
                        tex("EntityNorthernHime"), 0.6F));
        event.registerEntityRenderer(ModEntities.SSNH.get(),
                ctx -> new ShipEntityRenderer<>(ctx, new ModelSSNH(ctx.bakeLayer(ModelSSNH.LAYER_LOCATION)),
                        tex("EntitySubmHimeNew"), 0.6F));
        event.registerEntityRenderer(ModEntities.SS_HIME.get(),
                ctx -> new ShipEntityRenderer<>(ctx, new ModelSubmHime(ctx.bakeLayer(ModelSubmHime.LAYER_LOCATION)),
                        tex("EntitySubmHime"), 0.6F));

        // ========== Hostile Ships (same model, same texture as friendly) ==========

        // --- Hostile Destroyers ---
        event.registerEntityRenderer(ModEntities.DESTROYER_SHIMAKAZE_MOB.get(),
                ctx -> new PlaceholderMobRenderer<>(ctx,
                        new ModelDestroyerShimakaze(ctx.bakeLayer(ModelDestroyerShimakaze.LAYER_LOCATION)),
                        tex("EntityDestroyerShimakaze"), 0.5F));
        event.registerEntityRenderer(ModEntities.DESTROYER_AKATSUKI_MOB.get(),
                ctx -> new PlaceholderMobRenderer<>(ctx,
                        new ModelDestroyerAkatsuki(ctx.bakeLayer(ModelDestroyerAkatsuki.LAYER_LOCATION)),
                        tex("EntityDestroyerAkatsuki"), 0.5F));
        event.registerEntityRenderer(ModEntities.DESTROYER_HIBIKI_MOB.get(),
                ctx -> new PlaceholderMobRenderer<>(ctx,
                        new ModelDestroyerHibiki(ctx.bakeLayer(ModelDestroyerHibiki.LAYER_LOCATION)),
                        tex("EntityDestroyerHibiki"), 0.5F));
        event.registerEntityRenderer(ModEntities.DESTROYER_IKAZUCHI_MOB.get(),
                ctx -> new PlaceholderMobRenderer<>(ctx,
                        new ModelDestroyerIkazuchi(ctx.bakeLayer(ModelDestroyerIkazuchi.LAYER_LOCATION)),
                        tex("EntityDestroyerIkazuchi"), 0.5F));
        event.registerEntityRenderer(ModEntities.DESTROYER_INAZUMA_MOB.get(),
                ctx -> new PlaceholderMobRenderer<>(ctx,
                        new ModelDestroyerInazuma(ctx.bakeLayer(ModelDestroyerInazuma.LAYER_LOCATION)),
                        tex("EntityDestroyerInazuma"), 0.5F));

        // --- Hostile Battleships ---
        event.registerEntityRenderer(ModEntities.BB_KONGOU_MOB.get(),
                ctx -> new PlaceholderMobRenderer<>(ctx, new ModelBBKongou(ctx.bakeLayer(ModelBBKongou.LAYER_LOCATION)),
                        tex("EntityBBKongou"), 0.5F));
        event.registerEntityRenderer(ModEntities.BB_HIEI_MOB.get(),
                ctx -> new PlaceholderMobRenderer<>(ctx, new ModelBBHiei(ctx.bakeLayer(ModelBBHiei.LAYER_LOCATION)),
                        tex("EntityBBHiei"), 0.5F));
        event.registerEntityRenderer(ModEntities.BB_HARUNA_MOB.get(),
                ctx -> new PlaceholderMobRenderer<>(ctx, new ModelBBHaruna(ctx.bakeLayer(ModelBBHaruna.LAYER_LOCATION)),
                        tex("EntityBBHaruna"), 0.5F));
        // 2026/04/07：GitHub Copilotによって確認済み - hostile系はPlaceholderMobRenderer経路を維持。
        event.registerEntityRenderer(ModEntities.BB_KIRISHIMA_MOB.get(),
                ctx -> new PlaceholderMobRenderer<>(ctx,
                        new ModelBBKirishima(ctx.bakeLayer(ModelBBKirishima.LAYER_LOCATION)), tex("EntityBBKirishima"),
                        0.5F));
        event.registerEntityRenderer(ModEntities.BB_NAGATO_MOB.get(),
                ctx -> new PlaceholderMobRenderer<>(ctx,
                        new ModelBattleshipNagato(ctx.bakeLayer(ModelBattleshipNagato.LAYER_LOCATION)),
                        tex("EntityBattleshipNagato"), 0.5F));
        event.registerEntityRenderer(ModEntities.BB_YAMATO_MOB.get(),
                ctx -> new PlaceholderMobRenderer<>(ctx,
                        new ModelBattleshipYamato(ctx.bakeLayer(ModelBattleshipYamato.LAYER_LOCATION)),
                        tex("EntityBattleshipYamato"), 0.5F));

        // --- Hostile Cruisers ---
        event.registerEntityRenderer(ModEntities.CL_TENRYUU_MOB.get(),
                ctx -> new PlaceholderMobRenderer<>(ctx,
                        new ModelCruiserTenryuu(ctx.bakeLayer(ModelCruiserTenryuu.LAYER_LOCATION)),
                        tex("EntityCruiserTenryuu"), 0.5F));
        event.registerEntityRenderer(ModEntities.CL_TATSUTA_MOB.get(),
                ctx -> new PlaceholderMobRenderer<>(ctx,
                        new ModelCruiserTatsuta(ctx.bakeLayer(ModelCruiserTatsuta.LAYER_LOCATION)),
                        tex("EntityCruiserTatsuta"), 0.5F));
        event.registerEntityRenderer(ModEntities.CA_ATAGO_MOB.get(),
                ctx -> new PlaceholderMobRenderer<>(ctx,
                        new ModelCruiserAtago(ctx.bakeLayer(ModelCruiserAtago.LAYER_LOCATION)),
                        tex("EntityCruiserAtago"), 0.5F));
        event.registerEntityRenderer(ModEntities.CA_TAKAO_MOB.get(),
                ctx -> new PlaceholderMobRenderer<>(ctx,
                        new ModelCruiserTakao(ctx.bakeLayer(ModelCruiserTakao.LAYER_LOCATION)),
                        tex("EntityCruiserTakao"), 0.5F));

        // --- Hostile Carriers ---
        event.registerEntityRenderer(ModEntities.CV_AKAGI_MOB.get(),
                ctx -> new PlaceholderMobRenderer<>(ctx,
                        new ModelCarrierAkagi(ctx.bakeLayer(ModelCarrierAkagi.LAYER_LOCATION)),
                        tex("EntityCarrierAkagi"), 0.5F));
        event.registerEntityRenderer(ModEntities.CV_KAGA_MOB.get(),
                ctx -> new PlaceholderMobRenderer<>(ctx,
                        new ModelCarrierKaga(ctx.bakeLayer(ModelCarrierKaga.LAYER_LOCATION)), tex("EntityCarrierKaga"),
                        0.5F));

        // --- Hostile Submarines ---
        event.registerEntityRenderer(ModEntities.SS_RO500_MOB.get(),
                ctx -> new PlaceholderMobRenderer<>(ctx,
                        new ModelSubmRo500(ctx.bakeLayer(ModelSubmRo500.LAYER_LOCATION)), tex("EntitySubmRo500"),
                        0.4F));
        event.registerEntityRenderer(ModEntities.SS_U511_MOB.get(),
                ctx -> new PlaceholderMobRenderer<>(ctx, new ModelSubmU511(ctx.bakeLayer(ModelSubmU511.LAYER_LOCATION)),
                        tex("EntitySubmU511"), 0.4F));

        // ========== Mount Entities ==========
        event.registerEntityRenderer(ModEntities.MOUNT_AFH.get(),
                ctx -> new PlaceholderMobRenderer<>(ctx, new ModelMountAfH(ctx.bakeLayer(ModelMountAfH.LAYER_LOCATION)),
                        tex("EntityMountAfH"), 0.6F));
        event.registerEntityRenderer(ModEntities.MOUNT_BAH.get(),
                ctx -> new PlaceholderMobRenderer<>(ctx, new ModelMountBaH(ctx.bakeLayer(ModelMountBaH.LAYER_LOCATION)),
                        tex("EntityMountBaH"), 0.6F));
        event.registerEntityRenderer(ModEntities.MOUNT_CAH.get(),
                ctx -> new PlaceholderMobRenderer<>(ctx, new ModelMountCaH(ctx.bakeLayer(ModelMountCaH.LAYER_LOCATION)),
                        tex("EntityMountCaH"), 0.5F));
        event.registerEntityRenderer(ModEntities.MOUNT_CAWD.get(),
                ctx -> new PlaceholderMobRenderer<>(ctx,
                        new ModelMountCaWD(ctx.bakeLayer(ModelMountCaWD.LAYER_LOCATION)), tex("EntityMountCaWD"),
                        0.5F));
        event.registerEntityRenderer(ModEntities.MOUNT_HBH.get(),
                ctx -> new PlaceholderMobRenderer<>(ctx, new ModelMountHbH(ctx.bakeLayer(ModelMountHbH.LAYER_LOCATION)),
                        tex("EntityMountHbH"), 0.6F));
        event.registerEntityRenderer(ModEntities.MOUNT_ISH.get(),
                ctx -> new PlaceholderMobRenderer<>(ctx, new ModelMountIsH(ctx.bakeLayer(ModelMountIsH.LAYER_LOCATION)),
                        tex("EntityMountIsH"), 0.6F));
        event.registerEntityRenderer(ModEntities.MOUNT_MIH.get(),
                ctx -> new PlaceholderMobRenderer<>(ctx, new ModelMountMiH(ctx.bakeLayer(ModelMountMiH.LAYER_LOCATION)),
                        tex("EntityMountMiH"), 0.6F));
        event.registerEntityRenderer(ModEntities.MOUNT_SUH.get(),
                ctx -> new PlaceholderMobRenderer<>(ctx, new ModelMountSuH(ctx.bakeLayer(ModelMountSuH.LAYER_LOCATION)),
                        tex("EntityMountSuH"), 0.6F));

        // ========== Airplane / Summon Entities ==========
        event.registerEntityRenderer(ModEntities.AIRPLANE.get(),
                ctx -> new PlaceholderMobRenderer<>(ctx, new ModelAirplane(ctx.bakeLayer(ModelAirplane.LAYER_LOCATION)),
                        tex("EntityAircraft"), 0.3F));
        event.registerEntityRenderer(ModEntities.AIRPLANE_ZERO.get(),
                ctx -> new PlaceholderMobRenderer<>(ctx,
                        new ModelAirplaneZero(ctx.bakeLayer(ModelAirplaneZero.LAYER_LOCATION)),
                        tex("EntityAirplaneZero"), 0.3F));
        event.registerEntityRenderer(ModEntities.AIRPLANE_ZERO_MOB.get(),
                ctx -> new PlaceholderMobRenderer<>(ctx,
                        new ModelAirplaneZero(ctx.bakeLayer(ModelAirplaneZero.LAYER_LOCATION)),
                        tex("EntityAirplaneZero"), 0.3F));
        event.registerEntityRenderer(ModEntities.AIRPLANE_TAKOYAKI.get(),
                ctx -> new PlaceholderMobRenderer<>(ctx, new ModelTakoyaki(ctx.bakeLayer(ModelTakoyaki.LAYER_LOCATION)),
                        tex("EntityAircraftTakoyaki"), 0.3F));
        event.registerEntityRenderer(ModEntities.AIRPLANE_T.get(),
                ctx -> new PlaceholderMobRenderer<>(ctx,
                        new ModelAirplaneT(ctx.bakeLayer(ModelAirplaneT.LAYER_LOCATION)), tex("EntityAirplaneT"),
                        0.3F));
        event.registerEntityRenderer(ModEntities.AIRPLANE_T_MOB.get(),
                ctx -> new PlaceholderMobRenderer<>(ctx,
                        new ModelAirplaneT(ctx.bakeLayer(ModelAirplaneT.LAYER_LOCATION)), tex("EntityAirplaneT"),
                        0.3F));
        event.registerEntityRenderer(ModEntities.FLOATING_FORT.get(),
                ctx -> new PlaceholderMobRenderer<>(ctx,
                        new ModelFloatingFort(ctx.bakeLayer(ModelFloatingFort.LAYER_LOCATION)),
                        tex("EntityFloatingFort"), 0.4F));
        event.registerEntityRenderer(ModEntities.RENSOUHOU.get(),
                ctx -> new PlaceholderMobRenderer<>(ctx,
                        new ModelRensouhou(ctx.bakeLayer(ModelRensouhou.LAYER_LOCATION)), tex("EntityRensouhou"),
                        0.3F));
        event.registerEntityRenderer(ModEntities.RENSOUHOU_MOB.get(),
                ctx -> new PlaceholderMobRenderer<>(ctx,
                        new ModelRensouhou(ctx.bakeLayer(ModelRensouhou.LAYER_LOCATION)), tex("EntityRensouhou"),
                        0.3F));
        event.registerEntityRenderer(ModEntities.RENSOUHOU_S.get(),
                ctx -> new PlaceholderMobRenderer<>(ctx,
                        new ModelRensouhouS(ctx.bakeLayer(ModelRensouhouS.LAYER_LOCATION)), tex("EntityRensouhouS"),
                        0.3F));

        // ========== Raw Entity Types ==========
        event.registerEntityRenderer(ModEntities.ABYSS_MISSILE.get(), RenderAbyssMissile::new);
        // 2026/04/07：GitHub Copilotによって確認済み -
        // 1.10.2ではInvisible指定のため、beam/staticはNoop維持。
        event.registerEntityRenderer(ModEntities.PROJECTILE_BEAM.get(), NoopEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.PROJECTILE_STATIC.get(), NoopEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.BASIC_ENTITY_ITEM.get(), RenderBasicEntityItem::new);
        event.registerEntityRenderer(ModEntities.FISHING_HOOK.get(), RenderShipFishing::new);

        // ========== Block Entity Renderers ==========
        event.registerBlockEntityRenderer(ModBlockEntities.SMALL_SHIPYARD.get(), RenderSmallShipyard::new);
        event.registerBlockEntityRenderer(ModBlockEntities.GRUDGE_HEAVY_MULTI.get(), RenderLargeShipyard::new);
        event.registerBlockEntityRenderer(ModBlockEntities.DESK.get(), RenderDesk::new);
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(ModMenuTypes.SHIP_INVENTORY.get(), GuiShipInventory::new);
            MenuScreens.register(ModMenuTypes.SMALL_SHIPYARD.get(), GuiSmallShipyard::new);
            MenuScreens.register(ModMenuTypes.LARGE_SHIPYARD.get(), GuiLargeShipyard::new);
            MenuScreens.register(ModMenuTypes.DESK.get(), GuiDesk::new);
            MenuScreens.register(ModMenuTypes.FORMATION.get(), GuiFormation::new);
            MenuScreens.register(ModMenuTypes.CRANE.get(), GuiCrane::new);
            MenuScreens.register(ModMenuTypes.VOL_CORE.get(), GuiVolCore::new);
            MenuScreens.register(ModMenuTypes.RECIPE_PAPER.get(), GuiRecipePaper::new);

            // Register equipment variant texture properties
            registerEquipIconProperty(ModItems.EQUIP_CANNON.get());
            registerEquipIconProperty(ModItems.EQUIP_AIRPLANE.get());
            registerEquipIconProperty(ModItems.EQUIP_DRUM.get());

            // Register pointer mode texture property (mode stored in PointerItem NBT)
            registerPointerModeProperty();

            // Register spawn egg variant texture property
            ItemProperties.register(ModItems.SHIP_SPAWN_EGG.get(),
                    new ResourceLocation(Reference.MOD_ID, "egg_icon"),
                    (stack, level, entity, seed) -> (float) ShipSpawnEgg.getEggIcon(stack));
        });
    }

    /**
     * Register the "equip_icon" item property for equipment items that have
     * variant textures. Maps EquipMeta NBT → icon index via getIconFromDamage().
     */
    private static void registerEquipIconProperty(Item item) {
        ItemProperties.register(item,
                new ResourceLocation(Reference.MOD_ID, "equip_icon"),
                (stack, level, entity, seed) -> {
                    if (stack.getItem() instanceof BasicEquip equip) {
                        return (float) equip.getIconFromDamage(BasicEquip.getEquipMeta(stack));
                    }
                    return 0.0F;
                });
    }

    /**
     * Register the "pointer_mode" item property for PointerItem.
     * Maps PointerItem NBT Mode (0-3) to model overrides in pointer.json.
     */
    private static void registerPointerModeProperty() {
        ItemProperties.register(ModItems.POINTER.get(),
                new ResourceLocation(Reference.MOD_ID, "pointer_mode"),
                (stack, level, entity, seed) -> (float) PointerItem.getMode(stack));
    }

    /**
     * Register particle providers/factories for all custom particle types.
     * <p>
     * Sprite-based particles (Spray, Smoke): Use real providers that create
     * actual particle instances with sprite sets.
     * <p>
     * Custom-rendered particles (Laser, Lightning, etc.): Use placeholder
     * providers.
     * These particles use custom vertex rendering and are spawned directly via
     * {@code Minecraft.getInstance().particleEngine.add()} in ParticleHelper,
     * bypassing the provider system. Providers must still be registered to satisfy
     * Forge's particle type registry requirements.
     */
    @SubscribeEvent
    public static void registerParticleProviders(RegisterParticleProvidersEvent event) {
        // --- Sprite-based particles: real providers ---

        // Spray particle: stores shared SpriteSet for reuse in tick()
        event.registerSpriteSet(ModParticles.SPRAY.get(), sprites -> {
            ParticleSpray.setSharedSprites(sprites);
            return (type, level, x, y, z, xSpeed, ySpeed, zSpeed) -> new ParticleSpray(level, x, y, z, xSpeed, ySpeed,
                    zSpeed, 0);
        });

        // Smoke particle: requires SpriteSet for animated smoke
        event.registerSpriteSet(ModParticles.SMOKE_CUSTOM.get(), sprites -> {
            ParticleSmoke.setSharedSprites(sprites);   // 追加
            return (type, level, x, y, z, xSpeed, ySpeed, zSpeed) -> new ParticleSmoke(level, x, y, z, xSpeed, ySpeed, zSpeed, 1.0F, sprites);
        });

        // --- Custom-rendered particles: placeholder providers ---
        // These particles extend Particle (not TextureSheetParticle) and use custom
        // vertex rendering. They are spawned directly by ParticleHelper and don't
        // use the provider's createParticle method in normal operation.
        event.registerSpriteSet(ModParticles.LASER.get(), PlaceholderParticleProvider::new);
        event.registerSpriteSet(ModParticles.CHI.get(), PlaceholderParticleProvider::new);
        event.registerSpriteSet(ModParticles.LIGHTNING.get(), PlaceholderParticleProvider::new);
        event.registerSpriteSet(ModParticles.EMOTION.get(), PlaceholderParticleProvider::new);
        event.registerSpriteSet(ModParticles.SPARKLE.get(), PlaceholderParticleProvider::new);
        event.registerSpriteSet(ModParticles.SWEEP.get(), PlaceholderParticleProvider::new);
        event.registerSpriteSet(ModParticles.CUBE.get(), PlaceholderParticleProvider::new);
        event.registerSpriteSet(ModParticles.GRADIENT.get(), PlaceholderParticleProvider::new);
        event.registerSpriteSet(ModParticles.SPHERE_LIGHT.get(), PlaceholderParticleProvider::new);
        event.registerSpriteSet(ModParticles.MISS_TEXT.get(), PlaceholderParticleProvider::new);
        event.registerSpriteSet(ModParticles.TEAM_CIRCLE.get(), PlaceholderParticleProvider::new);
        event.registerSpriteSet(ModParticles.LINE.get(), PlaceholderParticleProvider::new);
        event.registerSpriteSet(ModParticles.AP_FIST.get(), PlaceholderParticleProvider::new);
        event.registerSpriteSet(ModParticles.CRANING.get(), PlaceholderParticleProvider::new);
    }

    /**
     * Placeholder particle provider for custom-rendered particle types.
     * These particles are normally spawned directly via ParticleHelper, not through
     * level.addParticle(). This provider exists to satisfy Forge's requirement that
     * all registered particle types have a provider.
     */
    private record PlaceholderParticleProvider(SpriteSet sprites)
            implements ParticleProvider<SimpleParticleType> {

        @Override
        public Particle createParticle(SimpleParticleType type,
                                       ClientLevel level, double x, double y, double z,
                                       double xSpeed, double ySpeed, double zSpeed) {
            TextureSheetParticle particle = new PlaceholderParticle(level, x, y, z, xSpeed, ySpeed, zSpeed);
            particle.pickSprite(this.sprites);
            return particle;
        }
    }

    /**
     * Minimal placeholder particle implementation for particle types that are
     * normally spawned directly (not through the provider system).
     */
    private static class PlaceholderParticle extends TextureSheetParticle {
        protected PlaceholderParticle(ClientLevel level, double x, double y, double z,
                                      double xSpeed, double ySpeed, double zSpeed) {
            super(level, x, y, z, xSpeed, ySpeed, zSpeed);
            this.lifetime = 20;
            this.gravity = 0.0F;
            this.hasPhysics = false;
        }

        @Override
        public net.minecraft.client.particle.ParticleRenderType getRenderType() {
            return net.minecraft.client.particle.ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        }
    }
}
