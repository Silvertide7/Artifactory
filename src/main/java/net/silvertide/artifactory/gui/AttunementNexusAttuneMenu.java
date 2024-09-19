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
import net.silvertide.artifactory.events.custom.PostAttuneEvent;
import net.silvertide.artifactory.events.custom.PreAttuneEvent;
import net.silvertide.artifactory.registry.BlockRegistry;
import net.silvertide.artifactory.registry.MenuRegistry;
import net.silvertide.artifactory.storage.ArtifactorySavedData;
import net.silvertide.artifactory.storage.AttunementNexusSlotInformation;
import net.silvertide.artifactory.util.*;
import org.jetbrains.annotations.NotNull;

public class AttunementNexusAttuneMenu extends AbstractContainerMenu {
    public final int MAX_PROGRESS = 60;
    private final ContainerLevelAccess access;
    private final Player player;
    private AttunementNexusSlotInformation attunementNexusSlotInformation= null;

    // Data Slot Fields
    protected final ContainerData data;
    private int progress = 0;
    private int isActive = 0;
    private int itemAtMaxLevel = 0;
    private int ascensionCanStart = 0;
    private int playerHasAttunedItem = 0;
    private int itemRequirementOneInfo = 0;
    private int itemRequirementTwoInfo = 0;
    private int itemRequirementThreeInfo = 0;

    // Data Slot Accessor Indices
    private final int PROGRESS_INDEX = 0;
    private final int IS_ACTIVE_INDEX = 1;
    private final int ITEM_AT_MAX_LEVEL = 2;
    private final int ASCENSION_CAN_START_INDEX = 3;
    private final int PLAYER_HAS_ATTUNED_ITEM_INDEX = 4;
    private final int ITEM_REQUIREMENT_ONE_INFO_INDEX = 5;
    private final int ITEM_REQUIREMENT_TWO_INFO_INDEX = 6;
    private final int ITEM_REQUIREMENT_THREE_INFO_INDEX = 7;

    // Slots
    private final Slot attunementInputSlot;
    protected final Container attunementInputContainer = new SimpleContainer(1);

    private final ItemRequirementSlot itemRequirementOneSlot;
    protected final Container itemRequirementOneContainer = new SimpleContainer(1);


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

        this.data = getContainerData();
        this.addDataSlots(this.data);

        this.attunementInputSlot = getAttunementInputSlot();
        this.addSlot(attunementInputSlot);

        this.itemRequirementOneSlot = getItemRequirementOneSlot();
        this.addSlot(itemRequirementOneSlot);

