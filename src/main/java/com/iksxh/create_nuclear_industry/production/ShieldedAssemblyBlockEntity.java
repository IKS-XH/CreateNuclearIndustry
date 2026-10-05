package com.iksxh.create_nuclear_industry.production;

import com.iksxh.create_nuclear_industry.goggle.GoggleTooltip;
import com.iksxh.create_nuclear_industry.CreateNuclearIndustry;
import com.iksxh.create_nuclear_industry.content.FuelProcessingContent;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * 八格装配台主控的服务端状态所有者。Create轴网只提供速度与过载判定；活动原料、成品和工时
 * 只写在同一个账本，客户端同步仅供渲染与护目镜展示，任何物料事务均在服务端完成。
 */
public final class ShieldedAssemblyBlockEntity extends KineticBlockEntity implements IHaveGoggleInformation {
    private static final ResourceLocation RECIPE_ID = ResourceLocation.fromNamespaceAndPath(
            CreateNuclearIndustry.MOD_ID, "shielded_assembly/fresh_fuel_assembly");
    private final ShieldedAssemblyState state = new ShieldedAssemblyState();
    private boolean removalHandled;
    private UUID ownerId;

    public ShieldedAssemblyBlockEntity(BlockPos pos, BlockState blockState) {
        super(FuelProcessingContent.SHIELDED_ASSEMBLY_BE.get(), pos, blockState);
    }

