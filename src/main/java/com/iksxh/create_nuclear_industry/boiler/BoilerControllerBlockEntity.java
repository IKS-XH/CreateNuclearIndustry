package com.iksxh.create_nuclear_industry.boiler;

import com.iksxh.create_nuclear_industry.compat.create.SharedFluidReceiver;
import com.iksxh.create_nuclear_industry.content.BoilerContent;
import com.iksxh.create_nuclear_industry.config.BoilerConfig;
import com.iksxh.create_nuclear_industry.heat.NuclearHeatExchangerBlockEntity;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.fluids.FluidPropagator;
import com.simibubi.create.content.fluids.FluidTransportBehaviour;
import com.simibubi.create.content.fluids.pipes.FluidPipeBlock;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.capabilities.Capabilities;

/**
 * 唯一持有水、汽、炉体热与排放账本的服务端控制器。
 * 端口只有逐次复核结构的代理能力，客户端只接收同步快照。
 */
public final class BoilerControllerBlockEntity extends BlockEntity implements IHaveGoggleInformation {
    private final BoilerState ledger = new BoilerState();
    private final BoilerSteamPressure steamPressure;
    private int epoch, sectionCount, paidView;
    private boolean available, formed, stopped;
    private boolean formDirty = true;
    private boolean portPipeDirty = true;
    private BoilerStructure.Form cachedForm;
    private final Set<BlockPos> pendingPortPipeCenters = new HashSet<>();
    private final Map<Long, Object> portFlowIdentities = new HashMap<>();
    private String status = "unformed";
    private int viewWaterCapacity = BoilerState.CAPACITY, viewSteamCapacity = BoilerState.CAPACITY;
    private double viewWarmPerSection = BoilerState.WARM_HU_PER_SECTION;

    public BoilerControllerBlockEntity(BlockPos pos, BlockState state) {
        super(BoilerContent.CONTROLLER_BE.get(), pos, state);
        steamPressure = new BoilerSteamPressure(pos);
    }
    public BoilerState ledger() { return ledger; }
    public boolean current() {
        return available && level != null && !isRemoved() && level.hasChunkAt(worldPosition)
                && level.getBlockEntity(worldPosition) == this && canTick();
    }

    private boolean canTick() {
        if (!(level instanceof ServerLevel server)) return level != null && level.isClientSide;
        var chunk = server.getChunkSource().getChunkNow(worldPosition.getX() >> 4, worldPosition.getZ() >> 4);
        return chunk != null && server.shouldTickBlocksAt(worldPosition)
                && chunk.getFullStatus().isOrAfter(FullChunkStatus.BLOCK_TICKING)
                && server.areEntitiesLoaded(ChunkPos.asLong(worldPosition))
                && server.getWorldBorder().isWithinBounds(worldPosition);
    }

    /** 邻块变动、卸载或放回时撤销旧归属与能力，下一次查询才重验全结构。 */
    public void invalidateForm() {
        BoilerStructure.Form old = cachedForm;
        steamPressure.release();
        portPipeDirty = true;
        if (old != null) pendingPortPipeCenters.add(old.center());
        formDirty = true;
        cachedForm = null;
        epoch++;
        if (level != null && !level.isClientSide) {
            BoilerStructure.forget(level, this);
            if (level.hasChunkAt(worldPosition)) level.invalidateCapabilities(worldPosition);
            if (old != null) {
                for (BlockPos port : old.waterPorts()) if (level.hasChunkAt(port)) level.invalidateCapabilities(port);
                for (BlockPos port : old.steamPorts()) if (level.hasChunkAt(port)) level.invalidateCapabilities(port);
            }
            // 成型前曾缓存空能力的口也须被通知重新查询，限于本结构侧面的候选格。
            if (level.hasChunkAt(worldPosition) && getBlockState().hasProperty(BoilerPartBlock.FACING)) {
                BlockPos center = worldPosition.relative(getBlockState().getValue(BoilerPartBlock.FACING).getOpposite(), 2);
                for (int y : new int[]{0, 2}) for (Direction side : Direction.Plane.HORIZONTAL) {
                    Direction tangent = side.getAxis() == Direction.Axis.X ? Direction.SOUTH : Direction.EAST;
                    for (int offset = -1; offset <= 1; offset++) {
                        BlockPos port = center.above(y).relative(side, 2).relative(tangent, offset);
                        if (level.hasChunkAt(port)) level.invalidateCapabilities(port);
                    }
                }
            }
        }
    }

