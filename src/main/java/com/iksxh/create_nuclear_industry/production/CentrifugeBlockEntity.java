package com.iksxh.create_nuclear_industry.production;

import com.iksxh.create_nuclear_industry.content.FuelProcessingContent;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.SidedFilteringBehaviour;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * 离心机服务器状态所有者。轴网由 Create 提供；本实体独占两罐、两粉、密闭批次及磨损。
 * 方向过滤由 Create 原生行为保存；客户端只显示过滤槽与护目镜信息，不参与加工和库存事务。
 */
public final class CentrifugeBlockEntity extends KineticBlockEntity {
    private final CentrifugeState state = new CentrifugeState();
    private SidedFilteringBehaviour filtering;

    public CentrifugeBlockEntity(BlockPos pos, BlockState blockState) {
        super(FuelProcessingContent.CENTRIFUGE_BE.get(), pos, blockState);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        filtering = new SidedFilteringBehaviour(this,
                new CentrifugeFilterSlotTransform(),
                this::createSideFilter, CentrifugeBlockEntity::isMaterialSide);
        behaviours.add(filtering);
    }

    private FilteringBehaviour createSideFilter(Direction side, FilteringBehaviour filter) {
        return filter;
    }

    private static boolean isMaterialSide(Direction side) {
        return side != null && side != Direction.DOWN;
    }

    private boolean itemMatchesFilter(Direction side, ItemStack stack) {
        return filtering == null || filtering.test(side, stack);
    }

    private boolean fluidMatchesFilter(Direction side, FluidStack stack) {
        return filtering == null || filtering.get(side) == null || filtering.get(side).test(stack);
    }

    /** Create 原生值框命中优先于桶交易、维修和空手取料。 */
    public boolean isFilterSlotHit(Direction side, BlockHitResult hit) {
        return filtering != null && isMaterialSide(side) && level != null
                && filtering.testHit(level, worldPosition, side, hit.getLocation());
    }

