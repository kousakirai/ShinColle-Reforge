package com.lulan.shincolle.client.gui;

import com.lulan.shincolle.client.gui.inventory.ContainerShipInventory;
import com.lulan.shincolle.entity.BasicEntityShip;
import com.lulan.shincolle.entity.BasicEntityShipCV;
import com.lulan.shincolle.network.C2SGUIInputPacket;
import com.lulan.shincolle.network.ModNetworking;
import com.lulan.shincolle.reference.ID;
import com.lulan.shincolle.reference.Values;
import com.lulan.shincolle.reference.unitclass.Attrs;
import com.lulan.shincolle.reference.unitclass.AttrsAdv;
import com.lulan.shincolle.utility.BuffHelper;
import com.lulan.shincolle.utility.GuiHelper;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * GUI screen for the ship entity inventory.
 * <p>
 * Features:
 * - Entity preview rendering in the top-right area
 * - Info page tabs (3 pages): kills/exp, ATK/DEF stats, marriage/formation
 * - 8-page AI settings system with toggle buttons and slider bars
 * - Morale icon indicator
 * - C2S packet sync for all AI controls
 */
public class GuiShipInventory extends AbstractContainerScreen<ContainerShipInventory> {

    private static final ResourceLocation TEXTURE = new ResourceLocation("shincolle",
            "textures/gui/guishipinventory.png");
    private static final ResourceLocation TEXTURE_ICON0 = new ResourceLocation("shincolle",
            "textures/gui/guinameicon0.png");
    private static final ResourceLocation TEXTURE_ICON1 = new ResourceLocation("shincolle",
            "textures/gui/guinameicon1.png");
    private static final ResourceLocation TEXTURE_ICON2 = new ResourceLocation("shincolle",
            "textures/gui/guinameicon2.png");

    // ========== State ==========
    /**
     * Page 1: Attack type toggles
     */
    private static final int[][] PAGE1_TOGGLES = {
            {ID.F.UseMelee, ID.B.ShipInv_Melee, -1},
            {ID.F.UseAmmoLight, ID.B.ShipInv_AmmoLight, ID.F.AtkType_Light},
            {ID.F.UseAmmoHeavy, ID.B.ShipInv_AmmoHeavy, ID.F.AtkType_Heavy},
            {ID.F.UseAirLight, ID.B.ShipInv_AirLight, ID.F.AtkType_AirLight},
            {ID.F.UseAirHeavy, ID.B.ShipInv_AirHeavy, ID.F.AtkType_AirHeavy},
            {ID.F.UseRingEffect, ID.B.ShipInv_AuraEffect, ID.F.HaveRingEffect},
    };
    private static final String[] PAGE1_LABELS = {
            "Melee", "Atk Light", "Atk Heavy", "Air Light", "Air Heavy", "Aura"
    };
    private static final String[] PAGE1_LABEL_KEYS = {
            "gui.shincolle.canmelee", "gui.shincolle.canlightattack", "gui.shincolle.canheavyattack",
            "gui.shincolle.canairlightattack", "gui.shincolle.canairheavyattack", "gui.shincolle.auraeffect"
    };
    /**
     * Page 3: Targeting AI toggles
     */
    private static final int[][] PAGE3_TOGGLES = {
            {ID.F.PassiveAI, ID.B.ShipInv_TarAI, -1},
            {ID.F.OnSightChase, ID.B.ShipInv_OnSightAI, -1},
            {ID.F.PVPFirst, ID.B.ShipInv_PVPAI, -1},
            {ID.F.AntiAir, ID.B.ShipInv_AAAI, -1},
            {ID.F.AntiSS, ID.B.ShipInv_ASMAI, -1},
            {ID.F.TimeKeeper, ID.B.ShipInv_TIMEKEEPAI, -1},
    };
    private static final String[] PAGE3_LABELS = {
            "Passive AI", "On Sight", "PVP First", "Anti-Air", "Anti-Sub", "Timekeeper"
    };

    // ========== AI Page Toggle Definitions ==========
    // Each entry: {flagId, buttonId, conditionFlagId (-1 = always visible)}
    private static final String[] PAGE3_LABEL_KEYS = {
            "gui.shincolle.targetAI", "gui.shincolle.onsightAI", "gui.shincolle.ai.pvp",
            "gui.shincolle.ai.aa", "gui.shincolle.ai.asm", "gui.shincolle.ai.timekeeper"
    };
    /**
     * Page 4: Item/Pump toggles
     */
    private static final int[][] PAGE4_TOGGLES = {
            {ID.F.PickItem, ID.B.ShipInv_PickitemAI, ID.F.CanPickItem},
            {ID.F.AutoPump, ID.B.ShipInv_AutoPump, -1},
    };
    private static final String[] PAGE4_LABELS = {
            "Pick Item", "Auto Pump"
    };
    private static final String[] PAGE4_LABEL_KEYS = {
            "gui.shincolle.ai.pickitem", "gui.shincolle.autopump"
    };
    private static final int SLIDER_TRACK_X = 191; // track left X (relative)
    private static final int SLIDER_TRACK_W = 43; // track width
    private static final int SLIDER_MAX_POS = 42; // max drag position
    private static final int SLIDER_HANDLE_W = 5; // handle width
    private static final int SLIDER_HANDLE_H = 9; // handle height

    // ========== Slider Bar Constants ==========
    /**
     * Slider Y positions for each bar row (track center Y, relative to GUI)
     */
    private static final int[] SLIDER_TRACK_Y = {148, 172, 196};
    /**
     * Current info page: 0=Kills/EXP, 1=ATK/DEF/SPD, 2=Marriage/Formation
     */
    private int infoPage = 0;
    /**
     * Attribute display on info page: false=surface attack, true=air attack.
     */
    private boolean showAirAttack = false;
    /**
     * Current AI settings page: 1-12 (pages 9-12 are empty)
     */
    private int showPageAI = 1;
    /**
     * Which slider is being dragged: -1=none, 0=followMin, 1=followMax, 2=fleeHP,
     * 3=wpStay, 4=autoCR
     */
    private int mousePressBar = -1;
    /**
     * Current slider drag position (0-42)
     */
    private int barPos = 0;

    // ========== Constructor ==========

