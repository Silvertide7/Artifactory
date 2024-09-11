package net.silvertide.artifactory.gui;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.silvertide.artifactory.client.utils.ClientAttunementNexusSlotInformation;
import net.silvertide.artifactory.config.Config;
import net.silvertide.artifactory.events.custom.PostAttuneEvent;
import net.silvertide.artifactory.events.custom.PreAttuneEvent;
import net.silvertide.artifactory.registry.BlockRegistry;
import net.silvertide.artifactory.registry.MenuRegistry;
import net.silvertide.artifactory.storage.ArtifactorySavedData;
import net.silvertide.artifactory.util.*;
import org.jetbrains.annotations.NotNull;

public class AttunementNexusAttuneMenu extends AbstractContainerMenu {
    public final int MAX_PROGRESS = 20;
    private final ContainerLevelAccess access;
    private final Player player;
    private final Slot attunementSlot;

    // Data Fields
    protected final ContainerData data;
    private int progress = 0;
    private int isActive = 0;
    private int canItemAscend = 0;
    private int playerHasAttunedItem = 0;

    // Data Accessor Indices
    private final int PROGRESS_INDEX = 0;
    private final int IS_ACTIVE_INDEX = 1;
    private final int CAN_ITEM_ASCEND_INDEX = 2;
    private final int PLAYER_HAS_ATTUNED_ITEM_INDEX = 3;

    protected final Container inputSlot = new SimpleContainer(1);

