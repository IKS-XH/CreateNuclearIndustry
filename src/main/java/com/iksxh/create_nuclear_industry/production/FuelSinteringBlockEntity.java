package com.iksxh.create_nuclear_industry.production;

import com.iksxh.create_nuclear_industry.content.FuelProcessingContent;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.content.processing.recipe.HeatCondition;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;

/** 单格炉的服务端库存与工时所有者；客户端仅接收视图，真实热级逐 tick 从下方 Create 方块读取。 */
public final class FuelSinteringBlockEntity extends BlockEntity implements IHaveGoggleInformation {
    private final FuelSinteringState state = new FuelSinteringState();
    private boolean removalHandled;

    public FuelSinteringBlockEntity(BlockPos pos, BlockState blockState) {
        super(FuelProcessingContent.FUEL_SINTERING_BE.get(), pos, blockState);
    }

    public FuelSinteringState state() { return state; }
    public boolean isRemovalHandled() { return removalHandled; }
    public void markRemovalHandled() { removalHandled = true; }

    public boolean current() {
        return level != null && !isRemoved() && level.getBlockEntity(worldPosition) == this
                && level.getBlockState(worldPosition).is(FuelProcessingContent.FUEL_SINTERING_FURNACE.get());
    }

    public boolean heated() {
        return level != null && HeatCondition.HEATED.testBlazeBurner(
                BlazeBurnerBlock.getHeatLevelOf(level.getBlockState(worldPosition.below())));
    }

    /** 热级达标即点亮观察窗；停热和堵料均不销毁已积累工时。 */
    public static void serverTick(net.minecraft.world.level.Level level, BlockPos pos, BlockState blockState,
                                  FuelSinteringBlockEntity machine) {
        if (level.isClientSide || !machine.current()) return;
        boolean hot = machine.heated();
        if (blockState.getValue(FuelSinteringBlock.LIT) != hot)
            level.setBlock(pos, blockState.setValue(FuelSinteringBlock.LIT, hot), 3);
        int prior = machine.state.progress();
        if (machine.state.tick(hot && machine.recipeReady())) {
            machine.setChanged();
            if (machine.state.progress() == 0 || prior / 20 != machine.state.progress() / 20)
                machine.syncView();
        }
    }

    /** 运行时核对已加载专用配方；缺配方或改变成本时不允许绕过数据合同产物。 */
    public boolean recipeReady() {
        if (level == null) return false;
        var holder = level.getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath(
                "create_nuclear_industry", "sintering/sintered_fuel_pellet"));
        if (holder.isEmpty() || !(holder.get().value() instanceof FuelSinteringRecipe recipe)) return false;
        return recipe.work() == FuelSinteringState.WORK
                && recipe.input().test(new ItemStack(FuelProcessingContent.GREEN_FUEL_PELLET.get()))
                && recipe.result().is(FuelProcessingContent.SINTERED_FUEL_PELLET.get())
                && recipe.result().getCount() == 1;
    }

    /** 返回新建面句柄，句柄每次复核 BE 和原位置，拆除后的旧引用失效。 */
    public IItemHandler itemPort(Direction side) {
        return side == null || side == Direction.DOWN ? null : new Port(side);
    }

    public int insert(ItemStack stack) {
        if (!current() || !stack.is(FuelProcessingContent.GREEN_FUEL_PELLET.get())) return 0;
        int count = state.insert(stack.getCount(), false);
        if (count > 0) changed();
        return count;
    }

    /** 先由原版背包收取可容纳数量，再从机器扣除同数；满背包不会移走机器库存。 */
    public int takeToPlayer(Player player, boolean input) {
        if (!current()) return 0;
        int available = input ? state.input() : state.output();
        if (available == 0) return 0;
        ItemStack offer = new ItemStack(input ? FuelProcessingContent.GREEN_FUEL_PELLET.get()
                : FuelProcessingContent.SINTERED_FUEL_PELLET.get(), available);
        ItemStack remainder = offer.copy();
        player.getInventory().add(remainder);
        int accepted = offer.getCount() - remainder.getCount();
        if (accepted == 0) return 0;
        if (input) state.takeInput(accepted, false); else state.takeOutput(accepted, false);
        changed();
        return accepted;
    }

    public CompoundTag savePortableData() { return state.save(); }
    public void loadPortableData(CompoundTag tag) { state.load(tag); changed(); }

    public void changed() {
        setChanged();
        syncView();
    }

    private void syncView() {
        if (level != null && !level.isClientSide)
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("FuelSintering", state.save());
    }

    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        state.load(tag.getCompound("FuelSintering"));
    }

    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.put("FuelSintering", state.save());
        return tag;
    }

    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet,
                                       HolderLookup.Provider registries) {
        super.onDataPacket(connection, packet, registries);
        if (packet.getTag() != null) state.load(packet.getTag().getCompound("FuelSintering"));
    }

    @Override public boolean addToGoggleTooltip(List<Component> tooltip, boolean sneaking) {
        tooltip.add(Component.translatable("block.create_nuclear_industry.fuel_sintering_furnace"));
        tooltip.add(Component.translatable("gui.create_nuclear_industry.sintering.input", state.input()));
        tooltip.add(Component.translatable("gui.create_nuclear_industry.sintering.output", state.output()));
        tooltip.add(Component.translatable("gui.create_nuclear_industry.sintering.heat",
                Component.translatable("gui.create_nuclear_industry.sintering.heat." + (heated() ? "sufficient" : "insufficient"))));
        tooltip.add(Component.translatable("gui.create_nuclear_industry.sintering.progress", state.progress(), FuelSinteringState.WORK));
        String reason = !heated() ? "heat" : state.input() == 0 ? "input"
                : state.output() == FuelSinteringState.CAPACITY ? "output"
                : !recipeReady() ? "recipe" : "running";
        tooltip.add(Component.translatable("gui.create_nuclear_industry.sintering.wait." + reason));
        return true;
    }

    private final class Port implements IItemHandler {
        private final Direction side;
        private Port(Direction side) { this.side = side; }
        private boolean valid() { return current(); }
        @Override public int getSlots() { return valid() ? 1 : 0; }
        @Override public ItemStack getStackInSlot(int slot) {
            if (!valid() || slot != 0) return ItemStack.EMPTY;
            int count = side == Direction.UP ? state.input() : state.output();
            return count == 0 ? ItemStack.EMPTY : new ItemStack(side == Direction.UP
                    ? FuelProcessingContent.GREEN_FUEL_PELLET.get() : FuelProcessingContent.SINTERED_FUEL_PELLET.get(), count);
        }
        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (!valid() || side != Direction.UP || slot != 0 || !isItemValid(slot, stack)) return stack;
            int count = state.insert(stack.getCount(), simulate);
            if (!simulate && count > 0) changed();
            return stack.copyWithCount(stack.getCount() - count);
        }
        @Override public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (!valid() || side == Direction.UP || slot != 0) return ItemStack.EMPTY;
            int count = state.takeOutput(amount, simulate);
            if (!simulate && count > 0) changed();
            return count == 0 ? ItemStack.EMPTY : new ItemStack(FuelProcessingContent.SINTERED_FUEL_PELLET.get(), count);
        }
        @Override public int getSlotLimit(int slot) { return valid() && slot == 0 ? FuelSinteringState.CAPACITY : 0; }
        @Override public boolean isItemValid(int slot, ItemStack stack) {
            return valid() && side == Direction.UP && slot == 0 && stack.is(FuelProcessingContent.GREEN_FUEL_PELLET.get());
        }
    }
}
