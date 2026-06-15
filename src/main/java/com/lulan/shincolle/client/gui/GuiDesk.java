package com.lulan.shincolle.client.gui;

import com.lulan.shincolle.capability.CapaTeitoku;
import com.lulan.shincolle.capability.CapaTeitokuProvider;
import com.lulan.shincolle.client.gui.inventory.ContainerDesk;
import com.lulan.shincolle.entity.BasicEntityShip;
import com.lulan.shincolle.item.ShipSpawnEgg;
import com.lulan.shincolle.network.C2SGUIInputPacket;
import com.lulan.shincolle.network.ModNetworking;
import com.lulan.shincolle.reference.Enums;
import com.lulan.shincolle.reference.ID;
import com.lulan.shincolle.reference.Values;
import com.lulan.shincolle.tileentity.TileEntityDesk;
import com.lulan.shincolle.utility.GuiHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * GUI screen for the admiral's desk block.
 * Supports 4 functional tabs: Radar, Book, Team, Target.
 * <p>
 * Type: 0=block, 1=radar item, 2=book item
 * Functions: 0=none, 1=radar, 2=book, 3=team, 4=target
 */
public class GuiDesk extends AbstractContainerScreen<ContainerDesk> {

    private static final ResourceLocation TEX_MAIN = new ResourceLocation("shincolle", "textures/gui/guidesk.png");
    private static final ResourceLocation TEX_RADAR = new ResourceLocation("shincolle",
            "textures/gui/guideskradar.png");
    private static final ResourceLocation TEX_BOOK = new ResourceLocation("shincolle", "textures/gui/guideskbook.png");
    private static final ResourceLocation TEX_TEAM = new ResourceLocation("shincolle", "textures/gui/guideskteam.png");
    private static final ResourceLocation TEX_TARGET = new ResourceLocation("shincolle",
            "textures/gui/guidesktarget.png");
    private static final ResourceLocation TEX_ICON0 = new ResourceLocation("shincolle",
            "textures/gui/guinameicon0.png");
    private static final ResourceLocation TEX_BOOK2 = new ResourceLocation("shincolle",
            "textures/gui/guideskbook2.png");
    private static final ResourceLocation TEX_ICON1 = new ResourceLocation("shincolle",
            "textures/gui/guinameicon1.png");
    private static final ResourceLocation TEX_ICON2 = new ResourceLocation("shincolle",
            "textures/gui/guinameicon2.png");

    private static final int CLICKCD = 60;
    private static final int LISTCLICK_RADAR = 0;
    private static final int LISTCLICK_TEAM = 1;
    private static final int LISTCLICK_TARGET = 2;
    private static final int LISTCLICK_ALLY = 3;
    private static final int LISTCLICK_BAN = 4;

    private static final int TEAMSTATE_MAIN = 0;
    private static final int TEAMSTATE_CREATE = 1;
    private static final int TEAMSTATE_ALLY = 2;
    private static final int TEAMSTATE_RENAME = 3;
    private static final int TEAMSTATE_BAN = 4;

    // GUI state
    private final TileEntityDesk tile;
    private final int type;
    // Player data
    private final Player player;
    private final CapaTeitoku capa;
    // List scrolling: 0=radar 1=team 2=target 3=ally 4=ban
    private final int[] listNum = {0, 0, 0, 0, 0};
    private final int[] listClicked = {-1, -1, -1, -1, -1};
    private final List<RadarShip> shipList = new ArrayList<>();
    // Target
    private final List<String> tarList = new ArrayList<>();
    // Localized strings (cached)
    private final String strPos;
    private final String strHeight;
    private final String strTeamID;
    private final String strBreak;
    private final String strAlly;
    private final String strOK;
    private final String strUnban;
    private final String strBan;
    private final String strCancel;
    private final String strAllyList;
    private final String strBanList;
    private final String strRename;
    private final String strDisband;
    private final String strCreate;
    private final String strNeutral;
    private final String strBelong;
    private final String strAllied;
    private final String strHostile;
    private final String strRemove;
    private int guiFunc;
    private int tickGUI;
    private int tempCD;
    // Radar
    private int radarZoomLv;
    // Book
    private int bookChapNum;
    private int bookPageNum;
    // Entity gallery (book chapters 4-5)
    private net.minecraft.world.entity.LivingEntity galleryEntity;
    private int galleryShipClass = -1;
    private float mRotateX = 0F;
    private float mRotateY = 0F;
    private int mScale = 50;
    // Team
    private int teamState = TEAMSTATE_MAIN;
    private int listFocus = LISTCLICK_TEAM;
    private EditBox textField;

    private static final float GUI_SCALE = 1.25F;
    private static final float GUI_SCALE_INV = 1.0F / GUI_SCALE;

    public GuiDesk(ContainerDesk menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title);
        this.imageWidth = 256;
        this.imageHeight = 192;
        this.type = menu.getGuiType();
        this.tile = menu.getTile();
        this.player = playerInv.player;
        this.tickGUI = 0;
        this.tempCD = CLICKCD;

        // Get capability
        this.capa = player.getCapability(CapaTeitokuProvider.CAPABILITY).orElse(null);

        // Set initial function based on open type
        if (type == 0 && tile != null) {
            this.guiFunc = tile.getGuiFunc();
            this.bookChapNum = tile.getBookChap();
            this.bookPageNum = tile.getBookPage();
            this.radarZoomLv = tile.getRadarZoomLv();
        } else {
            this.guiFunc = type; // 1=radar, 2=book
            this.bookChapNum = 0;
            this.bookPageNum = 0;
            this.radarZoomLv = 0;
        }

        // Cache localized strings
        strPos = Component.translatable("gui.shincolle.radar.position").getString();
        strHeight = Component.translatable("gui.shincolle.radar.height").getString();
        strTeamID = Component.translatable("gui.shincolle.team.teamid").getString();
        strBreak = Component.translatable("gui.shincolle.team.break").getString();
        strAlly = Component.translatable("gui.shincolle.team.ally").getString();
        strOK = Component.translatable("gui.shincolle.general.ok").getString();
        strUnban = Component.translatable("gui.shincolle.team.unban").getString();
        strBan = Component.translatable("gui.shincolle.team.ban").getString();
        strCancel = Component.translatable("gui.shincolle.general.cancel").getString();
        strAllyList = Component.translatable("gui.shincolle.team.allylist").getString();
        strBanList = Component.translatable("gui.shincolle.team.banlist").getString();
        strRename = Component.translatable("gui.shincolle.team.rename").getString();
        strDisband = Component.translatable("gui.shincolle.team.disband").getString();
        strCreate = Component.translatable("gui.shincolle.team.create").getString();
        strNeutral = Component.translatable("gui.shincolle.team.neutral").getString();
        strBelong = Component.translatable("gui.shincolle.team.belong").getString();
        strAllied = Component.translatable("gui.shincolle.team.allied").getString();
        strHostile = Component.translatable("gui.shincolle.team.hostile").getString();
        strRemove = Component.translatable("gui.shincolle.target.remove").getString();

