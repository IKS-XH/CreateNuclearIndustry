package com.iksxh.create_nuclear_industry.turbine;

import com.iksxh.create_nuclear_industry.config.TurbineConfig;
import com.iksxh.create_nuclear_industry.content.BoilerContent;
import com.iksxh.create_nuclear_industry.content.TurbineContent;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.fluids.FluidPropagator;
import com.simibubi.create.content.fluids.FluidTransportBehaviour;
import com.simibubi.create.content.fluids.pipes.FluidPipeBlock;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.infrastructure.config.AllConfigs;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * 汽轮机唯一服务端 owner，持有两份 mB 库存、成型关系与总 SU；后轴和各端口仅代理读取。
 * tick 先核验全结构和当前配置，再成交流体、发布双轴份额；客户端只接收显示快照。
 */
public final class TurbineControllerBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {
    private final TurbineState ledger = new TurbineState();
    private final TurbineSteamPressure steamPressure;
    private TurbineStructure.Form activeForm;
    private int epoch, lastLength, lastDiameter, viewInput, viewExhaust, viewInputCapacity, viewExhaustCapacity;
    private BlockPos lastFront;
    private Direction lastFacing = Direction.NORTH;
    private int viewProcessed, viewRatedFlow, viewRpm;
    private long nextProbeTick;
    private boolean legacyKineticMigrationPending;
    private float viewTotalSu, viewFrontSu, viewRearSu;
    private String status = "unformed";

    public TurbineControllerBlockEntity(BlockPos pos, BlockState state) {
        super(TurbineContent.CONTROLLER_BE.get(), pos, state);
        steamPressure = new TurbineSteamPressure(pos);
    }

