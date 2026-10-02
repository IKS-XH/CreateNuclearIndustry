package com.iksxh.create_nuclear_industry.production;

import com.iksxh.create_nuclear_industry.content.FuelProcessingContent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

/** 前面板只暴露两格只取产物与只读状态；服务端每次操作仍验证方块和玩家距离。 */
public final class CentrifugeMenu extends AbstractContainerMenu {
    private final BlockPos position;
    private final ContainerData data;

    /** 客户端由服务器传来的方块位置找到同一设备；库存显示仍以同步数据为准。 */
    public CentrifugeMenu(int id, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        this(id, playerInventory, findMachine(playerInventory, buffer.readBlockPos()), new SimpleContainerData(8));
    }

    public CentrifugeMenu(int id, Inventory playerInventory, CentrifugeBlockEntity machine, ContainerData data) {
        super(FuelProcessingContent.CENTRIFUGE_MENU.get(), id);
        this.position = machine.getBlockPos();
        this.data = data;
        addDataSlots(data);
        for (int slot = 0; slot < 2; slot++) {
            addSlot(new OutputSlot(machine.outputForMenu(), slot, 62 + slot * 36, 35,
                    playerInventory.player.level().isClientSide));
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(playerInventory, column + row * 9 + 9, 8 + column * 18, 84 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(playerInventory, column, 8 + column * 18, 142));
        }
    }

    private static CentrifugeBlockEntity findMachine(Inventory inventory, BlockPos pos) {
        BlockEntity blockEntity = inventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof CentrifugeBlockEntity machine) return machine;
        throw new IllegalStateException("离心机菜单对应方块不存在");
    }

    public int status(int field) { return data.get(field); }

    @Override
    public boolean stillValid(Player player) {
        return player.level().getBlockEntity(position) instanceof CentrifugeBlockEntity
                && player.distanceToSqr(position.getX() + .5, position.getY() + .5, position.getZ() + .5) <= 64;
    }

    /** Shift 取出仅从两个产物槽流向玩家背包，绝不反向插入机器。 */
    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        if (slotIndex < 0 || slotIndex >= slots.size() || slotIndex >= 2 || !stillValid(player)) return ItemStack.EMPTY;
        Slot slot = slots.get(slotIndex);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack original = slot.getItem().copy();
        ItemStack moving = original.copy();
        if (!moveItemStackTo(moving, 2, slots.size(), true)) return ItemStack.EMPTY;
        int extracted = original.getCount() - moving.getCount();
        if (extracted <= 0) return ItemStack.EMPTY;
        slot.remove(extracted);
        slot.setChanged();
        return original;
    }

    /**
     * 菜单输出槽只在客户端保存服务器同步的显示副本；服务端取出始终调用只读输出端口。
     * NeoForge 原槽位的 set 会强转 IItemHandlerModifiable，不能直接用于外部只读端口。
     */
    static final class OutputSlot extends SlotItemHandler {
        private final boolean clientSide;
        private ItemStack clientStack = ItemStack.EMPTY;

        OutputSlot(IItemHandler output, int index, int x, int y, boolean clientSide) {
            super(output, index, x, y);
            this.clientSide = clientSide;
        }

        @Override public boolean mayPlace(ItemStack stack) { return false; }

        @Override public ItemStack getItem() {
            return clientSide ? clientStack : super.getItem();
        }

        @Override public void set(ItemStack stack) {
            if (clientSide) clientStack = stack.copy();
        }

        @Override public void initialize(ItemStack stack) {
            set(stack);
        }

        @Override public boolean mayPickup(Player player) {
            return clientSide ? !clientStack.isEmpty() : super.mayPickup(player);
        }

        @Override public ItemStack remove(int amount) {
            if (!clientSide) return super.remove(amount);
            if (amount <= 0 || clientStack.isEmpty()) return ItemStack.EMPTY;
            ItemStack taken = clientStack.split(amount);
            return taken;
        }
    }
}