        // Build target list
        updateTargetClassList();

        // Request full data sync from server
        ModNetworking.sendToServer(new C2SGUIInputPacket(C2SGUIInputPacket.Desk_FuncSync, new int[0]));
    }

    @Override
    protected void init() {
        super.init();

        // Create text input field for team create/rename
        this.textField = new EditBox(this.font, this.leftPos + 10, this.topPos + 24, 153, 12, Component.empty());
        this.textField.setMaxLength(64);
        this.textField.setVisible(false);
        this.textField.setFocused(false);
        this.addWidget(this.textField);
    }

    @Override
    public void containerTick() {
        super.containerTick();
        this.tickGUI++;
        if (this.tempCD > 0)
            this.tempCD--;
        if (this.textField != null)
            this.textField.tick();
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        // Draw main background (only for tile entity GUI)
        if (this.type == 0) {
            g.blit(TEX_MAIN, this.leftPos, this.topPos, 0, 0, 256, 192);

            // Draw function button highlight
            switch (this.guiFunc) {
                case 1:
                    g.blit(TEX_MAIN, leftPos + 3, topPos + 2, 0, 192, 16, 16);
                    break;
                case 2:
                    g.blit(TEX_MAIN, leftPos + 22, topPos + 2, 16, 192, 16, 16);
                    break;
                case 3:
                    g.blit(TEX_MAIN, leftPos + 41, topPos + 2, 32, 192, 16, 16);
                    break;
                case 4:
                    g.blit(TEX_MAIN, leftPos + 60, topPos + 2, 48, 192, 16, 16);
                    break;
            }
        }

        // Draw function-specific background
        switch (this.guiFunc) {
            case 1: // Radar
                g.blit(TEX_RADAR, leftPos, topPos, 0, 0, 256, 192);
                // Zoom level button
                int texty = 192 + radarZoomLv * 8;
                g.blit(TEX_RADAR, leftPos + 9, topPos + 160, 24, texty, 44, 8);
                // Ship selection highlight
                if (listClicked[LISTCLICK_RADAR] >= 0 && listClicked[LISTCLICK_RADAR] < 5) {
                    int cirY = 25 + listClicked[LISTCLICK_RADAR] * 32;
                    g.blit(TEX_RADAR, leftPos + 142, topPos + cirY, 68, 192, 108, 31);
                }
                // Draw radar icons
                drawRadarIcons(g);
                break;
            case 2: // Book
                g.blit(TEX_BOOK, leftPos, topPos, 0, 0, 256, 192);
                // Draw entity gallery background overlay for chap 4/5
                if ((bookChapNum == 4 || bookChapNum == 5) && bookPageNum > 0) {
                    g.blit(TEX_BOOK2, leftPos + 20, topPos + 48, 0, 0, 87, 130);
                }
                // Page button hover
                if (mouseX < leftPos + 137) {
                    g.blit(TEX_BOOK, leftPos + 53, topPos + 182, 0, 192, 18, 10);
                } else {
                    g.blit(TEX_BOOK, leftPos + 175, topPos + 182, 0, 202, 18, 10);
                }
                break;
            case 3: // Team
                g.blit(TEX_TEAM, leftPos, topPos, 0, 0, 256, 192);
                drawTeamPic(g);
                break;
            case 4: // Target
                g.blit(TEX_TARGET, leftPos, topPos, 0, 0, 256, 192);
                // Target selection highlight
                if (listClicked[LISTCLICK_TARGET] >= 0 && listClicked[LISTCLICK_TARGET] < 13) {
                    int cirY = 25 + listClicked[LISTCLICK_TARGET] * 12;
                    g.blit(TEX_TARGET, leftPos + 142, topPos + cirY, 68, 192, 108, 31);
                }
                break;
        }
    }

    // ==================== Background Rendering ====================

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        // Redraw function background in label pass to cover inventory slots.
        // AbstractContainerScreen renders slots between renderBg and renderLabels,
        // so the function background drawn in renderBg gets covered by slot items.
        // Drawing the background again here ensures it covers the slots.
        if (this.guiFunc > 0) {
            switch (this.guiFunc) {
                case 1: // Radar
                    g.blit(TEX_RADAR, 0, 0, 0, 0, 256, 192);
                    int texty = 192 + radarZoomLv * 8;
                    g.blit(TEX_RADAR, 9, 160, 24, texty, 44, 8);
                    if (listClicked[LISTCLICK_RADAR] >= 0 && listClicked[LISTCLICK_RADAR] < 5) {
                        int cirY = 25 + listClicked[LISTCLICK_RADAR] * 32;
                        g.blit(TEX_RADAR, 142, cirY, 68, 192, 108, 31);
                    }
                    // drawRadarIcons uses absolute coords; undo label translation
                    g.pose().pushPose();
                    g.pose().translate(-leftPos, -topPos, 0);
                    drawRadarIcons(g);
                    g.pose().popPose();
                    break;
                case 2: // Book
                    g.blit(TEX_BOOK, 0, 0, 0, 0, 256, 192);
                    if ((bookChapNum == 4 || bookChapNum == 5) && bookPageNum > 0) {
                        g.blit(TEX_BOOK2, 20, 48, 0, 0, 87, 130);
                    }
                    if (mouseX < leftPos + 137) {
                        g.blit(TEX_BOOK, 53, 182, 0, 192, 18, 10);
                    } else {
                        g.blit(TEX_BOOK, 175, 182, 0, 202, 18, 10);
                    }
                    break;
                case 3: // Team
                    g.blit(TEX_TEAM, 0, 0, 0, 0, 256, 192);
                    // drawTeamPic uses absolute coords; undo label translation
                    g.pose().pushPose();
                    g.pose().translate(-leftPos, -topPos, 0);
                    drawTeamPic(g);
                    g.pose().popPose();
                    break;
                case 4: // Target
                    g.blit(TEX_TARGET, 0, 0, 0, 0, 256, 192);
                    if (listClicked[LISTCLICK_TARGET] >= 0 && listClicked[LISTCLICK_TARGET] < 13) {
                        int cirY2 = 25 + listClicked[LISTCLICK_TARGET] * 12;
                        g.blit(TEX_TARGET, 142, cirY2, 68, 192, 108, 31);
                    }
                    break;
            }
        }

        switch (this.guiFunc) {
            case 1:
                drawRadarText(g);
                break;
            case 2:
                drawBookText(g);
                break;
            case 3:
                drawTeamText(g);
                break;
            case 4:
                drawTargetText(g);
                break;
        }
    }

    // ==================== Foreground Rendering ====================

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);
        // Apply 1.25x scale (matching original 1.10.2)
        g.pose().pushPose();
        g.pose().scale(GUI_SCALE, GUI_SCALE, 1.0F);
        super.render(g, (int) (mouseX * GUI_SCALE_INV), (int) (mouseY * GUI_SCALE_INV), partialTick);
        g.pose().popPose();

        // Draw text field for team create/rename
        if (this.textField != null && this.textField.isVisible()) {
            this.textField.render(g, mouseX, mouseY, partialTick);
        }

        this.renderTooltip(g, mouseX, mouseY);

        // Book-specific tooltips
        if (this.guiFunc == 2) {
            int localX = (int) (mouseX * GUI_SCALE_INV) - this.leftPos;
            int localY = (int) (mouseY * GUI_SCALE_INV) - this.topPos;

            // Item icon tooltip
            ItemStack hovered = GuiBook.getHoveredItem(bookChapNum, bookPageNum, localX, localY);
            if (hovered != null && !hovered.isEmpty()) {
                g.renderTooltip(this.font, hovered, mouseX, mouseY);
            }

            // Chapter tab tooltip (tabs at x:243-256, y varies per chapter)
            if (localX >= 243 && localX <= 256) {
                for (int i = 0; i < 7; i++) {
                    int tabY1 = 34 + i * 13;
                    int tabY2 = tabY1 + 12;
                    if (i == 3) {
                        tabY1 = 72;
                        tabY2 = 82;
                    }
                    if (i == 4) {
                        tabY1 = 83;
                        tabY2 = 96;
                    }
                    if (i == 5) {
                        tabY1 = 97;
                        tabY2 = 109;
                    }
                    if (i == 6) {
                        tabY1 = 110;
                        tabY2 = 121;
                    }
                    if (localY >= tabY1 && localY <= tabY2) {
                        String chapTitle = Component.translatable("gui.shincolle.book.chap" + i + ".title").getString();
                        g.renderTooltip(this.font, Component.literal(chapTitle), mouseX, mouseY);
                        break;
                    }
                }
            }
        }
    }

    private void drawRadarIcons(GuiGraphics g) {
        if (this.capa == null)
            return;

        List<Integer> ships = this.capa.getShipList();
        if (ships == null)
            return;

        this.shipList.clear();

        // Origin position
        double ox, oz;
        if (this.type == 0 && this.tile != null) {
            ox = tile.getBlockPos().getX();
            oz = tile.getBlockPos().getZ();
        } else {
            ox = player.getX();
            oz = player.getZ();
        }

        // Scale factor for zoom level
        float radarScale = 1F;
        if (radarZoomLv == 1)
            radarScale = 2F;
        else if (radarZoomLv == 2)
            radarScale = 4F;

        Minecraft mc = Minecraft.getInstance();

        for (int eid : ships) {
            if (eid <= 0 || mc.level == null)
                continue;
            Entity entity = mc.level.getEntity(eid);
            if (entity == null)
                continue;

            RadarShip rs = new RadarShip();
            rs.ship = entity;

            // Name
            if (entity.hasCustomName()) {
                rs.name = Objects.requireNonNull(entity.getCustomName()).getString();
            } else {
                rs.name = entity.getName().getString();
            }

            // Position
            double px = (entity.getX() - ox) * radarScale;
            double pz = (entity.getZ() - oz) * radarScale;
            px = Mth.clamp(px, -64, 64);
            pz = Mth.clamp(pz, -64, 64);

            rs.pixelX = leftPos + 69 + px;
            rs.pixelZ = topPos + 88 + pz;
            rs.posX = Mth.ceil(entity.getX());
            rs.posY = (int) entity.getY();
            rs.posZ = Mth.ceil(entity.getZ());

            if (entity instanceof BasicEntityShip ship) {
                rs.level = ship.getLevel();
                rs.healthPercent = ship.getHealth() / Math.max(1F, ship.getMaxHealth());
            }

            shipList.add(rs);

            // Draw icon (animated dot)
            int absP = Math.max(Math.abs((int) px), Math.abs((int) pz));
            int phase = absP > 48 ? 6 : absP > 32 ? 4 : absP > 16 ? 2 : 0;
            int sIcon = (int) (this.tickGUI * 0.125F + phase) % 8 * 3;

            // Selected ship = red, others = pink
            int idx = shipList.size() - 1;
            if (idx == listNum[LISTCLICK_RADAR] + listClicked[LISTCLICK_RADAR]) {
                g.setColor(1F, 0F, 0F, 1F);
            } else {
                g.setColor(1F, 0.684F, 0.788F, 1F);
            }
            g.blit(TEX_RADAR, (int) (leftPos + 69 + px), (int) (topPos + 88 + pz), sIcon, 192, 3, 3);
            g.setColor(1F, 1F, 1F, 1F);
        }
    }

    // ==================== Radar Drawing ====================

    private void drawRadarText(GuiGraphics g) {
        int texty = 27;

        for (int i = listNum[LISTCLICK_RADAR]; i < shipList.size() && i < listNum[LISTCLICK_RADAR] + 5; i++) {
            RadarShip rs = shipList.get(i);
            if (rs == null)
                continue;

            // Name
            g.drawString(this.font, rs.name, 147, texty, Enums.EnumColors.WHITE.getValue(), false);
            texty += 12;

            // Level + HP
            String str;
            if (rs.ship instanceof BasicEntityShip ship) {
                str = "LV " + ChatFormatting.YELLOW + rs.level + "   " +
                        ChatFormatting.GOLD + (int) ship.getHealth() +
                        ChatFormatting.RED + " / " + (int) ship.getMaxHealth();
            } else {
                str = ChatFormatting.GRAY + "(unknown)";
            }
            g.drawString(this.font, str, 147, texty, Enums.EnumColors.CYAN.getValue(), false);
            texty += 9;

            // Position
            String str2 = strPos + " " + ChatFormatting.YELLOW +
                    rs.posX + " , " + rs.posZ + "  " +
                    ChatFormatting.LIGHT_PURPLE + strHeight + " " +
                    ChatFormatting.YELLOW + rs.posY;
            g.drawString(this.font, str2, 147, texty, Enums.EnumColors.PURPLE_LIGHT.getValue(), false);
            texty += 11;
        }

        // Draw morale icons
        drawMoraleIcons(g);
    }

    private void drawMoraleIcons(GuiGraphics g) {
        int texty = 37;
        for (int i = listNum[LISTCLICK_RADAR]; i < shipList.size() && i < listNum[LISTCLICK_RADAR] + 5; i++) {
            RadarShip rs = shipList.get(i);
            if (rs != null && rs.ship instanceof BasicEntityShip ship) {
                int morale = ship.getMorale();
                int ix; // default
                if (morale >= 85)
                    ix = 0; // excited
                else if (morale >= 50)
                    ix = 11; // happy
                else if (morale >= 20)
                    ix = 22; // normal
                else
                    ix = 33; // tired

                g.blit(TEX_ICON0, 237, texty - 1, ix, 240, 11, 11);
            }
            texty += 32;
        }
    }

    private void drawBookText(GuiGraphics g) {
        // Draw chapter/page indicator
        String str = Component.translatable("gui.shincolle.book.chap" + bookChapNum + ".title").getString();
        g.drawString(this.font, str, 10, 27, Enums.EnumColors.WHITE.getValue(), true);

        // Chapters 4/5 page>0: entity gallery mode
        if ((bookChapNum == 4 || bookChapNum == 5) && bookPageNum > 0) {
            drawEntityGallery(g);
        } else {
            // Draw book content using GuiBook helper
            GuiBook.drawBookContent(g, this.font, this.bookChapNum, this.bookPageNum);
        }
    }

    // ==================== Book Drawing ====================

    /**
     * Set up the gallery entity for the current chapter/page.
     */
    private void updateGalleryEntity() {
        int shipClass = -1;

        if (bookChapNum == 4 && bookPageNum > 0) {
            int idx = bookPageNum - 1;
            List<Integer> list = Values.ShipBookList;
            if (idx < list.size()) {
                shipClass = list.get(idx);
            }
        } else if (bookChapNum == 5 && bookPageNum > 0) {
            int idx = bookPageNum - 1;
            List<Integer> list = Values.EnemyBookList;
            if (idx < list.size()) {
                shipClass = list.get(idx);
            }
        }

        // Only recreate if class changed
        if (shipClass == galleryShipClass && galleryEntity != null) {
            return;
        }

        galleryShipClass = shipClass;
        galleryEntity = null;

        if (shipClass < 0)
            return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null)
            return;

        EntityType<?> type = ShipSpawnEgg.getEntityTypeForClass(shipClass);
        if (type == null)
            return;

        Entity ent = type.create(mc.level);
        if (ent instanceof LivingEntity living) {
            galleryEntity = living;
        }
    }

    /**
     * Draw the entity gallery for chapters 4/5.
     */
    private void drawEntityGallery(GuiGraphics g) {
        updateGalleryEntity();

        // Draw page number
        String pageStr = "No. " + bookPageNum;
        int pageColor = (bookChapNum == 4)
                ? Enums.EnumColors.RED_DARK.getValue()
                : Enums.EnumColors.CYAN.getValue();
        g.drawString(this.font, pageStr, 22, 36, pageColor, true);

        // Draw ship name icon
        drawShipNameIcon(g);

        // Draw right page text (description)
        String key = "gui.shincolle.book.chap" + bookChapNum + ".text" + bookPageNum + "d1";
        String text = Component.translatable(key).getString();
        if (!text.equals(key)) {
            var pose = g.pose();
            pose.pushPose();
            pose.scale(0.8F, 0.8F, 0.8F);
            g.drawWordWrap(this.font, Component.literal(text),
                    GuiBook.PageTRX, GuiBook.PageTY, GuiBook.PageWidth, 0x000000);
            pose.popPose();
        }

        // Render entity model
        if (galleryEntity != null) {
            int centerX = leftPos + 64;
            int centerY = topPos + 140;

            // Use InventoryScreen's entity rendering with manual rotation
            org.joml.Quaternionf rotY = new org.joml.Quaternionf()
                    .rotateY((float) Math.toRadians(mRotateX));
            org.joml.Quaternionf rotX = new org.joml.Quaternionf()
                    .rotateX((float) Math.toRadians(mRotateY));

            InventoryScreen.renderEntityInInventory(g, centerX, centerY,
                    mScale, rotY, rotX, galleryEntity);
        }
    }

    /**
     * Draw ship name icon from the icon sprite sheets.
     */
    private void drawShipNameIcon(GuiGraphics g) {
        if (galleryShipClass < 0)
            return;

        int[] iconData = Values.ShipNameIconMap.get(galleryShipClass);
        if (iconData == null || iconData.length < 3)
            return;

        int fileLineId = iconData[0];
        int iconX = iconData[1];
        int iconY = iconData[2];

        int file = fileLineId / 100;
        ResourceLocation tex = (file == 0) ? TEX_ICON1 : TEX_ICON2;

        // Draw ship name kanji icon (11x39 or 11x59 depending on line)
        int iconHeight = 39;
        int line = fileLineId % 100;
        if (file == 0 && (line <= 3)) {
            iconHeight = 59;
        }
        if (file == 0 && line == 4) {
            iconHeight = 71;
        }

        g.blit(tex, 110, 36, iconX, iconY, 11, iconHeight);

        // Draw ship type icon
        drawShipTypeIcon(g);
    }

    /**
     * Draw ship type icon (DD, CL, CA, etc.).
     */
    private void drawShipTypeIcon(GuiGraphics g) {
        // Look up ship type from class
        Byte shipType = Values.ShipTypeMap.get(galleryShipClass);
        if (shipType == null)
            return;

        int[] typeIcon = Values.ShipTypeIconMap.get(shipType);
        if (typeIcon == null || typeIcon.length < 2)
            return;

        g.blit(TEX_ICON0, 96, 36, typeIcon[0], typeIcon[1], 11, 29);
    }

    private void drawTeamPic(GuiGraphics g) {
        // Team selection highlight
        if (listFocus == LISTCLICK_TEAM && listClicked[LISTCLICK_TEAM] >= 0 && listClicked[LISTCLICK_TEAM] < 5) {
            int cirY = 25 + listClicked[LISTCLICK_TEAM] * 32;
            g.blit(TEX_TEAM, leftPos + 142, topPos + cirY, 0, 192, 108, 31);
        }
        // Ally selection highlight
        else if (listFocus == LISTCLICK_ALLY && listClicked[LISTCLICK_ALLY] >= 0 && listClicked[LISTCLICK_ALLY] < 3) {
            int cirY = 61 + listClicked[LISTCLICK_ALLY] * 31;
            g.blit(TEX_TEAM, leftPos + 6, topPos + cirY, 109, 192, 129, 31);
        }
        // Ban selection highlight
        else if (listFocus == LISTCLICK_BAN && listClicked[LISTCLICK_BAN] >= 0 && listClicked[LISTCLICK_BAN] < 3) {
            int cirY = 61 + listClicked[LISTCLICK_BAN] * 31;
            g.blit(TEX_TEAM, leftPos + 6, topPos + cirY, 109, 192, 129, 31);
        }
    }

    // ==================== Team Drawing ====================

    private void drawTeamText(GuiGraphics g) {
        if (this.capa == null)
            return;

        // Draw own team info
        boolean hasTeam = this.capa.getPlayerUID() > 0 && !this.capa.getTeamName().isEmpty();
        if (hasTeam) {
            g.drawString(this.font, ChatFormatting.GRAY + strTeamID + ":  " +
                            ChatFormatting.YELLOW + this.capa.getPlayerUID(),
                    9, 27, 0, false);
            g.drawString(this.font, ChatFormatting.WHITE + this.capa.getTeamName(),
                    9, 37, 0, false);
        }

        // Draw button text based on team state
        String strLT = null, strLB = null, strRT = null, strRB = null;
        int colorLT = Enums.EnumColors.WHITE.getValue();
        int colorLB = colorLT, colorRT = colorLT, colorRB = colorLT;

        switch (this.teamState) {
            case TEAMSTATE_ALLY:
                if (tempCD > 0) {
                    strLT = String.valueOf((int) (tempCD * 0.05F));
                    colorLT = Enums.EnumColors.GRAY_LIGHT.getValue();
                } else {
                    strLT = strAlly;
                    colorLT = Enums.EnumColors.CYAN.getValue();
                }
                strLB = strOK;
                break;
            case TEAMSTATE_BAN:
                if (tempCD > 0) {
                    strLT = String.valueOf((int) (tempCD * 0.05F));
                    colorLT = Enums.EnumColors.GRAY_LIGHT.getValue();
                } else {
                    strLT = strBan;
                    colorLT = Enums.EnumColors.YELLOW.getValue();
                }
                strLB = strOK;
                break;
            case TEAMSTATE_CREATE:
                g.drawString(this.font, ChatFormatting.WHITE + strTeamID + "  " +
                                ChatFormatting.YELLOW + this.capa.getPlayerUID(),
                        10, 43, 0, false);
                strLB = strOK;
                strLT = strCancel;
                colorLT = Enums.EnumColors.GRAY_LIGHT.getValue();
                break;
            case TEAMSTATE_RENAME:
                strLB = strOK;
                strLT = strCancel;
                colorLT = Enums.EnumColors.GRAY_LIGHT.getValue();
                break;
            default: // TEAMSTATE_MAIN
                if (hasTeam) {
                    strLT = strAllyList;
                    colorLT = Enums.EnumColors.CYAN.getValue();
                    strLB = strBanList;
                    colorLB = Enums.EnumColors.YELLOW.getValue();
                    strRT = strRename;

                    if (capa.getTeamCooldown() > 0) {
                        strRB = String.valueOf(capa.getTeamCooldown() / 20);
                        colorRB = Enums.EnumColors.GRAY_LIGHT.getValue();
                    } else {
                        strRB = strDisband;
                        colorRB = Enums.EnumColors.GRAY_DARK.getValue();
                    }
                } else {
                    if (capa.getTeamCooldown() > 0) {
                        strRB = String.valueOf(capa.getTeamCooldown() / 20);
                        colorRB = Enums.EnumColors.GRAY_LIGHT.getValue();
                    } else {
                        strRB = strCreate;
                        colorRB = Enums.EnumColors.CYAN.getValue();
                    }
                }
                break;
        }

        // Draw 4 corner buttons
        if (strLT != null) {
            int w = this.font.width(strLT) / 2;
            g.drawString(this.font, strLT, 31 - w, 160, colorLT, false);
        }
        if (strLB != null) {
            int w = this.font.width(strLB) / 2;
            g.drawString(this.font, strLB, 31 - w, 174, colorLB, false);
        }
        if (strRT != null) {
            int w = this.font.width(strRT) / 2;
            g.drawString(this.font, strRT, 110 - w, 160, colorRT, false);
        }
        if (strRB != null) {
            int w = this.font.width(strRB) / 2;
            g.drawString(this.font, strRB, 110 - w, 174, colorRB, false);
        }

        // Draw ally/ban list info (left side in ALLY/BAN state)
        if (hasTeam && (teamState == TEAMSTATE_ALLY || teamState == TEAMSTATE_BAN)) {
            List<Integer> subList = teamState == TEAMSTATE_ALLY ? capa.getAllyList() : capa.getBanList();
            int listID = teamState == TEAMSTATE_ALLY ? LISTCLICK_ALLY : LISTCLICK_BAN;

            if (subList != null) {
                int ty = 65;
                for (int i = listNum[listID]; i < subList.size() && i < listNum[listID] + 3; i++) {
                    int tid = subList.get(i);
                    g.drawString(this.font, ChatFormatting.YELLOW + "" + tid, 10, ty, 0, false);
                    ty += 31;
                }
            }

            // Draw team relation list (right side)
            List<Integer> knownTeams = getKnownTeamIdsForDisplay();
            int selectedAbsIndex = listNum[LISTCLICK_TEAM] + listClicked[LISTCLICK_TEAM];
            int ty = 27;
            for (int i = listNum[LISTCLICK_TEAM]; i < knownTeams.size() && i < listNum[LISTCLICK_TEAM] + 5; i++) {
                int tid = knownTeams.get(i);
                boolean selected = (i == selectedAbsIndex && listFocus == LISTCLICK_TEAM);

                String idLabel = "ID " + tid;
                int idColor = selected ? Enums.EnumColors.YELLOW.getValue() : Enums.EnumColors.WHITE.getValue();
                g.drawString(this.font, idLabel, 145, ty, idColor, false);

                String relLabel = getTeamRelationLabel(tid);
                int relColor = getTeamRelationColor(tid);
                g.drawString(this.font, relLabel, 145, ty + 10, relColor, false);

                ty += 32;
            }
        }
    }

    private void drawTargetText(GuiGraphics g) {
        // Draw "Remove" button text
        int w = this.font.width(strRemove) / 2;
        g.drawString(this.font, strRemove, 31 - w, 160, Enums.EnumColors.WHITE.getValue(), false);

        // Draw target class list
        int texty = 27;
        for (int i = listNum[LISTCLICK_TARGET]; i < tarList.size() && i < listNum[LISTCLICK_TARGET] + 13; i++) {
            String className = tarList.get(i);
            if (className != null) {
                int color = (i == listNum[LISTCLICK_TARGET] + listClicked[LISTCLICK_TARGET])
                        ? Enums.EnumColors.YELLOW.getValue()
                        : Enums.EnumColors.WHITE.getValue();
                g.drawString(this.font, className, 145, texty, color, false);
            }
            texty += 12;
        }
    }

    // ==================== Target Drawing ====================

    private void updateTargetClassList() {
        this.tarList.clear();
        if (this.capa != null) {
            List<Integer> targetIds = capa.getTargetClassList();
            if (targetIds != null) {
                for (int id : targetIds) {
                    tarList.add("TargetClass_" + id);
                }
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Text field focus
        if (this.textField != null && this.textField.isVisible()) {
            this.textField.mouseClicked(mouseX, mouseY, button);
        }

        // Scale mouse to unscaled GUI coordinates (original uses GuiScaleInv)
        int posX = (int) (mouseX * GUI_SCALE_INV);
        int posY = (int) (mouseY * GUI_SCALE_INV);
        int xClick = posX - this.leftPos;
        int yClick = posY - this.topPos;

        // Function button clicks (desk block only)
        if (this.type == 0) {
            int getFunc = GuiHelper.getButton(ID.Gui.ADMIRALDESK, 0, xClick, yClick);
            if (getFunc >= 0) {
                setDeskFunction(getFunc);
                return true;
            }
        }

        // Tab-specific button clicks
        switch (this.guiFunc) {
            case 1: // Radar
                int radarBtn = GuiHelper.getButton(ID.Gui.ADMIRALDESK, 1, xClick, yClick);
                handleRadarClick(radarBtn);
                break;
            case 2: // Book
                int bookBtn = GuiHelper.getButton(ID.Gui.ADMIRALDESK, 2, xClick, yClick);
                handleBookClick(bookBtn, button);
                break;
            case 3: // Team
                int teamBtn = GuiHelper.getButton(ID.Gui.ADMIRALDESK, 3, xClick, yClick);
                handleTeamClick(teamBtn);
                break;
            case 4: // Target
                int targetBtn = GuiHelper.getButton(ID.Gui.ADMIRALDESK, 4, xClick, yClick);
                handleTargetClick(targetBtn);
                break;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    // ==================== Mouse Input ====================

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        handleWheelMove(delta > 0);
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.textField != null && this.textField.isVisible() && this.textField.isFocused()) {
            this.textField.keyPressed(keyCode, scanCode, modifiers);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (this.textField != null && this.textField.isVisible() && this.textField.isFocused()) {
            return this.textField.charTyped(codePoint, modifiers);
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        // Entity gallery rotation via mouse drag
        if (this.guiFunc == 2 && (bookChapNum == 4 || bookChapNum == 5) && bookPageNum > 0) {
            int localX = (int) (mouseX * GUI_SCALE_INV) - this.leftPos;
            int localY = (int) (mouseY * GUI_SCALE_INV) - this.topPos;
            // Only drag within the model area (18,45 to 110,157)
            if (localX >= 18 && localX <= 110 && localY >= 45 && localY <= 157) {
                mRotateX += (float) dragX * 1.5F;
                mRotateY += (float) dragY * 1.5F;
                mRotateY = Mth.clamp(mRotateY, -60F, 60F);
                return true;
            }
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    private void setDeskFunction(int button) {
        if (button >= 0) {
            int newFunc = button + 1;
            this.guiFunc = (this.guiFunc != newFunc) ? newFunc : 0;
            syncTileEntityC2S();
        }
    }

    // ==================== Click Handlers ====================

    private void syncTileEntityC2S() {
        if (this.type == 0 && this.tile != null) {
            tile.setGuiFunc(this.guiFunc);
            tile.setBookChap(this.bookChapNum);
            tile.setBookPage(this.bookPageNum);
            tile.setRadarZoomLv(this.radarZoomLv);
        }
    }

    private void handleRadarClick(int btn) {
        switch (btn) {
            case 0: // Zoom
                radarZoomLv = (radarZoomLv + 1) % 3;
                break;
            case 1:
            case 2:
            case 3:
            case 4:
            case 5: // Ship slots 0-4
                int oldClick = listClicked[LISTCLICK_RADAR];
                listClicked[LISTCLICK_RADAR] = btn - 1;
                // Double click = open ship GUI
                if (oldClick == listClicked[LISTCLICK_RADAR]) {
                    openShipGUI();
                }
                break;
        }
    }

    private void handleBookClick(int btn, int mouseBtn) {
        switch (btn) {
            case 0: // Left page
                bookPageNum -= (mouseBtn == 0 ? 1 : 10);
                if (bookPageNum < 0)
                    bookPageNum = 0;
                break;
            case 1: // Right page
                bookPageNum += (mouseBtn == 0 ? 1 : 10);
                int maxPage = GuiBook.getMaxPageNumber(bookChapNum);
                if (bookPageNum > maxPage)
                    bookPageNum = maxPage;
                break;
            case 2:
            case 3:
            case 4:
            case 5:
            case 6:
            case 7:
            case 8: // Chapters 0-6
                bookChapNum = btn - 2;
                bookPageNum = 0;
                GuiBook.resetPageCycling();
                break;
        }
    }

    private void handleTeamClick(int btn) {
        switch (this.teamState) {
            case TEAMSTATE_MAIN:
                handleClickTeamMain(btn);
                break;
            case TEAMSTATE_ALLY:
                handleClickTeamAlly(btn);
                break;
            case TEAMSTATE_CREATE:
                handleClickTeamCreate(btn);
                break;
            case TEAMSTATE_RENAME:
                handleClickTeamRename(btn);
                break;
            case TEAMSTATE_BAN:
                handleClickTeamBan(btn);
                break;
        }
    }

    private void handleClickTeamMain(int btn) {
        boolean hasTeam = capa != null && capa.getPlayerUID() > 0 && !capa.getTeamName().isEmpty();

        switch (btn) {
            case 0: // Left top: Ally List
                if (hasTeam) {
                    this.teamState = TEAMSTATE_ALLY;
                    this.listFocus = LISTCLICK_TEAM;
                    showTextField("");
                }
                break;
            case 6: // Left bottom: Ban List
                if (hasTeam) {
                    this.teamState = TEAMSTATE_BAN;
                    this.listFocus = LISTCLICK_TEAM;
                    showTextField("");
                }
                break;
            case 7: // Right top: Rename
                if (hasTeam && tempCD <= 0) {
                    this.teamState = TEAMSTATE_RENAME;
                    showTextField(capa.getTeamName());
                }
                break;
            case 8: // Right bottom: Disband / Create
                if (hasTeam) {
                    if (capa.getTeamCooldown() <= 0) {
                        ModNetworking.sendToServer(new C2SGUIInputPacket(
                                C2SGUIInputPacket.Desk_Disband, new int[0]));
                        this.tempCD = CLICKCD;
                    }
                } else {
                    if (capa != null && capa.getTeamCooldown() <= 0) {
                        this.teamState = TEAMSTATE_CREATE;
                        showTextField("");
                    }
                }
                break;
        }
    }

    private void handleClickTeamAlly(int btn) {
        switch (btn) {
            case 0: // Left top: Ally/Break toggle based on selected team
                handleToggleAllyAction();
                break;
            case 6: // Left bottom: OK (back to main)
                this.teamState = TEAMSTATE_MAIN;
                hideTextField();
                break;
            case 1:
            case 2:
            case 3:
            case 4:
            case 5: // Team list slots
                listFocus = LISTCLICK_TEAM;
                listClicked[LISTCLICK_TEAM] = btn - 1;
                break;
            case 9:
            case 10:
            case 11: // Ally list slots
                listFocus = LISTCLICK_ALLY;
                listClicked[LISTCLICK_ALLY] = btn - 9;
                break;
        }
    }

    private void handleClickTeamCreate(int btn) {
        switch (btn) {
            case 0: // Cancel
                this.teamState = TEAMSTATE_MAIN;
                hideTextField();
                break;
            case 6: // OK - create team
                String name = textField.getValue();
                if (name.length() > 1) {
                    ModNetworking.sendToServer(new C2SGUIInputPacket(
                            C2SGUIInputPacket.Desk_Create, new int[0], name));
                    this.tempCD = CLICKCD;
                }
                this.teamState = TEAMSTATE_MAIN;
                hideTextField();
                break;
        }
    }

    private void handleClickTeamRename(int btn) {
        switch (btn) {
            case 0: // Cancel
                this.teamState = TEAMSTATE_MAIN;
                hideTextField();
                break;
            case 6: // OK - rename team
                String name = textField.getValue();
                if (name.length() > 1) {
                    ModNetworking.sendToServer(new C2SGUIInputPacket(
                            C2SGUIInputPacket.Desk_Rename, new int[0], name));
                    this.tempCD = CLICKCD;
                }
                this.teamState = TEAMSTATE_MAIN;
                hideTextField();
                break;
        }
    }

    private void handleClickTeamBan(int btn) {
        switch (btn) {
            case 0: // Left top: Ban/Unban toggle based on selected team
                handleToggleBanAction();
                break;
            case 6: // Left bottom: OK (back to main)
                this.teamState = TEAMSTATE_MAIN;
                hideTextField();
                break;
            case 1:
            case 2:
            case 3:
            case 4:
            case 5: // Team list slots
                listFocus = LISTCLICK_TEAM;
                listClicked[LISTCLICK_TEAM] = btn - 1;
                break;
            case 9:
            case 10:
            case 11: // Ban list slots
                listFocus = LISTCLICK_BAN;
                listClicked[LISTCLICK_BAN] = btn - 9;
                break;
        }
    }

    private void handleToggleAllyAction() {
        if (tempCD > 0 || capa == null || capa.getPlayerUID() <= 0) {
            return;
        }

        int targetTid = getSelectedTeamIdForRelationAction(TEAMSTATE_ALLY);
        if (targetTid > 0 && targetTid != capa.getPlayerUID()) {
            boolean alreadyAlly = capa.getAllyList() != null && capa.getAllyList().contains(targetTid);
            byte packetType = alreadyAlly ? C2SGUIInputPacket.Desk_Break : C2SGUIInputPacket.Desk_Ally;
            ModNetworking.sendToServer(new C2SGUIInputPacket(packetType, new int[]{targetTid}));
            this.tempCD = CLICKCD;
            return;
        }

        String leaderName = textField != null ? textField.getValue() : null;
        if (leaderName != null && !leaderName.isEmpty()) {
            byte packetType = (listFocus == LISTCLICK_ALLY) ? C2SGUIInputPacket.Desk_Break
                    : C2SGUIInputPacket.Desk_Ally;
            ModNetworking.sendToServer(new C2SGUIInputPacket(packetType, new int[0], leaderName));
            this.tempCD = CLICKCD;
        }
    }

    private void handleToggleBanAction() {
        if (tempCD > 0 || capa == null || capa.getPlayerUID() <= 0) {
            return;
        }

        int targetTid = getSelectedTeamIdForRelationAction(TEAMSTATE_BAN);
        if (targetTid > 0 && targetTid != capa.getPlayerUID()) {
            boolean alreadyBanned = capa.getBanList() != null && capa.getBanList().contains(targetTid);
            byte packetType = alreadyBanned ? C2SGUIInputPacket.Desk_Unban : C2SGUIInputPacket.Desk_Ban;
            ModNetworking.sendToServer(new C2SGUIInputPacket(packetType, new int[]{targetTid}));
            this.tempCD = CLICKCD;
            return;
        }

        String leaderName = textField != null ? textField.getValue() : null;
        if (leaderName != null && !leaderName.isEmpty()) {
            byte packetType = (listFocus == LISTCLICK_BAN) ? C2SGUIInputPacket.Desk_Unban : C2SGUIInputPacket.Desk_Ban;
            ModNetworking.sendToServer(new C2SGUIInputPacket(packetType, new int[0], leaderName));
            this.tempCD = CLICKCD;
        }
    }

    private int getSelectedTeamIdForRelationAction(int relationState) {
        if (capa == null) {
            return 0;
        }

        if (listFocus == LISTCLICK_TEAM) {
            int idx = listClicked[LISTCLICK_TEAM] + listNum[LISTCLICK_TEAM];
            List<Integer> knownTeams = getKnownTeamIdsForDisplay();
            if (idx >= 0 && idx < knownTeams.size()) {
                return knownTeams.get(idx);
            }
        }

        if (relationState == TEAMSTATE_ALLY && listFocus == LISTCLICK_ALLY) {
            int idx = listClicked[LISTCLICK_ALLY] + listNum[LISTCLICK_ALLY];
            List<Integer> allyList = capa.getAllyList();
            if (allyList != null && idx >= 0 && idx < allyList.size()) {
                return allyList.get(idx);
            }
        }

        if (relationState == TEAMSTATE_BAN && listFocus == LISTCLICK_BAN) {
            int idx = listClicked[LISTCLICK_BAN] + listNum[LISTCLICK_BAN];
            List<Integer> banList = capa.getBanList();
            if (banList != null && idx >= 0 && idx < banList.size()) {
                return banList.get(idx);
            }
        }

        return 0;
    }

    private List<Integer> getKnownTeamIdsForDisplay() {
        if (capa == null) {
            return Collections.emptyList();
        }

        List<Integer> known = new ArrayList<>();
        List<Integer> syncedKnown = capa.getKnownTeamIds();
        if (syncedKnown != null) {
            for (Integer tid : syncedKnown) {
                if (tid != null && tid > 0 && !known.contains(tid)) {
                    known.add(tid);
                }
            }
        }

        if (known.isEmpty()) {
            int myTid = capa.getPlayerUID();
            if (myTid > 0) {
                known.add(myTid);
            }

            List<Integer> allyList = capa.getAllyList();
            if (allyList != null) {
                for (Integer tid : allyList) {
                    if (tid != null && tid > 0 && !known.contains(tid)) {
                        known.add(tid);
                    }
                }
            }

            List<Integer> banList = capa.getBanList();
            if (banList != null) {
                for (Integer tid : banList) {
                    if (tid != null && tid > 0 && !known.contains(tid)) {
                        known.add(tid);
                    }
                }
            }
        }

        Collections.sort(known);
        return known;
    }

    private String getTeamRelationLabel(int tid) {
        if (capa == null || tid <= 0) {
            return strNeutral;
        }

        if (tid == capa.getPlayerUID()) {
            return strBelong;
        }

        List<Integer> banList = capa.getBanList();
        if (banList != null && banList.contains(tid)) {
            return strHostile;
        }

        List<Integer> allyList = capa.getAllyList();
        if (allyList != null && allyList.contains(tid)) {
            return strAllied;
        }

        return strNeutral;
    }

    private int getTeamRelationColor(int tid) {
        if (capa == null || tid <= 0) {
            return Enums.EnumColors.GRAY_LIGHT.getValue();
        }

        if (tid == capa.getPlayerUID()) {
            return Enums.EnumColors.WHITE.getValue();
        }

        List<Integer> banList = capa.getBanList();
        if (banList != null && banList.contains(tid)) {
            return Enums.EnumColors.YELLOW.getValue();
        }

        List<Integer> allyList = capa.getAllyList();
        if (allyList != null && allyList.contains(tid)) {
            return Enums.EnumColors.CYAN.getValue();
        }

        return Enums.EnumColors.GRAY_LIGHT.getValue();
    }

    private void handleTargetClick(int btn) {
        if (btn == 0) { // Remove target
            int clicked = listClicked[LISTCLICK_TARGET] + listNum[LISTCLICK_TARGET];
            if (clicked >= 0 && clicked < tarList.size()) {
                String tarStr = tarList.get(clicked);
                ModNetworking.sendToServer(new C2SGUIInputPacket(
                        C2SGUIInputPacket.SetTarClass, new int[0], tarStr));
                tarList.remove(clicked);
                listClicked[LISTCLICK_TARGET] = -1;
            }
        } else {
            if (btn >= 1 && btn <= 13) {
                listClicked[LISTCLICK_TARGET] = btn - 1;
            }
        }
    }

    private void handleWheelMove(boolean isWheelUp) {
        int listSize = 0;
        int maxVisible = 5;
        int listId = LISTCLICK_RADAR;

        switch (this.guiFunc) {
            case 1: // Radar
                listSize = shipList.size();
                break;
            case 2: // Book (entity gallery zoom)
                if ((bookChapNum == 4 || bookChapNum == 5) && bookPageNum > 0) {
                    if (isWheelUp) {
                        mScale += 2;
                        if (mScale > 200)
                            mScale = 200;
                    } else {
                        mScale -= 2;
                        if (mScale < 5)
                            mScale = 5;
                    }
                }
                return;
            case 3: // Team
                if (listFocus == LISTCLICK_TEAM) {
                    listSize = getKnownTeamIdsForDisplay().size();
                    listId = LISTCLICK_TEAM;
                } else if (listFocus == LISTCLICK_ALLY) {
                    listSize = capa != null ? capa.getAllyList().size() : 0;
                    maxVisible = 3;
                    listId = LISTCLICK_ALLY;
                } else if (listFocus == LISTCLICK_BAN) {
                    listSize = capa != null ? capa.getBanList().size() : 0;
                    maxVisible = 3;
                    listId = LISTCLICK_BAN;
                }
                break;
            case 4: // Target
                listSize = tarList.size();
                maxVisible = 13;
                listId = LISTCLICK_TARGET;
                break;
            default:
                return;
        }

        if (isWheelUp) {
            listNum[listId]--;
            if (listNum[listId] < 0)
                listNum[listId] = 0;
        } else {
            listNum[listId]++;
            int maxScroll = Math.max(0, listSize - maxVisible);
            if (listNum[listId] > maxScroll)
                listNum[listId] = maxScroll;
        }
    }

    // ==================== Scroll Handler ====================

    private void openShipGUI() {
        int idx = listNum[LISTCLICK_RADAR] + listClicked[LISTCLICK_RADAR];
        if (idx >= 0 && idx < shipList.size()) {
            RadarShip rs = shipList.get(idx);
            if (rs != null && rs.ship != null) {
                ModNetworking.sendToServer(new C2SGUIInputPacket(
                        C2SGUIInputPacket.OpenShipGUI,
                        new int[]{player.getId(), 0, rs.ship.getId()}));
            }
        }
    }

    // ==================== Ship GUI ====================

    private void showTextField(String text) {
        if (this.textField != null) {
            this.textField.setVisible(true);
            this.textField.setFocused(true);
            this.textField.setValue(text != null ? text : "");
        }
    }

    // ==================== Text Field Helpers ====================

    private void hideTextField() {
        if (this.textField != null) {
            this.textField.setVisible(false);
            this.textField.setFocused(false);
        }
    }

    /**
     * Radar entry: ship entity + display data
     */
    private static class RadarShip {
        Entity ship;
        String name;
        double pixelX, pixelZ;
        float healthPercent;
        int level;
        int posX, posY, posZ;
    }
}