    @Override public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        // 侧控制器不提供 Create 动力，运行许可及库存只由服务端账本决定。
    }

    public TurbineState ledger() { return ledger; }
    public TurbineStructure.Form currentForm() { return activeForm; }
    public TurbineStructure.Form claimedForm() { return live() ? activeForm : null; }

    /** 构件变化或区块卸载先撤销历史 SU 与旧句柄，再让下次 tick 重扫。 */
    public void invalidateForm() {
        TurbineStructure.Form old = activeForm;
        activeForm = null;
        ledger.stop();
        epoch++;
        steamPressure.release();
        if (level != null && !level.isClientSide) {
            TurbineStructure.forget(level, this);
            // 方块移除回调可能仍在写世界；旧外观留给下一 owner tick 统一撤销，避免重入 setBlock。
            if (old != null) for (BlockPos port : allPorts(old)) if (level.hasChunkAt(port)) level.invalidateCapabilities(port);
            sendData();
        }
    }

    private static List<BlockPos> allPorts(TurbineStructure.Form form) {
        List<BlockPos> ports = new ArrayList<>(form.inlets());
        ports.addAll(form.exhausts());
        return ports;
    }

    @Override public void tick() {
        if (level != null && !level.isClientSide) serverTick();
        super.tick();
    }

    private void serverTick() {
        if (legacyKineticMigrationPending) clearLegacyKineticSource();
        long now = level.getGameTime();
        if (!getBlockState().is(TurbineContent.CONTROLLER.get())
                || !TurbineStructure.ticking(level, worldPosition)) {
            if (activeForm != null) invalidateForm();
            ledger.tick(now, false);
            return;
        }
        TurbineState.Settings settings = TurbineConfig.settings();
        int maxRpm = AllConfigs.server().kinetics.maxRotationSpeed.get();
        if (activeForm != null) {
            TurbineState.Tier configured = settings.tierForRotors(activeForm.rotors());
            if (configured == null || configured.diameter() != activeForm.diameter()
                    || !TurbineStructure.quickLive(level, activeForm)) invalidateForm();
        }
        if (activeForm == null && now < nextProbeTick) {
            ledger.applySettings(settings, lastLength > 0 ? lastLength - 2 : 0, maxRpm);
            ledger.tick(now, false);
            return;
        }
        TurbineStructure.Form found = activeForm != null ? activeForm
                : TurbineStructure.inspect(level, worldPosition, settings);
        if (found == null || !settings.valid(maxRpm)) {
            if (activeForm != null) invalidateForm();
            nextProbeTick = now + 10;
            clearSavedAppearance();
            ledger.applySettings(settings, lastLength > 0 ? lastLength - 2 : 0, maxRpm);
            ledger.tick(now, false);
            status = !settings.valid(maxRpm) ? "invalid_config" : "unformed";
            refreshView();
            if (now % 5 == 0) sendData();
            return;
        }
        ledger.applySettings(settings, found.rotors(), maxRpm);
        if (!TurbineStructure.unique(level, this, found)) {
            if (activeForm != null) invalidateForm();
            nextProbeTick = now + 10;
            clearSavedAppearance();
            ledger.tick(now, false);
            status = "overlap";
            refreshView(); sendData();
            return;
        }
        boolean changedForm = activeForm == null || activeForm.rotors() != found.rotors()
                || activeForm.diameter() != found.diameter()
                || !activeForm.front().equals(found.front())
                || !activeForm.rear().equals(found.rear()) || !activeForm.inlets().equals(found.inlets())
                || !activeForm.exhausts().equals(found.exhausts());
        if (changedForm) {
            if (activeForm != null) invalidateForm();
            if (lastLength > 0 && (lastLength != found.length()
                    || lastDiameter != found.diameter() || !found.front().equals(lastFront)))
                clearSavedAppearance();
            if ((lastLength != found.length() || lastDiameter != found.diameter())
                    && !ledger.canFormForNewTier(found.rotors())) {
                ledger.tick(now, false);
                status = "stock_over_capacity";
                refreshView(); sendData();
                return;
            }
            activeForm = found;
            lastLength = found.length();
            lastDiameter = found.diameter();
            lastFront = found.front();
            lastFacing = found.facing();
            epoch++;
            TurbineStructure.claim(level, this);
            style(found, true);
            for (BlockPos port : allPorts(found)) level.invalidateCapabilities(port);
            refreshPipeConnections(found);
        } else {
            activeForm = found;
            TurbineStructure.claim(level, this);
        }
        boolean redstone = level.hasNeighborSignal(worldPosition);
        ledger.tick(now, !redstone);
        if (redstone) steamPressure.release();
        else {
            pushAdjacentSteam(found, now);
            steamPressure.refresh(level, found, ledger.exhaust() > 0,
                    settings.exhaustPortFlowMbPerTick());
        }
        status = redstone ? "redstone" : ledger.processed() > 0 ? "running"
                : ledger.exhaust() >= ledger.exhaustCapacity() ? "exhaust_full" : "no_steam";
        refreshView();
        setChanged();
        if (changedForm || now % 5 == 0 || ledger.processed() > 0) sendData();
    }

    /**
     * 旧版控制器本身是 Create 动力源。新 SmartBlockEntity 不读取旧 Speed/Network，
     * 但邻接轴的 Source 可能仍指向这里；只切断该旧源的分支，保留其他原生动力源。
     * 邻区块未加载时保留迁移标记，待六面均可检查后才一次性清除。
     */
    private void clearLegacyKineticSource() {
        boolean allLoaded = true;
        for (Direction face : Direction.values()) {
            BlockPos adjacent = worldPosition.relative(face);
            if (!level.hasChunkAt(adjacent)) {
                allLoaded = false;
                continue;
            }
            if (level.getBlockEntity(adjacent) instanceof KineticBlockEntity kinetic
                    && worldPosition.equals(kinetic.source)) {
                kinetic.detachKinetics();
                kinetic.removeSource();
                kinetic.sendData();
            }
        }
        if (allLoaded) {
            legacyKineticMigrationPending = false;
            setChanged();
        }
    }

    /** 成型外观由权威几何表写入；清理时只触及同朝向的已成型构件。 */
    private void style(TurbineStructure.Form form, boolean formed) {
        style(form, formed, null);
    }

    private void style(TurbineStructure.Form form, boolean formed, BlockPos skip) {
        for (int z = 0; z < form.length(); z++) {
            TurbineGeometry.Section section = z == 0 ? TurbineGeometry.Section.FRONT
                    : z == form.length() - 1 ? TurbineGeometry.Section.REAR
                    : TurbineGeometry.Section.MIDDLE;
            for (TurbineGeometry.Piece piece : TurbineGeometry.pieces()) {
                if (piece.diameter() != form.diameter() || piece.section() != section) continue;
                BlockPos pos = TurbineStructure.at(form.front(), form.facing(), piece.x(), piece.y(), z);
                stylePart(pos, form, piece, z, formed, skip);
            }
            BlockPos axis = TurbineStructure.at(form.front(), form.facing(), 0, 0, z);
            stylePart(axis, form, null, z, formed, skip);
        }
    }

    private void stylePart(BlockPos pos, TurbineStructure.Form form, TurbineGeometry.Piece piece,
                           int z, boolean formed, BlockPos skip) {
        if (pos.equals(skip) || !level.hasChunkAt(pos)) return;
        BlockState before = level.getBlockState(pos);
        if (!isTurbinePart(before)) return;
        if (!formed && (!before.getValue(TurbinePartBlock.FORMED)
                || before.getValue(TurbinePartBlock.MACHINE_FACING) != form.facing())) return;
        BlockState after = before.setValue(TurbinePartBlock.FORMED, formed);
        if (formed) {
            after = after.setValue(TurbinePartBlock.MACHINE_FACING, form.facing());
            if (before.is(TurbineContent.CASING.get()) || before.is(TurbineContent.WINDOW.get()))
                after = after.setValue(TurbinePartBlock.PIECE, piece.id());
            if (before.is(TurbineContent.ROTOR.get()))
                after = after.setValue(TurbinePartBlock.DIAMETER,
                        TurbinePartBlock.Diameter.of(form.diameter()));
            if (before.is(TurbineContent.OUTPUT_SHAFT.get()))
                after = after.setValue(TurbineShaftBlock.END,
                        z == 0 ? TurbineShaftBlock.End.FRONT : TurbineShaftBlock.End.REAR);
            if (before.is(TurbineContent.CONTROLLER.get()))
                after = after.setValue(TurbinePartBlock.SIDE,
                        TurbinePartBlock.Side.at(piece.x(), piece.y()));
            if (before.is(TurbineContent.INLET.get()) || before.is(TurbineContent.EXHAUST.get()))
                after = after.setValue(TurbinePartBlock.RING_ROLE, ringRole(piece.x(), piece.y()))
                        .setValue(TurbinePartBlock.OUTWARD,
                                TurbineStructure.outward(form.facing(), piece.x(), piece.y()));
        }
        if (after != before) level.setBlock(pos, after, 3);
    }

    private static TurbinePartBlock.RingRole ringRole(int x, int y) {
        return switch (TurbinePartBlock.Side.at(x, y)) {
            case UP -> TurbinePartBlock.RingRole.TOP;
            case DOWN -> TurbinePartBlock.RingRole.BOTTOM;
            case LEFT -> TurbinePartBlock.RingRole.LEFT;
            case RIGHT -> TurbinePartBlock.RingRole.RIGHT;
        };
    }

    /** 旧前端控制器缺失新几何 NBT 时，仅在旧 3×3 区域撤销残留 formed 提示。 */
    private void clearSavedAppearance() {
        if (lastLength < 5 || lastLength > 18 || level == null) return;
        if (lastFront != null && TurbineGeometry.supportedDiameter(lastDiameter)) {
            int radius = (lastDiameter - 1) / 2;
            for (int z = 0; z < lastLength; z++) for (int y = -radius; y <= radius; y++)
                for (int x = -radius; x <= radius; x++) {
                    if (!TurbineGeometry.footprint(lastDiameter, x, y)) continue;
                    clearStyleAt(TurbineStructure.at(lastFront, lastFacing, x, y, z));
                }
        } else {
            for (int z = 0; z < lastLength; z++) for (int y = -1; y <= 1; y++)
                for (int x = -1; x <= 1; x++)
                    clearStyleAt(TurbineStructure.at(worldPosition, lastFacing, x, y, z));
        }
    }

    private void clearStyleAt(BlockPos pos) {
        if (pos.equals(worldPosition) || !level.hasChunkAt(pos)) return;
        BlockState before = level.getBlockState(pos);
        if (isTurbinePart(before) && before.getValue(TurbinePartBlock.FORMED)
                && before.getValue(TurbinePartBlock.MACHINE_FACING) == lastFacing)
            level.setBlock(pos, before.setValue(TurbinePartBlock.FORMED, false), 3);
    }

    /** 控制器拆除清理其他已加载构件的显示状态，拆放 NBT 仍只携带一次库存。 */
    public void onControllerBroken(Direction oldFacing) {
        if (level != null && !level.isClientSide) {
            if (activeForm != null) style(activeForm, false, worldPosition);
            else clearSavedAppearance();
        }
        invalidateForm();
    }

    private static boolean isTurbinePart(BlockState state) {
        return state.is(TurbineContent.CASING.get()) || state.is(TurbineContent.WINDOW.get())
                || state.is(TurbineContent.ROTOR.get())
                || state.is(TurbineContent.CONTROLLER.get()) || state.is(TurbineContent.OUTPUT_SHAFT.get())
                || state.is(TurbineContent.INLET.get()) || state.is(TurbineContent.EXHAUST.get());
    }

    /** 已缓存结构仍校验全部区块票据和内腔，部分卸载或堵塞立即撤销两轴。 */
    private boolean live() {
        if (activeForm == null || level == null || level.isClientSide || isRemoved()
                || !getBlockState().is(TurbineContent.CONTROLLER.get())
                || !TurbineStructure.ticking(level, worldPosition)) return false;
        return TurbineStructure.quickLive(level, activeForm);
    }
    public boolean validShaft(BlockPos shaft, TurbineShaftBlock.End end) {
        BlockPos expected = end == TurbineShaftBlock.End.FRONT ? activeForm == null ? null
                : activeForm.front() : activeForm == null ? null : activeForm.rear();
        return expected != null && expected.equals(shaft) && live()
                && level.getBlockState(shaft).is(TurbineContent.OUTPUT_SHAFT.get())
                && level.getBlockState(shaft).getValue(TurbinePartBlock.MACHINE_FACING)
                == activeForm.facing()
                && level.getBlockState(shaft).getValue(TurbineShaftBlock.END) == end;
    }
    public boolean validPort(BlockPos port, boolean input) {
        return live() && (input ? activeForm.inlets() : activeForm.exhausts()).contains(port)
                && level.getBlockState(port).is(input ? TurbineContent.INLET.get() : TurbineContent.EXHAUST.get());
    }
    public float frontSu() { return live() && !level.hasNeighborSignal(worldPosition) ? (float) ledger.frontSu() : 0; }
    public float rearSu() { return live() && !level.hasNeighborSignal(worldPosition) ? (float) ledger.rearSu() : 0; }
    public float signedRpm() { return live() ? ledger.rpm() : 0; }

    /** 句柄带成型世代；卸载、拆件或换档后旧管路引用不能继续交易。 */
    public IFluidHandler port(BlockPos part, Direction side, boolean input) {
        if (!validPort(part, input) || level.getBlockState(part).getValue(TurbinePartBlock.OUTWARD) != side) return null;
        return new Port(part.immutable(), side, input);
    }
    private final class Port implements IFluidHandler {
        private final BlockPos part;
        private final Direction side;
        private final boolean input;
        private final int issuedEpoch = epoch;
        private Port(BlockPos part, Direction side, boolean input) {
            this.part = part; this.side = side; this.input = input;
        }
        private boolean valid() {
            return issuedEpoch == epoch && validPort(part, input)
                    && level.getBlockState(part).getValue(TurbinePartBlock.OUTWARD) == side;
        }
        @Override public int getTanks() { return valid() ? 1 : 0; }
        @Override public FluidStack getFluidInTank(int tank) {
            if (!valid() || tank != 0) return FluidStack.EMPTY;
            int amount = input ? ledger.input() : ledger.exhaust();
            return amount <= 0 ? FluidStack.EMPTY : new FluidStack(input
                    ? BoilerContent.SUPERCRITICAL_STEAM.get() : TurbineContent.STEAM.get(), amount);
        }
        @Override public int getTankCapacity(int tank) {
            return valid() && tank == 0 ? input ? ledger.inletCapacity() : ledger.exhaustCapacity() : 0;
        }
        @Override public boolean isFluidValid(int tank, FluidStack stack) {
            return valid() && input && tank == 0 && TurbineContent.isSupercritical(stack);
        }
        @Override public int fill(FluidStack stack, FluidAction action) {
            if (!isFluidValid(0, stack)) return 0;
            int accepted = ledger.fillInput(part.asLong(), stack.getAmount(), action.simulate(), level.getGameTime());
            if (accepted > 0 && action.execute()) changed();
            return accepted;
        }
        @Override public FluidStack drain(FluidStack stack, FluidAction action) {
            return TurbineContent.isOrdinarySteam(stack) ? drain(stack.getAmount(), action) : FluidStack.EMPTY;
        }
        @Override public FluidStack drain(int amount, FluidAction action) {
            if (!valid() || input) return FluidStack.EMPTY;
            int taken = ledger.drainExhaust(part.asLong(), amount, action.simulate(), level.getGameTime());
            if (taken > 0 && action.execute()) changed();
            return taken == 0 ? FluidStack.EMPTY : new FluidStack(TurbineContent.STEAM.get(), taken);
        }
    }

    /** 非管道容器没有主动抽取机制；执行目标 fill 后按实际接收量扣同一物理口预算。 */
    private void pushAdjacentSteam(TurbineStructure.Form form, long now) {
        for (BlockPos port : form.exhausts()) {
            int available = ledger.remainingExhaust(port.asLong(), now);
            if (available <= 0) continue;
            Direction outward = level.getBlockState(port).getValue(TurbinePartBlock.OUTWARD);
            BlockPos targetPos = port.relative(outward);
            if (!TurbineStructure.ticking(level, targetPos) || FluidPropagator.getPipe(level, targetPos) != null) continue;
            IFluidHandler target = level.getCapability(Capabilities.FluidHandler.BLOCK, targetPos, outward.getOpposite());
            if (target == null) continue;
            int simulated = target.fill(new FluidStack(TurbineContent.STEAM.get(), available), IFluidHandler.FluidAction.SIMULATE);
            if (simulated <= 0) continue;
            int accepted = target.fill(new FluidStack(TurbineContent.STEAM.get(), Math.min(simulated, available)),
                    IFluidHandler.FluidAction.EXECUTE);
            if (accepted > 0) ledger.drainExhaust(port.asLong(), Math.min(accepted, available), false, now);
        }
    }

    private void refreshPipeConnections(TurbineStructure.Form form) {
        for (BlockPos port : allPorts(form)) {
            Direction outward = level.getBlockState(port).getValue(TurbinePartBlock.OUTWARD);
            BlockPos pipePos = port.relative(outward);
            if (!TurbineStructure.ticking(level, pipePos)) continue;
            BlockState state = level.getBlockState(pipePos);
            if (state.getBlock() instanceof FluidPipeBlock pipe) {
                BlockState refreshed = pipe.updateBlockState(state, outward.getOpposite(), null, level, pipePos);
                if (refreshed != state) level.setBlock(pipePos, refreshed, 3);
                state = refreshed;
            }
            if (BlockEntityBehaviour.get(level, pipePos, FluidTransportBehaviour.TYPE) != null)
                FluidPropagator.propagateChangedPipe(level, pipePos, state);
        }
    }

    private void changed() { setChanged(); refreshView(); sendData(); }
    private void refreshView() {
        viewInput = ledger.input(); viewExhaust = ledger.exhaust();
        viewInputCapacity = ledger.inletCapacity(); viewExhaustCapacity = ledger.exhaustCapacity();
        viewProcessed = ledger.processed(); viewRatedFlow = ledger.ratedFlowMbPerTick(); viewRpm = ledger.rpm();
        viewTotalSu = (float) ledger.totalSu(); viewFrontSu = (float) ledger.frontSu(); viewRearSu = (float) ledger.rearSu();
    }

    public CompoundTag savePortableData() {
        CompoundTag tag = new CompoundTag();
        tag.put("Ledger", ledger.save());
        return tag;
    }
    public void loadPortableData(CompoundTag tag) {
        ledger.load(tag.getCompound("Ledger"));
        // 搬运只携库存与成交时间，不携旧机器坐标/外观归属。
        lastLength = 0;
        lastDiameter = 0;
        lastFront = null;
        invalidateForm();
        changed();
    }
    @Override public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) invalidateForm();
    }
    @Override public void onChunkUnloaded() {
        ledger.stop(); steamPressure.release(); activeForm = null; epoch++;
        if (level != null && !level.isClientSide) TurbineStructure.forget(level, this);
        super.onChunkUnloaded();
    }
    @Override protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        if (!clientPacket) {
            tag.put("Turbine", ledger.save());
            if (legacyKineticMigrationPending) tag.putBoolean("LegacyKineticMigrationPending", true);
            tag.putInt("TurbineLength", lastLength);
            tag.putInt("TurbineDiameter", lastDiameter);
            if (lastFront != null) tag.putLong("TurbineFront", lastFront.asLong());
            tag.putString("TurbineFacing", lastFacing.getName());
        }
        CompoundTag view = new CompoundTag();
        view.putString("Status", status); view.putInt("Length", lastLength);
        view.putInt("Input", viewInput); view.putInt("Exhaust", viewExhaust);
        view.putInt("InputCapacity", viewInputCapacity); view.putInt("ExhaustCapacity", viewExhaustCapacity);
        view.putInt("Processed", viewProcessed); view.putInt("RatedFlow", viewRatedFlow); view.putInt("Rpm", viewRpm);
        view.putFloat("TotalSu", viewTotalSu); view.putFloat("FrontSu", viewFrontSu); view.putFloat("RearSu", viewRearSu);
        tag.put("TurbineView", view);
    }
    @Override protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        if (!clientPacket) {
            ledger.load(tag.getCompound("Turbine"));
            legacyKineticMigrationPending = tag.getBoolean("LegacyKineticMigrationPending")
                    || tag.contains("Speed") || tag.contains("Network") || tag.contains("Source");
            lastLength = Math.clamp(tag.getInt("TurbineLength"), 0, 18);
            lastDiameter = tag.getInt("TurbineDiameter");
            lastFront = tag.contains("TurbineFront") ? BlockPos.of(tag.getLong("TurbineFront")) : null;
            Direction savedFacing = Direction.byName(tag.getString("TurbineFacing"));
            lastFacing = savedFacing != null && savedFacing.getAxis().isHorizontal() ? savedFacing
                    : getBlockState().getValue(TurbinePartBlock.MACHINE_FACING);
        }
        if (tag.contains("TurbineView")) {
            CompoundTag view = tag.getCompound("TurbineView");
            status = view.getString("Status"); lastLength = view.getInt("Length");
            viewInput = view.getInt("Input"); viewExhaust = view.getInt("Exhaust");
            viewInputCapacity = view.getInt("InputCapacity"); viewExhaustCapacity = view.getInt("ExhaustCapacity");
            viewProcessed = view.getInt("Processed"); viewRatedFlow = view.getInt("RatedFlow"); viewRpm = view.getInt("Rpm");
            viewTotalSu = view.getFloat("TotalSu"); viewFrontSu = view.getFloat("FrontSu"); viewRearSu = view.getFloat("RearSu");
        }
    }
    /** 世界内短诊断显示首个缺口坐标；客户端状态始终取服务端同步值。 */
    public Component diagnostic() {
        String key = "gui.create_nuclear_industry.turbine.";
        if (activeForm != null) return Component.translatable(key + "formed", activeForm.rotors());
        if (status.equals("invalid_config") || status.equals("stock_over_capacity") || status.equals("overlap"))
            return Component.translatable(key + "state." + status);
        TurbineStructure.Issue issue = TurbineStructure.issue(level, worldPosition, TurbineConfig.settings());
        return issue == null ? Component.translatable(key + "wait_stock")
                : Component.translatable(key + "inspect", Component.translatable(key + "issue." + issue.reason()),
                        issue.pos().getX(), issue.pos().getY(), issue.pos().getZ());
    }
    @Override public boolean addToGoggleTooltip(List<Component> tooltip, boolean sneaking) {
        String key = "gui.create_nuclear_industry.turbine.";
        tooltip.add(Component.translatable("block.create_nuclear_industry.turbine_controller"));
        tooltip.add(Component.translatable(key + "state." + status));
        tooltip.add(Component.translatable(key + "rotors_rpm", Math.max(0, lastLength - 2), viewRpm));
        tooltip.add(Component.translatable(key + "flow", viewProcessed, viewRatedFlow));
        tooltip.add(Component.translatable(key + "tanks", viewInput, viewInputCapacity, viewExhaust, viewExhaustCapacity));
        tooltip.add(Component.translatable(key + "su", (long)viewTotalSu, (long)viewFrontSu, (long)viewRearSu));
        tooltip.add(Component.translatable(key + "hint"));
        return true;
    }
}
