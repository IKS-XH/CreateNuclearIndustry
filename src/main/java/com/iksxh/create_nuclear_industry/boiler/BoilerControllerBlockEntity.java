package com.iksxh.create_nuclear_industry.boiler;

import com.iksxh.create_nuclear_industry.compat.create.SharedFluidReceiver;
import com.iksxh.create_nuclear_industry.config.BoilerConfig;
import com.iksxh.create_nuclear_industry.config.HeatExchangerConfig;
import com.iksxh.create_nuclear_industry.content.BoilerContent;
import com.iksxh.create_nuclear_industry.content.ModFluids;
import com.iksxh.create_nuclear_industry.content.TurbineContent;
import com.iksxh.create_nuclear_industry.goggle.GoggleTooltip;
import com.iksxh.create_nuclear_industry.heat.HeatExchangerMode;
import com.iksxh.create_nuclear_industry.heat.NuclearHeatExchangerBlockEntity;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.fluids.FluidPropagator;
import com.simibubi.create.content.fluids.FluidTransportBehaviour;
import com.simibubi.create.content.fluids.pipes.FluidPipeBlock;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/** 唯一持有整炉物料与已付热；端口仅带失效代次的代理，客户端只显示服务器快照。 */
public final class BoilerControllerBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {
    private final BoilerState ledger = new BoilerState();
    private final BoilerSteamPressure steamPressure;
    private BoilerControls.ModeBehaviour modeControl;
    private BoilerControls.PressureBehaviour pressureControl;
    private int epoch, producedView, ventedView, widthView, heightView, depthView, pairsView;
    private int waterCapacityView = BoilerState.CAPACITY, steamCapacityView = BoilerState.CAPACITY;
    private double paidView, waterTemperatureView, steamTemperatureView, pressureView, minimumView;
    private boolean available, formDirty = true, scanning;
    private BoilerStructure.Form cachedForm;
    private final Set<ChunkPos> missingChunks = new HashSet<>();
    private BoilerState.Settings lastGeometrySettings;
    private final Set<BlockPos> dirtyPorts = new HashSet<>();
    private final Map<Long, Object> flowIdentities = new HashMap<>();
    private final Object hotIdentity = new Object();
    private String status = "unformed";
    public BoilerControllerBlockEntity(BlockPos pos, BlockState state) {
        super(BoilerContent.CONTROLLER_BE.get(), pos, state); steamPressure = new BoilerSteamPressure(pos);
    }
    @Override public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        modeControl = new BoilerControls.ModeBehaviour(this); pressureControl = new BoilerControls.PressureBehaviour(this);
        behaviours.add(modeControl); behaviours.add(pressureControl);
    }
    public BoilerState ledger() { return ledger; }
    /** 只对本包围盒内的方块更新失效；外侧运输管道变化不触发体积重扫。 */
    public boolean affectedBy(BlockPos pos) { return cachedForm == null || cachedForm.contains(pos); }
    /** 仅已成型快照需要被后来发现的实交结构撤销，防止两份失败缓存互相逐tick置脏。 */
    void rejectOverlap() { if (cachedForm != null) invalidateForm(); }
    /** 邻炉改变可能解除先前重叠；只唤醒失败检查，不撤销不相交正常炉的能力或运输压力。 */
    void retryFailedForm() { if (cachedForm == null) formDirty = true; }
    /** 原生控件回调仅服务端提交；旧流体能力、运输压力与管面连接立即撤销。 */
    public void selectMode(boolean critical) {
        if (level == null || level.isClientSide || ledger.supercritical() == critical) return;
        ledger.setSupercritical(critical); epoch++; steamPressure.release();
        if (cachedForm != null) { dirtyPorts.addAll(cachedForm.ports()); invalidatePortCapabilities(cachedForm); }
        synchronizeControls(); setChanged(); sendData();
    }
    public void selectMinimum(int percent) {
        if (level == null || level.isClientSide) return;
        ledger.setMinimumPressure(percent / 100D); synchronizeControls(); setChanged(); sendData();
    }
    private void synchronizeControls() {
        modeControl.value = ledger.supercritical() ? 1 : 0;
        pressureControl.value = (int) Math.round(ledger.minimumPressure() * 100);
    }
    public boolean current() {
        if (!available || level == null || isRemoved() || !level.hasChunkAt(worldPosition) || level.getBlockEntity(worldPosition) != this) return false;
        if (!(level instanceof ServerLevel server)) return level.isClientSide;
        var chunk = server.getChunkSource().getChunkNow(worldPosition.getX() >> 4, worldPosition.getZ() >> 4);
        return chunk != null && server.shouldTickBlocksAt(worldPosition) && chunk.getFullStatus().isOrAfter(FullChunkStatus.BLOCK_TICKING)
                && server.areEntitiesLoaded(ChunkPos.asLong(worldPosition)) && server.getWorldBorder().isWithinBounds(worldPosition);
    }
    private void invalidatePortCapabilities(BoilerStructure.Form form) {
        for (BlockPos p : form.ports()) if (level.hasChunkAt(p)) level.invalidateCapabilities(p);
    }
    /** 放拆、模式和区块生命周期撤销全部旧句柄，库存始终留在控制器。 */
    public void invalidateForm() {
        BoilerStructure.Form old = cachedForm; cachedForm = null; formDirty = true; missingChunks.clear(); epoch++;
        if (steamPressure != null) steamPressure.release();
        if (old != null && level != null && !level.isClientSide) {
            dirtyPorts.addAll(old.ports()); invalidatePortCapabilities(old);
            for (BlockPos p : old.exchangers()) if (level.hasChunkAt(p)
                    && level.getBlockEntity(p) instanceof NuclearHeatExchangerBlockEntity m) m.setBoilerOwner(null);
            for (BlockPos p : old.windows()) if (level.hasChunkAt(p)
                    && level.getBlockEntity(p) instanceof BoilerWindowBlockEntity window) window.setFill(0);
        }
    }
    /** 无事件时仅检查区块可用性；全体积验证不放入正常 tick 循环。 */
    public BoilerStructure.Form currentForm() {
        if (!current() || scanning) return null;
        var cfg = BoilerConfig.settings();
        if (!cfg.equals(lastGeometrySettings)) { lastGeometrySettings = cfg; invalidateForm(); }
        if (cachedForm != null && !cachedForm.loaded(level)) invalidateForm();
        // 失败缓存只轮询当时缺失的区块；FULL暂退后恢复也有效，不依赖是否真正触发ChunkEvent.Load。
        if (!formDirty && missingChunks.removeIf(chunk -> level.hasChunk(chunk.x, chunk.z))) formDirty = true;
        if (!formDirty) return cachedForm;
        formDirty = false; scanning = true;
        try {
            var form = BoilerStructure.inspect(level, worldPosition);
            cachedForm = form != null && BoilerStructure.unique(level, worldPosition, form) ? form : null;
            if (cachedForm != null) { dirtyPorts.addAll(cachedForm.ports()); invalidatePortCapabilities(cachedForm); }
            missingChunks.clear();
            if (cachedForm == null) {
                // 单炉最大边长32：从控制器向任一轴至多31格，候选最多5×5区块；只检查可用性，不读块或强载。
                int radius = Math.max(0, Math.min(31, cfg.maxDimension() - 1));
                for (int x = (worldPosition.getX() - radius) >> 4; x <= (worldPosition.getX() + radius) >> 4; x++)
                    for (int z = (worldPosition.getZ() - radius) >> 4; z <= (worldPosition.getZ() + radius) >> 4; z++)
                        if (!level.hasChunk(x, z)) missingChunks.add(new ChunkPos(x, z));
            }
        } finally { scanning = false; }
        return cachedForm;
    }
    /** 成型接管先全组预检，再无外部回调地逐机转移，空成员重复认领不会增量。 */
    private boolean adoptMembers(BoilerStructure.Form form) {
        long hot = 0, cold = 0; double hu = 0;
        for (BlockPos p : form.exchangers()) {
            if (!(level.getBlockEntity(p) instanceof NuclearHeatExchangerBlockEntity m) || !m.current() || !m.canTick()
                    || m.ledger().activeMode() == HeatExchangerMode.CONDENSATION) return false;
            hot += m.ledger().hot(); cold += m.ledger().cold(); hu += m.ledger().reserve();
        }
        if (hot > Integer.MAX_VALUE || cold > Integer.MAX_VALUE || !ledger.canImport((int) hot, (int) cold, hu)) return false;
        for (BlockPos p : form.exchangers()) ((NuclearHeatExchangerBlockEntity) level.getBlockEntity(p)).joinBoiler(this);
        return true;
    }
    /** 本机 ticker 同时驱动 Create 原生行为；客户端只处理行为动画，不运行账本。 */
    @Override public void tick() {
        super.tick();
        if (level == null || level.isClientSide || !current()) return;
        ledger.setSettings(BoilerConfig.settings());
        var form = currentForm();
        var heat = HeatExchangerConfig.settings();
        boolean membersReady = form != null && heat.valid() && adoptMembers(form);
        if (form != null) ledger.setGeometry(form.waterCells(), form.steamCells(), form.exchangers().size(), form.sections().size(), heat.hotCapacityMb(), heat.coldCapacityMb());
        refreshPipes();
        long now = level.getGameTime();
        boolean clear = form != null && level.hasChunkAt(form.valve().above()) && level.getBlockState(form.valve().above()).isAir();
        boolean stopped = level.hasNeighborSignal(worldPosition);
        ledger.prepare(now, ledger.pairs());
        paidView = membersReady && !stopped ? ledger.collectHeat(now, Math.min(ledger.pairs() * ledger.settings().pairHeatHuPerTick(),
                form.exchangers().size() * heat.rate()), heat.density(), ledger.demand(clear)) : 0;
        // 拆件只允许显热自然冷却，不继续汽化；成型后才恢复同一账本。
        if (membersReady) ledger.tick(now, ledger.pairs(), paidView, clear, stopped);
        else ledger.idle(now);
        pushSteam(membersReady ? form : null, now);
        ledger.vent(now, membersReady && clear);
        steamPressure.refresh(level, membersReady ? form : null, ledger.outputQualified(), ledger.settings().portFlowMbPerTick());
        producedView = ledger.produced(); ventedView = ledger.vented();
        status = !ledger.settings().valid() || !heat.valid() ? "invalid" : form == null ? "unformed" : !membersReady ? "member_conflict"
                : stopped ? "redstone" : ledger.valveBlocked() ? "vent_blocked"
                : ledger.cold() >= ledger.coldCapacity() && ledger.demand(clear) > 0 ? "cold_full"
                : ledger.hot() == 0 && ledger.coolantHu() == 0 && ledger.demand(clear) > 0 ? "no_hot_coolant"
                : ledger.water() == 0 ? "no_water"
                : ledger.steam() >= ledger.steamCapacity() ? "steam_full" : paidView == 0 ? "no_heat" : ledger.ready() ? "running" : "warming";
        if (form != null) {
            widthView = form.width(); heightView = form.height(); depthView = form.depth(); pairsView = ledger.pairs();
            double height = form.partitionY() - form.min().getY() - 1;
            double surface = form.min().getY() + 1 + height * Math.min(1, (double) ledger.water() / Math.max(1, ledger.waterCapacity()));
            for (BlockPos p : form.windows()) if (level.getBlockEntity(p) instanceof BoilerWindowBlockEntity window)
                window.setFill(p.getY() < form.partitionY() ? (float) Math.clamp(surface - p.getY(), 0, 1) : 0);
        }
        if (ventedView > 0 && clear && level instanceof ServerLevel server) {
            BlockPos p = form.valve().above(); server.sendParticles(ParticleTypes.CLOUD, p.getX() + .5, p.getY() + .15, p.getZ() + .5, 3, .12, .08, .12, .01);
        }
        synchronizeControls(); setChanged();
        if (now % 5 == 0 || producedView > 0 || ventedView > 0) sendData();
    }
    public static void serverTick(Level level, BlockPos pos, BlockState state, BoilerControllerBlockEntity owner) { owner.tick(); }
    private void refreshPipes() {
        for (BlockPos p : Set.copyOf(dirtyPorts)) for (Direction side : Direction.Plane.HORIZONTAL) {
            BlockPos pipePos = p.relative(side);
            if (!level.hasChunkAt(pipePos)) continue;
            BlockState s = level.getBlockState(pipePos);
            if (s.getBlock() instanceof FluidPipeBlock pipe) {
                BlockState next = pipe.updateBlockState(s, side.getOpposite(), null, level, pipePos);
                if (next != s) level.setBlock(pipePos, next, 3); s = next;
            }
            if (BlockEntityBehaviour.get(level, pipePos, FluidTransportBehaviour.TYPE) != null) FluidPropagator.propagateChangedPipe(level, pipePos, s);
        }
        dirtyPorts.clear();
    }
    private Fluid outputFluid() { return ledger.supercritical() ? BoilerContent.SUPERCRITICAL_STEAM.get() : TurbineContent.STEAM.get(); }
    /** 邻罐先模拟接收，再提交真实接收量；Create 管网由原生压力抽取，同口共享额度。 */
    private void pushSteam(BoilerStructure.Form form, long now) {
        if (form == null) return;
        for (BlockPos p : form.steamPorts()) {
            int amount = ledger.remainingDrain(p, now); if (amount == 0) continue;
            Direction side = form.outward(p); BlockPos targetPos = p.relative(side);
            if (!level.hasChunkAt(targetPos) || FluidPropagator.getPipe(level, targetPos) != null) continue;
            IFluidHandler target = level.getCapability(Capabilities.FluidHandler.BLOCK, targetPos, side.getOpposite());
            if (target == null) continue;
            int simulated = Math.min(amount, target.fill(new FluidStack(outputFluid(), amount), IFluidHandler.FluidAction.SIMULATE));
            if (simulated > 0) {
                int actual = target.fill(new FluidStack(outputFluid(), simulated), IFluidHandler.FluidAction.EXECUTE);
                ledger.drainSteam(p, Math.min(simulated, actual), false, now);
            }
        }
    }
    /** 四类端口能力只有外向面可用；形状或模式代次变化后旧句柄永久失效。 */
    public IFluidHandler port(BlockPos p, Direction side, boolean ignoredInput) { return port(p, side); }
    public IFluidHandler port(BlockPos p, Direction side) {
        if (level == null || side == null || level.isClientSide) return null;
        var form = currentForm(); if (form == null || !form.ports().contains(p) || form.outward(p) != side) return null;
        ledger.setSettings(BoilerConfig.settings());
        var heat = HeatExchangerConfig.settings();
        ledger.setGeometry(form.waterCells(), form.steamCells(), form.exchangers().size(), form.sections().size(), heat.hotCapacityMb(), heat.coldCapacityMb());
        return new Port(p.immutable(), side, form.waterPorts().contains(p) ? 0 : form.steamPorts().contains(p) ? 1 : form.hotPorts().contains(p) ? 2 : 3);
    }
    private final class Port implements IFluidHandler, SharedFluidReceiver {
        private final BlockPos pos; private final Direction side; private final int kind, issued = epoch;
        private Port(BlockPos pos, Direction side, int kind) { this.pos = pos; this.side = side; this.kind = kind; }
        private boolean valid() {
            if (issued != epoch || !current() || level.isClientSide) return false;
            var form = currentForm();
            return issued == epoch && form != null && form.ports().contains(pos) && form.outward(pos) == side && ledger.settings().valid();
        }
        private Fluid fluid() { return kind == 0 ? Fluids.WATER : kind == 1 ? outputFluid()
                : kind == 2 ? ModFluids.HOT_COMPOUND_COOLANT_SOURCE.get() : ModFluids.COMPOUND_COOLANT_SOURCE.get(); }
        private int amount() { return kind == 0 ? ledger.water() : kind == 1 ? ledger.steam() : kind == 2 ? ledger.hot() : ledger.cold(); }
        @Override public Limits sharedFluidLimits() {
            if (!valid() || kind != 0 && kind != 2) return null;
            int room = kind == 0 ? Math.max(0, ledger.waterCapacity() - ledger.water()) : Math.max(0, ledger.hotCapacity() - ledger.hot());
            int flow = kind == 0 ? ledger.remainingFill(pos, level.getGameTime()) : ledger.remainingHotFill(pos, level.getGameTime());
            return new Limits(kind == 0 ? ledger : hotIdentity, room, flowIdentities.computeIfAbsent(pos.asLong(), ignored -> new Object()), flow);
        }
        @Override public int getTanks() { return valid() ? 1 : 0; }
        @Override public FluidStack getFluidInTank(int tank) { return valid() && tank == 0 && amount() > 0 ? new FluidStack(fluid(), amount()) : FluidStack.EMPTY; }
        @Override public int getTankCapacity(int tank) { return !valid() || tank != 0 ? 0 : kind == 0 ? ledger.waterCapacity()
                : kind == 1 ? ledger.steamCapacity() : kind == 2 ? ledger.hotCapacity() : ledger.coldCapacity(); }
        @Override public boolean isFluidValid(int tank, FluidStack stack) { return valid() && tank == 0 && (kind == 0 || kind == 2) && stack.is(fluid()); }
        @Override public int fill(FluidStack stack, FluidAction action) {
            if (!isFluidValid(0, stack)) return 0;
            int n = kind == 0 ? ledger.fillWater(pos, stack.getAmount(), action.simulate(), level.getGameTime())
                    : ledger.fillHot(pos, stack.getAmount(), action.simulate(), level.getGameTime());
            if (n > 0 && action.execute()) setChanged(); return n;
        }
        @Override public FluidStack drain(FluidStack stack, FluidAction action) { return valid() && stack.is(fluid()) ? drain(stack.getAmount(), action) : FluidStack.EMPTY; }
        @Override public FluidStack drain(int amount, FluidAction action) {
            if (!valid() || kind != 1 && kind != 3) return FluidStack.EMPTY;
            int n = kind == 1 ? ledger.drainSteam(pos, amount, action.simulate(), level.getGameTime()) : ledger.drainCold(pos, amount, action.simulate(), level.getGameTime());
            if (n > 0 && action.execute()) setChanged(); return n > 0 ? new FluidStack(fluid(), n) : FluidStack.EMPTY;
        }
    }
    public CompoundTag savePortableData() { CompoundTag tag = new CompoundTag(); tag.put("Ledger", ledger.save()); return tag; }
    public void loadPortableData(CompoundTag tag) { ledger.load(tag.getCompound("Ledger")); synchronizeControls(); setChanged(); if (level != null) sendData(); }
    @Override public void onLoad() { super.onLoad(); available = true; BoilerStructure.register(level, this); invalidateForm(); }
    @Override public void invalidate() { available = false; invalidateForm(); BoilerStructure.forget(level, this); super.invalidate(); }
    @Override public void onChunkUnloaded() { available = false; invalidateForm(); BoilerStructure.forget(level, this); super.onChunkUnloaded(); }
    @Override protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket); tag.put("Boiler", ledger.save());
        if (clientPacket) {
            tag.putString("Status", status); tag.putInt("Produced", producedView); tag.putInt("Vented", ventedView); tag.putDouble("Paid", paidView);
            tag.putInt("Width", widthView); tag.putInt("Height", heightView); tag.putInt("Depth", depthView); tag.putInt("Pairs", pairsView);
            tag.putInt("WaterCapacity", ledger.waterCapacity()); tag.putInt("SteamCapacity", ledger.steamCapacity());
            tag.putDouble("Tw", ledger.waterTemperature()); tag.putDouble("Ts", ledger.steamTemperature());
            tag.putDouble("Pressure", ledger.pressure()); tag.putDouble("Minimum", ledger.minimumPressure());
        }
    }
    @Override protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket); ledger.load(tag.getCompound("Boiler")); synchronizeControls();
        if (clientPacket) {
            status = tag.getString("Status"); producedView = tag.getInt("Produced"); ventedView = tag.getInt("Vented"); paidView = tag.getDouble("Paid");
            widthView = tag.getInt("Width"); heightView = tag.getInt("Height"); depthView = tag.getInt("Depth"); pairsView = tag.getInt("Pairs");
            waterCapacityView = tag.getInt("WaterCapacity"); steamCapacityView = tag.getInt("SteamCapacity");
            waterTemperatureView = tag.getDouble("Tw"); steamTemperatureView = tag.getDouble("Ts"); pressureView = tag.getDouble("Pressure"); minimumView = tag.getDouble("Minimum");
        }
    }
    @Override public boolean addToGoggleTooltip(List<Component> tooltip, boolean sneaking) {
        String key = "gui.create_nuclear_industry.boiler.";
        tooltip.add(GoggleTooltip.indentFirstLine(Component.translatable("block.create_nuclear_industry.high_pressure_boiler_controller")));
        tooltip.add(Component.translatable(key + "state." + status));
        tooltip.add(Component.translatable(key + "geometry", widthView, heightView, depthView, ledger.waterCells(), ledger.steamCells(), pairsView));
        tooltip.add(Component.translatable(key + "tanks", ledger.water(), waterCapacityView, ledger.steam(), steamCapacityView));
        tooltip.add(Component.translatable(key + "coolant", ledger.hot(), ledger.hotCapacity(), ledger.cold(), ledger.coldCapacity()));
        tooltip.add(Component.translatable(key + "temperature", format(waterTemperatureView), format(steamTemperatureView)));
        tooltip.add(Component.translatable(key + "real_pressure", format(pressureView), format(minimumView)));
        tooltip.add(Component.translatable(key + "flow", format(paidView), producedView));
        tooltip.add(Component.translatable(key + "vent", ventedView, ledger.totalVented())); return true;
    }
    private static String format(double value) { return String.format(java.util.Locale.ROOT, "%.2f", value); }
}