    public AttunementNexusAttuneMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory, ContainerLevelAccess.NULL);
    }

    public AttunementNexusAttuneMenu(int containerId, Inventory playerInventory, ContainerLevelAccess access) {
        super(MenuRegistry.ATTUNEMENT_NEXUS_ATTUNE_MENU.get(), containerId);
        this.access = access;
        this.player = playerInventory.player;

        checkContainerSize(playerInventory, 1);

        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);

        this.data = new ContainerData() {
            @Override
            public int get(int index) {
                return switch(index) {
                    case PROGRESS_INDEX -> AttunementNexusAttuneMenu.this.progress;
                    case IS_ACTIVE_INDEX -> AttunementNexusAttuneMenu.this.isActive;
                    case CAN_ITEM_ASCEND_INDEX -> AttunementNexusAttuneMenu.this.canItemAscend;
                    case PLAYER_HAS_ATTUNED_ITEM_INDEX -> AttunementNexusAttuneMenu.this.playerHasAttunedItem;
                    default -> PROGRESS_INDEX;
                };
            }

            @Override
            public void set(int index, int value) {
                switch(index) {
                    case PROGRESS_INDEX -> AttunementNexusAttuneMenu.this.progress = value;
                    case IS_ACTIVE_INDEX -> AttunementNexusAttuneMenu.this.isActive = value;
                    case CAN_ITEM_ASCEND_INDEX -> AttunementNexusAttuneMenu.this.canItemAscend = value;
                    case PLAYER_HAS_ATTUNED_ITEM_INDEX -> AttunementNexusAttuneMenu.this.playerHasAttunedItem = value;
                }
            }

            @Override
            public int getCount() {
                return 4;
            }
        };

        this.addDataSlots(this.data);

        attunementSlot = new Slot(inputSlot, 0, 80, 23) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return AttunementUtil.isValidAttunementItem(stack);
            }

            @Override
            public void onTake(Player player, ItemStack stack) {
                initializeDataSlots();
                ClientAttunementNexusSlotInformation.clearSlotInformation();
                super.onTake(player, stack);
            }

            @Override
            public void set(ItemStack pStack) {
                super.set(pStack);
                if(pStack != null) AttunementNexusAttuneMenu.this.inputSlotChanged(this);
            }
        };

        this.addSlot(attunementSlot);

        setPlayerHasAttunedItem(!ArtifactorySavedData.get().getAttunedItems(player.getUUID()).isEmpty());
    }

    public void initializeDataSlots() {
        setIsActive(0);
        setProgress(0);
        setCanItemAscend(0);
    }

    // Block Data Methods
    public int getProgress() { return this.data.get(PROGRESS_INDEX); }
    public void setProgress(int value) { this.data.set(PROGRESS_INDEX, value); }
    public boolean getIsActive() { return this.data.get(IS_ACTIVE_INDEX) > 0; }
    public void setIsActive(int value) { this.data.set(IS_ACTIVE_INDEX, value); }
    public boolean canItemAscend() { return this.data.get(CAN_ITEM_ASCEND_INDEX) > 0; }
    public void setCanItemAscend(int value) { this.data.set(CAN_ITEM_ASCEND_INDEX, value); }
    public boolean playerHasAttunedItem() { return this.data.get(PLAYER_HAS_ATTUNED_ITEM_INDEX) > 0; }
    public void setPlayerHasAttunedItem(boolean hasAttunedItem) { this.data.set(PLAYER_HAS_ATTUNED_ITEM_INDEX, hasAttunedItem ? 1 : 0); }
    @Override
    public boolean clickMenuButton(@NotNull Player player, int pId) {
        if(pId == 1 && attunementSlot.hasItem()) {
            if(progress > 0) {
                setProgress(0);
                setIsActive(0);
            } else {
                setIsActive(1);
            }
        }
        return super.clickMenuButton(player, pId);
    }

    @Override
    public void slotsChanged(Container container) {
        super.slotsChanged(container);
        updateAttunementState();
    }

    private void inputSlotChanged(Slot slot) {
        if(slot.hasItem() && this.player instanceof ServerPlayer serverPlayer) {
            // TODO: Why is this firing 3 times?
            NetworkUtil.updateAttunementNexusSlotInformation(serverPlayer, slot.getItem());
        }
    }

    @Override
    public void broadcastChanges() {
        if(!player.level().isClientSide() && getIsActive()) {
            if(getProgress() < MAX_PROGRESS) {
                setProgress(getProgress() + 1);
            } else {
                if(this.attunementSlot.hasItem() &&
                        (player.getAbilities().instabuild || this.meetsRequirementsToAttune()) ) {
                    ItemStack stack = this.attunementSlot.getItem();
                    if(!MinecraftForge.EVENT_BUS.post(new PreAttuneEvent(player, stack))) {
                        handleAttunement(stack);
                        MinecraftForge.EVENT_BUS.post(new PostAttuneEvent(player, stack));
                    }
                }
                setIsActive(0);
                setProgress(0);
            }
        }
        super.broadcastChanges();
    }

    // Block Entity Data Methods
    public int getScaledProgress() {
        int progress = getProgress();
        int progressArrowSize = 18;

        return progress != 0 ? progress * progressArrowSize / MAX_PROGRESS : 0;
    }

    private boolean meetsRequirementsToAttune() {
        int cost = 11; //getCost();
        int threshold = 15; //getThreshold();

        if(cost <= 0 && threshold <= 0) return true;

        int playerLevel = player.experienceLevel;
        if(threshold > 0 && playerLevel < threshold){
            PlayerMessenger.displayTranslatabelClientMessage(player, "playermessage.artifactory.failed_level_threshold", String.valueOf(threshold));
            return false;
        }
        if(cost > 0 && playerLevel < cost){
            PlayerMessenger.displayTranslatabelClientMessage(player, "playermessage.artifactory.failed_level_cost", String.valueOf(cost));
            return false;
        }

        return true;
    }

    private void payCostForAttunement() {
        int cost = 10; //getCost();
        if(cost > 0) player.giveExperienceLevels(-cost);
    }

    private void handleAttunement(ItemStack stackToAttune) {
        AttunementService.increaseLevelOfAttunement(player, stackToAttune);
        if(!player.getAbilities().instabuild) this.payCostForAttunement();
        updateAttunementState();
    }

    public void updateAttunementState() {
        if(this.player.level().isClientSide()) return;
        setPlayerHasAttunedItem(!ArtifactorySavedData.get().getAttunedItems(this.player.getUUID()).isEmpty());

        if(!inputSlot.isEmpty()) {
            ItemStack attuneableItemStack = inputSlot.getItem(0);
            if(AttunementUtil.canIncreaseAttunementLevel(player, attuneableItemStack) && (player.getAbilities().instabuild || meetsRequirementsToAttune())) {
                setCanItemAscend(1);
            } else {
                if(this.canItemAscend != 0) setCanItemAscend(0);
            }
        } else {
            if(this.canItemAscend != 0) setCanItemAscend(0);
        }
    }

    public boolean hasAttuneableItemInSlot() {
        return !this.inputSlot.isEmpty();
    }

    @Override
    public boolean stillValid(Player player) {
        return this.access.evaluate((level, blockPos) -> level.getBlockState(blockPos).is(BlockRegistry.ATTUNEMENT_NEXUS_BLOCK.get()) && player.distanceToSqr((double) blockPos.getX() + 0.5D, (double) blockPos.getY() + 0.5D, (double) blockPos.getZ() + 0.5D) <= 64.0D, true);
    }

    private void addPlayerInventory(Inventory playerInventory) {
        for(int i = 0; i < 3; ++i) {
            for(int j = 0; j < 9; ++j) {
                int slot = j + i * 9 + 9;
                int x = 8 + j * 18;
                int y = 84 + i * 18;
                this.addSlot(new Slot(playerInventory, slot, x, y));
            }
        }
    }

    private void addPlayerHotbar(Inventory playerInventory) {
        for(int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 142));
        }
    }

    @Override
    public void removed(Player player) {
        initializeDataSlots();
        setPlayerHasAttunedItem(false);
        super.removed(player);
        this.access.execute((p_39796_, p_39797_) -> {
            this.clearContainer(player, this.inputSlot);
        });
    }

    // CREDIT GOES TO: diesieben07 | https://github.com/diesieben07/SevenCommons
    // must assign a slot number to each of the slots used by the GUI.
    // For this container, we can see both the tile inventory's slots as well as the player inventory slots and the hotbar.
    // Each time we add a Slot to the container, it automatically increases the slotIndex, which means
    //  0 - 8 = hotbar slots (which will map to the InventoryPlayer slot numbers 0 - 8)
    //  9 - 35 = player inventory slots (which map to the InventoryPlayer slot numbers 9 - 35)
    //  36 - 44 = TileInventory slots, which map to our TileEntity slot numbers 0 - 8)
    private static final int HOTBAR_SLOT_COUNT = 9;
    private static final int PLAYER_INVENTORY_ROW_COUNT = 3;
    private static final int PLAYER_INVENTORY_COLUMN_COUNT = 9;
    private static final int PLAYER_INVENTORY_SLOT_COUNT = PLAYER_INVENTORY_COLUMN_COUNT * PLAYER_INVENTORY_ROW_COUNT;
    private static final int VANILLA_SLOT_COUNT = HOTBAR_SLOT_COUNT + PLAYER_INVENTORY_SLOT_COUNT;
    private static final int VANILLA_FIRST_SLOT_INDEX = 0;
    private static final int TE_INVENTORY_FIRST_SLOT_INDEX = VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT;

    // THIS YOU HAVE TO DEFINE!
    private static final int TE_INVENTORY_SLOT_COUNT = 1;  // must be the number of slots you have!
    @Override
    public ItemStack quickMoveStack(Player playerIn, int pIndex) {
        Slot sourceSlot = slots.get(pIndex);
        if (sourceSlot == null || !sourceSlot.hasItem()) return ItemStack.EMPTY;  //EMPTY_ITEM
        ItemStack sourceStack = sourceSlot.getItem();
        ItemStack copyOfSourceStack = sourceStack.copy();

        // Check if the slot clicked is one of the vanilla container slots
        if (pIndex < VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT) {
            // This is a vanilla container slot so merge the stack into the tile inventory
            if (!moveItemStackTo(sourceStack, TE_INVENTORY_FIRST_SLOT_INDEX, TE_INVENTORY_FIRST_SLOT_INDEX
                    + TE_INVENTORY_SLOT_COUNT, false)) {
                return ItemStack.EMPTY;  // EMPTY_ITEM
            }
        } else if (pIndex < TE_INVENTORY_FIRST_SLOT_INDEX + TE_INVENTORY_SLOT_COUNT) {
            // This is a TE slot so merge the stack into the players inventory
            if (!moveItemStackTo(sourceStack, VANILLA_FIRST_SLOT_INDEX, VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            System.out.println("Invalid slotIndex:" + pIndex);
            return ItemStack.EMPTY;
        }
        // If stack size == 0 (the entire stack was moved) set slot contents to null
        if (sourceStack.getCount() == 0) {
            sourceSlot.set(ItemStack.EMPTY);
        } else {
            sourceSlot.setChanged();
        }
        sourceSlot.onTake(playerIn, sourceStack);
        return copyOfSourceStack;
    }
}
