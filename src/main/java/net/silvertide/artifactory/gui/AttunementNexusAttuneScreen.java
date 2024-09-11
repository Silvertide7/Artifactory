package net.silvertide.artifactory.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.silvertide.artifactory.Artifactory;
import net.silvertide.artifactory.client.utils.ClientAttunementNexusSlotInformation;
import net.silvertide.artifactory.storage.AttunementNexusSlotInformation;
import net.silvertide.artifactory.util.GUIUtil;
import org.apache.commons.compress.utils.Lists;

import java.util.List;

public class AttunementNexusAttuneScreen extends AbstractContainerScreen<AttunementNexusAttuneMenu> {
    private static float TEXT_SCALE = 0.85F;
    private static int BUTTON_TEXT_COLOR = 0xFFFFFF;
    private static final int ATTUNE_BUTTON_X = 61;
    private static final int ATTUNE_BUTTON_Y = 65;
    private static final int ATTUNE_BUTTON_WIDTH = 54;
    private static final int ATTUNE_BUTTON_HEIGHT = 12;
    private static final int MANAGE_BUTTON_X = 141;
    private static final int MANAGE_BUTTON_Y = 8;
    private static final int MANAGE_BUTTON_WIDTH = 12;
    private static final int MANAGE_BUTTON_HEIGHT = 12;
    private boolean attuneButtonDown = false;
    private boolean manageButtonDown = false;
    private static final ResourceLocation TEXTURE = new ResourceLocation(Artifactory.MOD_ID, "textures/gui/gui_attunement_nexus_attune.png");

    public AttunementNexusAttuneScreen(AttunementNexusAttuneMenu pMenu, Inventory playerInventory, Component pTitle) {
        super(pMenu, playerInventory, pTitle);
    }

    @Override
    protected void init() {
        super.init();
        // Move the label to get rid of it
        this.titleLabelX = 10000;
        this.titleLabelY = 10000;
        this.inventoryLabelY = 10000;
        this.inventoryLabelX = 10000;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionColorTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, TEXTURE);

        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;


        guiGraphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);

        renderTitle(guiGraphics, x, y);
        renderButtons(guiGraphics, mouseX, mouseY);
        renderTooltips(guiGraphics, mouseX, mouseY);
        renderProgressGraphic(guiGraphics, x, y);
        renderAttunementInformation(guiGraphics, x, y);
    }

    private void renderTitle(GuiGraphics guiGraphics, int x, int y) {
        Component buttonTextComp = Component.literal("Attune Gear");
        guiGraphics.drawWordWrap(this.font, buttonTextComp, x - this.font.width(buttonTextComp)/2 + this.imageWidth / 2, y - this.font.lineHeight/2 + 13, 100, BUTTON_TEXT_COLOR);
    }

    private void renderButtons(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        renderAttuneButton(guiGraphics, mouseX, mouseY);
        renderManageButton(guiGraphics, mouseX, mouseY);
    }

    private void renderAttuneButton(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int buttonX = leftPos + ATTUNE_BUTTON_X;
        int buttonY = topPos + ATTUNE_BUTTON_Y;

        int buttonOffset = getAttuneButtonOffsetToRender(mouseX, mouseY);
        guiGraphics.blit(TEXTURE, buttonX, buttonY, 177, buttonOffset, ATTUNE_BUTTON_WIDTH, ATTUNE_BUTTON_HEIGHT);

        Component buttonTextComp = getAttuneButtonText();
        int buttonTextX = buttonX + ATTUNE_BUTTON_WIDTH / 2;
        int buttonTextY = buttonY + ATTUNE_BUTTON_HEIGHT / 2;

        GUIUtil.drawScaledCenteredWordWrap(guiGraphics, TEXT_SCALE, this.font, buttonTextComp, buttonTextX, buttonTextY, ATTUNE_BUTTON_WIDTH, BUTTON_TEXT_COLOR);
    }

    private void renderManageButton(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int buttonX = leftPos + MANAGE_BUTTON_X;
        int buttonY = topPos + MANAGE_BUTTON_Y;

        int buttonOffset = getManageButtonOffsetToRender(mouseX, mouseY);
        guiGraphics.blit(TEXTURE, buttonX, buttonY, 177, buttonOffset, MANAGE_BUTTON_WIDTH, MANAGE_BUTTON_HEIGHT);
    }

    private Component getAttuneButtonText() {
        if(menu.getProgress() > 0) {
            return Component.translatable("screen.button.artifactory.attune.attune_in_progress");
        } else if (menu.canItemAscend()) {
            return Component.translatable("screen.button.artifactory.attune.attune_not_in_progress");
        } else if (menu.hasAttuneableItemInSlot() && !menu.canItemAscend()) {
            return Component.translatable("screen.button.artifactory.attune.max_attunement_reached");
        } else {
            return Component.literal("");
        }
    }

    private void renderTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        renderCostTooltip(guiGraphics, mouseX, mouseY);
        renderManageTooltip(guiGraphics, mouseX, mouseY);
    }

    private void renderManageTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if(isHoveringManageButton(mouseX, mouseY)) {
            List<Component> list = Lists.newArrayList();
            list.add(Component.literal("Manage Attunements"));
            guiGraphics.renderComponentTooltip(this.font, list, mouseX, mouseY);
        }
    }

    private void renderCostTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if(isHoveringAttuneButton(mouseX, mouseY)) {
            List<Component> list = Lists.newArrayList();
            if(menu.hasAttuneableItemInSlot() && menu.canItemAscend() && ClientAttunementNexusSlotInformation.getSlotInformation() != null) {
                AttunementNexusSlotInformation slotInformation = ClientAttunementNexusSlotInformation.getSlotInformation();
                list.add(Component.translatable("screen.tooltip.artifactory.xp_level_threshold", slotInformation.xpThreshold()));
                list.add(Component.translatable("screen.tooltip.artifactory.xp_levels_consumed", slotInformation.xpConsumed()));
            } else if (menu.hasAttuneableItemInSlot() && !menu.canItemAscend()) {
                list.add(Component.translatable("screen.tooltip.artifactory.item_in_slot_is_max_level"));
            } else {
                list.add(Component.translatable("screen.tooltip.artifactory.no_item_in_slot"));
            }
            guiGraphics.renderComponentTooltip(this.font, list, mouseX, mouseY);
        }
    }

    private void renderAttunementInformation(GuiGraphics guiGraphics, int x, int y) {
        AttunementNexusSlotInformation slotInformation = ClientAttunementNexusSlotInformation.getSlotInformation();
        if(slotInformation != null && this.menu.hasAttuneableItemInSlot()){
            Component attunementLevelComponent = Component.literal(String.valueOf(slotInformation.levelAchieved()));
            guiGraphics.drawWordWrap(this.font, attunementLevelComponent, x - this.font.width(attunementLevelComponent)/2 + this.imageWidth / 8, y - this.font.lineHeight/2 + 40, 100, BUTTON_TEXT_COLOR);

            if(menu.canItemAscend()) {
                if (slotInformation.xpConsumed() > 0) {
                    Component levelCostComponent = Component.literal(String.valueOf(slotInformation.xpConsumed()));
                    guiGraphics.drawWordWrap(this.font, levelCostComponent, x - this.font.width(levelCostComponent) / 2 + this.imageWidth / 8, y - this.font.lineHeight / 2 + 50, 100, BUTTON_TEXT_COLOR);
                }

                if (slotInformation.xpThreshold() > 0) {
                    Component levelThresholdComponent = Component.literal(String.valueOf(slotInformation.xpThreshold()));
                    guiGraphics.drawWordWrap(this.font, levelThresholdComponent, x - this.font.width(levelThresholdComponent) / 2 + this.imageWidth / 8, y - this.font.lineHeight / 2 + 60, 100, BUTTON_TEXT_COLOR);
                }
            }
        }
    }

    private void renderProgressGraphic(GuiGraphics guiGraphics, int x, int y) {
        if(menu.getProgress() > 0) {
            guiGraphics.blit(TEXTURE, x + 79, y + 22, 177, 104, 18, menu.getScaledProgress() / 2);
            guiGraphics.blit(TEXTURE, x + 97, y + 40, 195, 122, -18, -1 * menu.getScaledProgress() / 2);
        }
    }

    // HELPERS
    private int getAttuneButtonOffsetToRender(int mouseX, int mouseY) {
        if(!menu.canItemAscend()) {
            return 39;
        }
        else if(attuneButtonDown) {
            return 26;
        }
        else if (isHoveringAttuneButton(mouseX, mouseY)) {
            return 13;
        }
        else {
            return 0;
        }
    }

    private int getManageButtonOffsetToRender(int mouseX, int mouseY) {
        if(!menu.playerHasAttunedItem()) {
            return 91;
        }
        else if(manageButtonDown) {
            return 78;
        }
        else if (isHoveringManageButton(mouseX, mouseY)) {
            return 65;
        }
        else {
            return 52;
        }
    }

    private boolean isHoveringAttuneButton(double mouseX, double mouseY) {
        return isHovering(ATTUNE_BUTTON_X, ATTUNE_BUTTON_Y, ATTUNE_BUTTON_WIDTH, ATTUNE_BUTTON_HEIGHT, mouseX, mouseY);
    }

    private boolean isHoveringManageButton(double mouseX, double mouseY) {
        return isHovering(MANAGE_BUTTON_X, MANAGE_BUTTON_Y, MANAGE_BUTTON_WIDTH, MANAGE_BUTTON_HEIGHT, mouseX, mouseY);
    }

    private void handleAttuneButtonPress() {
        if(this.minecraft != null && this.minecraft.gameMode != null && menu.canItemAscend()) {
            this.minecraft.gameMode.handleInventoryButtonClick((this.menu).containerId, 1);
        }
    }

    private void handleManageButtonPress() {
        if(this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.pushGuiLayer(new AttunementNexusManageScreen(this));
        }
    }

    public int getImageWidth() {
        return this.imageWidth;
    }

    public int getImageHeight() {
        return this.imageHeight;
    }

    public int getScreenLeftPos() {
        return this.leftPos;
    }

    public int getScreenTopPos() {
        return this.topPos;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if(attuneButtonDown && isHoveringAttuneButton(mouseX, mouseY)) {
            handleAttuneButtonPress();
        } else if(manageButtonDown && isHoveringManageButton(mouseX, mouseY)) {
            handleManageButtonPress();
        }
        manageButtonDown = false;
        attuneButtonDown = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if(isHoveringAttuneButton(mouseX, mouseY)) {
            attuneButtonDown = true;
            return true;
        } else if (isHoveringManageButton(mouseX, mouseY) && menu.playerHasAttunedItem()) {
            manageButtonDown = true;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