    @Override public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        // 本机不用数值框；活动配方决定最多四个输入槽，外置Create物流负责筛选。
    }
    @Override public float calculateStressApplied() {
        lastStressApplied = 4;
        return 4;
    }
    public ShieldedAssemblyState state() { return state; }
    public boolean isRemovalHandled() { return removalHandled; }
    public void markRemovalHandled() { removalHandled = true; }
    public UUID ownerId() { return ownerId; }
    public boolean matchesOwner(UUID id) { return ownerId != null && ownerId.equals(id); }
    public void setOwnerId(UUID id) { ownerId = id; changed(); }
    public boolean expanded() { return getBlockState().getValue(ShieldedAssemblyBlock.EXPANDED); }
    public boolean complete() { return ShieldedAssemblyStructure.complete(this); }
    public boolean current() {
        return level != null && !isRemoved() && level.getBlockEntity(worldPosition) == this
                && level.getBlockState(worldPosition).is(FuelProcessingContent.SHIELDED_ASSEMBLY_STATION.get());
    }

    /** 配方重载逐tick复核；无效配方只清工时，不能删除库存。 */
    @Override public void tick() {
        super.tick();
        if (level == null || level.isClientSide || !current()) return;
        boolean structure = complete();
        int oldProgress = state.progress();
        boolean recipe = structure && recipeReady();
        if (oldProgress != state.progress()) changed();
        if (structure && !recipe && oldProgress > 0) {
            state.invalidateProgress();
            changed();
        }
        boolean working = structure && recipe && state.ready() && state.outputEmpty()
                && !isOverStressed() && Math.abs(getSpeed()) >= 32;
        BlockState blockState = getBlockState();
        if (blockState.getValue(ShieldedAssemblyBlock.WORKING) != working)
            level.setBlock(worldPosition, blockState.setValue(ShieldedAssemblyBlock.WORKING, working), 3);
        ShieldedAssemblyStructure.syncWorking(this, working);
        if (working && state.advance(getSpeed(), currentRecipe().actualResult(state.input(0)))) {
            setChanged();
            if (state.progress() == 0 || oldProgress / 256 != state.progress() / 256) sendData();
        }
    }

    private ShieldedAssemblyRecipe recipeFor(String operation) {
        if (level == null) return null;
        ResourceLocation id = "sealing".equals(operation) ? ResourceLocation.fromNamespaceAndPath(
                CreateNuclearIndustry.MOD_ID, "shielded_assembly/sealed_spent_fuel_cask") : RECIPE_ID;
        var holder = level.getRecipeManager().byKey(id);
        if (holder.isEmpty() || !(holder.get().value() instanceof ShieldedAssemblyRecipe recipe)
                || !recipe.valid() || !recipe.operation().equals(operation)) return null;
        return recipe;
    }
    private ShieldedAssemblyRecipe currentRecipe() {
        return recipeFor(state.operation().isEmpty() ? "manufacture" : state.operation());
    }
    private ShieldedAssemblyRecipe offeredRecipe(int slot, ItemStack offered) {
        if (!state.operation().isEmpty()) {
            ShieldedAssemblyRecipe recipe = currentRecipe();
            return recipe != null && recipe.accepts(slot, offered) ? recipe : null;
        }
        for (String mode : List.of("manufacture", "sealing")) {
            ShieldedAssemblyRecipe recipe = recipeFor(mode);
            if (recipe != null && recipe.accepts(slot, offered)) return recipe;
        }
        return null;
    }
    /** 活动配方匹配完整已存栈后绑定数量及工时；重载失效不吞料、不沿用旧配方进度。 */
    public boolean recipeReady() {
        ShieldedAssemblyRecipe recipe = currentRecipe();
        if (recipe == null) return false;
        for (int slot = 0; slot < 4; slot++) {
            ItemStack stored = state.input(slot);
            if (!stored.isEmpty() && !recipe.accepts(slot, stored)) return false;
        }
        state.configure(recipe.operation(), recipe.operation(), recipe.costs(), recipe.work());
        return true;
    }
    /** 模拟只读，不绑定工序或配方；真正接受首料才修改批次配置和选择。 */
    private ItemStack insertSlot(int slot, ItemStack offered, boolean simulate) {
        ShieldedAssemblyRecipe recipe = offeredRecipe(slot, offered);
        if (recipe == null || level == null || level.isClientSide) return offered.copy();
        ItemStack preview = state.insert(slot, offered, true, recipe.operation());
        if (simulate || preview.getCount() == offered.getCount()) return preview;
        state.configure(recipe.operation(), recipe.operation(), recipe.costs(), recipe.work());
        ItemStack remainder = state.insert(slot, offered, false, recipe.operation());
        changed();
        return remainder;
    }

    public IItemHandler itemPort(BlockPos portPos, Direction side) {
        if (side == null || side == Direction.DOWN || !complete()) return null;
        int part = ShieldedAssemblyLayout.partAt(worldPosition,
                getBlockState().getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING), portPos);
        if (part < 0 || !ShieldedAssemblyLayout.exterior(part,
                getBlockState().getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING), side)) return null;
        return new Port(portPos, part, side);
    }
    public IItemHandler itemPort(Direction side) { return itemPort(worldPosition, side); }
    public ItemStack insert(ItemStack stack) {
        if (!complete()) return stack.copy();
        for (int slot = 0; slot < 4; slot++) if (offeredRecipe(slot, stack) != null) return insertSlot(slot, stack, false);
        return stack.copy();
    }

    /** 手持交互复用数据配方谓词，允许标签中的合法替代材料；不修改选择和账本。 */
    public boolean acceptsInput(ItemStack stack) {
        for (int slot = 0; slot < 4; slot++) if (offeredRecipe(slot, stack) != null) return true;
        return false;
    }

    /** 玩家背包先接受物品，再从机器账本扣除同数，背包满时机器保持原样。 */
    public int takeToPlayer(Player player, boolean takeInput) {
        if (!current()) return 0;
        int slot = -1;
        if (takeInput) {
            for (int i = 0; i < 4; i++) if (!state.input(i).isEmpty()) { slot = i; break; }
        }
        ItemStack offered = takeInput ? slot < 0 ? ItemStack.EMPTY : state.input(slot) : state.output();
        if (offered.isEmpty()) return 0;
        ItemStack remainder = offered.copy();
        player.getInventory().add(remainder);
        int accepted = offered.getCount() - remainder.getCount();
        if (accepted > 0) {
            if (takeInput) state.extractInput(slot, accepted, false);
            else state.extractOutput(accepted, false);
            changed();
        }
        return accepted;
    }

    public CompoundTag savePortableData() {
        return state.save(level == null ? net.minecraft.core.RegistryAccess.EMPTY : level.registryAccess());
    }
    public void loadPortableData(CompoundTag tag) {
        if (level == null || level.isClientSide) return;
        state.load(tag, level.registryAccess());
        changed();
    }
    public void changed() { setChanged(); sendData(); }

    @Override protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.put("ShieldedAssembly", state.save(registries));
        if (ownerId != null) tag.putUUID("ShieldedAssemblyOwner", ownerId);
    }
    @Override protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        state.load(tag.getCompound("ShieldedAssembly"), registries);
        ownerId = tag.hasUUID("ShieldedAssemblyOwner") ? tag.getUUID("ShieldedAssemblyOwner") : null;
    }

    private String waitReason() {
        if (!expanded()) return "legacy";
        if (!complete()) return "structure";
        if (!recipeReady()) return "recipe";
        if (!state.ready()) return "input";
        if (!state.outputEmpty()) return "output";
        if (isOverStressed()) return "overload";
        if (getSpeed() == 0) return "power";
        if (Math.abs(getSpeed()) < 32) return "speed";
        return "running";
    }
    /** 世界内状态提示与护目镜共用同一等待原因，避免客户端另算判定。 */
    public Component waitStatus() {
        return Component.translatable("gui.create_nuclear_industry.shielded_assembly.wait." + waitReason());
    }
    @Override public boolean addToGoggleTooltip(List<Component> tooltip, boolean sneaking) {
        tooltip.add(GoggleTooltip.indentFirstLine(Component.translatable("block.create_nuclear_industry.shielded_assembly_station")));
        tooltip.add(Component.translatable("gui.create_nuclear_industry.shielded_assembly.operation." +
                (state.operation().isEmpty() ? "unselected" : state.operation())));
        ShieldedAssemblyRecipe recipe = currentRecipe();
        int slots = recipe == null ? 4 : recipe.inputs().size();
        for (int slot = 0; slot < slots; slot++)
            tooltip.add(Component.translatable("gui.create_nuclear_industry.shielded_assembly.material",
                    state.input(slot).isEmpty() ? recipe == null ? Component.literal("—")
                    : recipe.inputs().get(slot).ingredient().getItems()[0].getHoverName() : state.input(slot).getHoverName(),
                    state.input(slot).getCount(), recipe == null ? state.cost(slot) : recipe.inputs().get(slot).count()));
        tooltip.add(Component.translatable("gui.create_nuclear_industry.shielded_assembly.output",
                state.output().isEmpty() ? 0 : 1));
        tooltip.add(Component.translatable("gui.create_nuclear_industry.shielded_assembly.speed", Math.round(getSpeed())));
        tooltip.add(Component.translatable("gui.create_nuclear_industry.shielded_assembly.progress",
                state.progress(), state.work()));
        tooltip.add(waitStatus());
        return true;
    }

    private final class Port implements IItemHandler {
        private final BlockPos portPos;
        private final int part;
        private final Direction side;
        private Port(BlockPos portPos, int part, Direction side) {
            this.portPos = portPos.immutable(); this.part = part; this.side = side;
        }
        private boolean valid() {
            if (!complete() || level == null || level.isClientSide || !level.hasChunkAt(portPos)) return false;
            BlockState portState = level.getBlockState(portPos);
            return ShieldedAssemblyStructure.master(level, portPos, portState) == ShieldedAssemblyBlockEntity.this
                    && ShieldedAssemblyLayout.exterior(part,
                    getBlockState().getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING), side);
        }
        @Override public int getSlots() { return !valid() ? 0 : side == Direction.UP ? 4 : 5; }
        @Override public ItemStack getStackInSlot(int slot) {
            if (!valid()) return ItemStack.EMPTY;
            return slot >= 0 && slot < 4 ? state.input(slot)
                    : side != Direction.UP && slot == 4 ? state.output() : ItemStack.EMPTY;
        }
        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (!valid() || !isItemValid(slot, stack)) return stack.copy();
            return insertSlot(slot, stack, simulate);
        }
        @Override public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (!valid() || side == Direction.UP || slot != 4) return ItemStack.EMPTY;
            ItemStack extracted = state.extractOutput(amount, simulate);
            if (!simulate && !extracted.isEmpty()) changed();
            return extracted;
        }
        @Override public int getSlotLimit(int slot) {
            return !valid() ? 0 : slot >= 0 && slot < 4 ? 64
                    : side != Direction.UP && slot == 4 ? 1 : 0;
        }
        @Override public boolean isItemValid(int slot, ItemStack stack) {
            return valid() && offeredRecipe(slot, stack) != null;
        }
    }
}
