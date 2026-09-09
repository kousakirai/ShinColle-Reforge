package com.lulan.shincolle.init;

import com.lulan.shincolle.entity.*;
import com.lulan.shincolle.entity.battleship.*;
import com.lulan.shincolle.entity.carrier.*;
import com.lulan.shincolle.entity.cruiser.*;
import com.lulan.shincolle.entity.destroyer.*;
import com.lulan.shincolle.entity.hime.*;
import com.lulan.shincolle.entity.mounts.*;
import com.lulan.shincolle.entity.other.*;
import com.lulan.shincolle.entity.submarine.*;
import com.lulan.shincolle.entity.transport.EntityTransportWa;
import com.lulan.shincolle.reference.Reference;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModEntities {

    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(
            ForgeRegistries.ENTITY_TYPES,
            Reference.MOD_ID);

    // ========== Friendly Ships (MobCategory.CREATURE) ==========

    // --- Destroyers ---
    public static final RegistryObject<EntityType<EntityDestroyerShimakaze>> DESTROYER_SHIMAKAZE = ENTITIES
            .register("destroyer_shimakaze",
                    () -> EntityType.Builder.of(EntityDestroyerShimakaze::new, MobCategory.CREATURE)
                            .sized(0.5F, 1.6F)
                            .clientTrackingRange(64)
                            .updateInterval(1)
                            .build("destroyer_shimakaze"));

    public static final RegistryObject<EntityType<EntityDestroyerAkatsuki>> DESTROYER_AKATSUKI = ENTITIES.register(
            "destroyer_akatsuki",
            () -> EntityType.Builder.of(EntityDestroyerAkatsuki::new, MobCategory.CREATURE)
                    .sized(0.5F, 1.6F)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build("destroyer_akatsuki"));

    public static final RegistryObject<EntityType<EntityDestroyerHibiki>> DESTROYER_HIBIKI = ENTITIES.register(
            "destroyer_hibiki",
            () -> EntityType.Builder.of(EntityDestroyerHibiki::new, MobCategory.CREATURE)
                    .sized(0.5F, 1.6F)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build("destroyer_hibiki"));

    public static final RegistryObject<EntityType<EntityDestroyerIkazuchi>> DESTROYER_IKAZUCHI = ENTITIES.register(
            "destroyer_ikazuchi",
            () -> EntityType.Builder.of(EntityDestroyerIkazuchi::new, MobCategory.CREATURE)
                    .sized(0.5F, 1.6F)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build("destroyer_ikazuchi"));

    public static final RegistryObject<EntityType<EntityDestroyerInazuma>> DESTROYER_INAZUMA = ENTITIES.register(
            "destroyer_inazuma",
            () -> EntityType.Builder.of(EntityDestroyerInazuma::new, MobCategory.CREATURE)
                    .sized(0.5F, 1.6F)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build("destroyer_inazuma"));

    public static final RegistryObject<EntityType<EntityDestroyerI>> DESTROYER_I = ENTITIES.register("destroyer_i",
            () -> EntityType.Builder.of(EntityDestroyerI::new, MobCategory.CREATURE)
                    .sized(0.5F, 1.5F)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build("destroyer_i"));

    public static final RegistryObject<EntityType<EntityDestroyerRo>> DESTROYER_RO = ENTITIES
            .register("destroyer_ro",
                    () -> EntityType.Builder.of(EntityDestroyerRo::new, MobCategory.CREATURE)
                            .sized(0.5F, 1.5F)
                            .clientTrackingRange(64)
                            .updateInterval(1)
                            .build("destroyer_ro"));

    public static final RegistryObject<EntityType<EntityDestroyerHa>> DESTROYER_HA = ENTITIES
            .register("destroyer_ha",
                    () -> EntityType.Builder.of(EntityDestroyerHa::new, MobCategory.CREATURE)
                            .sized(0.5F, 1.5F)
                            .clientTrackingRange(64)
                            .updateInterval(1)
                            .build("destroyer_ha"));

    public static final RegistryObject<EntityType<EntityDestroyerNi>> DESTROYER_NI = ENTITIES
            .register("destroyer_ni",
                    () -> EntityType.Builder.of(EntityDestroyerNi::new, MobCategory.CREATURE)
                            .sized(0.5F, 1.7F)
                            .clientTrackingRange(64)
                            .updateInterval(1)
                            .build("destroyer_ni"));

    // --- Battleships ---
    public static final RegistryObject<EntityType<EntityBBKongou>> BB_KONGOU = ENTITIES.register("bb_kongou",
            () -> EntityType.Builder.of(EntityBBKongou::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.875F)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build("bb_kongou"));

    public static final RegistryObject<EntityType<EntityBBHiei>> BB_HIEI = ENTITIES.register("bb_hiei",
            () -> EntityType.Builder.of(EntityBBHiei::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.875F)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build("bb_hiei"));

    public static final RegistryObject<EntityType<EntityBBHaruna>> BB_HARUNA = ENTITIES.register("bb_haruna",
            () -> EntityType.Builder.of(EntityBBHaruna::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.875F)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build("bb_haruna"));

    public static final RegistryObject<EntityType<EntityBBKirishima>> BB_KIRISHIMA = ENTITIES
            .register("bb_kirishima",
                    () -> EntityType.Builder.of(EntityBBKirishima::new, MobCategory.CREATURE)
                            .sized(0.6F, 1.875F)
                            .clientTrackingRange(64)
                            .updateInterval(1)
                            .build("bb_kirishima"));

    public static final RegistryObject<EntityType<EntityBattleshipNagato>> BB_NAGATO = ENTITIES
            .register("bb_nagato",
                    () -> EntityType.Builder.of(EntityBattleshipNagato::new, MobCategory.CREATURE)
                            .sized(0.7F, 2.0F)
                            .clientTrackingRange(64)
                            .updateInterval(1)
                            .build("bb_nagato"));

    public static final RegistryObject<EntityType<EntityBattleshipYamato>> BB_YAMATO = ENTITIES
            .register("bb_yamato",
                    () -> EntityType.Builder.of(EntityBattleshipYamato::new, MobCategory.CREATURE)
                            .sized(0.8F, 2.1F)
                            .clientTrackingRange(64)
                            .updateInterval(1)
                            .build("bb_yamato"));

    public static final RegistryObject<EntityType<EntityBattleshipRe>> BB_RE = ENTITIES.register("bb_re",
            () -> EntityType.Builder.of(EntityBattleshipRe::new, MobCategory.CREATURE)
                    .sized(0.7F, 2.0F)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build("bb_re"));

    public static final RegistryObject<EntityType<EntityBattleshipRu>> BB_RU = ENTITIES.register("bb_ru",
            () -> EntityType.Builder.of(EntityBattleshipRu::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.875F)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build("bb_ru"));

    public static final RegistryObject<EntityType<EntityBattleshipTa>> BB_TA = ENTITIES.register("bb_ta",
            () -> EntityType.Builder.of(EntityBattleshipTa::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.875F)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build("bb_ta"));

    // --- Light Cruisers ---
    public static final RegistryObject<EntityType<EntityCLTenryuu>> CL_TENRYUU = ENTITIES.register("cl_tenryuu",
            () -> EntityType.Builder.of(EntityCLTenryuu::new, MobCategory.CREATURE)
                    .sized(0.5F, 1.65F)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build("cl_tenryuu"));

    public static final RegistryObject<EntityType<EntityCLTatsuta>> CL_TATSUTA = ENTITIES.register("cl_tatsuta",
            () -> EntityType.Builder.of(EntityCLTatsuta::new, MobCategory.CREATURE)
                    .sized(0.5F, 1.65F)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build("cl_tatsuta"));

    // --- Heavy Cruisers ---
    public static final RegistryObject<EntityType<EntityCAAtago>> CA_ATAGO = ENTITIES.register("ca_atago",
            () -> EntityType.Builder.of(EntityCAAtago::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.75F)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build("ca_atago"));

    public static final RegistryObject<EntityType<EntityCATakao>> CA_TAKAO = ENTITIES.register("ca_takao",
            () -> EntityType.Builder.of(EntityCATakao::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.75F)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build("ca_takao"));

    public static final RegistryObject<EntityType<EntityCANe>> CA_NE = ENTITIES.register("ca_ne",
            () -> EntityType.Builder.of(EntityCANe::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.75F)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build("ca_ne"));

    public static final RegistryObject<EntityType<EntityCARi>> CA_RI = ENTITIES.register("ca_ri",
            () -> EntityType.Builder.of(EntityCARi::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.75F)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build("ca_ri"));

    // --- Carriers ---
    public static final RegistryObject<EntityType<EntityCarrierAkagi>> CV_AKAGI = ENTITIES.register("cv_akagi",
            () -> EntityType.Builder.of(EntityCarrierAkagi::new, MobCategory.CREATURE)
                    .sized(0.7F, 1.9F)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build("cv_akagi"));

    public static final RegistryObject<EntityType<EntityCarrierKaga>> CV_KAGA = ENTITIES.register("cv_kaga",
            () -> EntityType.Builder.of(EntityCarrierKaga::new, MobCategory.CREATURE)
                    .sized(0.7F, 1.9F)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build("cv_kaga"));

    public static final RegistryObject<EntityType<EntityCarrierWo>> CV_WO = ENTITIES.register("cv_wo",
            () -> EntityType.Builder.of(EntityCarrierWo::new, MobCategory.CREATURE)
                    .sized(0.7F, 1.9F)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build("cv_wo"));

    // --- Submarines ---
    public static final RegistryObject<EntityType<EntitySubmRo500>> SS_RO500 = ENTITIES.register("ss_ro500",
            () -> EntityType.Builder.of(EntitySubmRo500::new, MobCategory.CREATURE)
                    .sized(0.5F, 1.5F)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build("ss_ro500"));

    public static final RegistryObject<EntityType<EntitySubmU511>> SS_U511 = ENTITIES.register("ss_u511",
            () -> EntityType.Builder.of(EntitySubmU511::new, MobCategory.CREATURE)
                    .sized(0.5F, 1.5F)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build("ss_u511"));

    public static final RegistryObject<EntityType<EntitySubmYo>> SS_YO = ENTITIES.register("ss_yo",
            () -> EntityType.Builder.of(EntitySubmYo::new, MobCategory.CREATURE)
                    .sized(0.5F, 1.5F)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build("ss_yo"));

    public static final RegistryObject<EntityType<EntitySubmKa>> SS_KA = ENTITIES.register("ss_ka",
            () -> EntityType.Builder.of(EntitySubmKa::new, MobCategory.CREATURE)
                    .sized(0.5F, 1.5F)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build("ss_ka"));

    public static final RegistryObject<EntityType<EntitySubmSo>> SS_SO = ENTITIES.register("ss_so",
            () -> EntityType.Builder.of(EntitySubmSo::new, MobCategory.CREATURE)
                    .sized(0.5F, 1.5F)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build("ss_so"));

    // --- Transport ---
    public static final RegistryObject<EntityType<EntityTransportWa>> AP_WA = ENTITIES.register("ap_wa",
            () -> EntityType.Builder.of(EntityTransportWa::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.7F)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build("ap_wa"));

    // --- Hime / Boss Ships ---
    public static final RegistryObject<EntityType<EntityAirfieldHime>> AIRFIELD_HIME = ENTITIES
            .register("airfield_hime",
                    () -> EntityType.Builder.of(EntityAirfieldHime::new, MobCategory.CREATURE)
                            .sized(0.7F, 1.9F)
                            .clientTrackingRange(64)
                            .updateInterval(1)
                            .build("airfield_hime"));

    public static final RegistryObject<EntityType<EntityBattleshipHime>> BB_HIME = ENTITIES.register("bb_hime",
            () -> EntityType.Builder.of(EntityBattleshipHime::new, MobCategory.CREATURE)
                    .sized(0.7F, 2.05F)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build("bb_hime"));

    public static final RegistryObject<EntityType<EntityCAHime>> CA_HIME = ENTITIES.register("ca_hime",
            () -> EntityType.Builder.of(EntityCAHime::new, MobCategory.CREATURE)
                    .sized(0.7F, 1.2F)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build("ca_hime"));

    public static final RegistryObject<EntityType<EntityCarrierHime>> CV_HIME = ENTITIES.register("cv_hime",
            () -> EntityType.Builder.of(EntityCarrierHime::new, MobCategory.CREATURE)
                    .sized(0.7F, 1.9F)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build("cv_hime"));

    public static final RegistryObject<EntityType<EntityCarrierWD>> CV_WD = ENTITIES.register("cv_wd",
            () -> EntityType.Builder.of(EntityCarrierWD::new, MobCategory.CREATURE)
                    .sized(0.7F, 1.9F)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build("cv_wd"));

    public static final RegistryObject<EntityType<EntityDestroyerHime>> DD_HIME = ENTITIES.register("dd_hime",
            () -> EntityType.Builder.of(EntityDestroyerHime::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.55F)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build("dd_hime"));

    public static final RegistryObject<EntityType<EntityHarbourHime>> HARBOUR_HIME = ENTITIES
            .register("harbour_hime",
                    () -> EntityType.Builder.of(EntityHarbourHime::new, MobCategory.CREATURE)
                            .sized(0.7F, 2.2F)
                            .clientTrackingRange(64)
                            .updateInterval(1)
                            .build("harbour_hime"));

    public static final RegistryObject<EntityType<EntityIsolatedHime>> ISOLATED_HIME = ENTITIES
            .register("isolated_hime",
                    () -> EntityType.Builder.of(EntityIsolatedHime::new, MobCategory.CREATURE)
                            .sized(0.6F, 1.6F)
                            .clientTrackingRange(64)
                            .updateInterval(1)
                            .build("isolated_hime"));

    public static final RegistryObject<EntityType<EntityMidwayHime>> MIDWAY_HIME = ENTITIES.register("midway_hime",
            () -> EntityType.Builder.of(EntityMidwayHime::new, MobCategory.CREATURE)
                    .sized(0.7F, 2.0F)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build("midway_hime"));

    public static final RegistryObject<EntityType<EntityNorthernHime>> NORTHERN_HIME = ENTITIES
            .register("northern_hime",
                    () -> EntityType.Builder.of(EntityNorthernHime::new, MobCategory.CREATURE)
                            .sized(0.5F, 0.9F)
                            .clientTrackingRange(64)
                            .updateInterval(1)
                            .build("northern_hime"));

    public static final RegistryObject<EntityType<EntitySSNH>> SSNH = ENTITIES.register("ssnh",
            () -> EntityType.Builder.of(EntitySSNH::new, MobCategory.CREATURE)
                    .sized(0.5F, 0.9F)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build("ssnh"));

    public static final RegistryObject<EntityType<EntitySubmHime>> SS_HIME = ENTITIES.register("ss_hime",
            () -> EntityType.Builder.of(EntitySubmHime::new, MobCategory.CREATURE)
                    .sized(0.7F, 1.85F)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build("ss_hime"));

    // ========== Hostile Ships (MobCategory.MONSTER) ==========

    // --- Hostile Destroyers ---
    public static final RegistryObject<EntityType<EntityDestroyerShimakazeMob>> DESTROYER_SHIMAKAZE_MOB = ENTITIES
            .register("destroyer_shimakaze_mob",
                    () -> EntityType.Builder
                            .of(EntityDestroyerShimakazeMob::new, MobCategory.MONSTER)
                            .sized(0.5F, 1.6F)
                            .clientTrackingRange(64)
                            .updateInterval(1)
                            .build("destroyer_shimakaze_mob"));

    public static final RegistryObject<EntityType<EntityDestroyerAkatsukiMob>> DESTROYER_AKATSUKI_MOB = ENTITIES
            .register("destroyer_akatsuki_mob",
                    () -> EntityType.Builder
                            .of(EntityDestroyerAkatsukiMob::new, MobCategory.MONSTER)
                            .sized(0.5F, 1.6F)
                            .clientTrackingRange(64)
                            .updateInterval(1)
                            .build("destroyer_akatsuki_mob"));

    public static final RegistryObject<EntityType<EntityDestroyerHibikiMob>> DESTROYER_HIBIKI_MOB = ENTITIES
            .register("destroyer_hibiki_mob",
                    () -> EntityType.Builder.of(EntityDestroyerHibikiMob::new, MobCategory.MONSTER)
                            .sized(0.5F, 1.6F)
                            .clientTrackingRange(64)
                            .updateInterval(1)
                            .build("destroyer_hibiki_mob"));

    public static final RegistryObject<EntityType<EntityDestroyerIkazuchiMob>> DESTROYER_IKAZUCHI_MOB = ENTITIES
            .register("destroyer_ikazuchi_mob",
                    () -> EntityType.Builder
                            .of(EntityDestroyerIkazuchiMob::new, MobCategory.MONSTER)
                            .sized(0.5F, 1.6F)
                            .clientTrackingRange(64)
                            .updateInterval(1)
                            .build("destroyer_ikazuchi_mob"));

    public static final RegistryObject<EntityType<EntityDestroyerInazumaMob>> DESTROYER_INAZUMA_MOB = ENTITIES
            .register("destroyer_inazuma_mob",
                    () -> EntityType.Builder.of(EntityDestroyerInazumaMob::new, MobCategory.MONSTER)
                            .sized(0.5F, 1.6F)
                            .clientTrackingRange(64)
                            .updateInterval(1)
                            .build("destroyer_inazuma_mob"));

    // --- Hostile Battleships ---
    public static final RegistryObject<EntityType<EntityBBKongouMob>> BB_KONGOU_MOB = ENTITIES
            .register("bb_kongou_mob",
                    () -> EntityType.Builder.of(EntityBBKongouMob::new, MobCategory.MONSTER)
                            .sized(0.6F, 1.875F)
                            .clientTrackingRange(64)
                            .updateInterval(1)
                            .build("bb_kongou_mob"));

    public static final RegistryObject<EntityType<EntityBBHieiMob>> BB_HIEI_MOB = ENTITIES.register("bb_hiei_mob",
            () -> EntityType.Builder.of(EntityBBHieiMob::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.875F)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build("bb_hiei_mob"));

    public static final RegistryObject<EntityType<EntityBBHarunaMob>> BB_HARUNA_MOB = ENTITIES
            .register("bb_haruna_mob",
                    () -> EntityType.Builder.of(EntityBBHarunaMob::new, MobCategory.MONSTER)
                            .sized(0.6F, 1.875F)
                            .clientTrackingRange(64)
                            .updateInterval(1)
                            .build("bb_haruna_mob"));

    public static final RegistryObject<EntityType<EntityBBKirishimaMob>> BB_KIRISHIMA_MOB = ENTITIES
            .register("bb_kirishima_mob",
                    () -> EntityType.Builder.of(EntityBBKirishimaMob::new, MobCategory.MONSTER)
                            .sized(0.6F, 1.875F)
                            .clientTrackingRange(64)
                            .updateInterval(1)
                            .build("bb_kirishima_mob"));

    public static final RegistryObject<EntityType<EntityBattleshipNagatoMob>> BB_NAGATO_MOB = ENTITIES.register(
            "bb_nagato_mob",
            () -> EntityType.Builder.of(EntityBattleshipNagatoMob::new, MobCategory.MONSTER)
                    .sized(0.7F, 2.0F)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build("bb_nagato_mob"));

    public static final RegistryObject<EntityType<EntityBattleshipYamatoMob>> BB_YAMATO_MOB = ENTITIES.register(
            "bb_yamato_mob",
            () -> EntityType.Builder.of(EntityBattleshipYamatoMob::new, MobCategory.MONSTER)
                    .sized(0.8F, 2.1F)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build("bb_yamato_mob"));

    // --- Hostile Cruisers ---
    public static final RegistryObject<EntityType<EntityCLTenryuuMob>> CL_TENRYUU_MOB = ENTITIES
            .register("cl_tenryuu_mob",
                    () -> EntityType.Builder.of(EntityCLTenryuuMob::new, MobCategory.MONSTER)
                            .sized(0.75F, 1.65F)
                            .clientTrackingRange(64)
                            .updateInterval(1)
                            .build("cl_tenryuu_mob"));

    public static final RegistryObject<EntityType<EntityCLTatsutaMob>> CL_TATSUTA_MOB = ENTITIES
            .register("cl_tatsuta_mob",
                    () -> EntityType.Builder.of(EntityCLTatsutaMob::new, MobCategory.MONSTER)
                            .sized(0.75F, 1.65F)
                            .clientTrackingRange(64)
                            .updateInterval(1)
                            .build("cl_tatsuta_mob"));

    public static final RegistryObject<EntityType<EntityCAAtagoMob>> CA_ATAGO_MOB = ENTITIES
            .register("ca_atago_mob",
                    () -> EntityType.Builder.of(EntityCAAtagoMob::new, MobCategory.MONSTER)
                            .sized(0.6F, 1.75F)
                            .clientTrackingRange(64)
                            .updateInterval(1)
                            .build("ca_atago_mob"));

    public static final RegistryObject<EntityType<EntityCATakaoMob>> CA_TAKAO_MOB = ENTITIES
            .register("ca_takao_mob",
                    () -> EntityType.Builder.of(EntityCATakaoMob::new, MobCategory.MONSTER)
                            .sized(0.6F, 1.75F)
                            .clientTrackingRange(64)
                            .updateInterval(1)
                            .build("ca_takao_mob"));

    // --- Hostile Carriers ---
    public static final RegistryObject<EntityType<EntityCarrierAkagiMob>> CV_AKAGI_MOB = ENTITIES
            .register("cv_akagi_mob",
                    () -> EntityType.Builder.of(EntityCarrierAkagiMob::new, MobCategory.MONSTER)
                            .sized(0.7F, 1.9F)
                            .clientTrackingRange(64)
                            .updateInterval(1)
                            .build("cv_akagi_mob"));

    public static final RegistryObject<EntityType<EntityCarrierKagaMob>> CV_KAGA_MOB = ENTITIES
            .register("cv_kaga_mob",
                    () -> EntityType.Builder.of(EntityCarrierKagaMob::new, MobCategory.MONSTER)
                            .sized(0.7F, 1.9F)
                            .clientTrackingRange(64)
                            .updateInterval(1)
                            .build("cv_kaga_mob"));

    // --- Hostile Submarines ---
    public static final RegistryObject<EntityType<EntitySubmRo500Mob>> SS_RO500_MOB = ENTITIES
            .register("ss_ro500_mob",
                    () -> EntityType.Builder.of(EntitySubmRo500Mob::new, MobCategory.MONSTER)
                            .sized(0.5F, 1.5F)
                            .clientTrackingRange(64)
                            .updateInterval(1)
                            .build("ss_ro500_mob"));

    public static final RegistryObject<EntityType<EntitySubmU511Mob>> SS_U511_MOB = ENTITIES.register("ss_u511_mob",
            () -> EntityType.Builder.of(EntitySubmU511Mob::new, MobCategory.MONSTER)
                    .sized(0.5F, 1.5F)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build("ss_u511_mob"));

    // ========== Mount Entities (MobCategory.CREATURE) ==========

    public static final RegistryObject<EntityType<EntityMountAfH>> MOUNT_AFH = ENTITIES.register("mount_afh",
            () -> EntityType.Builder.of(EntityMountAfH::new, MobCategory.CREATURE)
                    .sized(1.9F, 1.3F)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build("mount_afh"));

    public static final RegistryObject<EntityType<EntityMountBaH>> MOUNT_BAH = ENTITIES.register("mount_bah",
            () -> EntityType.Builder.of(EntityMountBaH::new, MobCategory.CREATURE)
                    .sized(1.9F, 3.1F)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build("mount_bah"));

    public static final RegistryObject<EntityType<EntityMountCaH>> MOUNT_CAH = ENTITIES.register("mount_cah",
            () -> EntityType.Builder.of(EntityMountCaH::new, MobCategory.CREATURE)
                    .sized(1.9F, 2.1F)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build("mount_cah"));

    public static final RegistryObject<EntityType<EntityMountCaWD>> MOUNT_CAWD = ENTITIES.register("mount_cawd",
            () -> EntityType.Builder.of(EntityMountCaWD::new, MobCategory.CREATURE)
                    .sized(1.9F, 2.1F)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build("mount_cawd"));

    public static final RegistryObject<EntityType<EntityMountHbH>> MOUNT_HBH = ENTITIES.register("mount_hbh",
            () -> EntityType.Builder.of(EntityMountHbH::new, MobCategory.CREATURE)
                    .sized(1.9F, 1.6F)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build("mount_hbh"));

    public static final RegistryObject<EntityType<EntityMountIsH>> MOUNT_ISH = ENTITIES.register("mount_ish",
            () -> EntityType.Builder.of(EntityMountIsH::new, MobCategory.CREATURE)
                    .sized(1.6F, 2.2F)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build("mount_ish"));

    public static final RegistryObject<EntityType<EntityMountMiH>> MOUNT_MIH = ENTITIES.register("mount_mih",
            () -> EntityType.Builder.of(EntityMountMiH::new, MobCategory.CREATURE)
                    .sized(2.5F, 2.9F)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build("mount_mih"));

    public static final RegistryObject<EntityType<EntityMountSuH>> MOUNT_SUH = ENTITIES.register("mount_suh",
            () -> EntityType.Builder.of(EntityMountSuH::new, MobCategory.CREATURE)
                    .sized(1.8F, 1.6F)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build("mount_suh"));

    // ========== Airplane / Summon Entities (MobCategory.MISC) ==========

    public static final RegistryObject<EntityType<EntityAirplane>> AIRPLANE = ENTITIES.register("airplane",
            () -> EntityType.Builder.of(EntityAirplane::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build("airplane"));

    public static final RegistryObject<EntityType<EntityAirplaneZero>> AIRPLANE_ZERO = ENTITIES
            .register("airplane_zero",
                    () -> EntityType.Builder.of(EntityAirplaneZero::new, MobCategory.MISC)
                            .sized(0.5F, 0.5F)
                            .clientTrackingRange(64)
                            .updateInterval(1)
                            .build("airplane_zero"));

    public static final RegistryObject<EntityType<EntityAirplaneZeroMob>> AIRPLANE_ZERO_MOB = ENTITIES
            .register("airplane_zero_mob",
                    () -> EntityType.Builder.of(EntityAirplaneZeroMob::new, MobCategory.MISC)
                            .sized(0.5F, 0.5F)
                            .clientTrackingRange(64)
                            .updateInterval(1)
                            .build("airplane_zero_mob"));

    public static final RegistryObject<EntityType<EntityAirplaneTakoyaki>> AIRPLANE_TAKOYAKI = ENTITIES
            .register("airplane_takoyaki",
                    () -> EntityType.Builder.of(EntityAirplaneTakoyaki::new, MobCategory.MISC)
                            .sized(0.6F, 0.6F)
                            .clientTrackingRange(64)
                            .updateInterval(1)
                            .build("airplane_takoyaki"));

    public static final RegistryObject<EntityType<EntityAirplaneT>> AIRPLANE_T = ENTITIES.register("airplane_t",
            () -> EntityType.Builder.of(EntityAirplaneT::new, MobCategory.MISC)
                    .sized(0.6F, 0.6F)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build("airplane_t"));

    public static final RegistryObject<EntityType<EntityAirplaneTMob>> AIRPLANE_T_MOB = ENTITIES
            .register("airplane_t_mob",
                    () -> EntityType.Builder.of(EntityAirplaneTMob::new, MobCategory.MISC)
                            .sized(0.6F, 0.6F)
                            .clientTrackingRange(64)
                            .updateInterval(1)
                            .build("airplane_t_mob"));

    public static final RegistryObject<EntityType<EntityFloatingFort>> FLOATING_FORT = ENTITIES
            .register("floating_fort",
                    () -> EntityType.Builder.of(EntityFloatingFort::new, MobCategory.MISC)
                            .sized(0.5F, 0.5F)
                            .clientTrackingRange(64)
                            .updateInterval(1)
                            .build("floating_fort"));

    public static final RegistryObject<EntityType<EntityRensouhou>> RENSOUHOU = ENTITIES.register("rensouhou",
            () -> EntityType.Builder.of(EntityRensouhou::new, MobCategory.MISC)
                    .sized(0.3F, 0.7F)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build("rensouhou"));

    public static final RegistryObject<EntityType<EntityRensouhouMob>> RENSOUHOU_MOB = ENTITIES
            .register("rensouhou_mob",
                    () -> EntityType.Builder.of(EntityRensouhouMob::new, MobCategory.MISC)
                            .sized(0.3F, 0.7F)
                            .clientTrackingRange(64)
                            .updateInterval(1)
                            .build("rensouhou_mob"));

    public static final RegistryObject<EntityType<EntityRensouhouS>> RENSOUHOU_S = ENTITIES.register("rensouhou_s",
            () -> EntityType.Builder.of(EntityRensouhouS::new, MobCategory.MISC)
                    .sized(0.5F, 1.4F)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build("rensouhou_s"));

    // ========== Raw Entity Types (MobCategory.MISC, no attribute registration)
    // ==========

    public static final RegistryObject<EntityType<EntityAbyssMissile>> ABYSS_MISSILE = ENTITIES.register(
            "abyss_missile",
            () -> EntityType.Builder.of(EntityAbyssMissile::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build("abyss_missile"));

    public static final RegistryObject<EntityType<EntityProjectileBeam>> PROJECTILE_BEAM = ENTITIES.register(
            "projectile_beam",
            () -> EntityType.Builder.of(EntityProjectileBeam::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build("projectile_beam"));

    public static final RegistryObject<EntityType<EntityProjectileStatic>> PROJECTILE_STATIC = ENTITIES.register(
            "projectile_static",
            () -> EntityType.Builder
                    .of(EntityProjectileStatic::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build("projectile_static"));

    public static final RegistryObject<EntityType<EntityShipFishingHook>> FISHING_HOOK = ENTITIES.register(
            "fishing_hook",
            () -> EntityType.Builder.of(EntityShipFishingHook::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build("fishing_hook"));

    public static final RegistryObject<EntityType<BasicEntityItem>> BASIC_ENTITY_ITEM = ENTITIES.register(
            "basic_entity_item",
            () -> EntityType.Builder.<BasicEntityItem>of(BasicEntityItem::new, MobCategory.MISC)
                    .sized(0.8F, 0.8F)
                    .fireImmune()
                    .clientTrackingRange(64)
                    .updateInterval(4)
                    .build("basic_entity_item"));

    // ========== Attribute Registration ==========

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        // Friendly Ships (BasicEntityShip.createShipAttributes)
        event.put(DESTROYER_SHIMAKAZE.get(), BasicEntityShip.createShipAttributes().build());
        event.put(DESTROYER_AKATSUKI.get(), BasicEntityShip.createShipAttributes().build());
        event.put(DESTROYER_HIBIKI.get(), BasicEntityShip.createShipAttributes().build());
        event.put(DESTROYER_IKAZUCHI.get(), BasicEntityShip.createShipAttributes().build());
        event.put(DESTROYER_INAZUMA.get(), BasicEntityShip.createShipAttributes().build());
        event.put(DESTROYER_I.get(), BasicEntityShip.createShipAttributes().build());
        event.put(DESTROYER_RO.get(), BasicEntityShip.createShipAttributes().build());
        event.put(DESTROYER_HA.get(), BasicEntityShip.createShipAttributes().build());
        event.put(DESTROYER_NI.get(), BasicEntityShip.createShipAttributes().build());
        event.put(BB_KONGOU.get(), BasicEntityShip.createShipAttributes().build());
        event.put(BB_HIEI.get(), BasicEntityShip.createShipAttributes().build());
        event.put(BB_HARUNA.get(), BasicEntityShip.createShipAttributes().build());
        event.put(BB_KIRISHIMA.get(), BasicEntityShip.createShipAttributes().build());
        event.put(BB_NAGATO.get(), BasicEntityShip.createShipAttributes().build());
        event.put(BB_YAMATO.get(), BasicEntityShip.createShipAttributes().build());
        event.put(BB_RE.get(), BasicEntityShip.createShipAttributes().build());
        event.put(BB_RU.get(), BasicEntityShip.createShipAttributes().build());
        event.put(BB_TA.get(), BasicEntityShip.createShipAttributes().build());
        event.put(CL_TENRYUU.get(), BasicEntityShip.createShipAttributes().build());
        event.put(CL_TATSUTA.get(), BasicEntityShip.createShipAttributes().build());
        event.put(CA_ATAGO.get(), BasicEntityShip.createShipAttributes().build());
        event.put(CA_TAKAO.get(), BasicEntityShip.createShipAttributes().build());
        event.put(CA_NE.get(), BasicEntityShip.createShipAttributes().build());
        event.put(CA_RI.get(), BasicEntityShip.createShipAttributes().build());
        event.put(CV_AKAGI.get(), BasicEntityShip.createShipAttributes().build());
        event.put(CV_KAGA.get(), BasicEntityShip.createShipAttributes().build());
        event.put(CV_WO.get(), BasicEntityShip.createShipAttributes().build());
        event.put(SS_RO500.get(), BasicEntityShip.createShipAttributes().build());
        event.put(SS_U511.get(), BasicEntityShip.createShipAttributes().build());
        event.put(SS_YO.get(), BasicEntityShip.createShipAttributes().build());
        event.put(SS_KA.get(), BasicEntityShip.createShipAttributes().build());
        event.put(SS_SO.get(), BasicEntityShip.createShipAttributes().build());
        event.put(AP_WA.get(), BasicEntityShip.createShipAttributes().build());
        event.put(AIRFIELD_HIME.get(), BasicEntityShip.createShipAttributes().build());
        event.put(BB_HIME.get(), BasicEntityShip.createShipAttributes().build());
        event.put(CA_HIME.get(), BasicEntityShip.createShipAttributes().build());
        event.put(CV_HIME.get(), BasicEntityShip.createShipAttributes().build());
        event.put(CV_WD.get(), BasicEntityShip.createShipAttributes().build());
        event.put(DD_HIME.get(), BasicEntityShip.createShipAttributes().build());
        event.put(HARBOUR_HIME.get(), BasicEntityShip.createShipAttributes().build());
        event.put(ISOLATED_HIME.get(), BasicEntityShip.createShipAttributes().build());
        event.put(MIDWAY_HIME.get(), BasicEntityShip.createShipAttributes().build());
        event.put(NORTHERN_HIME.get(), BasicEntityShip.createShipAttributes().build());
        event.put(SSNH.get(), BasicEntityShip.createShipAttributes().build());
        event.put(SS_HIME.get(), BasicEntityShip.createShipAttributes().build());

        // Hostile Ships (BasicEntityShipHostile.createHostileShipAttributes)
        event.put(DESTROYER_SHIMAKAZE_MOB.get(), BasicEntityShipHostile.createHostileShipAttributes().build());
        event.put(DESTROYER_AKATSUKI_MOB.get(), BasicEntityShipHostile.createHostileShipAttributes().build());
        event.put(DESTROYER_HIBIKI_MOB.get(), BasicEntityShipHostile.createHostileShipAttributes().build());
        event.put(DESTROYER_IKAZUCHI_MOB.get(), BasicEntityShipHostile.createHostileShipAttributes().build());
        event.put(DESTROYER_INAZUMA_MOB.get(), BasicEntityShipHostile.createHostileShipAttributes().build());
        event.put(BB_KONGOU_MOB.get(), BasicEntityShipHostile.createHostileShipAttributes().build());
        event.put(BB_HIEI_MOB.get(), BasicEntityShipHostile.createHostileShipAttributes().build());
        event.put(BB_HARUNA_MOB.get(), BasicEntityShipHostile.createHostileShipAttributes().build());
        event.put(BB_KIRISHIMA_MOB.get(), BasicEntityShipHostile.createHostileShipAttributes().build());
        event.put(BB_NAGATO_MOB.get(), BasicEntityShipHostile.createHostileShipAttributes().build());
        event.put(BB_YAMATO_MOB.get(), BasicEntityShipHostile.createHostileShipAttributes().build());
        event.put(CL_TENRYUU_MOB.get(), BasicEntityShipHostile.createHostileShipAttributes().build());
        event.put(CL_TATSUTA_MOB.get(), BasicEntityShipHostile.createHostileShipAttributes().build());
        event.put(CA_ATAGO_MOB.get(), BasicEntityShipHostile.createHostileShipAttributes().build());
        event.put(CA_TAKAO_MOB.get(), BasicEntityShipHostile.createHostileShipAttributes().build());
        event.put(CV_AKAGI_MOB.get(), BasicEntityShipHostile.createHostileShipAttributes().build());
        event.put(CV_KAGA_MOB.get(), BasicEntityShipHostile.createHostileShipAttributes().build());
        event.put(SS_RO500_MOB.get(), BasicEntityShipHostile.createHostileShipAttributes().build());
        event.put(SS_U511_MOB.get(), BasicEntityShipHostile.createHostileShipAttributes().build());

        // Mount Entities (BasicEntityMount.createMountAttributes)
        event.put(MOUNT_AFH.get(), BasicEntityMount.createMountAttributes().build());
        event.put(MOUNT_BAH.get(), BasicEntityMount.createMountAttributes().build());
        event.put(MOUNT_CAH.get(), BasicEntityMount.createMountAttributes().build());
        event.put(MOUNT_CAWD.get(), BasicEntityMount.createMountAttributes().build());
        event.put(MOUNT_HBH.get(), BasicEntityMount.createMountAttributes().build());
        event.put(MOUNT_ISH.get(), BasicEntityMount.createMountAttributes().build());
        event.put(MOUNT_MIH.get(), BasicEntityMount.createMountAttributes().build());
        event.put(MOUNT_SUH.get(), BasicEntityMount.createMountAttributes().build());

        // Airplane Entities
        event.put(AIRPLANE.get(), BasicEntityAirplane.createAirplaneAttributes().build());
        event.put(AIRPLANE_ZERO.get(), BasicEntityAirplane.createAirplaneAttributes().build());
        event.put(AIRPLANE_ZERO_MOB.get(), BasicEntityAirplane.createAirplaneAttributes().build());
        event.put(AIRPLANE_TAKOYAKI.get(), BasicEntityAirplane.createAirplaneAttributes().build());
        event.put(AIRPLANE_T.get(), BasicEntityAirplane.createAirplaneAttributes().build());
        event.put(AIRPLANE_T_MOB.get(), BasicEntityAirplane.createAirplaneAttributes().build());

        // Summon Entities (rensouhou, floating fort)
        event.put(FLOATING_FORT.get(), BasicEntitySummon.createSummonAttributes().build());
        event.put(RENSOUHOU.get(), BasicEntitySummon.createSummonAttributes().build());
        event.put(RENSOUHOU_MOB.get(), BasicEntitySummon.createSummonAttributes().build());
        event.put(RENSOUHOU_S.get(), BasicEntitySummon.createSummonAttributes().build());

        // Note: Raw entities (ABYSS_MISSILE, PROJECTILE_BEAM, PROJECTILE_STATIC,
        // FISHING_HOOK, BASIC_ENTITY_ITEM)
        // do NOT need attribute registration as they extend Entity directly, not
        // LivingEntity.
    }
}