        setPlayerHasAttunedItem(!ArtifactorySavedData.get().getAttunedItems(player.getUUID()).isEmpty());
    }

    // Block Data Methods
    public int getProgress() { return this.data.get(PROGRESS_INDEX); }
    public void setProgress(int value) { this.data.set(PROGRESS_INDEX, value); }
    public boolean getIsActive() { return this.data.get(IS_ACTIVE_INDEX) > 0; }
    public void setIsActive(boolean value) { this.data.set(IS_ACTIVE_INDEX, value ? 1 : 0); }
    public boolean isItemAtMaxLevel() { return this.data.get(ITEM_AT_MAX_LEVEL) > 0; }
    public void setItemAtMaxLevel(boolean value) { this.data.set(ITEM_AT_MAX_LEVEL, value ? 1 : 0); }
    public boolean ascensionCanStart() { return this.data.get(ASCENSION_CAN_START_INDEX) > 0; }
    public void setAscensionCanStart(boolean value) { this.data.set(ASCENSION_CAN_START_INDEX, value ? 1 : 0); }
    public boolean playerHasAttunedItem() { return this.data.get(PLAYER_HAS_ATTUNED_ITEM_INDEX) > 0; }
    public void setPlayerHasAttunedItem(boolean hasAttunedItem) { this.data.set(PLAYER_HAS_ATTUNED_ITEM_INDEX, hasAttunedItem ? 1 : 0); }
    public int getItemRequirementOneInfo() { return this.data.get(ITEM_REQUIREMENT_ONE_INFO_INDEX); }
    public void setItemRequirementOneInfo(int value) { this.data.set(ITEM_REQUIREMENT_ONE_INFO_INDEX, value); }
    public int getItemRequirementTwoInfo() { return this.data.get(ITEM_REQUIREMENT_TWO_INFO_INDEX); }
    public void setItemRequirementTwoInfo(int value) { this.data.set(ITEM_REQUIREMENT_TWO_INFO_INDEX, value); }
    public int getItemRequirementThreeInfo() { return this.data.get(ITEM_REQUIREMENT_THREE_INFO_INDEX); }
    public void setItemRequirementThreeInfo(int value) { this.data.set(ITEM_REQUIREMENT_THREE_INFO_INDEX, value); }

    private ContainerData getContainerData() {
        return new ContainerData() {
            @Override
            public int get(int index) {
                return switch(index) {
                    case PROGRESS_INDEX -> AttunementNexusAttuneMenu.this.progress;
                    case IS_ACTIVE_INDEX -> AttunementNexusAttuneMenu.this.isActive;
                    case ITEM_AT_MAX_LEVEL -> AttunementNexusAttuneMenu.this.itemAtMaxLevel;
                    case ASCENSION_CAN_START_INDEX -> AttunementNexusAttuneMenu.this.ascensionCanStart;
                    case PLAYER_HAS_ATTUNED_ITEM_INDEX -> AttunementNexusAttuneMenu.this.playerHasAttunedItem;
                    case ITEM_REQUIREMENT_ONE_INFO_INDEX -> AttunementNexusAttuneMenu.this.itemRequirementOneInfo;
                    case ITEM_REQUIREMENT_TWO_INFO_INDEX -> AttunementNexusAttuneMenu.this.itemRequirementTwoInfo;
                    case ITEM_REQUIREMENT_THREE_INFO_INDEX -> AttunementNexusAttuneMenu.this.itemRequirementThreeInfo;
                    default -> PROGRESS_INDEX;
                };
            }

            @Override
            public void set(int index, int value) {
                switch(index) {
                    case PROGRESS_INDEX -> AttunementNexusAttuneMenu.this.progress = value;
                    case IS_ACTIVE_INDEX -> AttunementNexusAttuneMenu.this.isActive = value;
                    case ITEM_AT_MAX_LEVEL -> AttunementNexusAttuneMenu.this.itemAtMaxLevel = value;
                    case ASCENSION_CAN_START_INDEX -> AttunementNexusAttuneMenu.this.ascensionCanStart = value;
                    case PLAYER_HAS_ATTUNED_ITEM_INDEX -> AttunementNexusAttuneMenu.this.playerHasAttunedItem = value;
                    case ITEM_REQUIREMENT_ONE_INFO_INDEX -> AttunementNexusAttuneMenu.this.itemRequirementOneInfo = value;
                    case ITEM_REQUIREMENT_TWO_INFO_INDEX -> AttunementNexusAttuneMenu.this.itemRequirementTwoInfo = value;
                    case ITEM_REQUIREMENT_THREE_INFO_INDEX -> AttunementNexusAttuneMenu.this.itemRequirementThreeInfo = value;
                }
            }

            @Override
            public int getCount() {
                return 8;
            }
        };
    }

    // Slots

    private Slot getAttunementInputSlot() {
        return new Slot(attunementInputContainer, 0, 80, 23) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                if(getIsActive()) return false;
                return AttunementUtil.isValidAttunementItem(stack);
            }

            @Override
            public void onTake(Player player, ItemStack stack) {
                clearItemDataSlotData();
                ClientAttunementNexusSlotInformation.clearSlotInformation();
                super.onTake(player, stack);
            }

            @Override
            public void set(ItemStack stack) {
                super.set(stack);
                AttunementNexusAttuneMenu.this.updateAttunementState();
            }

            @Override
            public boolean mayPickup(Player player) {
                if(AttunementNexusAttuneMenu.this.getIsActive()) return false;
                return super.mayPickup(player);
            }
        };
    }

    private ItemRequirementSlot getItemRequirementOneSlot() {
        return new ItemRequirementSlot(itemRequirementOneContainer, 0, 60, 50) {

            @Override
            public boolean isAttunementActive() {
                return getIsActive();
            }

            @Override
            public void onTake(Player player, ItemStack stack) {
                if(!player.level().isClientSide()) {
                    AttunementNexusAttuneMenu.this.updateItemRequirementDataSlots();
                }
                super.onTake(player, stack);
            }

            @Override
            public void setChanged() {
                if(!player.level().isClientSide()) {
                    AttunementNexusAttuneMenu.this.updateItemRequirementDataSlots();
                }
                super.setChanged();
            }
        };
    }


    @Override
    public boolean clickMenuButton(@NotNull Player player, int pId) {
        if(pId == 1 && attunementInputSlot.hasItem()) {
            if(progress > 0) {
                setProgress(0);
                setIsActive(false);
            } else {
                setIsActive(true);
            }
        }
        return super.clickMenuButton(player, pId);
    }


    @Override
    public void broadcastChanges() {
        if(!player.level().isClientSide() && getIsActive()) {
            if(getProgress() < MAX_PROGRESS) {
                setProgress(getProgress() + 1);
            } else {
                if(this.ascensionCanStart()) {
                    ItemStack stack = this.attunementInputSlot.getItem();

                    if(!MinecraftForge.EVENT_BUS.post(new PreAttuneEvent(player, stack))) {
                        handleAttunement(stack);
                        MinecraftForge.EVENT_BUS.post(new PostAttuneEvent(player, stack));
                    }
                }
                setIsActive(false);
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
        if(this.attunementNexusSlotInformation == null) return false;

        if(!this.itemRequirementOneSlot.hasRequiredItems()) return false;

        int cost = this.attunementNexusSlotInformation.xpConsumed();
        int threshold = this.attunementNexusSlotInformation.xpThreshold();

        int playerLevel = player.experienceLevel;
        if(threshold > 0 && playerLevel < threshold){
            return false;
        }
        if(cost > 0 && playerLevel < cost){
            return false;
        }

        return true;
    }

    private void payCostForAttunement() {
        int cost = this.attunementNexusSlotInformation.xpConsumed();
        if(cost > 0) player.giveExperienceLevels(-cost);

        itemRequirementOneSlot.consumeRequiredItems();
    }

    private void handleAttunement(ItemStack stackToAttune) {
        AttunementService.increaseLevelOfAttunement(player, stackToAttune);
        if(!player.getAbilities().instabuild) this.payCostForAttunement();
        this.access.execute((level, blockPos) -> {
            this.clearContainer(player, this.itemRequirementOneContainer);
        });
        updateAttunementState();
    }

    public void updateAttunementState() {
        if(this.player.level().isClientSide()) return;
        clearItemDataSlotData();
        setPlayerHasAttunedItem(!ArtifactorySavedData.get().getAttunedItems(this.player.getUUID()).isEmpty());

        if(!attunementInputContainer.isEmpty()) {
            ItemStack stack = attunementInputContainer.getItem(0);
            if(this.player instanceof ServerPlayer serverPlayer) {
                this.attunementNexusSlotInformation = AttunementNexusSlotInformation.createAttunementNexusSlotInformation(serverPlayer, stack);
                if(this.attunementNexusSlotInformation != null) {
                    NetworkUtil.updateAttunementNexusSlotInformation(serverPlayer, this.attunementNexusSlotInformation);
                    updateItemSlotRequirements(this.attunementNexusSlotInformation);
                }
            }

            setItemAtMaxLevel(!AttunementUtil.canIncreaseAttunementLevel(player, stack));
        }

        updateAscensionCanStart();
    }

    private void updateAscensionCanStart() {
        boolean ascensionCanStart = this.attunementInputSlot.hasItem() && !isItemAtMaxLevel() &&
                (player.getAbilities().instabuild || this.meetsRequirementsToAttune());
        setAscensionCanStart(ascensionCanStart);

    }

    private void updateItemSlotRequirements(AttunementNexusSlotInformation attunementNexusSlotInformation) {
        if(attunementNexusSlotInformation.hasItemRequirement(0)) {
            this.itemRequirementOneSlot.setItemRequired(attunementNexusSlotInformation.itemRequirementOne(), attunementNexusSlotInformation.itemRequirementOneQuantity());
        }
        updateItemRequirementDataSlots();
    }


    private void updateItemRequirementDataSlots() {
        setItemRequirementOneInfo(itemRequirementOneSlot.getItemRequirementState().getValue());
        updateAscensionCanStart();
    }


    public void clearItemDataSlotData() {
        setItemAtMaxLevel(false);
        setAscensionCanStart(false);

        setItemRequirementOneInfo(ItemRequirementState.NOT_REQUIRED.getValue());
        setItemRequirementTwoInfo(ItemRequirementState.NOT_REQUIRED.getValue());
        setItemRequirementThreeInfo(ItemRequirementState.NOT_REQUIRED.getValue());

        this.attunementNexusSlotInformation = null;

        this.itemRequirementOneSlot.clearItemRequired();
    }

    public boolean hasAttuneableItemInSlot() {
        return !this.attunementInputContainer.isEmpty();
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
        clearItemDataSlotData();
        setPlayerHasAttunedItem(false);
        setItemAtMaxLevel(false);
        setProgress(0);
        setIsActive(false);

        super.removed(player);
        this.access.execute((level, blockPos) -> {
            this.clearContainer(player, this.attunementInputContainer);
            this.clearContainer(player, this.itemRequirementOneContainer);
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
    private static final int TE_INVENTORY_SLOT_COUNT = 2;  // must be the number of slots you have!
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