    /** 正常 tick 直接读取已验证结构；只有被标记失效时重扫并检查共享占位。 */
    public BoilerStructure.Form currentForm() {
        if (!current()) return null;
        if (!formDirty && cachedForm != null) {
            BlockPos center = cachedForm.center();
            boolean invalid = !getBlockState().hasProperty(BoilerPartBlock.FACING)
                    || !worldPosition.relative(getBlockState().getValue(BoilerPartBlock.FACING).getOpposite(), 2)
                    .below().equals(center)
                    || !level.hasChunkAt(center.offset(-2, 0, -2))
                    || !level.hasChunkAt(center.offset(-2, 0, 2))
                    || !level.hasChunkAt(center.offset(2, 0, -2))
                    || !level.hasChunkAt(center.offset(2, 0, 2));
            // 内部阻塞不一定触发本模组壳体事件，故每次只复核27格空气。
            if (!invalid) for (int y = 1; y <= 3 && !invalid; y++)
                for (int x = -1; x <= 1 && !invalid; x++)
                    for (int z = -1; z <= 1; z++)
                        if (!level.getBlockState(center.offset(x, y, z)).isAir()) invalid = true;
            if (invalid || !BoilerStructure.specialPartsStillMatch(level, cachedForm)) invalidateForm();
        }
        if (!formDirty) return cachedForm;
        formDirty = false;
        BoilerStructure.Form form = BoilerStructure.inspect(level, worldPosition);
        cachedForm = form != null && BoilerStructure.unique(level, worldPosition, form) ? form : null;
        if (cachedForm != null) BoilerStructure.cache(level, this, cachedForm);
        return cachedForm;
    }

    /** 结构/水量/汽空位均由本机决定需求；每段只从正下方已加载源领取一次实付 HU。 */
    public static void serverTick(Level level, BlockPos pos, BlockState state, BoilerControllerBlockEntity owner) {
        if (level.isClientSide || !owner.current()) return;
        owner.ledger.setSettings(BoilerConfig.settings());
        long now = level.getGameTime();
        BoilerStructure.Form form = owner.currentForm();
        if (owner.portPipeDirty) {
            owner.portPipeDirty = false;
            if (form != null) owner.pendingPortPipeCenters.add(form.center());
            else if (state.hasProperty(BoilerPartBlock.FACING)) owner.pendingPortPipeCenters.add(
                    pos.relative(state.getValue(BoilerPartBlock.FACING).getOpposite(), 2).below());
            owner.refreshPortPipeConnections();
        }
        owner.formed = form != null;
        if (form != null && owner.sectionCount != form.sections().size()) {
            owner.sectionCount = form.sections().size();
            owner.ledger.sectionsChanged(owner.sectionCount);
        }
        owner.stopped = level.hasNeighborSignal(pos);
        owner.ledger.prepare(now, owner.sectionCount);
        double demand = form == null || owner.stopped ? 0 : owner.ledger.demand(form.sections().size());
        double paid = 0;
        if (demand > 0) for (BlockPos segment : form.sections()) {
            if (paid >= demand) break;
            BlockPos source = segment.below();
            if (!level.hasChunkAt(source) || !(level.getBlockEntity(source) instanceof NuclearHeatExchangerBlockEntity machine)) continue;
            paid += machine.claimBoilerHeat(owner, Math.min(owner.ledger.settings().sectionHeatHuPerTick(), demand - paid));
        }
        owner.paidView = (int) Math.round(paid);
        boolean clear = form != null && level.hasChunkAt(form.valve().above())
                && level.getBlockState(form.valve().above()).isAir();
        owner.ledger.tick(now, owner.sectionCount, paid, clear, owner.stopped);
        owner.pushAdjacentSteam(form, now);
        owner.steamPressure.refresh(level, form, owner.ledger.steam() > 0 && owner.ledger.settings().valid(),
                owner.ledger.settings().portFlowMbPerTick());
        if (clear && owner.ledger.vented() > 0 && level instanceof ServerLevel server) {
            BlockPos mouth = form.valve().above();
            server.sendParticles(ParticleTypes.CLOUD, mouth.getX() + 0.5, mouth.getY() + 0.15,
                    mouth.getZ() + 0.5, Math.min(4, Math.max(1, owner.ledger.vented() / 64)),
                    0.12, 0.08, 0.12, 0.01);
        }
        owner.status = !owner.ledger.settings().valid() ? "invalid" : form == null ? "unformed" : owner.stopped ? "redstone"
                : owner.ledger.valveBlocked() ? "vent_blocked"
                : owner.ledger.water() == 0 ? "no_water"
                : owner.ledger.steam() >= owner.ledger.settings().steamCapacityMb() ? "steam_full"
                : paid == 0 ? "no_heat" : owner.ledger.ready() ? "running" : "warming";
        owner.setChanged();
        if (now % 5 == 0 || owner.ledger.produced() > 0 || owner.ledger.vented() > 0) owner.sync();
    }

