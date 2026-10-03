package com.iksxh.create_nuclear_industry.production;

import com.iksxh.create_nuclear_industry.CreateNuclearIndustry;
import com.iksxh.create_nuclear_industry.content.BasicMaterialContent;
import com.iksxh.create_nuclear_industry.content.FuelProcessingContent;
import com.iksxh.create_nuclear_industry.content.ModItems;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * 八格装配台主控的服务端状态所有者。Create轴网只提供速度与过载判定；四料、成品和工时
 * 只写在同一个账本，客户端同步仅供渲染与护目镜展示，任何物料事务均在服务端完成。
 */
public final class ShieldedAssemblyBlockEntity extends KineticBlockEntity implements IHaveGoggleInformation {
    private static final TagKey<net.minecraft.world.item.Item> SOLDER = TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath("c", "ingots/solder"));
    private static final ResourceLocation RECIPE_ID = ResourceLocation.fromNamespaceAndPath(
            CreateNuclearIndustry.MOD_ID, "shielded_assembly/fresh_fuel_assembly");
    private final ShieldedAssemblyState state = new ShieldedAssemblyState();
    private boolean removalHandled;
    private UUID ownerId;

    public ShieldedAssemblyBlockEntity(BlockPos pos, BlockState blockState) {
        super(FuelProcessingContent.SHIELDED_ASSEMBLY_BE.get(), pos, blockState);
    }

    @Override public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        // 本机不用数值框；物料筛选由四个固定槽及外置Create物流完成。
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

    /** 配方重载与旧进度必须逐tick复核；无效配方只清工时，不能删除库存。 */
    @Override public void tick() {
        super.tick();
        if (level == null || level.isClientSide || !current()) return;
        boolean structure = complete();
        boolean recipe = structure && recipeReady();
        int oldProgress = state.progress();
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
        if (working && state.advance(getSpeed(), new ItemStack(ModItems.FRESH_FUEL_ASSEMBLY.get()))) {
            setChanged();
            if (state.progress() == 0 || oldProgress / 256 != state.progress() / 256) sendData();
        }
    }

    /** 数据包必须保持专用四料、正式新组件及25600 RPM·tick合同。 */
    public boolean recipeReady() {
        if (level == null) return false;
        var holder = level.getRecipeManager().byKey(RECIPE_ID);
        if (holder.isEmpty() || !(holder.get().value() instanceof ShieldedAssemblyRecipe recipe)) return false;
        if (recipe.work() != ShieldedAssemblyState.WORK
                || recipe.pellet().count() != 8 || recipe.cladding().count() != 4
                || recipe.solder().count() != 2 || recipe.grate().count() != 1
                || !recipe.result().is(ModItems.FRESH_FUEL_ASSEMBLY.get())
                || recipe.result().getCount() != 1 || recipe.result().getDamageValue() != 0
                || !recipe.pellet().test(new ItemStack(FuelProcessingContent.SINTERED_FUEL_PELLET.get()))
                || !recipe.cladding().test(new ItemStack(BasicMaterialContent.FUEL_CLADDING_TUBE.get()))
                || !recipe.solder().test(new ItemStack(BasicMaterialContent.SOLDER_INGOT.get()))
                || !recipe.grate().test(new ItemStack(BasicMaterialContent.STEEL_GRATE.get()))) return false;
        for (int slot = 0; slot < 4; slot++) {
            ItemStack stored = state.input(slot);
            if (stored.isEmpty()) continue;
            boolean matches = switch (slot) {
                case 0 -> recipe.pellet().test(stored);
                case 1 -> recipe.cladding().test(stored);
                case 2 -> recipe.solder().test(stored);
                default -> recipe.grate().test(stored);
            };
            if (!matches || !accepts(slot, stored)) return false;
        }
        return true;
    }

    public static boolean accepts(int slot, ItemStack stack) {
        if (stack.isEmpty()) return false;
        return switch (slot) {
            case 0 -> stack.is(FuelProcessingContent.SINTERED_FUEL_PELLET.get());
            case 1 -> stack.is(BasicMaterialContent.FUEL_CLADDING_TUBE.get());
            case 2 -> stack.is(SOLDER);
            case 3 -> stack.is(BasicMaterialContent.STEEL_GRATE.get());
            default -> false;
        };
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
        for (int slot = 0; slot < 4; slot++) if (accepts(slot, stack)) {
            ItemStack remainder = state.insert(slot, stack, false);
            if (remainder.getCount() != stack.getCount()) changed();
            return remainder;
        }
        return stack.copy();
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
        tooltip.add(Component.translatable("block.create_nuclear_industry.shielded_assembly_station"));
        for (int slot = 0; slot < 4; slot++)
            tooltip.add(Component.translatable("gui.create_nuclear_industry.shielded_assembly.input." + slot,
                    state.input(slot).getCount(), ShieldedAssemblyState.COST[slot]));
        tooltip.add(Component.translatable("gui.create_nuclear_industry.shielded_assembly.output",
                state.output().isEmpty() ? 0 : 1));
        tooltip.add(Component.translatable("gui.create_nuclear_industry.shielded_assembly.speed", Math.round(getSpeed())));
        tooltip.add(Component.translatable("gui.create_nuclear_industry.shielded_assembly.progress",
                state.progress(), ShieldedAssemblyState.WORK));
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
            if (!complete() || level == null || !level.hasChunkAt(portPos)) return false;
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
            ItemStack remainder = state.insert(slot, stack, simulate);
            if (!simulate && remainder.getCount() != stack.getCount()) changed();
            return remainder;
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
            return valid() && accepts(slot, stack);
        }
    }
}