    public GuiShipInventory(ContainerShipInventory menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title);
        this.imageWidth = 256;
        this.imageHeight = 214;
    }

    // ========== Background Rendering ==========

    private static String getMoraleDisplayName(int morale) {
        if (morale > ID.Morale.L_Excited) {
            return tr("gui.shincolle.morale0", "Excited");
        }
        if (morale > ID.Morale.L_Happy) {
            return tr("gui.shincolle.morale1", "Happy");
        }
        if (morale > ID.Morale.L_Normal) {
            return tr("gui.shincolle.morale2", "Normal");
        }
        if (morale > ID.Morale.L_Tired) {
            return tr("gui.shincolle.morale3", "Tired");
        }
        return tr("gui.shincolle.morale4", "Exhausted");
    }

    private static int getMoraleDisplayColor(int morale) {
        if (morale > ID.Morale.L_Excited) {
            return 0xFF5500;
        }
        if (morale > ID.Morale.L_Happy) {
            return 0xFFFF00;
        }
        if (morale > ID.Morale.L_Normal) {
            return 0x00FF00;
        }
        if (morale > ID.Morale.L_Tired) {
            return 0xAAAAAA;
        }
        return 0xFF0000;
    }

    /**
     * Get display name for a formation type ID
     */
    private static String getFormationName(int formatType) {
        return switch (formatType) {
            case 1 -> tr("gui.shincolle.formation.format1", "Line Ahead");
            case 2 -> tr("gui.shincolle.formation.format2", "Double Line");
            case 3 -> tr("gui.shincolle.formation.format3", "Diamond");
            case 4 -> tr("gui.shincolle.formation.format4", "Echelon");
            case 5 -> tr("gui.shincolle.formation.format5", "Line Abreast");
            default -> tr("gui.shincolle.formation.format0", "None");
        };
    }

    /**
     * Get short display name for a ship type ID
     */
    private static String getShipTypeName(int shipType) {
        return switch (shipType) {
            case ID.ShipType.DESTROYER -> "DD";
            case ID.ShipType.LIGHT_CRUISER -> "CL";
            case ID.ShipType.HEAVY_CRUISER -> "CA";
            case ID.ShipType.TORPEDO_CRUISER -> "CLT";
            case ID.ShipType.LIGHT_CARRIER -> "CVL";
            case ID.ShipType.STANDARD_CARRIER -> "CV";
            case ID.ShipType.BATTLESHIP -> "BB";
            case ID.ShipType.TRANSPORT -> "AP";
            case ID.ShipType.SUBMARINE -> "SS";
            case ID.ShipType.DEMON -> "Demon";
            case ID.ShipType.HIME -> "Hime";
            default -> "??";
        };
    }

    /**
     * Get morale level display name from auto-CR value
     */
    private static String getMoraleLevelName(int level) {
        return switch (level) {
            case 1 -> tr("gui.shincolle.morale4", "Exhausted");
            case 2 -> tr("gui.shincolle.morale3", "Tired");
            case 3 -> tr("gui.shincolle.morale2", "Normal");
            case 4 -> tr("gui.shincolle.morale1", "Happy");
            default -> tr("gui.shincolle.general.off", "Off");
        };
    }

    private static String tr(String key, String fallback) {
        String localized = I18n.get(key);
        return localized.equals(key) ? fallback : localized;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        BasicEntityShip ship = this.menu.getShip();
        if (ship != null) {
            renderInventoryPageIndicators(graphics, ship);
            renderInfoPageTabIndicator(graphics);
            renderEntityPreview(graphics, mouseX, mouseY, ship);
            renderMoraleIcon(graphics, ship);
            renderShipIdentityIcons(graphics, ship);
            renderAIPageTabs(graphics);
            renderAIPageContentBg(graphics, ship);
            renderInventoryTaskOverlay(graphics, ship);
        }
    }

    // ========== AI Page Tab Rendering ==========

    /**
     * Render inventory page lock overlays and current page indicator (left tabs).
     */
    private void renderInventoryPageIndicators(GuiGraphics graphics, BasicEntityShip ship) {
        int maxPage = ship.getInventoryPageSize();

        if (maxPage <= 1) {
            graphics.blit(TEXTURE, this.leftPos + 62, this.topPos + 90, 80, 214, 6, 34);
        }
        if (maxPage <= 0) {
            graphics.blit(TEXTURE, this.leftPos + 62, this.topPos + 54, 80, 214, 6, 34);
        }

        int invPage = this.menu.getInventoryPage();
        int tabY = this.topPos + 18 + Mth.clamp(invPage, 0, 2) * 36;
        graphics.blit(TEXTURE, this.leftPos + 62, tabY, 74, 214, 6, 34);
    }

    // ========== AI Page Content Background Rendering ==========

    /**
     * Render legacy task icon/slot overlay in the inventory panel (page 0 only).
     * Keeps 1.10.2 behavior where active task is shown near lower-left inventory
     * area.
     */
    private void renderInventoryTaskOverlay(GuiGraphics graphics, BasicEntityShip ship) {
        if (this.menu.getInventoryPage() != 0) {
            return;
        }

        int task = ship.getStateMinor(ID.M.Task);
        if (task < 1 || task > 4) {
            return;
        }

        graphics.blit(TEXTURE, this.leftPos + 25, this.topPos + 107, 33, 225, 18, 18);
        graphics.blit(TEXTURE, this.leftPos + 26, this.topPos + 109, 151 + (task - 1) * 16, 236, 18, 18);

        // Crafting task uses dedicated 3x3 task slots (slot 12..20 in ship inventory).
        if (task == 4) {
            for (int i = 0; i < 9; i++) {
                ItemStack stack = ship.getCapaShipInventory().getStackInSlot(i + 12);
                int u = stack.isEmpty() ? 33 : 51;
                int x = this.leftPos + 7 + (i % 3) * 18;
                int y = this.topPos + 53 + (i / 3) * 18;
                graphics.blit(TEXTURE, x, y, u, 225, 18, 18);
            }
        }
    }

    /**
     * Render the selected info page tab indicator
     */
    private void renderInfoPageTabIndicator(GuiGraphics graphics) {
        int tabY = this.topPos + 18 + infoPage * 36;
        graphics.blit(TEXTURE, this.leftPos + 135, tabY, 74, 214, 6, 34);
    }

    /**
     * Render entity preview in the top-right area
     */
    private void renderEntityPreview(GuiGraphics graphics, int mouseX, int mouseY, BasicEntityShip ship) {
        // [PORT] 1.10.2 -> 1.20.1: keep legacy modelPos offsets so model anchor remains
        // consistent.
        float[] modelPos = ship.getModelPos();
        int offsetX = (modelPos != null && modelPos.length > 0) ? (int) modelPos[0] : 0;
        int offsetY = (modelPos != null && modelPos.length > 1) ? (int) modelPos[1] : 0;
        int scale = (modelPos != null && modelPos.length > 3 && modelPos[3] > 0)
                ? (int) modelPos[3]
                : 50;
        int renderX = this.leftPos + 218 + offsetX;
        int renderY = this.topPos + 100 + offsetY;
        float lookX = (float) (this.leftPos + 215 - mouseX);
        float lookY = (float) (this.topPos + 60 - mouseY);
        InventoryScreen.renderEntityInInventoryFollowsMouse(
                graphics,
                renderX,
                renderY,
                scale,
                lookX,
                lookY,
                ship);
    }

    /**
     * Render morale icon (11x11 sprite)
     */
    private void renderMoraleIcon(GuiGraphics graphics, BasicEntityShip ship) {
        int morale = ship.getMorale();
        int moraleIdx = BuffHelper.getMoraleLevel(morale);
        graphics.blit(TEXTURE, this.leftPos + 239, this.topPos + 18,
                moraleIdx * 11, 240, 11, 11);
    }

    /**
     * Render legacy ship type/name icons in model panel area.
     */
    private void renderShipIdentityIcons(GuiGraphics graphics, BasicEntityShip ship) {
        int[] typeIcon = Values.ShipTypeIconMap.get(ship.getShipType());
        if (typeIcon != null && typeIcon.length >= 2) {
            if (ship.getStateMinor(ID.M.ShipLevel) > 99) {
                graphics.blit(TEXTURE_ICON0, this.leftPos + 165, this.topPos + 18, 0, 0, 40, 42);
                graphics.blit(TEXTURE_ICON0, this.leftPos + 167, this.topPos + 22, typeIcon[0], typeIcon[1], 28, 28);
            } else {
                graphics.blit(TEXTURE_ICON0, this.leftPos + 165, this.topPos + 18, 0, 43, 30, 30);
                graphics.blit(TEXTURE_ICON0, this.leftPos + 165, this.topPos + 18, typeIcon[0], typeIcon[1], 28, 28);
            }
        }

        int[] nameIcon = Values.ShipNameIconMap.get(ship.getShipClass());
        if (nameIcon == null || nameIcon.length < 3) {
            return;
        }

        ResourceLocation nameTexture = nameIcon[0] < 100 ? TEXTURE_ICON1 : TEXTURE_ICON2;
        int offY = 0;
        if (nameIcon[0] < 100) {
            if (nameIcon[0] == 4) {
                offY = -10;
            }
        } else {
            offY = 10;
        }
        graphics.blit(nameTexture, this.leftPos + 176, this.topPos + 63 + offY, nameIcon[1], nameIcon[2], 11, 59);
    }

    /**
     * Render AI page tab indicators (two columns of 6 tabs)
     */
    private void renderAIPageTabs(GuiGraphics graphics) {
        // [PORT] 1.10.2 -> 1.20.1: use legacy single tab-indicator sprite for AI pages.
        int indicatorX = showPageAI <= 6 ? 239 : 246;
        int indicatorY = switch (showPageAI) {
            case 1, 7 -> 131;
            case 2, 8 -> 144;
            case 3, 9 -> 157;
            case 4, 10 -> 170;
            case 5, 11 -> 183;
            default -> 196;
        };
        graphics.blit(TEXTURE, this.leftPos + indicatorX, this.topPos + indicatorY, 74, 214, 6, 11);
    }

    /**
     * Dispatch AI page background rendering based on current page
     */
    private void renderAIPageContentBg(GuiGraphics graphics, BasicEntityShip ship) {
        switch (showPageAI) {
            case 1:
                renderToggleButtonBg(graphics, ship, PAGE1_TOGGLES);
                break;
            case 2:
                renderSliderBarsBg(graphics, ship, new int[]{0, 1, 2});
                break;
            case 3:
                renderToggleButtonBg(graphics, ship, PAGE3_TOGGLES);
                break;
            case 4:
                renderToggleButtonBg(graphics, ship, PAGE4_TOGGLES);
                break;
            case 5:
                renderSliderBarsBg(graphics, ship, new int[]{3, 4});
                break;
            case 6:
                renderPage6Bg(graphics, ship);
                break;
            case 7:
                renderPage7Bg(graphics, ship);
                break;
            case 8:
                renderPage8Bg(graphics, ship);
                break;
        }
    }

    // ========== Label/Text Rendering ==========

    /**
     * Render toggle button backgrounds for a toggle page
     */
    private void renderToggleButtonBg(GuiGraphics graphics, BasicEntityShip ship, int[][] toggles) {
        for (int i = 0; i < toggles.length; i++) {
            int flagId = toggles[i][0];
            int conditionFlag = toggles[i][2];

            if (conditionFlag >= 0 && !ship.getStateFlag(conditionFlag))
                continue;

            int bx = this.leftPos + 174;
            int by = this.topPos + 131 + i * 13;
            boolean isOn = ship.getStateFlag(flagId);
            graphics.blit(TEXTURE, bx, by, isOn ? 0 : 11, 214, 11, 11);
        }
    }

    /**
     * Render slider bar backgrounds (track + handle)
     */
    private void renderSliderBarsBg(GuiGraphics graphics, BasicEntityShip ship, int[] barIndices) {
        for (int idx = 0; idx < barIndices.length; idx++) {
            int barIndex = barIndices[idx];
            int trackY = this.topPos + SLIDER_TRACK_Y[idx];
            graphics.blit(TEXTURE, this.leftPos + SLIDER_TRACK_X, trackY, 31, 214, 43, 3);

            // Get slider position
            int pos;
            if (mousePressBar == barIndex) {
                pos = barPos;
            } else {
                pos = stateToBarPos(barIndex, getSliderState(ship, barIndex));
            }
            pos = Mth.clamp(pos, 0, SLIDER_MAX_POS);

            // Handle
            int hx = this.leftPos + 189 + pos;
            int hy = trackY - 3;
            graphics.blit(TEXTURE, hx, hy, 22, 214, 9, 9);
        }
    }

    // ========== Info Page Text Rendering ==========

    /**
     * Render page 6 background: Show Held toggle + Model State grid
     */
    private void renderPage6Bg(GuiGraphics graphics, BasicEntityShip ship) {
        // Show Held Item toggle
        int bx = this.leftPos + 174;
        int by = this.topPos + 131;
        boolean showHeld = ship.getStateFlag(ID.F.ShowHeldItem);
        drawToggleButton(graphics, bx, by, showHeld);

        // Model state button grid (4x4)
        int numStates = ship.getStateMinor(ID.M.NumState);
        int modelState = ship.getStateEmotion(ID.S.State);
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 4; col++) {
                int stateIdx = row * 4 + col;
                if (stateIdx >= numStates)
                    return;

                int sx = this.leftPos + 176 + col * 16;
                int sy = this.topPos + 157 + row * 13;
                boolean stateOn = (modelState & (1 << stateIdx)) != 0;
                graphics.blit(TEXTURE, sx, sy, stateOn ? 0 : 11, 214, 11, 11);
            }
        }
    }

    /**
     * Render page 7 background: Task selection
     */
    private void renderPage7Bg(GuiGraphics graphics, BasicEntityShip ship) {
        int currentTask = ship.getStateMinor(ID.M.Task);
        // Task icon row and overlay labels.
        graphics.blit(TEXTURE, this.leftPos + 174, this.topPos + 136, 87, 214, 64, 16);
        graphics.blit(TEXTURE, this.leftPos + 174, this.topPos + 138, 151, 237, 64, 16);

        switch (currentTask) {
            case 1 -> graphics.blit(TEXTURE, this.leftPos + 174, this.topPos + 136, 87, 230, 16, 16);
            case 2 -> graphics.blit(TEXTURE, this.leftPos + 190, this.topPos + 136, 103, 230, 16, 16);
            case 3 -> graphics.blit(TEXTURE, this.leftPos + 206, this.topPos + 136, 119, 230, 16, 16);
            case 4 -> graphics.blit(TEXTURE, this.leftPos + 222, this.topPos + 136, 135, 230, 16, 16);
            default -> {
            }
        }

        // Task side toggle: metadata, ore dict, NBT tag checkboxes
        int taskSide = ship.getStateMinor(ID.M.TaskSide);
        graphics.blit(TEXTURE, this.leftPos + 177, this.topPos + 157,
                0,
                (taskSide & (1 << 18)) != 0 ? 236 : 225,
                11, 11);
        graphics.blit(TEXTURE, this.leftPos + 177, this.topPos + 170,
                11,
                (taskSide & (1 << 19)) != 0 ? 236 : 225,
                11, 11);
        graphics.blit(TEXTURE, this.leftPos + 177, this.topPos + 183,
                22,
                (taskSide & (1 << 20)) != 0 ? 236 : 225,
                11, 11);
    }

    /**
     * Render page 8 background: Task side I/O/Fuel direction buttons
     */
    private void renderPage8Bg(GuiGraphics graphics, BasicEntityShip ship) {
        int taskSide = ship.getStateMinor(ID.M.TaskSide);

        graphics.blit(TEXTURE, this.leftPos + 173, this.topPos + 144, 151, 214, 66, 11);
        graphics.blit(TEXTURE, this.leftPos + 173, this.topPos + 170, 151, 214, 66, 11);
        graphics.blit(TEXTURE, this.leftPos + 173, this.topPos + 196, 151, 214, 66, 11);

        for (int i = 0; i < 18; i++) {
            if ((taskSide & (1 << i)) != 0) {
                int dx = (i % 6) * 11;
                int dy = (i / 6) * 26;
                graphics.blit(TEXTURE, this.leftPos + 173 + dx, this.topPos + 144 + dy, 151 + dx, 225, 11, 11);
            }
        }
    }

    /**
     * Helper: draw a single 11x11 toggle button
     */
    private void drawToggleButton(GuiGraphics graphics, int x, int y, boolean isOn) {
        graphics.blit(TEXTURE, x, y, isOn ? 0 : 11, 214, 11, 11);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        BasicEntityShip ship = this.menu.getShip();
        if (ship == null) {
            graphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x404040, false);
            graphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY,
                    0x404040, false);
            return;
        }

        // Ship Name
        String shipName = ship.hasCustomName() ? Objects.requireNonNull(ship.getCustomName()).getString() : ship.getName().getString();
        graphics.drawString(this.font, shipName, 8, 6, 0x000000, false);

        // Level (right-aligned, gold for 150+)
        int level = ship.getStateMinor(ID.M.ShipLevel);
        String levelStr = "Lv." + level;
        int levelColor = level >= 150 ? 0xFFD700 : 0xFFFFFF;
        graphics.drawString(this.font, levelStr, this.imageWidth - 6 - this.font.width(levelStr), 6, levelColor, true);

        // HP Text
        renderHPText(graphics, ship);

        // Info Page Content
        switch (infoPage) {
            case 0:
                renderInfoPage0(graphics, ship);
                break;
            case 1:
                renderInfoPage1(graphics, ship);
                break;
            case 2:
                renderInfoPage2(graphics, ship);
                break;
        }

        // AI Page Labels
        renderAIPageLabels(graphics, ship);

        // Player inventory title (vanilla位置を維持)
        graphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, 121, 0x404040, false);
    }

    /**
     * Render HP text label and value
     */
    private void renderHPText(GuiGraphics graphics, BasicEntityShip ship) {
        int hpCurrent = Mth.ceil(ship.getHealth());
        int hpMax = Mth.ceil(ship.getMaxHealth());
        // original: "HP" label right-aligned at x=145, current HP at x=147, "/max" after
        graphics.drawString(this.font, "HP", 145 - this.font.width("HP"), 6, 0x00FFFF, true);

        float hpRatio = hpMax > 0 ? (float) hpCurrent / (float) hpMax : 1.0f;
        int hpColor = GuiHelper.getBonusPointColor(ship.getAttrs().getAttrsBonus(ID.AttrsBase.HP));
        String curStr = String.valueOf(hpCurrent);
        String maxStr = "/" + hpMax;
        int curColor = hpCurrent < hpMax ? GuiHelper.getDarkerColor(hpColor, 0.8F) : hpColor;
        graphics.drawString(this.font, curStr, 147, 6, curColor, true);
        graphics.drawString(this.font, maxStr, 148 + this.font.width(curStr), 6, hpColor, true);
    }

    // ========== AI Page Label Rendering ==========

    /**
     * Page 0: Kills, EXP, Ammo, Grudge, Morale
     */
    private void renderInfoPage0(GuiGraphics graphics, BasicEntityShip ship) {
        int textX = 75;
        int textY = 20;
        int lc = 0x404040;
        int vc = 0xFFFFFF;

        drawStatLine(graphics, textX, textY, tr("gui.shincolle.kills", "Kills") + ":",
                String.valueOf(ship.getStateMinor(ID.M.Kills)), lc, vc);
        textY += 21;
        drawStatLine(graphics, textX, textY, tr("gui.shincolle.exp", "EXP") + ":",
                ship.getStateMinor(ID.M.ExpCurrent) + "/" + ship.getStateMinor(ID.M.ExpNext), lc, vc);
        textY += 21;
        drawStatLine(graphics, textX, textY, tr("gui.shincolle.ammolight", "Ammo(L)") + ":",
                String.valueOf(ship.getStateMinor(ID.M.NumAmmoLight)), lc, vc);
        textY += 21;
        drawStatLine(graphics, textX, textY, tr("gui.shincolle.ammoheavy", "Ammo(H)") + ":",
                String.valueOf(ship.getStateMinor(ID.M.NumAmmoHeavy)), lc, vc);
        textY += 21;
        drawStatLine(graphics, textX, textY, tr("gui.shincolle.grudge", "Grudge") + ":",
                String.valueOf(ship.getStateMinor(ID.M.NumGrudge)), lc, vc);
    }

    /**
     * Page 1: ATK, DEF, SPD, MOV, HIT
     */
    private void renderInfoPage1(GuiGraphics graphics, BasicEntityShip ship) {
        Attrs attrs = ship.getAttrs();
        if (attrs == null)
            return;

        int textX = 75;
        int textY = 20;
        int lc = 0x404040;
        int vc = 0xFFFFFF;

        String atkKey = showAirAttack ? "gui.shincolle.firepower2" : "gui.shincolle.firepower1";
        float atkVal = showAirAttack ? attrs.getAttrsBuffed(ID.Attrs.ATK_AL) : attrs.getAttrsBuffed(ID.Attrs.ATK_L);
        float torpedoVal = showAirAttack ? attrs.getAttrsBuffed(ID.Attrs.ATK_AH) : attrs.getAttrsBuffed(ID.Attrs.ATK_H);
        String atkText = String.format("%.1f / %.1f", atkVal, torpedoVal);

        drawStatLine(graphics, textX, textY, I18n.get(atkKey),
                atkText, lc, GuiHelper.getBonusPointColor(attrs.getAttrsBonus(ID.AttrsBase.ATK)));
        textY += 21;
        drawStatLine(graphics, textX, textY, I18n.get("gui.shincolle.armor"),
                String.format("%.1f%%", attrs.getAttrsBuffed(ID.Attrs.DEF) * 100F), lc,
                GuiHelper.getBonusPointColor(attrs.getAttrsBonus(ID.AttrsBase.DEF)));
        textY += 21;
        drawStatLine(graphics, textX, textY, I18n.get("gui.shincolle.attackspeed"),
                String.format("%.2f", attrs.getAttrsBuffed(ID.Attrs.SPD)), lc,
                GuiHelper.getBonusPointColor(attrs.getAttrsBonus(ID.AttrsBase.SPD)));
        textY += 21;
        drawStatLine(graphics, textX, textY, I18n.get("gui.shincolle.movespeed"),
                String.format("%.2f", attrs.getAttrsBuffed(ID.Attrs.MOV)), lc,
                GuiHelper.getBonusPointColor(attrs.getAttrsBonus(ID.AttrsBase.MOV)));
        textY += 21;
        drawStatLine(graphics, textX, textY, I18n.get("gui.shincolle.range"),
                String.format("%.1f", attrs.getAttrsBuffed(ID.Attrs.HIT)), lc,
                GuiHelper.getBonusPointColor(attrs.getAttrsBonus(ID.AttrsBase.HIT)));
    }

    /**
     * Page 2: Marriage, Ring, Formation, Ship type, UID
     */
    private void renderInfoPage2(GuiGraphics graphics, BasicEntityShip ship) {
        int textX = 75;
        int textY = 20;
        int lc = 0x404040;
        int vc = 0xFFFFFF;

        boolean isMarried = ship.getStateFlag(ID.F.IsMarried);
        drawStatLine(graphics, textX, textY, tr("gui.shincolle.marriage", "Marriage") + ":",
                isMarried ? tr("gui.shincolle.married", "Married") : tr("gui.shincolle.unmarried", "Unmarried"),
                lc, 0xFFFF00);
        textY += 21;
        drawStatLine(graphics, textX, textY, tr("gui.shincolle.formation.formation", "Formation") + ":",
                getFormationName(ship.getStateMinor(ID.M.FormatType)), lc, vc);

        if (ship instanceof BasicEntityShipCV cvShip) {
            textY += 42;
            drawStatLine(graphics, textX, textY, tr("gui.shincolle.airplanelight", "Light Aircraft") + ":",
                    String.valueOf(cvShip.getNumAircraftLight()), lc, 0xFFFF00);
            textY += 21;
            drawStatLine(graphics, textX, textY, tr("gui.shincolle.airplaneheavy", "Heavy Aircraft") + ":",
                    String.valueOf(cvShip.getNumAircraftHeavy()), lc, 0xFFFF00);
        }
    }

    /**
     * Draw label at textX, value right-aligned at x=133
     */
    private void drawStatLine(GuiGraphics graphics, int textX, int textY,
                              String label, String value, int labelColor, int valueColor) {
        graphics.drawString(this.font, label, textX, textY, labelColor, false);
        graphics.drawString(this.font, value, 133 - this.font.width(value), textY, valueColor, true);
    }

    /**
     * Dispatch AI page label rendering
     */
    private void renderAIPageLabels(GuiGraphics graphics, BasicEntityShip ship) {
        switch (showPageAI) {
            case 1:
                renderToggleLabels(graphics, ship, PAGE1_TOGGLES, PAGE1_LABELS, PAGE1_LABEL_KEYS);
                break;
            case 2:
                renderSliderLabelsPage2(graphics, ship);
                break;
            case 3:
                renderToggleLabels(graphics, ship, PAGE3_TOGGLES, PAGE3_LABELS, PAGE3_LABEL_KEYS);
                break;
            case 4:
                renderToggleLabels(graphics, ship, PAGE4_TOGGLES, PAGE4_LABELS, PAGE4_LABEL_KEYS);
                break;
            case 5:
                renderSliderLabelsPage5(graphics, ship);
                break;
            case 6:
                renderPage6Labels(graphics, ship);
                break;
            case 7:
                renderPage7Labels(graphics, ship);
                break;
            case 8:
                renderPage8Labels(graphics);
                break;
        }
    }

    /**
     * Render labels for toggle pages
     */
    private void renderToggleLabels(GuiGraphics graphics, BasicEntityShip ship,
                                    int[][] toggles, String[] labels, String[] labelKeys) {
        for (int i = 0; i < toggles.length; i++) {
            int conditionFlag = toggles[i][2];
            if (conditionFlag >= 0 && !ship.getStateFlag(conditionFlag))
                continue;

            graphics.drawString(this.font, tr(labelKeys[i], labels[i]), 187, 133 + i * 13, 0x000000, false);
        }
    }

    /**
     * Render slider labels for page 2: Follow Min, Follow Max, Flee HP
     */
    private void renderSliderLabelsPage2(GuiGraphics graphics, BasicEntityShip ship) {
        int lc = 0x000000;

        graphics.drawString(this.font, tr("gui.shincolle.followmin", "Follow Min"), 174, 134, lc, false);
        int fmin = (mousePressBar == 0) ? barPosToState(0, barPos) : ship.getStateMinor(ID.M.FollowMin);
        int fminColor = (mousePressBar == 0) ? 0xFF4444 : 0xFFFF00;
        graphics.drawString(this.font, String.valueOf(fmin), 174, 145, fminColor, true);

        graphics.drawString(this.font, tr("gui.shincolle.followmax", "Follow Max"), 174, 158, lc, false);
        int fmax = (mousePressBar == 1) ? barPosToState(1, barPos) : ship.getStateMinor(ID.M.FollowMax);
        int fmaxColor = (mousePressBar == 1) ? 0xFF4444 : 0xFFFF00;
        graphics.drawString(this.font, String.valueOf(fmax), 174, 169, fmaxColor, true);

        graphics.drawString(this.font, tr("gui.shincolle.fleehp", "Flee HP%"), 174, 182, lc, false);
        int flee = (mousePressBar == 2) ? barPosToState(2, barPos) : ship.getStateMinor(ID.M.FleeHP);
        int fleeColor = (mousePressBar == 2) ? 0xFF4444 : 0xFFFF00;
        graphics.drawString(this.font, String.valueOf(flee), 174, 193, fleeColor, true);
    }

    /**
     * Render slider labels for page 5: Waypoint Stay, Auto Combat Ration
     */
    private void renderSliderLabelsPage5(GuiGraphics graphics, BasicEntityShip ship) {
        int lc = 0x000000;

        graphics.drawString(this.font, tr("gui.shincolle.ai.wpstay", "Waypoint Stay"), 174, 134, lc, false);
        int wpStay = (mousePressBar == 3) ? barPosToState(3, barPos) : ship.getStateMinor(ID.M.WpStay);
        int wpColor = (mousePressBar == 3) ? 0xFF4444 : 0xFFFF00;
        String wpText = wpStay + "s";
        graphics.drawString(this.font, wpText, 174, 145, wpColor, true);

        graphics.drawString(this.font, tr("gui.shincolle.autocombatration", "Auto CR"), 174, 158, lc, false);
        int autoCR = (mousePressBar == 4) ? barPosToState(4, barPos) : ship.getStateMinor(ID.M.UseCombatRation);
        int crColor = (mousePressBar == 4) ? 0xFF4444 : 0xFFFF00;
        String crText = getMoraleLevelName(autoCR);
        graphics.drawString(this.font, crText, 174, 169, crColor, true);
    }

    // ========== Input Handling ==========

    /**
     * Render page 6 labels: Show Held + Model State grid
     */
    private void renderPage6Labels(GuiGraphics graphics, BasicEntityShip ship) {
        boolean showHeld = ship.getStateFlag(ID.F.ShowHeldItem);
        graphics.drawString(this.font, tr("gui.shincolle.showhelditem", "Show Held Item"), 187, 133,
                0x000000, false);
        graphics.drawString(this.font, tr("gui.shincolle.appearance", "Appearance"), 177, 146, 0x000000, false);
    }

    /**
     * Render page 7 labels: Task selection
     */
    private void renderPage7Labels(GuiGraphics graphics, BasicEntityShip ship) {
        graphics.drawString(this.font, " " + tr("gui.shincolle.crane.usemeta", "Metadata"), 187, 159, 0x000000, false);
        graphics.drawString(this.font, " " + tr("gui.shincolle.crane.useoredict", "Ore Dict"), 187, 172, 0x000000,
                false);
        graphics.drawString(this.font, " " + tr("gui.shincolle.crane.usenbt", "NBT Tag"), 187, 185, 0x000000, false);
    }

    /**
     * Render page 8 labels: Task side directions
     */
    private void renderPage8Labels(GuiGraphics graphics) {
        String[] rowLabels = {
                tr("gui.shincolle.ai.inputside", "Input"),
                tr("gui.shincolle.ai.outputside", "Output"),
                tr("gui.shincolle.ai.fuelside", "Fuel")
        };
        int[] rowLabelY = {133, 159, 185};
        for (int i = 0; i < 3; i++) {
            graphics.drawString(this.font, rowLabels[i], 177, rowLabelY[i], 0x000000, false);
        }
    }

    /**
     * Render small page numbers on each AI tab
     */
    private void renderAIPageTabNumbers(GuiGraphics graphics) {
        for (int i = 0; i < 6; i++) {
            int tabY = 132 + i * 13;
            int color = (showPageAI == i + 1) ? 0xFFFFFF : 0x999999;
            graphics.drawString(this.font, String.valueOf(i + 1), 240, tabY, color, false);
        }
        for (int i = 0; i < 6; i++) {
            int tabY = 132 + i * 13;
            int color = (showPageAI == i + 7) ? 0xFFFFFF : 0x999999;
            String num = String.valueOf(i + 7);
            // Right column numbers are tiny, just show first digit for 10+
            if (i + 7 >= 10)
                num = String.valueOf(i + 7 - 10);
            graphics.drawString(this.font, num, 247, tabY, color, false);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int relX = (int) mouseX - this.leftPos;
            int relY = (int) mouseY - this.topPos;

            // Info page tab clicks (x=133-142)
            if (relX >= 133 && relX <= 142) {
                if (relY >= 18 && relY <= 52) {
                    infoPage = 0;
                    return true;
                } else if (relY >= 53 && relY <= 88) {
                    infoPage = 1;
                    return true;
                } else if (relY >= 89 && relY <= 125) {
                    infoPage = 2;
                    return true;
                }
            }

            // AI page tab clicks - left column (x=239-245, pages 1-6)
            if (relX >= 239 && relX <= 245 && relY >= 131 && relY <= 208) {
                int tabIdx = (relY - 131) / 13;
                showPageAI = tabIdx + 1;
                return true;
            }

            // AI page tab clicks - right column (x=246-253, pages 7-12)
            if (relX >= 246 && relX <= 253 && relY >= 131 && relY <= 208) {
                int tabIdx = (relY - 131) / 13;
                showPageAI = tabIdx + 7;
                return true;
            }

            // AI page content clicks
            BasicEntityShip ship = this.menu.getShip();
            if (ship != null) {
                if (handleInfoPageClick(relX, relY))
                    return true;
                if (handleInventoryPageClick(ship, relX, relY))
                    return true;
                if (handleAIPageClick(ship, relX, relY))
                    return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    /**
     * Handle clicks inside info-page controls.
     */
    private boolean handleInfoPageClick(int relX, int relY) {
        // [PORT] 1.10.2 -> 1.20.1: keep the attack/air view toggle on attribute page.
        if (infoPage == 1 && relX >= 73 && relX <= 132 && relY >= 18 && relY <= 40) {
            showAirAttack = !showAirAttack;
            return true;
        }
        return false;
    }

    /**
     * Handle inventory page tab clicks (left column) and sync to server.
     */
    private boolean handleInventoryPageClick(BasicEntityShip ship, int relX, int relY) {
        if (relX < 61 || relX > 70) {
            return false;
        }

        int page = -1;
        if (relY >= 18 && relY <= 52) {
            page = 0;
        } else if (relY >= 53 && relY <= 88) {
            page = 1;
        } else if (relY >= 89 && relY <= 125) {
            page = 2;
        }

        if (page < 0) {
            return false;
        }

        int maxPage = ship.getInventoryPageSize();
        if (page > maxPage) {
            return false;
        }

        // [PORT] 1.10.2 -> 1.20.1: keep local menu page in sync for slot mapping.
        this.menu.setInventoryPage(page);
        sendShipButton(ship, ID.B.ShipInv_InvPage, page);
        return true;
    }

    /**
     * Handle click within the AI page content area. Returns true if handled.
     */
    private boolean handleAIPageClick(BasicEntityShip ship, int relX, int relY) {
        return switch (showPageAI) {
            case 1 -> handleToggleClick(ship, relX, relY, PAGE1_TOGGLES);
            case 2 -> handleSliderClick(relX, relY, new int[]{0, 1, 2});
            case 3 -> handleToggleClick(ship, relX, relY, PAGE3_TOGGLES);
            case 4 -> handleToggleClick(ship, relX, relY, PAGE4_TOGGLES);
            case 5 -> handleSliderClick(relX, relY, new int[]{3, 4});
            case 6 -> handlePage6Click(ship, relX, relY);
            case 7 -> handlePage7Click(ship, relX, relY);
            case 8 -> handlePage8Click(ship, relX, relY);
            default -> false;
        };
    }

    /**
     * Handle click on toggle button pages. Toggle buttons at x=174-184, y=131+i*13.
     */
    private boolean handleToggleClick(BasicEntityShip ship, int relX, int relY, int[][] toggles) {
        if (relX < 174 || relX > 184)
            return false;

        for (int i = 0; i < toggles.length; i++) {
            int by = 131 + i * 13;
            if (relY >= by && relY <= by + 11) {
                int conditionFlag = toggles[i][2];
                if (conditionFlag >= 0 && !ship.getStateFlag(conditionFlag))
                    return false;

                int flagId = toggles[i][0];
                int buttonId = toggles[i][1];
                boolean current = ship.getStateFlag(flagId);
                int newValue = current ? 0 : 1;

                ship.setStateFlagI(flagId, newValue);
                sendShipButton(ship, buttonId, newValue);
                return true;
            }
        }
        return false;
    }

    // ========== Slider Drag Handling ==========

    /**
     * Handle click on slider bar pages. Start drag if clicked on a slider area.
     */
    private boolean handleSliderClick(int relX, int relY, int[] barIndices) {
        if (relX < 187 || relX > 238)
            return false;

        for (int idx = 0; idx < barIndices.length; idx++) {
            int trackY = SLIDER_TRACK_Y[idx];
            int clickTop = trackY - 3;
            int clickBot = trackY + 6;
            if (relY >= clickTop && relY <= clickBot) {
                mousePressBar = barIndices[idx];
                barPos = Mth.clamp(relX - SLIDER_TRACK_X, 0, SLIDER_MAX_POS);
                return true;
            }
        }
        return false;
    }

    /**
     * Handle click on page 6 (Show Held toggle + model states)
     */
    private boolean handlePage6Click(BasicEntityShip ship, int relX, int relY) {
        // Show Held Item toggle at (174, 131)
        if (relX >= 174 && relX <= 184 && relY >= 131 && relY <= 142) {
            boolean current = ship.getStateFlag(ID.F.ShowHeldItem);
            int newVal = current ? 0 : 1;
            ship.setStateFlagI(ID.F.ShowHeldItem, newVal);
            sendShipButton(ship, ID.B.ShipInv_ShowHeld, newVal);
            return true;
        }

        // Model state grid (4x4 starting at x=176, y=157)
        if (relX >= 176 && relX <= 240 && relY >= 157 && relY <= 208) {
            int col = (relX - 176) / 16;
            int row = (relY - 157) / 13;
            if (col < 4) {
                int stateIdx = row * 4 + col;
                int numStates = ship.getStateMinor(ID.M.NumState);
                if (stateIdx < numStates) {
                    int modelState = ship.getStateEmotion(ID.S.State);
                    int newState = modelState ^ (1 << stateIdx);
                    ship.setStateEmotion(ID.S.State, newState, false);
                    sendShipButton(ship, ID.B.ShipInv_ModelState01 + stateIdx, newState);
                    return true;
                }
            }
        }
        return false;
    }

    // ========== Main Render ==========

    /**
     * Handle click on page 7 (Task selection + task settings)
     */
    private boolean handlePage7Click(BasicEntityShip ship, int relX, int relY) {
        // Task buttons (x=174-237, y=136-149)
        if (relX >= 174 && relX <= 237 && relY >= 136 && relY <= 149) {
            int taskIdx = (relX - 174) / 16;
            int currentTask = ship.getStateMinor(ID.M.Task);
            int newTask = (currentTask == taskIdx + 1) ? 0 : taskIdx + 1;
            ship.setStateMinor(ID.M.Task, newTask);
            sendShipButton(ship, ID.B.ShipInv_Task, newTask);
            return true;
        }

        // Task setting checkboxes (x=177-187, y=157/170/183)
        if (relX >= 177 && relX <= 187) {
            for (int i = 0; i < 3; i++) {
                int cy = 157 + i * 13;
                if (relY >= cy && relY <= cy + 11) {
                    int taskSide = ship.getStateMinor(ID.M.TaskSide);
                    int newTaskSide = taskSide ^ (1 << (18 + i));
                    ship.setStateMinor(ID.M.TaskSide, newTaskSide);
                    sendShipButton(ship, ID.B.ShipInv_TaskSide, newTaskSide);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Handle click on page 8 (Task side direction buttons)
     */
    private boolean handlePage8Click(BasicEntityShip ship, int relX, int relY) {
        if (relX < 173 || relX > 238)
            return false;

        int[] rowYs = {144, 170, 196};
        for (int row = 0; row < 3; row++) {
            if (relY >= rowYs[row] && relY <= rowYs[row] + 11) {
                int col = (relX - 173) / 11;
                int bit = row * 6 + col;
                int taskSide = ship.getStateMinor(ID.M.TaskSide);
                int newTaskSide = taskSide ^ (1 << bit);
                ship.setStateMinor(ID.M.TaskSide, newTaskSide);
                sendShipButton(ship, ID.B.ShipInv_TaskSide, newTaskSide);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (mousePressBar >= 0) {
            int relX = (int) mouseX - this.leftPos;
            barPos = Mth.clamp(relX - SLIDER_TRACK_X, 0, SLIDER_MAX_POS);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (mousePressBar >= 0) {
            BasicEntityShip ship = this.menu.getShip();
            if (ship != null) {
                int value = barPosToState(mousePressBar, barPos);
                int buttonId = getSliderButtonId(mousePressBar);
                setSliderState(ship, mousePressBar, value);
                sendShipButton(ship, buttonId, value);
            }
            mousePressBar = -1;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    // ========== Slider Conversion Methods ==========

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);

        BasicEntityShip ship = this.menu.getShip();
        if (ship != null && isHoveringMoraleIcon(mouseX, mouseY)) {
            renderMoraleTooltip(graphics, ship, mouseX, mouseY);
        }
        if (ship != null) {
            renderAIPageTooltip(graphics, mouseX, mouseY);
        }
    }

    /**
     * Render small hover tooltips for AI page task controls (legacy parity).
     */
    private void renderAIPageTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (showPageAI != 7) {
            return;
        }

        int relX = mouseX - this.leftPos;
        int relY = mouseY - this.topPos;
        if (relX < 173 || relX >= 239 || relY < 131 || relY >= 207) {
            return;
        }

        List<Component> tips = new ArrayList<>();
        if (relY <= 157) {
            if (relX < 190) {
                tips.add(Component.literal(tr("gui.shincolle.ai.cooking", "Cooking")));
            } else if (relX < 206) {
                tips.add(Component.literal(tr("gui.shincolle.ai.fishing", "Fishing")));
            } else if (relX < 222) {
                tips.add(Component.literal(tr("gui.shincolle.ai.mining", "Mining")));
            } else {
                tips.add(Component.literal(tr("gui.shincolle.ai.crafting", "Crafting")));
            }
        } else if (relY <= 170) {
            tips.add(Component.literal(tr("gui.shincolle.crane.usemeta", "Metadata")));
        } else if (relY <= 183) {
            tips.add(Component.literal(tr("gui.shincolle.crane.useoredict", "Ore Dict")));
        } else if (relY <= 196) {
            tips.add(Component.literal(tr("gui.shincolle.crane.usenbt", "NBT Tag")));
        }

        if (!tips.isEmpty()) {
            graphics.renderComponentTooltip(this.font, tips, mouseX, mouseY);
        }
    }

    /**
     * Morale icon hover region near the top-right model panel.
     */
    private boolean isHoveringMoraleIcon(int mouseX, int mouseY) {
        int x0 = this.leftPos + 238;
        int y0 = this.topPos + 17;
        return mouseX >= x0 && mouseX < x0 + 13 && mouseY >= y0 && mouseY < y0 + 13;
    }

    /**
     * Render morale tooltip with current morale and morale-derived attribute
     * modifiers.
     */
    private void renderMoraleTooltip(GuiGraphics graphics, BasicEntityShip ship, int mouseX, int mouseY) {
        List<Component> lines = new ArrayList<>();
        int morale = ship.getMorale();
        lines.add(Component.literal(getMoraleDisplayName(morale) + " (" + morale + ")"));

        Attrs attrs = ship.getAttrs();
        if (attrs instanceof AttrsAdv adv) {
            lines.add(Component.literal(tr("gui.shincolle.firepower1", "Firepower") + ": x "
                    + String.format("%.0f", adv.getAttrsMorale(ID.Attrs.ATK_L) * 100F)
                    + "% / "
                    + String.format("%.0f", adv.getAttrsMorale(ID.Attrs.ATK_H) * 100F) + "%"));
            lines.add(Component.literal(tr("gui.shincolle.airfirepower", "Air Firepower") + ": x "
                    + String.format("%.0f", adv.getAttrsMorale(ID.Attrs.ATK_AL) * 100F)
                    + "% / "
                    + String.format("%.0f", adv.getAttrsMorale(ID.Attrs.ATK_AH) * 100F) + "%"));
            lines.add(Component.literal(tr("gui.shincolle.attackspeed", "Attack Speed") + ": x "
                    + String.format("%.0f", adv.getAttrsMorale(ID.Attrs.SPD) * 100F) + "%"));
            lines.add(Component.literal(tr("gui.shincolle.range", "Range") + ": + "
                    + String.format("%.1f", adv.getAttrsMorale(ID.Attrs.HIT))));
            lines.add(Component.literal(tr("gui.shincolle.critical", "Critical") + ": x "
                    + String.format("%.0f", adv.getAttrsMorale(ID.Attrs.CRI) * 100F) + "%"));
            lines.add(Component.literal("DHIT: x "
                    + String.format("%.0f", adv.getAttrsMorale(ID.Attrs.DHIT) * 100F) + "%"));
            lines.add(Component.literal("THIT: x "
                    + String.format("%.0f", adv.getAttrsMorale(ID.Attrs.THIT) * 100F) + "%"));
            lines.add(Component.literal(tr("gui.shincolle.missrate", "Miss") + ": x "
                    + String.format("%.0f", adv.getAttrsMorale(ID.Attrs.MISS) * 100F) + "%"));
            lines.add(Component.literal("AA: x "
                    + String.format("%.0f", adv.getAttrsMorale(ID.Attrs.AA) * 100F) + "%"));
            lines.add(Component.literal("ASM: x "
                    + String.format("%.0f", adv.getAttrsMorale(ID.Attrs.ASM) * 100F) + "%"));
            lines.add(Component.literal(tr("gui.shincolle.armor", "Armor") + ": + "
                    + String.format("%.0f", adv.getAttrsMorale(ID.Attrs.DEF) * 100F) + "%"));
            lines.add(Component.literal(tr("gui.shincolle.dodge", "Dodge") + ": + "
                    + String.format("%.0f", adv.getAttrsMorale(ID.Attrs.DODGE) * 100F) + "%"));
            lines.add(Component.literal("XP: + "
                    + String.format("%.0f", adv.getAttrsMorale(ID.Attrs.XP) * 100F) + "%"));
            lines.add(Component.literal(tr("gui.shincolle.grudge", "Grudge") + ": + "
                    + String.format("%.0f", adv.getAttrsMorale(ID.Attrs.GRUDGE) * 100F) + "%"));
            lines.add(Component.literal(tr("gui.shincolle.ammo", "Ammo") + ": + "
                    + String.format("%.0f", adv.getAttrsMorale(ID.Attrs.AMMO) * 100F) + "%"));
            lines.add(Component.literal("HPRES: + "
                    + String.format("%.0f", adv.getAttrsMorale(ID.Attrs.HPRES) * 100F) + "%"));
            lines.add(Component.literal("KB: + "
                    + String.format("%.0f", adv.getAttrsMorale(ID.Attrs.KB) * 100F) + "%"));
            lines.add(Component.literal(tr("gui.shincolle.movespeed", "Move Speed") + ": + "
                    + String.format("%.2f", adv.getAttrsMorale(ID.Attrs.MOV))));
        }

        graphics.renderComponentTooltip(this.font, lines, mouseX, mouseY);
    }

    /**
     * Convert entity state value to slider bar position (0-42)
     */
    private int stateToBarPos(int barIndex, int stateValue) {
        return switch (barIndex) {
            case 0 -> (int) ((stateValue - 1) / 30.0 * SLIDER_MAX_POS); // FollowMin: 1-31
            case 1 -> (int) ((stateValue - 2) / 30.0 * SLIDER_MAX_POS); // FollowMax: 2-32
            case 2 -> (int) (stateValue / 100.0 * SLIDER_MAX_POS); // FleeHP: 0-100
            case 3 -> (int) (stateValue / 16.0 * SLIDER_MAX_POS); // WpStay: 0-16
            case 4 -> Mth.clamp((stateValue - 1) * 14, 0, SLIDER_MAX_POS); // AutoCR: 1-4
            default -> 0;
        };
    }

    // ========== Packet Sending ==========

    /**
     * Convert slider bar position (0-42) to entity state value
     */
    private int barPosToState(int barIndex, int pos) {
        pos = Mth.clamp(pos, 0, SLIDER_MAX_POS);
        return switch (barIndex) {
            case 0 -> Mth.clamp((int) (pos / (double) SLIDER_MAX_POS * 30 + 1), 1, 31);
            case 1 -> Mth.clamp((int) (pos / (double) SLIDER_MAX_POS * 30 + 2), 2, 32);
            case 2 -> Mth.clamp((int) (pos / (double) SLIDER_MAX_POS * 100), 0, 100);
            case 3 -> Mth.clamp((int) (pos / (double) SLIDER_MAX_POS * 16), 0, 16);
            case 4 -> Mth.clamp(pos / 14 + 1, 1, 4);
            default -> 0;
        };
    }

    // ========== Utility Methods ==========

    /**
     * Get the ID.M state index for a slider bar
     */
    private int getSliderState(BasicEntityShip ship, int barIndex) {
        return switch (barIndex) {
            case 0 -> ship.getStateMinor(ID.M.FollowMin);
            case 1 -> ship.getStateMinor(ID.M.FollowMax);
            case 2 -> ship.getStateMinor(ID.M.FleeHP);
            case 3 -> ship.getStateMinor(ID.M.WpStay);
            case 4 -> ship.getStateMinor(ID.M.UseCombatRation);
            default -> 0;
        };
    }

    /**
     * Set the entity state for a slider bar (client-side preview)
     */
    private void setSliderState(BasicEntityShip ship, int barIndex, int value) {
        switch (barIndex) {
            case 0:
                ship.setStateMinor(ID.M.FollowMin, value);
                break;
            case 1:
                ship.setStateMinor(ID.M.FollowMax, value);
                break;
            case 2:
                ship.setStateMinor(ID.M.FleeHP, value);
                break;
            case 3:
                ship.setStateMinor(ID.M.WpStay, value);
                break;
            case 4:
                ship.setStateMinor(ID.M.UseCombatRation, value);
                break;
        }
    }

    /**
     * Get the button ID for a slider bar
     */
    private int getSliderButtonId(int barIndex) {
        return switch (barIndex) {
            case 0 -> ID.B.ShipInv_FollowMin;
            case 1 -> ID.B.ShipInv_FollowMax;
            case 2 -> ID.B.ShipInv_FleeHP;
            case 3 -> ID.B.ShipInv_WpStay;
            case 4 -> ID.B.ShipInv_AutoCR;
            default -> -1;
        };
    }

    /**
     * Send a ship GUI button packet to the server.
     * Format: type=ShipBtn, values={entityId, 0, buttonId, value}
     */
    private void sendShipButton(BasicEntityShip ship, int buttonId, int value) {
        ModNetworking.sendToServer(new C2SGUIInputPacket(
                C2SGUIInputPacket.ShipBtn,
                new int[]{ship.getId(), 0, buttonId, value}));
    }
}