    /** 两层端口成型或拆除后重算相邻 Create 管面，避免先铺管在能力失效后保持封闭。 */
    private void refreshPortPipeConnections() {
        for (BlockPos center : pendingPortPipeCenters) {
            for (int layer : new int[]{1, 3}) for (Direction side : Direction.Plane.HORIZONTAL) {
                // 先取外法线，再沿面内切线枚举三格，南北面偏移不能用x坐标推法线。
                Direction tangent = side.getAxis() == Direction.Axis.X ? Direction.SOUTH : Direction.EAST;
                for (int offset = -1; offset <= 1; offset++) {
                    BlockPos pipePos = center.above(layer).relative(side, 3).relative(tangent, offset);
                    if (!level.hasChunkAt(pipePos)) continue;
                    BlockState pipeState = level.getBlockState(pipePos);
                    if (pipeState.getBlock() instanceof FluidPipeBlock pipe) {
                        BlockState refreshed = pipe.updateBlockState(pipeState, side.getOpposite(), null, level, pipePos);
                        if (refreshed != pipeState) level.setBlock(pipePos, refreshed, 3);
                        pipeState = refreshed;
                    }
                    if (BlockEntityBehaviour.get(level, pipePos, FluidTransportBehaviour.TYPE) != null)
                        FluidPropagator.propagateChangedPipe(level, pipePos, pipeState);
                }
            }
        }
        pendingPortPipeCenters.clear();
    }

    /**
     * 邻接非管道的流体容器不会主动抽汽，故控制器沿汽口外向面推送。
     * 先由目标模拟确认，再执行目标写入，最后按实际接收量扣共享汽账；Create 管网仍走原生压力流。
     */
    private void pushAdjacentSteam(BoilerStructure.Form form, long now) {
        if (form == null || ledger.steam() == 0) return;
        boolean moved = false;
        for (BlockPos port : form.steamPorts()) {
            int availableSteam = ledger.remainingDrain(port, now);
            if (availableSteam <= 0) continue;
            Direction outward = level.getBlockState(port).getValue(BoilerPartBlock.FACING);
            BlockPos targetPos = port.relative(outward);
            if (!level.hasChunkAt(targetPos) || FluidPropagator.getPipe(level, targetPos) != null) continue;
            IFluidHandler target = level.getCapability(Capabilities.FluidHandler.BLOCK,
                    targetPos, outward.getOpposite());
            if (target == null) continue;
            FluidStack offered = new FluidStack(BoilerContent.SUPERCRITICAL_STEAM.get(), availableSteam);
            int simulated = target.fill(offered, IFluidHandler.FluidAction.SIMULATE);
            if (simulated <= 0) continue;
            int accepted = target.fill(new FluidStack(BoilerContent.SUPERCRITICAL_STEAM.get(),
                    Math.min(simulated, availableSteam)), IFluidHandler.FluidAction.EXECUTE);
            if (accepted > 0) {
                ledger.drainSteam(port, Math.min(accepted, availableSteam), false, now);
                moved = true;
            }
        }
        if (moved) setChanged();
    }

