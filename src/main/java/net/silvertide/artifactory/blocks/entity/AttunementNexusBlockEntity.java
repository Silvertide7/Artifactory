package net.silvertide.artifactory.blocks.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.silvertide.artifactory.registry.BlockEntityRegistry;
import net.silvertide.artifactory.screen.AttunementNexusMenu;
import net.silvertide.artifactory.util.ArtifactUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class AttunementNexusBlockEntity extends BlockEntity implements MenuProvider {
    private final ItemStackHandler itemHandler = new ItemStackHandler(2);
    private static final int INPUT_SLOT = 0;
    private static final int OUTPUT_SLOT = 1;
    private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.empty();

    protected final ContainerData data;
    private int progress = 0;
    private int maxProgress = 78;
    @Nullable
    private UUID playerToAttuneUUID;

    public AttunementNexusBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(BlockEntityRegistry.ATTUNEMENT_NEXUS_BLOCK_ENTITY.get(), pPos, pBlockState);
        this.data = new ContainerData() {
            @Override
            public int get(int index) {
                return switch(index) {
                    case 0 -> AttunementNexusBlockEntity.this.progress;
                    case 1 -> AttunementNexusBlockEntity.this.maxProgress;
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
                switch(index) {
                    case 0 -> AttunementNexusBlockEntity.this.progress = value;
                    case 1 -> AttunementNexusBlockEntity.this.maxProgress = value;
                }
            }

            @Override
            public int getCount() {
                return 2;
            }
        };
    }

    public void drops() {
        SimpleContainer inventory = new SimpleContainer(itemHandler.getSlots());
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            inventory.setItem(i, itemHandler.getStackInSlot(i));
        }

        Containers.dropContents(this.level, this.worldPosition, inventory);
    }

    public ItemStack getStackInSlot(int slot) {
        return this.itemHandler.getStackInSlot(slot);
    }
    @Override
    public Component getDisplayName() {
        return Component.translatable("block.artifactory.attunement_nexus");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int pContainerId, Inventory pPlayerInventory, Player pPlayer) {
        return new AttunementNexusMenu(pContainerId, pPlayerInventory, this, this.data);
    }

    public void setPlayerToAttuneUUID(Player player) {
        this.playerToAttuneUUID = player.getUUID();
    }

    private final String PROGRESS_NBT_KEY = "attunement_nexus.progress";
    private final String PLAYER_TO_ATTUNE_UUID_NBT_KEY = "attunement_nexus.player_to_attune_uuid";
    @Override
    protected void saveAdditional(CompoundTag tag) {
        tag.put("inventory", itemHandler.serializeNBT());
        tag.putInt(PROGRESS_NBT_KEY, progress);
        if(this.playerToAttuneUUID != null) {
            tag.putUUID(PLAYER_TO_ATTUNE_UUID_NBT_KEY, this.playerToAttuneUUID);
        }
        super.saveAdditional(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        itemHandler.deserializeNBT(tag.getCompound("inventory"));
        progress = tag.getInt(PROGRESS_NBT_KEY);
        if(tag.contains(PLAYER_TO_ATTUNE_UUID_NBT_KEY)) {
            this.playerToAttuneUUID = tag.getUUID(PLAYER_TO_ATTUNE_UUID_NBT_KEY);
        }
    }

    public void tick(Level level, BlockPos pos, BlockState state) {
        if(canAttune() && this.playerToAttuneUUID != null) {
            increaseAttunementProgress();
            setChanged(level, pos, state);

            if (hasAttunementFinished()) {
                Player playerToAttune = level.getPlayerByUUID(this.playerToAttuneUUID);
                attuneItem(playerToAttune);
                resetProgress();
            }
        } else {
            resetProgress();
        }
    }

    private boolean canAttune() {
        if(!inputSlotEmpty() && ArtifactUtil.isAttuneable(getStackInSlot(INPUT_SLOT))) {
            return outputSlotEmpty();
        }
        return false;
    }

    private boolean inputSlotEmpty() {
        return getStackInSlot(INPUT_SLOT).isEmpty();
    }

    private boolean outputSlotEmpty() {
        return getStackInSlot(OUTPUT_SLOT).isEmpty();
    }

    private void attuneItem(Player player) {
        ItemStack inputStack = this.itemHandler.extractItem(INPUT_SLOT, 1, false);

        ArtifactUtil.attuneItem(player, inputStack);

        clearPlayerToAttuneToUUID();
        this.itemHandler.setStackInSlot(OUTPUT_SLOT, inputStack);
    }

    private void resetProgress() {
        progress = 0;
    }

    private void increaseAttunementProgress() {
        progress++;
    }

    private boolean hasAttunementFinished() {
        return progress >= maxProgress;
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if(cap == ForgeCapabilities.ITEM_HANDLER) {
            return lazyItemHandler.cast();
        }
        return super.getCapability(cap, side);
    }
    @Override
    public void onLoad() {
        super.onLoad();
        lazyItemHandler = LazyOptional.of(() -> itemHandler);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyItemHandler.invalidate();
    }

    public void clearPlayerToAttuneToUUID() {
        this.playerToAttuneUUID = null;
    }
}