    /** 应力系数为每 RPM 8 SU，不因库存堵塞而消失。 */
    @Override
    public float calculateStressApplied() {
        lastStressApplied = 8;
        return 8;
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide) return;
        float speed = getSpeed();
        float oldSpeed = state.observedSpeed;
        int oldStable = state.stableTicks;
        PauseReason oldReason = pauseReason();
        state.observeSpeed(speed);
        if (state.stableTicks != oldStable) setChanged();
        // 空机没有加工包可触发常规同步；只在转速、稳定门或停机原因变化时更新护目镜。
        if (Float.compare(speed, oldSpeed) != 0
                || (oldStable < 20 && state.stableTicks == 20)
                || pauseReason() != oldReason) {
            sendData();
        }
        if (speed == 0 || isOverStressed() || state.wearUnits >= CentrifugeState.WEAR_LIMIT) return;
        if (state.batch == null) tryStartBatch();
        if (state.batch != null && state.canFit(state.batch) && state.stableTicks >= 20) {
            state.advance(speed);
            setChanged();
            sendData();
        }
    }

    /** 新批次只在真实配方结果、料浆身份与全部输出空间匹配时接管输入。 */
    private void tryStartBatch() {
        CentrifugeState.Batch candidate = findCandidate();
        if (candidate != null && state.begin(candidate)) {
            setChanged();
            sendData();
        }
    }

    private CentrifugeState.Batch findCandidate() {
        if (level == null) return null;
        for (RecipeHolder<CentrifugeRecipe> holder : level.getRecipeManager().getAllRecipesFor(FuelProcessingContent.CENTRIFUGING_TYPE.get())) {
            CentrifugeRecipe recipe = holder.value();
            if (!recipe.input().is(FuelProcessingContent.URANIUM_SLURRY.get())
                    || !recipe.water().is(Fluids.WATER) || recipe.input().getAmount() <= 0
                    || recipe.enriched().getCount() <= 0 || recipe.depleted().getCount() <= 0) continue;
            return new CentrifugeState.Batch(holder.id(), recipe.input(),
                    recipe.enriched(), recipe.depleted(), recipe.water(), recipe.work());
        }
        return null;
    }

    public enum PauseReason { RUNNING, NO_POWER, OVERLOADED, STABILIZING, NEED_SLURRY, OUTPUT_BLOCKED, BEARING_WORN }

    public PauseReason pauseReason() {
        if (state.wearUnits >= CentrifugeState.WEAR_LIMIT) return PauseReason.BEARING_WORN;
        if (isOverStressed()) return PauseReason.OVERLOADED;
        if (getSpeed() == 0) return PauseReason.NO_POWER;
        if (state.stableTicks < 20) return PauseReason.STABILIZING;
        if (state.batch == null) {
            CentrifugeState.Batch candidate = findCandidate();
            if (candidate == null || state.slurryMb < candidate.input().getAmount()) return PauseReason.NEED_SLURRY;
            return state.canFit(candidate) ? PauseReason.RUNNING : PauseReason.OUTPUT_BLOCKED;
        }
        if (!state.canFit(state.batch)) return PauseReason.OUTPUT_BLOCKED;
        return PauseReason.RUNNING;
    }

    /** 服务端以零转速和一枚真实重型轴承为维修前提。 */
    public boolean repairBearing() {
        if (level == null || level.isClientSide || getSpeed() != 0 || !state.repair()) return false;
        setChanged();
        sendData();
        return true;
    }

    public IFluidHandler fluidPort(Direction side) {
        if (!isMaterialSide(side) || isRemoved()) return null;
        return new PortFluidHandler(side);
    }

    public IItemHandler itemPort(Direction side) {
        if (!isMaterialSide(side) || isRemoved()) return null;
        return new PortItemHandler(side);
    }

    /** 手工桶事务先模拟两端，再提交单个整桶；失败时原手持栈和机器均不改变。 */
    public void transferHeldBucket(Player player, InteractionHand hand, Direction side) {
        if (level == null || level.isClientSide) return;
        IFluidHandler port = fluidPort(side);
        ItemStack held = player.getItemInHand(hand);
        if (port == null || held.isEmpty()) return;
        int extraSlot = held.getCount() > 1 ? player.getInventory().getFreeSlot() : -1;
        if (held.getCount() > 1 && extraSlot < 0) return;
        ItemStack copy = held.copyWithCount(1);
        var item = copy.getCapability(Capabilities.FluidHandler.ITEM);
        if (item == null) return;
        ItemStack result = CentrifugeContainerTransaction.transfer(port, item);
        if (!result.isEmpty()) returnContainer(player, hand, held, result, extraSlot);
    }

    private static void returnContainer(Player player, InteractionHand hand, ItemStack held,
                                        ItemStack result, int extraSlot) {
        if (held.getCount() == 1) {
            player.setItemInHand(hand, result);
        } else {
            held.shrink(1);
            player.getInventory().setItem(extraSlot, result);
        }
    }

    /** 空手从任一物料面收取该面当前允许的两种粉末；满背包时沿玩家原生返物路径处理。 */
    public void extractOutputsToPlayer(Player player, Direction side) {
        if (level == null || level.isClientSide || !isCurrentMaterialPort(side)) return;
        IItemHandler port = new PortItemHandler(side);
        for (int slot = 0; slot < 2; slot++) {
            ItemStack extracted = port.extractItem(slot, Integer.MAX_VALUE, false);
            if (!extracted.isEmpty()) player.getInventory().placeItemBackInInventory(extracted);
        }
    }

    /** 携物快照不包含 Create 的旧轴网；用于单件掉落和重新放置。 */
    public CompoundTag savePortableData() {
        CompoundTag tag = new CompoundTag();
        HolderLookup.Provider registries = level == null ? net.minecraft.core.RegistryAccess.EMPTY : level.registryAccess();
        writeState(tag, registries);
        if (filtering != null) {
            CompoundTag filters = new CompoundTag();
            filtering.write(filters, registries, false);
            tag.put("CentrifugeFilters", filters);
        }
        return tag;
    }

    void readPortableFilters(CompoundTag tag, HolderLookup.Provider registries) {
        if (filtering == null) return;
        for (Direction side : Direction.values()) {
            if (isMaterialSide(side)) filtering.setFilter(side, ItemStack.EMPTY);
        }
        if (tag.contains("CentrifugeFilters"))
            filtering.read(tag.getCompound("CentrifugeFilters"), registries, false);
    }

    public void loadPortableData(CompoundTag tag) {
        if (level != null && !level.isClientSide) {
            readState(tag, level.registryAccess());
            readPortableFilters(tag, level.registryAccess());
            setChanged();
            sendData();
        }
    }

    @Override protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        writeState(tag, registries);
    }

    @Override protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        readState(tag, registries);
    }

    /** 版本化保存所有库存和密闭结果；Batch 字段不能只保存配方 ID。 */
    private void writeState(CompoundTag tag, HolderLookup.Provider registries) {
        state.write(tag, registries);
    }

    /** 对损坏的旧/外部 NBT 做有界恢复；不凭空重建缺失的已扣输入批次。 */
    private void readState(CompoundTag tag, HolderLookup.Provider registries) {
        state.read(tag, registries);
    }

    private boolean isCurrentMaterialPort(Direction side) {
        if (!isMaterialSide(side) || isRemoved()
                || getBlockState().getBlock() != FuelProcessingContent.ENRICHMENT_CENTRIFUGE.get()) return false;
        return level == null || level.getBlockEntity(worldPosition) == this;
    }

    private FluidStack storedFluid(int tank) {
        return switch (tank) {
            case 0 -> state.slurryMb == 0 ? FluidStack.EMPTY
                    : new FluidStack(FuelProcessingContent.URANIUM_SLURRY.get(), state.slurryMb);
            case 1 -> state.waterMb == 0 ? FluidStack.EMPTY : new FluidStack(Fluids.WATER, state.waterMb);
            default -> FluidStack.EMPTY;
        };
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean sneaking) {
        tooltip.add(getDisplayName());
        tooltip.add(Component.translatable("gui.create_nuclear_industry.centrifuge.slurry", state.slurryMb));
        tooltip.add(Component.translatable("gui.create_nuclear_industry.centrifuge.water", state.waterMb));
        tooltip.add(Component.translatable("gui.create_nuclear_industry.centrifuge.enriched", state.enriched.getCount()));
        tooltip.add(Component.translatable("gui.create_nuclear_industry.centrifuge.depleted", state.depleted.getCount()));
        tooltip.add(Component.translatable("gui.create_nuclear_industry.centrifuge.progress",
                state.batch == null ? 0 : Math.round(state.progress * 100 / state.batch.work())));
        tooltip.add(Component.translatable("gui.create_nuclear_industry.centrifuge.speed", Math.round(getSpeed())));
        tooltip.add(Component.translatable("gui.create_nuclear_industry.centrifuge.bearing",
                (CentrifugeState.WEAR_LIMIT - state.wearUnits) * 100 / CentrifugeState.WEAR_LIMIT));
        tooltip.add(Component.translatable("gui.create_nuclear_industry.centrifuge.pause." + pauseReason().name().toLowerCase()));
        return true;
    }

    public Component getDisplayName() {
        return Component.translatable("block.create_nuclear_industry.enrichment_centrifuge");
    }

    private final class PortFluidHandler implements IFluidHandler {
        private final Direction side;
        private PortFluidHandler(Direction side) { this.side = side; }
        private boolean valid() { return isCurrentMaterialPort(side); }
        @Override public int getTanks() { return valid() ? 2 : 0; }
        @Override public FluidStack getFluidInTank(int tank) {
            if (!valid()) return FluidStack.EMPTY;
            FluidStack stored = storedFluid(tank);
            return stored.isEmpty() || !fluidMatchesFilter(side, stored) ? FluidStack.EMPTY : stored;
        }
        @Override public int getTankCapacity(int tank) {
            return valid() && (tank == 0 || tank == 1) ? CentrifugeState.TANK_CAPACITY : 0;
        }
        @Override public boolean isFluidValid(int tank, FluidStack stack) {
            return valid() && tank == 0 && stack.is(FuelProcessingContent.URANIUM_SLURRY.get())
                    && fluidMatchesFilter(side, stack);
        }
        @Override public int fill(FluidStack resource, FluidAction action) {
            if (!valid() || !isFluidValid(0, resource) || resource.isEmpty()) return 0;
            int accepted = Math.min(resource.getAmount(), CentrifugeState.TANK_CAPACITY - state.slurryMb);
            if (accepted > 0 && action.execute()) {
                state.slurryMb += accepted;
                setChanged(); sendData();
            }
            return accepted;
        }
        @Override public FluidStack drain(FluidStack resource, FluidAction action) {
            if (!valid() || resource.isEmpty() || !fluidMatchesFilter(side, resource)) return FluidStack.EMPTY;
            if (resource.is(Fluids.WATER)) return drainTank(1, resource.getAmount(), action);
            if (resource.is(FuelProcessingContent.URANIUM_SLURRY.get())) return drainTank(0, resource.getAmount(), action);
            return FluidStack.EMPTY;
        }
        @Override public FluidStack drain(int maxDrain, FluidAction action) {
            if (!valid() || maxDrain <= 0) return FluidStack.EMPTY;
            FluidStack water = storedFluid(1);
            if (!water.isEmpty() && fluidMatchesFilter(side, water)) return drainTank(1, maxDrain, action);
            FluidStack slurry = storedFluid(0);
            if (!slurry.isEmpty() && fluidMatchesFilter(side, slurry)) return drainTank(0, maxDrain, action);
            return FluidStack.EMPTY;
        }
        private FluidStack drainTank(int tank, int maxDrain, FluidAction action) {
            FluidStack stored = storedFluid(tank);
            if (maxDrain <= 0 || stored.isEmpty() || !fluidMatchesFilter(side, stored)) return FluidStack.EMPTY;
            int amount = Math.min(maxDrain, stored.getAmount());
            if (action.execute()) {
                if (tank == 0) state.slurryMb -= amount; else state.waterMb -= amount;
                setChanged(); sendData();
            }
            return new FluidStack(stored.getFluid(), amount);
        }
    }

    private final class PortItemHandler implements IItemHandler {
        private final Direction side;
        private PortItemHandler(Direction side) { this.side = side; }
        private boolean valid() { return isCurrentMaterialPort(side); }
        @Override public int getSlots() { return valid() ? 2 : 0; }
        @Override public ItemStack getStackInSlot(int slot) {
            if (!valid() || slot < 0 || slot > 1) return ItemStack.EMPTY;
            ItemStack stored = slot == 0 ? state.enriched : state.depleted;
            return stored.isEmpty() || !itemMatchesFilter(side, stored) ? ItemStack.EMPTY : stored.copy();
        }
        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) { return stack; }
        @Override public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (!valid() || slot < 0 || slot > 1 || amount <= 0) return ItemStack.EMPTY;
            ItemStack stored = slot == 0 ? state.enriched : state.depleted;
            if (stored.isEmpty() || !itemMatchesFilter(side, stored)) return ItemStack.EMPTY;
            ItemStack result = stored.copyWithCount(Math.min(amount, stored.getCount()));
            if (!simulate) {
                ItemStack remaining = stored.copy();
                remaining.shrink(result.getCount());
                if (slot == 0) state.enriched = remaining; else state.depleted = remaining;
                setChanged(); sendData();
            }
            return result;
        }
        @Override public int getSlotLimit(int slot) { return valid() && slot >= 0 && slot < 2 ? 64 : 0; }
        @Override public boolean isItemValid(int slot, ItemStack stack) { return false; }
    }
}