    /** 端口侧仅允许朝外且结构完整；缓存句柄带 epoch，卸载后不可复活。 */
    public IFluidHandler port(BlockPos part, Direction side, boolean input) {
        if (side == null || !level.hasChunkAt(part)) return null;
        if (!level.isClientSide) ledger.setSettings(BoilerConfig.settings());
        BlockState state = level.getBlockState(part);
        if (!state.hasProperty(BoilerPartBlock.FACING) || state.getValue(BoilerPartBlock.FACING) != side) return null;
        return new Port(part.immutable(), side, input);
    }

    private final class Port implements IFluidHandler, SharedFluidReceiver {
        private final BlockPos part;
        private final Direction side;
        private final boolean input;
        private final int issuedEpoch = epoch;
        Port(BlockPos part, Direction side, boolean input) {
            this.part = part; this.side = side; this.input = input;
        }
        private boolean valid() {
            if (issuedEpoch != epoch || !current() || level.isClientSide) return false;
            BoilerStructure.Form form = currentForm();
            if (form == null || !(input ? form.waterPorts() : form.steamPorts()).contains(part)) return false;
            BlockState state = level.getBlockState(part);
            return state.hasProperty(BoilerPartBlock.FACING) && state.getValue(BoilerPartBlock.FACING) == side;
        }
        @Override public Limits sharedFluidLimits() {
            if (!valid() || !input) return null;
            int remaining = ledger.remainingFill(part, level.getGameTime());
            Object budget = portFlowIdentities.computeIfAbsent(part.asLong(), ignored -> new Object());
            return new Limits(ledger, Math.max(0, ledger.settings().waterCapacityMb() - ledger.water()), budget, remaining);
        }
        @Override public int getTanks() { return valid() ? 1 : 0; }
        @Override public FluidStack getFluidInTank(int tank) {
            if (!valid() || tank != 0) return FluidStack.EMPTY;
            int amount = input ? ledger.water() : ledger.steam();
            return amount == 0 ? FluidStack.EMPTY : new FluidStack(input
                    ? net.minecraft.world.level.material.Fluids.WATER : BoilerContent.SUPERCRITICAL_STEAM.get(), amount);
        }
        @Override public int getTankCapacity(int tank) {
            return valid() && tank == 0 ? (input ? ledger.settings().waterCapacityMb()
                    : ledger.settings().steamCapacityMb()) : 0;
        }
        @Override public boolean isFluidValid(int tank, FluidStack stack) {
            return valid() && input && tank == 0 && stack.is(net.minecraft.world.level.material.Fluids.WATER);
        }
        @Override public int fill(FluidStack stack, FluidAction action) {
            if (!isFluidValid(0, stack)) return 0;
            int accepted = ledger.fillWater(part, stack.getAmount(), action.simulate(), level.getGameTime());
            if (accepted > 0 && action.execute()) changed();
            return accepted;
        }
        @Override public FluidStack drain(FluidStack stack, FluidAction action) {
            return BoilerContent.isSteam(stack) ? drain(stack.getAmount(), action) : FluidStack.EMPTY;
        }
        @Override public FluidStack drain(int amount, FluidAction action) {
            if (!valid() || input) return FluidStack.EMPTY;
            int taken = ledger.drainSteam(part, amount, action.simulate(), level.getGameTime());
            if (taken > 0 && action.execute()) changed();
            return taken == 0 ? FluidStack.EMPTY : new FluidStack(BoilerContent.SUPERCRITICAL_STEAM.get(), taken);
        }
    }

