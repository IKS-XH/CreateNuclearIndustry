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
public final class TurbineControllerBlockEntity extends TurbineShaftPowerSource implements IHaveGoggleInformation {
    private final TurbineState ledger = new TurbineState();
    private final TurbineSteamPressure steamPressure;
    private TurbineStructure.Form activeForm;
    private int epoch, lastLength, viewInput, viewExhaust, viewInputCapacity, viewExhaustCapacity;
    private int viewProcessed, viewRatedFlow, viewRpm;
    private float viewTotalSu, viewFrontSu, viewRearSu;
    private String status = "unformed";

    public TurbineControllerBlockEntity(BlockPos pos, BlockState state) {
        super(TurbineContent.CONTROLLER_BE.get(), pos, state);
        steamPressure = new TurbineSteamPressure(pos);
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
        long now = level.getGameTime();
        if (!getBlockState().is(TurbineContent.CONTROLLER.get())
                || !TurbineStructure.ticking(level, worldPosition)) {
            if (activeForm != null) invalidateForm();
            ledger.tick(now, false);
            return;
        }
        TurbineState.Settings settings = TurbineConfig.settings();
        int maxRpm = AllConfigs.server().kinetics.maxRotationSpeed.get();
        Direction facing = getBlockState().getValue(TurbinePartBlock.MACHINE_FACING);
        TurbineStructure.Form found = TurbineStructure.inspect(level, worldPosition, settings);
        if (found == null || !settings.valid(maxRpm)) {
            if (activeForm != null) invalidateForm();
            if (lastLength > 0) styleSavedLength(facing, lastLength, false);
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
            if (lastLength > 0) styleSavedLength(facing, lastLength, false);
            ledger.tick(now, false);
            status = "overlap";
            refreshView(); sendData();
            return;
        }
        boolean changedForm = activeForm == null || activeForm.rotors() != found.rotors()
                || !activeForm.rear().equals(found.rear()) || !activeForm.inlets().equals(found.inlets())
                || !activeForm.exhausts().equals(found.exhausts());
        if (changedForm) {
            if (activeForm != null) invalidateForm();
            if (lastLength > 0 && lastLength != found.length()) styleSavedLength(facing, lastLength, false);
            if (lastLength != found.length() && !ledger.canFormForNewTier(found.rotors())) {
                ledger.tick(now, false);
                status = "stock_over_capacity";
                refreshView(); sendData();
                return;
            }
            activeForm = found;
            lastLength = found.length();
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

    /** 结构坐标与身份只在已加载格写状态；这些状态不参与结构验证本身。 */
    private void style(TurbineStructure.Form form, boolean formed) {
        if (!formed) { styleSavedLength(form.facing(), form.length(), false); return; }
        for (int z = 0; z < form.length(); z++) for (int y = 0; y <= 2; y++) for (int x = -1; x <= 1; x++) {
            BlockPos pos = TurbineStructure.at(worldPosition, form.facing(), x, y, z);
            if (!level.hasChunkAt(pos)) continue;
            BlockState before = level.getBlockState(pos);
            if (!isTurbinePart(before)) continue;
            BlockState after = before.setValue(TurbinePartBlock.FORMED, formed);
            if (formed) {
                after = after.setValue(TurbinePartBlock.MACHINE_FACING, form.facing());
                if (before.is(TurbineContent.CASING.get()))
                    after = after.setValue(TurbinePartBlock.RING_ROLE, TurbineStructure.ringRole(x, y))
                            .setValue(TurbinePartBlock.AXIAL_ROLE, z == 0 ? TurbinePartBlock.AxialRole.FRONT
                                    : z == form.length() - 1 ? TurbinePartBlock.AxialRole.REAR
                                    : TurbinePartBlock.AxialRole.MIDDLE);
                if (before.is(TurbineContent.INLET.get()) || before.is(TurbineContent.EXHAUST.get()))
                    after = after.setValue(TurbinePartBlock.RING_ROLE, TurbineStructure.ringRole(x, y))
                            .setValue(TurbinePartBlock.OUTWARD, TurbineStructure.outward(form.facing(), x, y));
            }
            if (after != before) level.setBlock(pos, after, 3);
        }
    }

    private void styleSavedLength(Direction facing, int length, boolean formed) {
        styleSavedLength(facing, length, formed, null);
    }

    private void styleSavedLength(Direction facing, int length, boolean formed, BlockPos skip) {
        if (length < 3 || length > 18) return;
        for (int z = 0; z < length; z++) for (int y = 0; y <= 2; y++) for (int x = -1; x <= 1; x++) {
            BlockPos pos = TurbineStructure.at(worldPosition, facing, x, y, z);
            if (pos.equals(skip)) continue;
            if (!level.hasChunkAt(pos)) continue;
            BlockState before = level.getBlockState(pos);
            boolean owned = isTurbinePart(before) && before.getValue(TurbinePartBlock.FORMED)
                    && before.getValue(TurbinePartBlock.MACHINE_FACING) == facing;
            if (owned && before.is(TurbineContent.CASING.get()))
                owned = before.getValue(TurbinePartBlock.RING_ROLE) == TurbineStructure.ringRole(x, y)
                        && before.getValue(TurbinePartBlock.AXIAL_ROLE) == (z == 0 ? TurbinePartBlock.AxialRole.FRONT
                        : z == length - 1 ? TurbinePartBlock.AxialRole.REAR : TurbinePartBlock.AxialRole.MIDDLE);
            if (owned && (before.is(TurbineContent.INLET.get()) || before.is(TurbineContent.EXHAUST.get())))
                owned = before.getValue(TurbinePartBlock.RING_ROLE) == TurbineStructure.ringRole(x, y)
                        && before.getValue(TurbinePartBlock.OUTWARD) == TurbineStructure.outward(facing, x, y);
            if (owned)
                level.setBlock(pos, before.setValue(TurbinePartBlock.FORMED, formed), 3);
        }
    }

    /** 控制器拆除只清理其他已加载构件的外观，不重写正在移除的控制器格。 */
    public void onControllerBroken(Direction oldFacing) {
        if (level != null && !level.isClientSide)
            styleSavedLength(oldFacing, lastLength, false, worldPosition);
        invalidateForm();
    }

    private static boolean isTurbinePart(BlockState state) {
        return state.is(TurbineContent.CASING.get()) || state.is(TurbineContent.ROTOR.get())
                || state.is(TurbineContent.CONTROLLER.get()) || state.is(TurbineContent.OUTPUT_SHAFT.get())
                || state.is(TurbineContent.INLET.get()) || state.is(TurbineContent.EXHAUST.get());
    }

    /** 已缓存完整结构时仍逐格检查区块票据；部分卸载立即撤销两个轴的当前值。 */
    private boolean live() {
        if (activeForm == null || level == null || level.isClientSide || isRemoved()
                || !getBlockState().is(TurbineContent.CONTROLLER.get())
                || !TurbineStructure.ticking(level, worldPosition)) return false;
        TurbineStructure.Form checked = TurbineStructure.inspect(level, worldPosition, ledger.settings());
        return checked != null && checked.rear().equals(activeForm.rear())
                && checked.inlets().equals(activeForm.inlets()) && checked.exhausts().equals(activeForm.exhausts());
    }
    public boolean validRear(BlockPos rear) {
        return live() && activeForm.rear().equals(rear)
                && level.getBlockState(rear).is(TurbineContent.OUTPUT_SHAFT.get());
    }
    public boolean validPort(BlockPos port, boolean input) {
        return live() && (input ? activeForm.inlets() : activeForm.exhausts()).contains(port)
                && level.getBlockState(port).is(input ? TurbineContent.INLET.get() : TurbineContent.EXHAUST.get());
    }
    public float rearSu() { return live() && !level.hasNeighborSignal(worldPosition) ? (float) ledger.rearSu() : 0; }
    public float signedRpm() { return live() ? ledger.rpm() : 0; }
    @Override protected float assignedSu() {
        return live() && !level.hasNeighborSignal(worldPosition) ? (float) ledger.frontSu() : 0;
    }
    @Override protected float assignedRpm() { return signedRpm(); }

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
        tag.putInt("LastLength", lastLength);
        return tag;
    }
    public void loadPortableData(CompoundTag tag) {
        ledger.load(tag.getCompound("Ledger"));
        lastLength = Math.clamp(tag.getInt("LastLength"), 0, 18);
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
        if (!clientPacket) { tag.put("Turbine", ledger.save()); tag.putInt("TurbineLength", lastLength); }
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
            lastLength = Math.clamp(tag.getInt("TurbineLength"), 0, 18);
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