    private void changed() { setChanged(); sync(); }
    private void sync() {
        if (level != null && !level.isClientSide)
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }
    public CompoundTag savePortableData() {
        var tag = new CompoundTag();
        tag.put("Ledger", ledger.save());
        tag.putInt("Sections", sectionCount);
        return tag;
    }
    public void loadPortableData(CompoundTag data) {
        ledger.load(data.getCompound("Ledger"));
        if (level != null && !level.isClientSide) ledger.setSettings(BoilerConfig.settings());
        sectionCount = Math.clamp(data.getInt("Sections"), 0, 9);
        changed();
    }
    @Override public void onLoad() {
        super.onLoad(); available = true;
        if (level != null && !level.isClientSide) ledger.setSettings(BoilerConfig.settings());
        invalidateForm();
    }
    @Override public void onChunkUnloaded() { available = false; invalidateForm(); super.onChunkUnloaded(); }
    @Override public void setRemoved() { available = false; invalidateForm(); super.setRemoved(); }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Boiler", ledger.save());
        tag.putInt("BoilerSections", sectionCount);
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ledger.load(tag.getCompound("Boiler"));
        sectionCount = Math.clamp(tag.getInt("BoilerSections"), 0, 9);
        if (tag.contains("View")) readView(tag.getCompound("View"));
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        var tag = super.getUpdateTag(registries);
        tag.put("Boiler", ledger.save());
        var view = new CompoundTag();
        view.putBoolean("Formed", formed); view.putBoolean("Stopped", stopped);
        view.putInt("Sections", sectionCount); view.putInt("Paid", paidView);
        view.putInt("WaterCapacity", ledger.settings().waterCapacityMb());
        view.putInt("SteamCapacity", ledger.settings().steamCapacityMb());
        view.putDouble("WarmPerSection", ledger.settings().warmHuPerSection());
        view.putString("Status", status);
        tag.put("View", view);
        return tag;
    }
    private void readView(CompoundTag view) {
        formed = view.getBoolean("Formed"); stopped = view.getBoolean("Stopped");
        sectionCount = view.getInt("Sections"); paidView = view.getInt("Paid");
        viewWaterCapacity = view.contains("WaterCapacity") ? view.getInt("WaterCapacity") : BoilerState.CAPACITY;
        viewSteamCapacity = view.contains("SteamCapacity") ? view.getInt("SteamCapacity") : BoilerState.CAPACITY;
        viewWarmPerSection = view.contains("WarmPerSection") ? view.getDouble("WarmPerSection") : BoilerState.WARM_HU_PER_SECTION;
        status = view.getString("Status");
    }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet, HolderLookup.Provider registries) {
        super.onDataPacket(connection, packet, registries);
        if (packet.getTag() != null) readView(packet.getTag().getCompound("View"));
    }

    @Override public boolean addToGoggleTooltip(List<Component> tooltip, boolean sneaking) {
        String key = "gui.create_nuclear_industry.boiler.";
        tooltip.add(Component.translatable("block.create_nuclear_industry.high_pressure_boiler_controller"));
        tooltip.add(Component.translatable(key + "state." + status));
        tooltip.add(Component.translatable(key + "tanks", ledger.water(), viewWaterCapacity,
                ledger.steam(), viewSteamCapacity));
        tooltip.add(Component.translatable(key + "warm", (int) ledger.warmHu(),
                (int) Math.round(sectionCount * viewWarmPerSection)));
        tooltip.add(Component.translatable(key + "flow", paidView, ledger.produced()));
        tooltip.add(Component.translatable(key + "pressure", ledger.steam() * 100 / Math.max(1, viewSteamCapacity)));
        tooltip.add(Component.translatable(key + "vent", ledger.vented(), ledger.totalVented()));
        return true;
    }
}
